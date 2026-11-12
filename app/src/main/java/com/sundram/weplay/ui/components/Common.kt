package com.sundram.weplay.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sundram.weplay.ui.theme.AvatarPalette
import com.sundram.weplay.ui.theme.BgSection
import com.sundram.weplay.ui.theme.CardWhite
import com.sundram.weplay.ui.theme.ChipBg
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.KtvRed
import com.sundram.weplay.ui.theme.LiveRed
import com.sundram.weplay.ui.theme.OnBrand
import com.sundram.weplay.ui.theme.ScrimHair
import com.sundram.weplay.ui.theme.Stroke
import com.sundram.weplay.ui.theme.TagBlue
import com.sundram.weplay.ui.theme.TagBlueBg
import com.sundram.weplay.ui.theme.TagGreen
import com.sundram.weplay.ui.theme.TagGreenBg
import com.sundram.weplay.ui.theme.TagOrange
import com.sundram.weplay.ui.theme.TagOrangeBg
import com.sundram.weplay.ui.theme.TagPink
import com.sundram.weplay.ui.theme.TagPinkBg
import com.sundram.weplay.ui.theme.TextMain
import com.sundram.weplay.ui.theme.TextMuted

fun avatarGradient(name: String): List<Color> {
    val pair = AvatarPalette[name.hashCode().ushr(1) % AvatarPalette.size]
    return listOf(pair.first, pair.second)
}

fun initialsOf(name: String): String {
    val words = name.trim().split(" ").filter { it.isNotBlank() }
    if (words.isEmpty()) return "?"
    return words.take(2).map { it.first().uppercaseChar() }.joinToString("")
}

fun tagColors(tag: String): Pair<Color, Color> = when (tag) {
    "Casual" -> TagGreenBg to TagGreen
    "KTV" -> TagBlueBg to TagBlue
    "Games" -> TagOrangeBg to TagOrange
    "Deduction" -> TagPinkBg to TagPink
    else -> TagBlueBg to TagBlue
}

/** Circular avatar built from a display name, with optional speaking ring. */
@Composable
fun InitialsAvatar(
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    ring: Color? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(Brush.linearGradient(avatarGradient(name)), CircleShape)
            .then(if (ring != null) Modifier.border(2.dp, ring, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initialsOf(name),
            color = Color.White,
            fontSize = (size.value * 0.34f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Rounded tile avatar used by the light profile and ranking cards. */
@Composable
fun InitialsTile(name: String, size: Dp, corner: Dp = 18.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .background(Brush.linearGradient(avatarGradient(name)), RoundedCornerShape(corner)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initialsOf(name),
            color = Color.White,
            fontSize = (size.value * 0.38f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Avatar that shows the user's photo when set, initials otherwise. */
@Composable
fun UserAvatar(
    name: String,
    photoPath: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    ring: Color? = null,
) {
    val photo = remember(photoPath) {
        photoPath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(avatarGradient(name)))
            .then(if (ring != null) Modifier.border(2.dp, ring, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                initialsOf(name),
                color = Color.White,
                fontSize = (size.value * 0.34f).sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Rounded room cover with gradient + monogram. */
@Composable
fun CoverThumb(name: String, modifier: Modifier = Modifier, corner: Int = 16) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(avatarGradient(name)),
            RoundedCornerShape(corner.dp)
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initialsOf(name),
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .width(20.dp)
                .size(20.dp)
                .background(ScrimHair, CircleShape)
        )
    }
}

@Composable
fun LivePill(text: String = "LIVE") {
    Row(
        modifier = Modifier
            .background(LiveRed, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(5.dp).background(Color.White, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(text, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TagPill(text: String, fg: Color = OnBrand, bg: Color = CyanBrand) {
    Text(
        text,
        color = fg,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

@Composable
fun SectionHeader(title: String, trailing: String? = null, onTrailingClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        if (trailing != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (onTrailingClick != null) Modifier.clickable { onTrailingClick() } else Modifier,
            ) {
                Text(trailing, color = TextMuted, fontSize = 12.sp)
                Icon(
                    painter = androidx.compose.ui.res.painterResource(com.sundram.weplay.R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun NoteBox(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(BgSection, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(text, fontSize = 12.sp, color = TextMuted, lineHeight = 17.sp)
    }
}

@Composable
fun RoundIconButton(
    painter: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    iconSize: Dp = 20.dp,
    bg: Color = ChipBg,
    tint: Color = TextMain,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(bg, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(text, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
}

@Composable
fun PlainField(value: String, onChange: (String) -> Unit, placeholder: String) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(color = TextMain, fontSize = 16.sp),
        cursorBrush = SolidColor(CyanBrand),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite, RoundedCornerShape(14.dp))
            .border(1.dp, Stroke, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        decorationBox = { field ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, color = TextMuted, fontSize = 16.sp)
                }
                field()
            }
        },
    )
}

@Composable
fun PrimaryButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(
                if (enabled) CyanBrand else ChipBg,
                RoundedCornerShape(14.dp),
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (enabled) OnBrand else TextMuted,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun RedDot(modifier: Modifier = Modifier, border: Color = CardWhite, borderDp: Dp = 2.dp) {
    Box(
        modifier
            .size(10.dp)
            .background(KtvRed, CircleShape)
            .then(if (borderDp > 0.dp) Modifier.border(borderDp, border, CircleShape) else Modifier)
    )
}

@Composable
fun UnreadBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(KtvRed, CircleShape)
            .border(2.dp, CardWhite, CircleShape)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(
            "$count",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
