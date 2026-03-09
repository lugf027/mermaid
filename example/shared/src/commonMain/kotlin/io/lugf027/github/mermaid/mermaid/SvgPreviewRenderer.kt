package io.lugf027.github.mermaid.mermaid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder

/**
 * SVG 图片预览组件 - 使用 Coil 3 (coil-svg) 第三方库渲染 SVG 文本
 *
 * 将 SVG XML 字符串的字节数据交给 Coil 3 的 SvgDecoder 进行解析和光栅化渲染，
 * 利用成熟的第三方 SVG 渲染引擎（Android 上为 AndroidSVG，其他平台为 Skia SVGDOM），
 * 确保兼容性与渲染质量。
 */
@Composable
fun SvgPreview(
    svgText: String,
    modifier: Modifier = Modifier
) {
    val platformContext = LocalPlatformContext.current

    // 创建带 SvgDecoder 的 ImageLoader（使用 remember 缓存避免重复创建）
    val imageLoader = remember(platformContext) {
        createSvgImageLoader(platformContext)
    }

    // 将 SVG 字符串转为字节数据，作为 Coil 的加载数据源
    val svgBytes = remember(svgText) {
        svgText.encodeToByteArray()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp, max = 600.dp)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(platformContext)
                .data(svgBytes)
                .build(),
            imageLoader = imageLoader,
            contentDescription = "SVG Preview",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            onState = { /* 可用于调试渲染状态 */ }
        )
    }
}

/**
 * 创建配置了 SvgDecoder 的 ImageLoader
 *
 * Coil 3 的 SvgDecoder 在不同平台使用不同的底层渲染引擎：
 * - Android: AndroidSVG 库
 * - JVM Desktop / iOS / JS / WasmJS: Skia (通过 Skiko)
 */
private fun createSvgImageLoader(context: PlatformContext): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            add(SvgDecoder.Factory())
        }
        .build()
}
