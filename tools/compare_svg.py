#!/usr/bin/env python3
"""
compare_svg.py — 通用 SVG 结构化对比工具
用于对比 mermaid-js 和 mermaid-kmp 生成的 SVG 文件差异。

支持的对比维度:
  - viewBox / max-width
  - 节点位置 (transform)
  - 边路径坐标 (path d 属性)
  - 边 CSS 类
  - 箭头标记 (marker-end / marker-start)
  - CSS 样式规则
  - foreignObject 文本宽度
  - data-points (base64 编码的 dagre 坐标)
  - 边标签文本和位置

适用于所有 mermaid 图表类型（flowchart, sequence, class, state, er 等）。

用法:
  # 比较两个 SVG 文件
  python compare_svg.py <js_svg> <kmp_svg>

  # 批量比较目录下所有匹配的 SVG 对
  python compare_svg.py --dir <directory> [--pattern "mermaid_*"]

  # 指定容差阈值
  python compare_svg.py <js_svg> <kmp_svg> --node-threshold 1.0 --edge-threshold 2.0

  # 输出 JSON 格式
  python compare_svg.py <js_svg> <kmp_svg> --json

来源: 从归档中的 compare_all.py、compare_svgs.py、compare_edges.py 等脚本整合而来。
"""

import re
import os
import sys
import json
import glob
import base64
import argparse
from dataclasses import dataclass, field, asdict
from typing import List, Tuple, Optional, Dict, Any


# ============================================================
# SVG 特征提取函数
# ============================================================

def extract_viewbox(content: str) -> Optional[List[float]]:
    """提取 SVG viewBox 值 [minX, minY, width, height]"""
    m = re.search(r'viewBox="([^"]+)"', content)
    if m:
        return [float(x) for x in m.group(1).split()]
    return None


def extract_max_width(content: str) -> Optional[float]:
    """提取 CSS max-width 值"""
    m = re.search(r'max-width:\s*([\d.]+)', content)
    return float(m.group(1)) if m else None


def extract_nodes(content: str) -> List[Tuple[str, float, float]]:
    """提取节点 ID 和 transform 位置坐标。
    
    返回 [(id, x, y), ...] 列表。
    支持 id 在 transform 前后两种 SVG 属性顺序。
    """
    nodes = []
    # 模式1: id 在 transform 之前 (KMP 格式)
    for m in re.finditer(
        r'<g[^>]*id="([^"]+)"[^>]*transform="translate\(([^)]+)\)"[^>]*class="[^"]*node[^"]*"',
        content
    ):
        nid = m.group(1)
        coords = m.group(2).replace(',', ' ').split()
        x, y = float(coords[0]), float(coords[1]) if len(coords) > 1 else 0
        nodes.append((nid, x, y))
    
    # 模式2: class + transform 在前，id 在后 (JS 格式)
    for m in re.finditer(
        r'<g[^>]*class="[^"]*node[^"]*"[^>]*transform="translate\(([^)]+)\)"[^>]*id="([^"]+)"',
        content
    ):
        nid = m.group(2)
        coords = m.group(1).replace(',', ' ').split()
        x, y = float(coords[0]), float(coords[1]) if len(coords) > 1 else 0
        if not any(n[0] == nid for n in nodes):
            nodes.append((nid, x, y))
    
    # 模式3: 简化匹配（不依赖 id 和 class 的顺序）
    if not nodes:
        for m in re.finditer(
            r'<g[^>]*class="[^"]*node[^"]*"[^>]*transform="translate\(([^)]+)\)"',
            content
        ):
            coords = m.group(1).replace(',', ' ').split()
            x, y = float(coords[0]), float(coords[1]) if len(coords) > 1 else 0
            nodes.append((f"node_{len(nodes)}", x, y))
    
    return nodes


