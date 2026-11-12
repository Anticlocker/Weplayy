package com.sundram.weplay.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sundram.weplay.R
import com.sundram.weplay.game.LudoGame
import com.sundram.weplay.game.LudoState
import com.sundram.weplay.game.Token
import com.sundram.weplay.ui.components.TopBackBar
import com.sundram.weplay.ui.theme.LudoBlue
import com.sundram.weplay.ui.theme.LudoEmptyCell
import com.sundram.weplay.ui.theme.LudoGloss
import com.sundram.weplay.ui.theme.OnBrand
import com.sundram.weplay.ui.theme.OverlaySoft
import com.sundram.weplay.ui.theme.LudoGreen
import com.sundram.weplay.ui.theme.LudoRed
import com.sundram.weplay.ui.theme.LudoYellow
import com.sundram.weplay.ui.theme.BgSoft
import com.sundram.weplay.ui.theme.CardWhite
import com.sundram.weplay.ui.theme.Stroke
import com.sundram.weplay.ui.theme.TextMain
import com.sundram.weplay.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val pColors = listOf(LudoRed, LudoGreen, LudoYellow, LudoBlue)

// 52-cell ring, clockwise (col,row) on 15x15 grid
private val ring = arrayOf(
    0 to 6, 1 to 6, 2 to 6, 3 to 6, 4 to 6, 5 to 6,           // red 0..5
    6 to 5, 6 to 4, 6 to 3, 6 to 2, 6 to 1, 6 to 0, 7 to 0,   // red 6..12
    8 to 0, 8 to 1, 8 to 2, 8 to 3, 8 to 4, 8 to 5,           // green 13..18
    9 to 6, 10 to 6, 11 to 6, 12 to 6, 13 to 6, 14 to 6, 14 to 7,
    14 to 8, 13 to 8, 12 to 8, 11 to 8, 10 to 8, 9 to 8,      // yellow 26..31
    8 to 9, 8 to 10, 8 to 11, 8 to 12, 8 to 13, 8 to 14, 7 to 14,
    6 to 14, 6 to 13, 6 to 12, 6 to 11, 6 to 10, 6 to 9,      // blue 39..44
    5 to 8, 4 to 8, 3 to 8, 2 to 8, 1 to 8, 0 to 8, 0 to 7,
)

// home stretch per player, 5 cells (6th is center)
private val homeCells = listOf(
    listOf(1 to 7, 2 to 7, 3 to 7, 4 to 7, 5 to 7),
    listOf(7 to 1, 7 to 2, 7 to 3, 7 to 4, 7 to 5),
    listOf(13 to 7, 12 to 7, 11 to 7, 10 to 7, 9 to 7),
    listOf(7 to 13, 7 to 12, 7 to 11, 7 to 10, 7 to 9),
)

private val yardOrigin = listOf(0 to 0, 9 to 0, 9 to 9, 0 to 9)

