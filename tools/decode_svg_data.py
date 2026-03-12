#!/usr/bin/env python3
"""
decode_svg_data.py — SVG 嵌入数据解码工具

从 mermaid 生成的 SVG 文件中解码 base64 编码的 data-points 属性，
提取 dagre 布局的原始 edge points 坐标。

用法:
  # 解码单个 SVG 文件的所有 data-points
  python decode_svg_data.py <svg_file>

  # 对比两个 SVG 文件的 data-points
  python decode_svg_data.py <js_svg> <kmp_svg>

  # 输出为 JSON
  python decode_svg_data.py <svg_file> --json

来源: 从归档中的 decode_points.py、debug_dagre.js 整合而来。
"""

import re
import sys
import json
import base64
import argparse
from typing import Dict, List, Optional


def decode_data_points(svg_content: str) -> Dict[str, Optional[List[dict]]]:
    """从 SVG 内容中解码所有 data-points 属性。
    
    返回 {edge_id: [{x, y}, ...], ...}
    """
    result = {}
    
    # 模式1: data-id 在 data-points 之前
    for m in re.finditer(r'data-id="([^"]+)"[^>]*?data-points="([^"]+)"', svg_content):
        edge_id = m.group(1)
        b64 = m.group(2)
        try:
            decoded = base64.b64decode(b64).decode('utf-8')
            points = json.loads(decoded)
            result[edge_id] = points
        except Exception as e:
            result[edge_id] = None
    
    # 模式2: data-points 在 data-id 之前
    if not result:
        for m in re.finditer(r'data-points="([^"]+)"[^>]*?data-id="([^"]+)"', svg_content):
            edge_id = m.group(2)
            b64 = m.group(1)
            try:
                decoded = base64.b64decode(b64).decode('utf-8')
                points = json.loads(decoded)
                result[edge_id] = points
            except Exception:
                result[edge_id] = None
    
    # 模式3: 只有 id（没有 data-id），但有 data-points
    if not result:
        for m in re.finditer(r'id="(L_[^"]+)"[^>]*?data-points="([^"]+)"', svg_content):
            edge_id = m.group(1)
            b64 = m.group(2)
            try:
                decoded = base64.b64decode(b64).decode('utf-8')
                points = json.loads(decoded)
                result[edge_id] = points
            except Exception:
                result[edge_id] = None
    
    return result


def print_points(label: str, data: Dict[str, Optional[List[dict]]]):
    """友好格式输出 data-points"""
    print(f"\n=== {label} data-points ===")
    for edge_id, points in data.items():
        print(f"\n{edge_id}:")
        if points is None:
            print("  ❌ 解码失败")
            continue
        for i, p in enumerate(points):
            print(f"  [{i}] x={p['x']:.6f}, y={p['y']:.6f}")


def compare_points(
    js_data: Dict[str, Optional[List[dict]]],
    kmp_data: Dict[str, Optional[List[dict]]],
):
    """对比两组 data-points"""
    print("\n=== Data-Points 对比 ===")
    
    all_ids = set(list(js_data.keys()) + list(kmp_data.keys()))
    max_diff = 0
    
    for edge_id in sorted(all_ids):
        js_pts = js_data.get(edge_id)
        kmp_pts = kmp_data.get(edge_id)
        
        if js_pts is None and kmp_pts is None:
            continue
        elif js_pts and not kmp_pts:
            print(f"\n❌ {edge_id}: 仅在 JS 中存在")
            continue
        elif kmp_pts and not js_pts:
            print(f"\n❌ {edge_id}: 仅在 KMP 中存在")
            continue
        
        print(f"\n{edge_id}:")
        if len(js_pts) != len(kmp_pts):
            print(f"  ⚠️ 控制点数量不同: JS={len(js_pts)}, KMP={len(kmp_pts)}")
        
        edge_max = 0
        for i, (jp, kp) in enumerate(zip(js_pts, kmp_pts)):
            dx = abs(jp['x'] - kp['x'])
            dy = abs(jp['y'] - kp['y'])
            d = max(dx, dy)
            edge_max = max(edge_max, d)
            if d > 0.001:
                print(f"  [{i}] JS=({jp['x']:.3f}, {jp['y']:.3f}) KMP=({kp['x']:.3f}, {kp['y']:.3f}) diff=({dx:.3f}, {dy:.3f})")
            else:
                print(f"  [{i}] MATCH ({jp['x']:.3f}, {jp['y']:.3f})")
        
        max_diff = max(max_diff, edge_max)
        ok = '✅' if edge_max < 0.5 else ('⚠️' if edge_max < 2.0 else '❌')
        print(f"  {ok} 最大差异: {edge_max:.3f}px")
    
    print(f"\n总最大差异: {max_diff:.3f}px")


def main():
    parser = argparse.ArgumentParser(
        description='SVG data-points 解码工具',
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument('files', nargs='+', help='SVG 文件 (1个解码, 2个对比)')
    parser.add_argument('--json', '-j', action='store_true', help='JSON 输出')
    
    args = parser.parse_args()
    
    if len(args.files) == 1:
        with open(args.files[0]) as f:
            content = f.read()
        data = decode_data_points(content)
        if args.json:
            print(json.dumps(data, indent=2, ensure_ascii=False))
        else:
            print_points(args.files[0], data)
    
    elif len(args.files) == 2:
        with open(args.files[0]) as f:
            js_content = f.read()
        with open(args.files[1]) as f:
            kmp_content = f.read()
        
        js_data = decode_data_points(js_content)
        kmp_data = decode_data_points(kmp_content)
        
        if args.json:
            print(json.dumps({'js': js_data, 'kmp': kmp_data}, indent=2, ensure_ascii=False))
        else:
            print_points("JS", js_data)
            print_points("KMP", kmp_data)
            compare_points(js_data, kmp_data)
    else:
        parser.error('最多支持 2 个文件')


if __name__ == '__main__':
    main()