def extract_edge_paths(content: str) -> List[Tuple[str, str]]:
    """提取边路径的 d 属性。
    
    返回 [(edge_id, path_d), ...] 列表。
    使用三级备选策略:
    1. flowchart-link 类的 path
    2. data-edge="true" 的 path
    3. 启发式匹配含贝塞尔曲线的长路径
    """
    paths = []
    
    # 策略1: flowchart-link 类路径（含 id）
    for m in re.finditer(r'<path\s[^>]*?class="[^"]*flowchart-link[^"]*"[^>]*/>', content):
        elem = m.group(0)
        d_match = re.search(r'\bd="(M[^"]+)"', elem)
        id_match = re.search(r'\bid="([^"]+)"', elem)
        if d_match:
            eid = id_match.group(1) if id_match else f"edge_{len(paths)}"
            paths.append((eid, d_match.group(1)))
    
    if paths:
        return paths
    
    # 策略2: data-edge 属性
    for m in re.finditer(r'<path\s[^>]*?data-edge="true"[^>]*/>', content):
        elem = m.group(0)
        d_match = re.search(r'\bd="(M[^"]+)"', elem)
        id_match = re.search(r'\bid="([^"]+)"', elem)
        if d_match:
            eid = id_match.group(1) if id_match else f"edge_{len(paths)}"
            paths.append((eid, d_match.group(1)))
    
    if paths:
        return paths
    
    # 策略3: 带 L_ 前缀 ID 的路径
    for m in re.finditer(r'<path\s[^>]*?id="(L_[^"]+)"[^>]*/>', content):
        elem = m.group(0)
        d_match = re.search(r'\bd="(M[^"]+)"', elem)
        if d_match:
            paths.append((m.group(1), d_match.group(1)))
    
    if paths:
        return paths
    
    # 策略4: 启发式 — 含贝塞尔曲线的长路径
    all_paths = re.findall(r'd="(M[^"]+)"', content)
    return [(f"edge_{i}", p) for i, p in enumerate(all_paths) if 'C' in p and len(p) > 50]


def extract_nums_from_path(path_d: str) -> List[float]:
    """从 SVG path d 属性提取所有数值坐标"""
    return [float(x) for x in re.findall(r'[-+]?\d*\.?\d+', path_d)]


def extract_edge_classes(content: str) -> List[str]:
    """提取边的 CSS class 列表"""
    return re.findall(r'class="([^"]*(?:edge|flowchart-link)[^"]*)"', content)


def extract_markers(content: str) -> Tuple[List[str], List[str]]:
    """提取 marker-end 和 marker-start 列表"""
    ends = re.findall(r'marker-end="url\(#([^)]+)\)"', content)
    starts = re.findall(r'marker-start="url\(#([^)]+)\)"', content)
    return ends, starts


def extract_css_rules(content: str) -> List[str]:
    """提取 <style> 块中的 CSS 规则"""
    m = re.search(r'<style>(.*?)</style>', content, re.DOTALL)
    if m:
        return [r.strip() for r in m.group(1).split('}') if r.strip()]
    return []


def extract_foreign_object_widths(content: str) -> List[float]:
    """提取所有 foreignObject 的 width 值"""
    return [float(w) for w in re.findall(r'foreignObject\s+width="([^"]+)"', content)]


def extract_data_points(content: str) -> Dict[str, list]:
    """解码 base64 编码的 data-points 属性。
    
    返回 {edge_id: [{x, y}, ...], ...}
    """
    result = {}
    for m in re.finditer(r'data-id="([^"]+)"[^>]*?data-points="([^"]+)"', content):
        edge_id = m.group(1)
        b64 = m.group(2)
        try:
            decoded = base64.b64decode(b64).decode('utf-8')
            points = json.loads(decoded)
            result[edge_id] = points
        except Exception:
            result[edge_id] = None
    return result


def extract_edge_labels(content: str) -> List[Tuple[str, float, float]]:
    """提取边标签的文本和位置。
    
    返回 [(text, x, y), ...] 列表。
    """
    labels = []
    for m in re.finditer(
        r'class="edgeLabel"[^>]*transform="translate\(([^)]+)\)".*?<p[^>]*>(.*?)</p>',
        content, re.DOTALL
    ):
        coords = m.group(1).replace(',', ' ').split()
        x, y = float(coords[0]), float(coords[1]) if len(coords) > 1 else 0
        text = re.sub(r'<[^>]+>', '', m.group(2)).strip()
        labels.append((text, x, y))
    return labels


