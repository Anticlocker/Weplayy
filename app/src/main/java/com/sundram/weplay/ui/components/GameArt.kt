package com.sundram.weplay.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/** Vector artwork for game tiles, drawn on Canvas. */
@Composable
fun GameArtwork(kind: String, modifier: Modifier = Modifier, corner: Int = 20) {
    Box(
        modifier
            .clip(RoundedCornerShape(corner.dp))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            when (kind) {
                "ludo" -> drawLudo(w, h)
                "spy" -> drawSpy(w, h)
                "draw" -> drawPalette(w, h)
                "bingo" -> drawBingo(w, h)
                "mic" -> drawMic(w, h)
                "hum" -> drawMusic(w, h)
                "squid" -> drawSquid(w, h)
                "sheep" -> drawSheep(w, h)
                else -> drawFallback(w, h)
            }
        }
    }
}

private fun DrawScope.bg(w: Float, h: Float, top: Color, bottom: Color) {
    drawRect(Brush.verticalGradient(listOf(top, bottom)))
}

private fun DrawScope.drawLudo(w: Float, h: Float) {
    bg(w, h, Color(0xFF332A5C), Color(0xFF15112E))
    // sparkles
    drawCircle(Color(0x66F7E25A), radius = w * 0.02f, center = Offset(w * 0.82f, h * 0.2f))
    drawCircle(Color(0x4400CCF9), radius = w * 0.012f, center = Offset(w * 0.9f, h * 0.3f))
    drawCircle(Color(0x55FF738F), radius = w * 0.015f, center = Offset(w * 0.12f, h * 0.72f))

    fun die(cx: Float, cy: Float, s: Float, deg: Float, pips: List<Pair<Float, Float>>) {
        rotate(deg, Offset(cx, cy)) {
            drawRoundRect(
                Color.White,
                topLeft = Offset(cx - s / 2, cy - s / 2),
                size = Size(s, s),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.18f, s * 0.18f),
            )
            val pr = s * 0.07f
            pips.forEach { (px, py) ->
                drawCircle(
                    Color(0xFF1B1D38), radius = pr,
                    center = Offset(cx - s / 2 + s * px, cy - s / 2 + s * py)
                )
            }
        }
    }
    val s = w * 0.34f
    die(w * 0.40f, h * 0.58f, s, -14f, listOf(0.25f to 0.25f, 0.5f to 0.5f, 0.75f to 0.75f, 0.75f to 0.25f, 0.25f to 0.75f))
    die(w * 0.66f, h * 0.40f, s, 12f, listOf(0.25f to 0.25f, 0.5f to 0.5f, 0.75f to 0.75f))
}