@Composable
fun LudoScreen(nav: NavHostController) {
    val game = remember { LudoGame() }
    var st by remember { mutableStateOf(game.state) }
    game.onChange = { st = it }

    LaunchedEffect(st.phase, st.current) {
        if (st.winner == -1 && st.players[st.current].isBot) {
            when (st.phase) {
                LudoState.Phase.ROLL -> {
                    delay(700)
                    if (isActive && st.winner == -1 && st.players[st.current].isBot && st.phase == LudoState.Phase.ROLL) game.roll()
                }
                LudoState.Phase.MOVE -> {
                    delay(650)
                    if (isActive && st.winner == -1 && st.players[st.current].isBot && st.phase == LudoState.Phase.MOVE) game.moveToken(game.botPick())
                }
                else -> Unit
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BgSoft)
            .systemBarsPadding()
            .padding(horizontal = 10.dp)
    ) {
        TopBackBar(nav, "Ludo", "You (Red) vs 3 bots · first 4 home wins")
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            st.players.forEachIndexed { i, p ->
                val active = i == st.current && st.winner == -1
                Column(
                    Modifier
                        .weight(1f)
                        .background(
                            if (active) pColors[i].copy(alpha = 0.18f) else CardWhite,
                            RoundedCornerShape(10.dp)
                        )
                        .border(1.dp, if (active) pColors[i].copy(alpha = 0.6f) else Stroke, RoundedCornerShape(10.dp))
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        listOf("RED", "GREEN", "YELLOW", "BLUE")[i],
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = pColors[i]
                    )
                    Text("${p.finished}/4 home", fontSize = 9.sp, color = TextMuted)
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                GameCanvas(st, Modifier.fillMaxSize(), onMove = { game.moveToken(it) })
                if (st.winner != -1) WinOverlay(st) { game.reset() }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DiceView(
                st,
                onRoll = { game.roll() }
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                val hint = when {
                    st.winner != -1 -> "Game over"
                    st.players[st.current].isBot -> "Bot turn..."
                    st.phase == LudoState.Phase.ROLL -> "Tap the dice to roll"
                    else -> "Tap a highlighted token"
                }
                Text(hint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                Text(
                    st.log.take(2).joinToString("  ·  "),
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DiceView(st: LudoState, onRoll: () -> Unit) {
    val face = if (st.dice in 1..6) st.dice else 6
    val canRoll = st.phase == LudoState.Phase.ROLL && !st.players[st.current].isBot && st.winner == -1
    Box(
        Modifier
            .width(64.dp)
            .height(64.dp)
            .background(
                if (canRoll) LudoYellow else CardWhite,
                RoundedCornerShape(16.dp)
            )
            .border(1.dp, if (canRoll) Color.Transparent else Stroke, RoundedCornerShape(16.dp))
            .clickable(enabled = canRoll) { onRoll() },
        contentAlignment = Alignment.Center
    ) {
        DiceFace(face, if (canRoll) OnBrand else TextMuted)
    }
}

@Composable
private fun DiceFace(face: Int, pipColor: Color) {
    Canvas(
        Modifier
            .fillMaxSize()
            .padding(11.dp)
    ) {
        val r = size.minDimension * 0.085f
        fun pip(x: Float, y: Float) = drawCircle(
            pipColor,
            r,
            Offset(size.width * x, size.height * y)
        )
        when (face) {
            1 -> pip(0.5f, 0.5f)
            2 -> { pip(0.28f, 0.28f); pip(0.72f, 0.72f) }
            3 -> { pip(0.28f, 0.28f); pip(0.5f, 0.5f); pip(0.72f, 0.72f) }
            4 -> { pip(0.28f, 0.28f); pip(0.72f, 0.28f); pip(0.28f, 0.72f); pip(0.72f, 0.72f) }
            5 -> {
                pip(0.28f, 0.28f); pip(0.72f, 0.28f); pip(0.5f, 0.5f)
                pip(0.28f, 0.72f); pip(0.72f, 0.72f)
            }
            else -> {
                pip(0.28f, 0.28f); pip(0.72f, 0.28f)
                pip(0.28f, 0.5f); pip(0.72f, 0.5f)
                pip(0.28f, 0.72f); pip(0.72f, 0.72f)
            }
        }
    }
}

@Composable
private fun GameCanvas(st: LudoState, modifier: Modifier, onMove: (Int) -> Unit) {
    var boardPx by remember { mutableStateOf(0f) }
    val movable: Set<Pair<Int, Int>> = if (st.phase == LudoState.Phase.MOVE && st.winner == -1 && !st.players[st.current].isBot) {
        st.players[st.current].tokens.filter { it.idx in st.legalMoves(st.dice) }.map { st.current to it.idx }.toSet()
    } else emptySet()

    fun tokenPixels(cell: Float): List<Triple<Int, Int, Offset>> {
        val hits = HashMap<Int, Int>()
        val out = mutableListOf<Triple<Int, Int, Offset>>()
        st.players.forEachIndexed { pi, p ->
            p.tokens.forEach { t ->
                val off: Offset = when {
                    t.pos == Token.BASE -> {
                        val (ox, oy) = yardOrigin[pi]
                        Offset((ox + 1.5f + (t.idx % 2) * 2.4f + 0.5f) * cell, (oy + 1.5f + (t.idx / 2) * 2.4f + 0.5f) * cell)
                    }
                    t.pos in 0..50 -> {
                        val abs = (p.start + t.pos) % 52
                        val (cx, cy) = ring[abs]
                        val n = hits.getOrDefault(abs, 0)
                        hits[abs] = n + 1
                        val jx = if (n % 2 == 1) 0.2f else -0.2f
                        val jy = if (n >= 2) 0.2f else -0.2f
                        Offset((cx + 0.5f + jx) * cell, (cy + 0.5f + jy) * cell)
                    }
                    else -> {
                        val idx = t.pos - 51
                        when {
                            t.pos == Token.DONE -> {
                                // spread finished tokens around the centre so they stay readable
                                val angle = Math.PI * 2.0 * (pi * 4 + t.idx) / 16
                                val rad = cell * 0.95f
                                Offset(
                                    7.5f * cell + (rad * kotlin.math.cos(angle)).toFloat(),
                                    7.5f * cell + (rad * kotlin.math.sin(angle)).toFloat(),
                                )
                            }
                            idx in 0..4 -> {
                                val (cx, cy) = homeCells[pi][idx]
                                Offset((cx + 0.5f) * cell, (cy + 0.5f) * cell)
                            }
                            else -> Offset(7.5f * cell, 7.5f * cell)
                        }
                    }
                }
                out.add(Triple(pi, t.idx, off))
            }
        }
        return out
    }

    Canvas(
        modifier
            .pointerInput(st) {
                detectTapGestures { tap ->
                    val cell = boardPx / 15f
                    if (cell <= 0f || movable.isEmpty()) return@detectTapGestures
                    var best = -1
                    var bestD = cell * 0.6f
                    tokenPixels(cell).forEach { (pi, ti, off) ->
                        if ((pi to ti) in movable) {
                            val d = (tap - off).getDistance()
                            if (d < bestD) { bestD = d; best = ti }
                        }
                    }
                    if (best >= 0) onMove(best)
                }
            }
    ) {
        val cell = size.width / 15f
        boardPx = size.width

        drawRoundRect(Color.White, cornerRadius = CornerRadius(cell * 0.5f, cell * 0.5f))

        // yards
        yardOrigin.forEachIndexed { i, (ox, oy) ->
            drawRoundRect(
                pColors[i],
                Offset(ox * cell, oy * cell),
                Size(cell * 6, cell * 6),
                cornerRadius = CornerRadius(cell * 0.5f, cell * 0.5f)
            )
            drawRoundRect(
                Color.White,
                Offset((ox + 1) * cell + cell * 0.15f, (oy + 1) * cell + cell * 0.15f),
                Size(cell * 3.7f, cell * 3.7f),
                cornerRadius = CornerRadius(cell * 0.4f, cell * 0.4f)
            )
        }

        // ring cells
        ring.forEachIndexed { abs, (cx, cy) ->
            val playerStart = abs % 13 == 0
            drawRoundRect(
                if (playerStart) pColors[abs / 13] else LudoEmptyCell,
                Offset(cx * cell + cell * 0.09f, cy * cell + cell * 0.09f),
                Size(cell * 0.82f, cell * 0.82f),
                cornerRadius = CornerRadius(cell * 0.15f, cell * 0.15f)
            )
        }
        // home columns
        homeCells.forEachIndexed { pi, cells ->
            cells.forEach { (cx, cy) ->
                drawRoundRect(
                    pColors[pi].copy(alpha = 0.9f),
                    Offset(cx * cell + cell * 0.09f, cy * cell + cell * 0.09f),
                    Size(cell * 0.82f, cell * 0.82f),
                    cornerRadius = CornerRadius(cell * 0.15f, cell * 0.15f)
                )
            }
        }
        // center triangles
        val cc = 7.5f * cell
        val tris = listOf(
            listOf(Offset(6 * cell, 6 * cell), Offset(6 * cell, 9 * cell)),
            listOf(Offset(6 * cell, 6 * cell), Offset(9 * cell, 6 * cell)),
            listOf(Offset(9 * cell, 6 * cell), Offset(9 * cell, 9 * cell)),
            listOf(Offset(6 * cell, 9 * cell), Offset(9 * cell, 9 * cell)),
        )
        tris.forEachIndexed { i, pair ->
            drawPath(
                Path().apply {
                    moveTo(pair[0].x, pair[0].y); lineTo(pair[1].x, pair[1].y); lineTo(cc, cc); close()
                },
                color = pColors[i]
            )
        }

        // tokens
        tokenPixels(cell).forEach { (pi, ti, off) ->
            val r = cell * 0.36f
            if ((pi to ti) in movable) {
                drawCircle(LudoYellow, radius = r * 1.4f, center = off)
            }
            drawCircle(pColors[pi], radius = r, center = off)
            drawCircle(Color.White, radius = r, center = off, style = Stroke(cell * 0.07f))
            drawCircle(LudoGloss, radius = r * 0.45f, center = off)
        }
    }
}

@Composable
private fun WinOverlay(st: LudoState, onRestart: () -> Unit) {
    val w = st.winner
    Box(
        Modifier
            .fillMaxSize()
            .background(OverlaySoft),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(64.dp)
                    .background(LudoYellow.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_star),
                    contentDescription = null,
                    tint = LudoYellow,
                    modifier = Modifier.size(34.dp)
                )
            }
            Text(
                "${listOf("RED", "GREEN", "YELLOW", "BLUE")[w]} WINS!",
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = pColors[w]
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .background(LudoYellow, RoundedCornerShape(14.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .clickable { onRestart() }
            ) { Text("Play again", color = OnBrand, fontWeight = FontWeight.Bold) }
        }
    }
}