# ============================================================
# 对比逻辑
# ============================================================

@dataclass
class DimensionResult:
    """单个对比维度的结果"""
    name: str
    status: str  # "pass", "warn", "fail", "skip"
    max_diff: float = 0.0
    details: List[str] = field(default_factory=list)
    score: float = 1.0


@dataclass
class CompareResult:
    """SVG 对比结果"""
    js_file: str
    kmp_file: str
    dimensions: List[DimensionResult] = field(default_factory=list)
    overall_score: float = 1.0
    
    def compute_overall(self, weights: Optional[Dict[str, float]] = None):
        """计算加权总分"""
        if weights is None:
            weights = {
                'viewBox': 0.05, 'max_width': 0.05, 'nodes': 0.30,
                'edges': 0.30, 'css_class': 0.10, 'markers': 0.05,
                'structure': 0.10, 'edge_labels': 0.05,
            }
        total_weight = 0
        weighted_sum = 0
        for dim in self.dimensions:
            w = weights.get(dim.name, 0.0)
            weighted_sum += dim.score * w
            total_weight += w
        self.overall_score = weighted_sum / total_weight if total_weight > 0 else 0.0


def linear_score(diff: float, perfect_threshold: float, zero_threshold: float) -> float:
    """线性评分函数: diff <= perfect → 1.0, diff >= zero → 0.0, 中间线性插值"""
    if diff <= perfect_threshold:
        return 1.0
    if diff >= zero_threshold:
        return 0.0
    return 1.0 - (diff - perfect_threshold) / (zero_threshold - perfect_threshold)


