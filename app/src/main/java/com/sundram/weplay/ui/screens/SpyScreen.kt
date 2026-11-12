package com.sundram.weplay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sundram.weplay.game.SpyData
import com.sundram.weplay.game.SpyGame
import com.sundram.weplay.game.SpyPhase
import com.sundram.weplay.game.SpyState
import com.sundram.weplay.ui.components.InitialsAvatar
import com.sundram.weplay.ui.components.NoteBox
import com.sundram.weplay.ui.components.TopBackBar
import com.sundram.weplay.ui.theme.BgSection
import com.sundram.weplay.ui.theme.BgSoft
import com.sundram.weplay.ui.theme.CardWhite
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.CyanDeep
import com.sundram.weplay.ui.theme.OnBrand
import com.sundram.weplay.ui.theme.PinkGift
import com.sundram.weplay.ui.theme.Stroke
import com.sundram.weplay.ui.theme.TagBlueBg
import com.sundram.weplay.ui.theme.TextMain
import com.sundram.weplay.ui.theme.TextMuted
import com.sundram.weplay.ui.theme.WarnOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val NAMES = listOf("Ankit", "Priya", "Raj", "Sam", "Sneha", "Mohit", "Kavya", "Kabir")

@Composable
fun SpyScreen(nav: NavHostController) {
    val game = remember { SpyGame() }
    var st by remember { mutableStateOf(game.state) }
    game.onChange = { st = it }

    LaunchedEffect(st.phase) {
        while (isActive && st.phase == SpyPhase.DISCUSS) {
            delay(1000)
            game.tick()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BgSoft)
            .systemBarsPadding()
            .padding(horizontal = 14.dp),
    ) {
        TopBackBar(nav, "Who's the Spy", "Deduction · pass and play")
        Spacer(Modifier.height(10.dp))
        when (st.phase) {
            SpyPhase.SETUP -> SetupScreen(game)
            SpyPhase.REVEAL -> RevealScreen(game, st)
            SpyPhase.DISCUSS -> DiscussScreen(game, st)
            SpyPhase.VOTE -> VoteScreen(game, st)
            SpyPhase.RESULT -> ResultScreen(game, st)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SetupScreen(game: SpyGame) {
    var count by remember { mutableIntStateOf(4) }
    var catId by remember { mutableStateOf(SpyData.categories.first().id) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Players (3-8)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMain)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (3..8).forEach { n ->
                val selected = n == count
                Box(
                    Modifier
                        .size(42.dp)
                        .background(if (selected) CyanBrand else CardWhite, RoundedCornerShape(10.dp))
                        .border(1.dp, if (selected) Color.Transparent else Stroke, RoundedCornerShape(10.dp))
                        .clickable { count = n },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "$n",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) OnBrand else TextMuted,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(NAMES.take(count).joinToString(" · "), fontSize = 11.sp, color = TextMuted)
        Spacer(Modifier.height(16.dp))
        Text("Category", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMain)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SpyData.categories.forEach { c ->
                val selected = c.id == catId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (selected) CyanBrand.copy(alpha = 0.10f) else CardWhite,
                            RoundedCornerShape(12.dp),
                        )
                        .border(1.dp, if (selected) CyanBrand else Stroke, RoundedCornerShape(12.dp))
                        .clickable { catId = c.id }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InitialsAvatar(c.name, size = 30.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        c.name,
                        color = TextMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.weight(1f))
                    Text("${c.words.size} words", color = TextMuted, fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .background(CyanBrand, RoundedCornerShape(14.dp))
                .clickable { game.start(NAMES.take(count), catId) }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Start game", color = OnBrand, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(18.dp))
        NoteBox("Pass one phone around. Crew gets the word, the Spy only sees the category. Describe, then vote.")
    }
}

@Composable
private fun RevealScreen(game: SpyGame, st: SpyState) {
    var shown by remember(st.revealIndex, st.phase) { mutableStateOf(false) }
    val player = st.players[st.revealIndex]
    val isSpy = player.isSpy
    Column(
        Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Pass the phone", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextMain)
        Spacer(Modifier.height(8.dp))
        InitialsAvatar(player.name, size = 64.dp)
        Spacer(Modifier.height(6.dp))
        Text(player.name, fontSize = 16.sp, color = CyanDeep, fontWeight = FontWeight.SemiBold)
        Text("${st.revealIndex + 1} of ${st.players.size}", fontSize = 10.sp, color = TextMuted)
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .background(CardWhite, RoundedCornerShape(18.dp))
                .border(1.dp, Stroke, RoundedCornerShape(18.dp))
                .clickable { shown = true }
                .padding(vertical = 36.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (shown) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (isSpy) "SPY" else "CREW",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSpy) WarnOrange else CyanDeep,
                    )
                    Spacer(Modifier.height(10.dp))
                    if (isSpy) {
                        Text(
                            "No word for you",
                            fontSize = 15.sp,
                            color = TextMain,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Category only: ${st.category?.name}",
                            fontSize = 13.sp,
                            color = TextMuted,
                        )
                    } else {
                        Text(
                            "Your secret word",
                            fontSize = 11.sp,
                            color = TextMuted,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            st.word,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMain,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Category: ${st.category?.name}",
                            fontSize = 11.sp,
                            color = TextMuted,
                        )
                    }
                }
            } else {
                Text("Tap to see your role", fontSize = 15.sp, color = TextMuted)
            }
        }
        Spacer(Modifier.height(14.dp))
        if (shown) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(CyanBrand, RoundedCornerShape(14.dp))
                    .clickable { game.nextReveal() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (st.revealIndex == st.players.size - 1) "Everyone has seen it — Discuss" else "Pass to next",
                    color = OnBrand,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun DiscussScreen(game: SpyGame, st: SpyState) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Discuss and describe", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
        Text(
            "Describe your secret word without saying it",
            fontSize = 11.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "%d:%02d".format(st.discussSeconds / 60, st.discussSeconds % 60),
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = if (st.discussSeconds < 30) WarnOrange else CyanDeep,
        )
        Spacer(Modifier.height(10.dp))
        st.players.forEachIndexed { i, p ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .background(CardWhite, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InitialsAvatar(p.name, size = 30.dp)
                Spacer(Modifier.width(9.dp))
                Text(
                    p.name,
                    color = if (p.votedOut) TextMuted else TextMain,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                if (p.votedOut) {
                    Text(
                        "out",
                        color = TextMuted,
                        fontSize = 9.sp,
                        modifier = Modifier
                            .background(BgSection, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .fillMaxWidth()
                .background(PinkGift, RoundedCornerShape(14.dp))
                .clickable { game.skipToVote() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Start voting", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun VoteScreen(game: SpyGame, st: SpyState) {
    val aliveIdx = st.players.indices.filter { !st.players[it].votedOut }
    val votersLeft = aliveIdx.filter { it !in st.votes.keys }
    val voter = remember(st) { mutableIntStateOf(votersLeft.firstOrNull() ?: -1) }
    var vi by voter

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Voting", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
        if (vi == -1) {
            Text("All votes are in", color = TextMuted, fontSize = 12.sp)
        } else {
            Text(
                "${st.players[vi].name}, who do you vote to eliminate?",
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${st.votes.size} of ${aliveIdx.size} votes cast",
                fontSize = 10.sp,
                color = CyanDeep,
            )
        }
        Spacer(Modifier.height(10.dp))
        aliveIdx.forEach { ti ->
            val target = st.players[ti]
            val disabled = vi == -1 || ti == vi
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .background(CardWhite, RoundedCornerShape(10.dp))
                    .clickable(enabled = !disabled) { game.castVote(vi, ti) }
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InitialsAvatar(target.name, size = 34.dp, ring = if (ti == vi) CyanBrand else null)
                Spacer(Modifier.width(10.dp))
                Text(
                    target.name,
                    color = if (disabled) TextMuted else TextMain,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                if (ti == vi) {
                    Text(
                        "YOU",
                        color = CyanDeep,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(TagBlueBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultScreen(game: SpyGame, st: SpyState) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            st.log.firstOrNull() ?: "Game over",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        st.players.forEach { p ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .background(CardWhite, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InitialsAvatar(p.name, size = 34.dp)
                Spacer(Modifier.width(10.dp))
                Text(p.name, color = TextMain, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(
                    if (p.isSpy) "SPY" else "Crew",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (p.isSpy) WarnOrange else CyanDeep,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Word: ${st.word} · ${st.category?.name}",
            fontSize = 12.sp,
            color = TextMuted,
        )
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .fillMaxWidth()
                .background(CyanBrand, RoundedCornerShape(14.dp))
                .clickable { game.reset() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("New game", color = OnBrand, fontWeight = FontWeight.Bold)
        }
    }
}