private fun DrawScope.drawSpy(w: Float, h: Float) {
    bg(w, h, Color(0xFF0E3B45), Color(0xFF081C26))
    // card
    rotate(-8f, Offset(w * 0.38f, h * 0.55f)) {
        drawRoundRect(
            Color(0xFFF3F4FA),
            topLeft = Offset(w * 0.14f, h * 0.24f),
            size = Size(w * 0.44f, h * 0.58f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f, w * 0.05f),
        )
        // question mark (drawn, no text)
        val qx = w * 0.36f
        val qy = h * 0.45f
        drawArc(
            Color(0xFF1B1D38),
            startAngle = 160f, sweepAngle = 250f, useCenter = false,
            topLeft = Offset(qx - w * 0.075f, qy - h * 0.09f),
            size = Size(w * 0.15f, h * 0.15f),
            style = Stroke(w * 0.028f, cap = StrokeCap.Round)
        )
        drawLine(
            Color(0xFF1B1D38),
            start = Offset(qx + w * 0.038f, qy + h * 0.055f),
            end = Offset(qx, qy + h * 0.10f),
            strokeWidth = w * 0.028f,
            cap = StrokeCap.Round
        )
        drawCircle(Color(0xFF1B1D38), radius = w * 0.02f, center = Offset(qx, qy + h * 0.16f))
    }
    // magnifier
    val lensC = Offset(w * 0.70f, h * 0.48f)
    val lensR = w * 0.17f
    drawCircle(Color(0x3300CCF9), radius = lensR, center = lensC)
    drawCircle(Color(0xFF00CCF9), radius = lensR, center = lensC, style = Stroke(w * 0.035f))
    rotate(38f, lensC) {
        drawLine(
            Color(0xFF00CCF9),
            start = Offset(lensC.x + lensR * 0.75f, lensC.y),
            end = Offset(lensC.x + lensR * 1.55f, lensC.y),
            strokeWidth = w * 0.05f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawPalette(w: Float, h: Float) {
    bg(w, h, Color(0xFF4A2131), Color(0xFF241019))
    // brush
    rotate(-32f, Offset(w * 0.68f, h * 0.5f)) {
        drawRoundRect(
            Color(0xFFE9D7B0),
            topLeft = Offset(w * 0.62f, h * 0.16f),
            size = Size(w * 0.035f, h * 0.5f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f, w * 0.02f)
        )
        drawRoundRect(
            Color(0xFF00CCF9),
            topLeft = Offset(w * 0.615f, h * 0.1f),
            size = Size(w * 0.045f, h * 0.12f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f, w * 0.02f)
        )
        drawCircle(Color(0xFFF7E25A), radius = w * 0.035f, center = Offset(w * 0.655f, h * 0.62f))
    }
    // palette body
    drawOval(
        Color(0xFFF3F4FA),
        topLeft = Offset(w * 0.10f, h * 0.30f),
        size = Size(w * 0.56f, h * 0.46f)
    )
    drawCircle(Color(0xFF4A2131), radius = w * 0.055f, center = Offset(w * 0.34f, h * 0.60f))
    val dabs = listOf(
        Triple(0.20f, 0.42f, Color(0xFFFF5A6E)),
        Triple(0.34f, 0.38f, Color(0xFFF7C948)),
        Triple(0.48f, 0.42f, Color(0xFF50DCA2)),
        Triple(0.55f, 0.55f, Color(0xFF4FA3FF)),
    )
    dabs.forEach { (x, y, c) ->
        drawCircle(c, radius = w * 0.045f, center = Offset(w * x, h * y))
    }
}

private fun DrawScope.drawBingo(w: Float, h: Float) {
    bg(w, h, Color(0xFF1F2A5C), Color(0xFF0D1330))
    // cage dome
    val cx = w * 0.5f
    val cy = h * 0.52f
    val r = w * 0.26f
    drawArc(
        Color(0xFFF3F4FA),
        startAngle = 180f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(cx - r, cy - r),
        size = Size(r * 2, r * 2),
        style = Stroke(w * 0.028f)
    )
    // mesh
    listOf(-0.6f, -0.2f, 0.2f, 0.6f).forEach { f ->
        drawArc(
            Color(0x66F3F4FA),
            startAngle = 180f + f * 90f * 0.0f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(cx - r * (1 - kotlin.math.abs(f) * 0.45f), cy - r),
            size = Size(r * 2 * (1 - kotlin.math.abs(f) * 0.45f), r * 2),
            style = Stroke(w * 0.012f)
        )
    }
    drawLine(
        Color(0xFFF3F4FA),
        start = Offset(cx - r, cy), end = Offset(cx + r, cy),
        strokeWidth = w * 0.028f, cap = StrokeCap.Round
    )
    // balls
    val balls = listOf(
        Triple(0.32f, 0.72f, Color(0xFFFF5A6E)),
        Triple(0.44f, 0.80f, Color(0xFFF7C948)),
        Triple(0.56f, 0.72f, Color(0xFF50DCA2)),
        Triple(0.66f, 0.80f, Color(0xFF4FA3FF)),
        Triple(0.50f, 0.64f, Color(0xFFFF738F)),
    )
    balls.forEach { (x, y, c) ->
        drawCircle(c, radius = w * 0.055f, center = Offset(w * x, h * y))
        drawCircle(Color(0x55FFFFFF), radius = w * 0.018f, center = Offset(w * x - w * 0.018f, h * y - h * 0.018f))
    }
}

private fun DrawScope.drawMic(w: Float, h: Float) {
    bg(w, h, Color(0xFF073A4A), Color(0xFF041E2A))
    val cx = w * 0.5f
    // capsule
    drawRoundRect(
        Color(0xFFF3F4FA),
        topLeft = Offset(cx - w * 0.11f, h * 0.16f),
        size = Size(w * 0.22f, h * 0.42f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.11f, w * 0.11f)
    )
    // stand
    drawArc(
        Color(0xFF00CCF9),
        startAngle = 0f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(cx - w * 0.20f, h * 0.38f),
        size = Size(w * 0.40f, h * 0.40f),
        style = Stroke(w * 0.045f, cap = StrokeCap.Round)
    )
    drawLine(Color(0xFF00CCF9), Offset(cx, h * 0.78f), Offset(cx, h * 0.88f), w * 0.045f, StrokeCap.Round)
    // waves
    drawArc(
        Color(0x6600CCF9), 130f, 90f, false,
        topLeft = Offset(cx - w * 0.36f, h * 0.22f),
        size = Size(w * 0.28f, h * 0.28f),
        style = Stroke(w * 0.03f, cap = StrokeCap.Round)
    )
    drawArc(
        Color(0x6600CCF9), -40f, 90f, false,
        topLeft = Offset(cx + w * 0.08f, h * 0.22f),
        size = Size(w * 0.28f, h * 0.28f),
        style = Stroke(w * 0.03f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawMusic(w: Float, h: Float) {
    bg(w, h, Color(0xFF4A1F33), Color(0xFF241019))
    val cx = w * 0.5f
    // double note
    drawRoundRect(
        Color(0xFFF3F4FA),
        topLeft = Offset(cx - w * 0.02f, h * 0.22f),
        size = Size(w * 0.04f, h * 0.46f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f, w * 0.02f)
    )
    drawRoundRect(
        Color(0xFFF3F4FA),
        topLeft = Offset(cx + w * 0.22f, h * 0.16f),
        size = Size(w * 0.04f, h * 0.46f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f, w * 0.02f)
    )
    drawRoundRect(
        Color(0xFFF3F4FA),
        topLeft = Offset(cx - w * 0.02f, h * 0.16f),
        size = Size(w * 0.28f, h * 0.09f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f, w * 0.02f)
    )
    drawCircle(Color(0xFFFF738F), radius = w * 0.085f, center = Offset(cx - w * 0.07f, h * 0.72f))
    drawCircle(Color(0xFFFF738F), radius = w * 0.085f, center = Offset(cx + w * 0.17f, h * 0.66f))
    drawCircle(Color(0x55000000), radius = w * 0.03f, center = Offset(cx - w * 0.07f, h * 0.72f))
}

private fun DrawScope.drawSquid(w: Float, h: Float) {
    bg(w, h, Color(0xFF3A1020), Color(0xFF1E0A12))
    val cx = w * 0.5f
    // head
    val head = Path().apply {
        moveTo(cx, h * 0.18f)
        cubicTo(w * 0.78f, h * 0.24f, w * 0.74f, h * 0.52f, cx, h * 0.58f)
        cubicTo(w * 0.26f, h * 0.52f, w * 0.22f, h * 0.24f, cx, h * 0.18f)
        close()
    }
    drawPath(head, Color(0xFFFF738F))
    // eyes
    drawCircle(Color.White, radius = w * 0.05f, center = Offset(cx - w * 0.09f, h * 0.38f))
    drawCircle(Color.White, radius = w * 0.05f, center = Offset(cx + w * 0.09f, h * 0.38f))
    drawCircle(Color(0xFF1B1D38), radius = w * 0.022f, center = Offset(cx - w * 0.085f, h * 0.39f))
    drawCircle(Color(0xFF1B1D38), radius = w * 0.022f, center = Offset(cx + w * 0.085f, h * 0.39f))
    // tentacles
    val tentacles = listOf(-0.16f, -0.06f, 0.06f, 0.16f)
    tentacles.forEach { off ->
        drawArc(
            Color(0xFFFF738F),
            startAngle = 90f, sweepAngle = 70f, useCenter = false,
            topLeft = Offset(cx + w * (off - 0.06f), h * 0.52f),
            size = Size(w * 0.12f, h * 0.34f),
            style = Stroke(w * 0.035f, cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawSheep(w: Float, h: Float) {
    bg(w, h, Color(0xFF12351F), Color(0xFF081F12))
    // grass
    drawLine(
        Color(0xFF50DCA2),
        start = Offset(w * 0.12f, h * 0.82f), end = Offset(w * 0.88f, h * 0.82f),
        strokeWidth = w * 0.02f, cap = StrokeCap.Round
    )
    // legs
    listOf(0.40f, 0.54f, 0.64f).forEach { x ->
        drawLine(
            Color(0xFF1B1D38),
            start = Offset(w * x, h * 0.66f), end = Offset(w * x, h * 0.80f),
            strokeWidth = w * 0.03f, cap = StrokeCap.Round
        )
    }
    // body
    val puffs = listOf(
        Triple(0.34f, 0.48f, 0.12f),
        Triple(0.47f, 0.42f, 0.14f),
        Triple(0.60f, 0.48f, 0.12f),
        Triple(0.42f, 0.56f, 0.13f),
        Triple(0.55f, 0.56f, 0.13f),
    )
    puffs.forEach { (x, y, r) ->
        drawCircle(Color(0xFFF3F4FA), radius = w * r, center = Offset(w * x, h * y))
    }
    // head
    drawCircle(Color(0xFF1B1D38), radius = w * 0.09f, center = Offset(w * 0.74f, h * 0.46f))
    // ears
    drawCircle(Color(0xFF1B1D38), radius = w * 0.03f, center = Offset(w * 0.70f, h * 0.40f))
    drawCircle(Color(0xFF1B1D38), radius = w * 0.03f, center = Offset(w * 0.79f, h * 0.40f))
    // eye
    drawCircle(Color.White, radius = w * 0.016f, center = Offset(w * 0.77f, h * 0.45f))
}

private fun DrawScope.drawFallback(w: Float, h: Float) {
    bg(w, h, Color(0xFF2A2F4A), Color(0xFF171A2E))
    drawCircle(Color(0x3300CCF9), radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.5f))
}