def compare_svgs(
    js_content: str,
    kmp_content: str,
    js_file: str = "js.svg",
    kmp_file: str = "kmp.svg",
    node_threshold: float = 1.0,
    edge_threshold: float = 2.0,
    normalize_ids: bool = True,
) -> CompareResult:
    """
    全面对比两个 SVG 内容。
    
    参数:
      js_content: mermaid-js 生成的 SVG 内容
      kmp_content: mermaid-kmp 生成的 SVG 内容
      node_threshold: 节点位置差异报错阈值 (px)
      edge_threshold: 边路径差异报错阈值 (px)
      normalize_ids: 是否归一化 diagram ID（消除随机 ID 差异）
    """
    result = CompareResult(js_file=js_file, kmp_file=kmp_file)
    
    # 可选: 归一化 diagram ID
    if normalize_ids:
        # mermaid-js 常用 id 如 "my-svg" 或随机 hash
        # mermaid-kmp 常用 "mermaid-1" 等
        js_content = re.sub(r'id="[^"]*-svg[^"]*"', 'id="DIAG_ID"', js_content)
        kmp_content = re.sub(r'id="mermaid-\d+"', 'id="DIAG_ID"', kmp_content)
    
    # ----- 1. viewBox -----
    js_vb = extract_viewbox(js_content)
    kmp_vb = extract_viewbox(kmp_content)
    dim_vb = DimensionResult(name='viewBox', status='skip')
    if js_vb and kmp_vb:
        vb_diffs = [abs(a - b) for a, b in zip(js_vb, kmp_vb)]
        dim_vb.max_diff = max(vb_diffs)
        rel_diff = dim_vb.max_diff / max(max(abs(v) for v in js_vb), 1)
        dim_vb.score = linear_score(rel_diff, 0.001, 0.5)
        if dim_vb.max_diff > node_threshold:
            dim_vb.status = 'fail'
            dim_vb.details = [f"JS: {js_vb}", f"KMP: {kmp_vb}"]
        else:
            dim_vb.status = 'pass'
    elif js_vb and not kmp_vb:
        dim_vb.status = 'fail'
        dim_vb.score = 0.0
        dim_vb.details = ["KMP 缺少 viewBox"]
    result.dimensions.append(dim_vb)
    
    # ----- 2. max-width -----
    js_mw = extract_max_width(js_content)
    kmp_mw = extract_max_width(kmp_content)
    dim_mw = DimensionResult(name='max_width', status='skip')
    if js_mw is not None and kmp_mw is not None:
        dim_mw.max_diff = abs(js_mw - kmp_mw)
        rel_diff = dim_mw.max_diff / max(js_mw, 1)
        dim_mw.score = linear_score(rel_diff, 0.001, 0.5)
        dim_mw.status = 'pass' if dim_mw.max_diff <= node_threshold else 'fail'
        if dim_mw.status == 'fail':
            dim_mw.details = [f"JS={js_mw}, KMP={kmp_mw}"]
    result.dimensions.append(dim_mw)
    
    # ----- 3. 节点位置 -----
    js_nodes = extract_nodes(js_content)
    kmp_nodes = extract_nodes(kmp_content)
    dim_nodes = DimensionResult(name='nodes', status='skip')
    
    # 结构维度
    dim_struct = DimensionResult(name='structure', status='pass', score=1.0)
    if len(js_nodes) != len(kmp_nodes):
        dim_struct.status = 'warn'
        ratio = min(len(js_nodes), len(kmp_nodes)) / max(len(js_nodes), len(kmp_nodes), 1)
        dim_struct.score = ratio
        dim_struct.details.append(f"节点数: JS={len(js_nodes)}, KMP={len(kmp_nodes)}")
    
    if js_nodes and kmp_nodes:
        max_node_diff = 0
        node_details = []
        for (jid, jx, jy), (kid, kx, ky) in zip(js_nodes, kmp_nodes):
            d = max(abs(jx - kx), abs(jy - ky))
            max_node_diff = max(max_node_diff, d)
            if d > 0.5:
                node_details.append(f"{jid}: JS=({jx:.3f},{jy:.3f}) KMP=({kx:.3f},{ky:.3f}) diff={d:.3f}px")
        dim_nodes.max_diff = max_node_diff
        dim_nodes.score = linear_score(max_node_diff, 0.05, 30.0)
        if max_node_diff > node_threshold:
            dim_nodes.status = 'fail'
            dim_nodes.details = node_details
        elif max_node_diff > 0.5:
            dim_nodes.status = 'warn'
            dim_nodes.details = node_details
        else:
            dim_nodes.status = 'pass'
    result.dimensions.append(dim_nodes)
    
    # ----- 4. 边路径 -----
    js_paths = extract_edge_paths(js_content)
    kmp_paths = extract_edge_paths(kmp_content)
    dim_edges = DimensionResult(name='edges', status='skip')
    
    if len(js_paths) != len(kmp_paths):
        dim_struct.details.append(f"边数: JS={len(js_paths)}, KMP={len(kmp_paths)}")
        if dim_struct.status == 'pass':
            dim_struct.status = 'warn'
        edge_ratio = min(len(js_paths), len(kmp_paths)) / max(len(js_paths), len(kmp_paths), 1)
        dim_struct.score = min(dim_struct.score, edge_ratio)
    
    if js_paths and kmp_paths:
        max_path_diff = 0
        edge_details = []
        for (jid, jp), (kid, kp) in zip(js_paths, kmp_paths):
            jn = extract_nums_from_path(jp)
            kn = extract_nums_from_path(kp)
            edge_max = 0
            for jv, kv in zip(jn, kn):
                edge_max = max(edge_max, abs(jv - kv))
            max_path_diff = max(max_path_diff, edge_max)
            if edge_max > 1.0:
                edge_details.append(f"{jid}: max_diff={edge_max:.3f}px")
        dim_edges.max_diff = max_path_diff
        dim_edges.score = linear_score(max_path_diff, 0.5, 50.0)
        if max_path_diff > edge_threshold:
            dim_edges.status = 'fail'
            dim_edges.details = edge_details
        elif max_path_diff > 0.5:
            dim_edges.status = 'warn'
            dim_edges.details = edge_details
        else:
            dim_edges.status = 'pass'
    result.dimensions.append(dim_edges)
    
    result.dimensions.append(dim_struct)
    
    # ----- 5. 边 CSS 类 -----
    js_classes = extract_edge_classes(js_content)
    kmp_classes = extract_edge_classes(kmp_content)
    dim_css = DimensionResult(name='css_class', status='pass', score=1.0)
    mismatches = 0
    for jc, kc in zip(js_classes, kmp_classes):
        if jc != kc:
            mismatches += 1
            dim_css.details.append(f"JS: {jc} ≠ KMP: {kc}")
    if mismatches > 0:
        dim_css.status = 'fail'
        total = max(len(js_classes), len(kmp_classes), 1)
        dim_css.score = 1.0 - mismatches / total
    result.dimensions.append(dim_css)
    
    # ----- 6. 箭头标记 -----
    js_ends, js_starts = extract_markers(js_content)
    kmp_ends, kmp_starts = extract_markers(kmp_content)
    dim_markers = DimensionResult(name='markers', status='pass', score=1.0)
    marker_mismatches = 0
    for je, ke in zip(js_ends, kmp_ends):
        je_base = je.split('-')[-1] if '-' in je else je
        ke_base = ke.split('-')[-1] if '-' in ke else ke
        if je_base != ke_base:
            marker_mismatches += 1
            dim_markers.details.append(f"marker-end: JS={je} ≠ KMP={ke}")
    if marker_mismatches > 0:
        dim_markers.status = 'fail'
        total = max(len(js_ends), len(kmp_ends), 1)
        dim_markers.score = 1.0 - marker_mismatches / total
    result.dimensions.append(dim_markers)
    
    # ----- 7. 边标签 -----
    js_labels = extract_edge_labels(js_content)
    kmp_labels = extract_edge_labels(kmp_content)
    dim_labels = DimensionResult(name='edge_labels', status='pass', score=1.0)
    if js_labels and kmp_labels:
        max_label_diff = 0
        for (jt, jx, jy), (kt, kx, ky) in zip(js_labels, kmp_labels):
            if jt != kt:
                dim_labels.details.append(f"标签文本: JS='{jt}' ≠ KMP='{kt}'")
            d = max(abs(jx - kx), abs(jy - ky))
            max_label_diff = max(max_label_diff, d)
        dim_labels.max_diff = max_label_diff
        dim_labels.score = linear_score(max_label_diff, 0.5, 30.0)
        if max_label_diff > edge_threshold:
            dim_labels.status = 'fail'
        elif max_label_diff > 0.5:
            dim_labels.status = 'warn'
    result.dimensions.append(dim_labels)
    
    # 计算总分
    result.compute_overall()
    return result


# ============================================================
# 报告输出
# ============================================================

STATUS_EMOJI = {'pass': '✅', 'warn': '⚠️', 'fail': '❌', 'skip': '⏭️'}

def print_result(result: CompareResult, verbose: bool = False):
    """终端输出对比结果"""
    score = result.overall_score
    if score >= 0.99:
        emoji = '🏆'
    elif score >= 0.95:
        emoji = '✅'
    elif score >= 0.80:
        emoji = '⚠️'
    else:
        emoji = '❌'
    
    basename = os.path.basename(result.js_file).replace('_js.svg', '')
    print(f"{emoji} {basename}: {score:.4f}")
    
    for dim in result.dimensions:
        status = STATUS_EMOJI.get(dim.status, '?')
        diff_str = f" (max_diff={dim.max_diff:.3f}px)" if dim.max_diff > 0 else ""
        print(f"  {status} {dim.name}: score={dim.score:.4f}{diff_str}")
        if verbose and dim.details:
            for d in dim.details[:5]:
                print(f"     {d}")
            if len(dim.details) > 5:
                print(f"     ... 还有 {len(dim.details) - 5} 项")
    print()


def to_json(result: CompareResult) -> dict:
    """将结果转换为 JSON 可序列化的字典"""
    return {
        'js_file': result.js_file,
        'kmp_file': result.kmp_file,
        'overall_score': round(result.overall_score, 6),
        'dimensions': [
            {
                'name': d.name,
                'status': d.status,
                'score': round(d.score, 6),
                'max_diff': round(d.max_diff, 6),
                'details': d.details[:10],
            }
            for d in result.dimensions
        ],
    }


# ============================================================
# 批量对比
# ============================================================

def find_svg_pairs(directory: str, pattern: str = "mermaid_*") -> List[Tuple[str, str]]:
    """在目录中查找 *_js.svg / *_kmp.svg 对"""
    js_files = sorted(glob.glob(os.path.join(directory, f'{pattern}_js.svg')))
    pairs = []
    for js_file in js_files:
        kmp_file = js_file.replace('_js.svg', '_kmp.svg')
        if os.path.exists(kmp_file):
            pairs.append((js_file, kmp_file))
    return pairs


def batch_compare(
    directory: str,
    pattern: str = "mermaid_*",
    verbose: bool = False,
    node_threshold: float = 1.0,
    edge_threshold: float = 2.0,
) -> List[CompareResult]:
    """批量对比目录下所有 SVG 对"""
    pairs = find_svg_pairs(directory, pattern)
    if not pairs:
        print(f"未找到匹配的 SVG 对 (pattern={pattern})")
        return []
    
    results = []
    for js_file, kmp_file in pairs:
        with open(js_file) as f:
            js_content = f.read()
        with open(kmp_file) as f:
            kmp_content = f.read()
        
        r = compare_svgs(
            js_content, kmp_content,
            js_file, kmp_file,
            node_threshold, edge_threshold,
        )
        results.append(r)
        print_result(r, verbose)
    
    # 汇总
    if results:
        avg = sum(r.overall_score for r in results) / len(results)
        passed = sum(1 for r in results if r.overall_score >= 0.95)
        failed = len(results) - passed
        print("=" * 50)
        print(f"总计: {len(results)} 个用例, 通过: {passed}, 失败: {failed}")
        print(f"平均分: {avg:.4f}")
        if avg >= 0.95:
            print("🎉 整体通过！")
        else:
            print("💥 存在需要修复的差异！")
    
    return results


# ============================================================
# CLI 入口
# ============================================================

def main():
    parser = argparse.ArgumentParser(
        description='通用 SVG 结构化对比工具 — 对比 mermaid-js 与 mermaid-kmp 的 SVG 输出',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  python compare_svg.py output_js.svg output_kmp.svg
  python compare_svg.py --dir tests/ --pattern "mermaid_flowchart_*"
  python compare_svg.py output_js.svg output_kmp.svg --json --verbose
        """,
    )
    parser.add_argument('files', nargs='*', help='JS 和 KMP SVG 文件 (两个)')
    parser.add_argument('--dir', '-d', help='批量对比目录')
    parser.add_argument('--pattern', '-p', default='mermaid_*', help='文件名匹配模式 (默认: mermaid_*)')
    parser.add_argument('--node-threshold', type=float, default=1.0, help='节点位置差异阈值 (px, 默认: 1.0)')
    parser.add_argument('--edge-threshold', type=float, default=2.0, help='边路径差异阈值 (px, 默认: 2.0)')
    parser.add_argument('--verbose', '-v', action='store_true', help='显示详细差异信息')
    parser.add_argument('--json', '-j', action='store_true', help='以 JSON 格式输出')
    
    args = parser.parse_args()
    
    if args.dir:
        results = batch_compare(args.dir, args.pattern, args.verbose, args.node_threshold, args.edge_threshold)
        if args.json:
            print(json.dumps([to_json(r) for r in results], indent=2, ensure_ascii=False))
    elif len(args.files) == 2:
        js_file, kmp_file = args.files
        with open(js_file) as f:
            js_content = f.read()
        with open(kmp_file) as f:
            kmp_content = f.read()
        r = compare_svgs(
            js_content, kmp_content,
            js_file, kmp_file,
            args.node_threshold, args.edge_threshold,
        )
        if args.json:
            print(json.dumps(to_json(r), indent=2, ensure_ascii=False))
        else:
            print_result(r, args.verbose)
    else:
        parser.print_help()
        sys.exit(1)


if __name__ == '__main__':
    main()
