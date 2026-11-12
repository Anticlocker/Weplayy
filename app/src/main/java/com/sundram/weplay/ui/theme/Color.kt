package com.sundram.weplay.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Brand and artwork colors stay fixed across themes.
val CyanBrand = Color(0xFF00CCF9)
val CyanDeep = Color(0xFF00AAD7)
val CyanPool = Color(0xFF0093C4)
val PinkGift = Color(0xFFFF738F)
val LiveRed = Color(0xFFE8455F)

// Ink used on brand gradients and filled buttons
val OnBrand = Color(0xFF06202B)
val OnBrandDim = Color(0xFF06313F)
val KtvRed = Color(0xFFFF597A)
val WarnOrange = Color(0xFFFF6512)
val OrangeAccent = Color(0xFFFF8A00)
val MintGreen = Color(0xFF50DCA2)
val SuccessGreen = Color(0xFF3ECF6E)
val Gold = Color(0xFFFFC53D)

// Category tag pills
val TagGreenBg = Color(0xFFE6F9EE)
val TagGreen = Color(0xFF34C77B)
val TagBlueBg = Color(0xFFE7F3FF)
val TagBlue = Color(0xFF3E9BFF)
val TagOrangeBg = Color(0xFFFDF1E3)
val TagOrange = Color(0xFFFF8A34)
val TagPinkBg = Color(0xFFFEEDF1)
val TagPink = Color(0xFFFF6E8B)
val TagPurpleBg = Color(0xFFF3EBFF)
val TagPurple = Color(0xFF8A5CF6)
val TealBg = Color(0xFFE0F7F1)
val Teal = Color(0xFF2FBF9B)

// Feature banners
val PurpleHot = Color(0xFF8A3FF0)
val PurpleHotDeep = Color(0xFF6C2BD9)
val RankBg = Color(0xFF2A1B4E)
val HeroCyan = Color(0xFF00C6F2)
val HeroBlue = Color(0xFF4FB8FF)

// Sign-in brand colors
val GoogleBlue = Color(0xFF4285F4)
val FacebookBlue = Color(0xFF4FA3FF)

// Ludo
val LudoRed = Color(0xFFFF5A6E)
val LudoGreen = Color(0xFF50DCA2)
val LudoYellow = Color(0xFFF7C948)
val LudoBlue = Color(0xFF4FA3FF)
val LudoEmptyCell = Color(0xFFE9EBF4)
val LudoGloss = Color(0x40FFFFFF)

// Scrims placed over covers and artwork
val ScrimHair = Color(0x33000000)
val ScrimIcon = Color(0x44000000)
val ScrimCircle = Color(0x660A0C16)
val ScrimBadge = Color(0x990A0C16)
val ScrimFade = Color(0xF20A0C16)
val ScrimHeaderMid = Color(0xB30E1020)
val ScrimHeaderEnd = Color(0xE6121A2B)

// Text and accents drawn on covers
val CoverText = Color(0xFFC3C7DA)
val CoverTextDim = Color(0xFFCBD0E4)
val PlayGold = Color(0xFFFFD54A)
val CoverGlass = Color(0x33FFFFFF)
val CoverGlassDeep = Color(0x55FFFFFF)

// Rank medals
val MedalBronze = Color(0xFFFF8A5C)
val MedalSilver = Color(0xFF9FB0C9)

/** Colors that change with the active theme. */
@Immutable
data class WePlayPalette(
    val page: Color,
    val bgSoft: Color,
    val bgSection: Color,
    val chipBg: Color,
    val card: Color,
    val divider: Color,
    val stroke: Color,
    val textMain: Color,
    val textBody: Color,
    val textMuted: Color,
    val overlaySoft: Color,
    val authSky: Color,
    val wallBlue: Color,
    val wallPurple: Color,
    val wallMint: Color,
    val accentInk: Color,
)

val PageWhite: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalWePlayPalette.current.page

val BgSoft: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalWePlayPalette.current.bgSoft

val BgSection: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalWePlayPalette.current.bgSection

val ChipBg: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalWePlayPalette.current.chipBg

val CardWhite: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalWePlayPalette.current.card

val Divider: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalWePlayPalette.current.divider

val Stroke: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalWePlayPalette.current.stroke

val TextMain: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalWePlayPalette.current.textMain

val TextBody: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalWePlayPalette.current.textBody

val TextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalWePlayPalette.current.textMuted

val OverlaySoft: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalWePlayPalette.current.overlaySoft

val AuthSky: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalWePlayPalette.current.authSky

val WallBlue: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalWePlayPalette.current.wallBlue

val WallPurple: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalWePlayPalette.current.wallPurple

val WallMint: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalWePlayPalette.current.wallMint

val CyanInk: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalWePlayPalette.current.accentInk

/** Deterministic flat avatar colours for each display name. */
val AvatarPalette = listOf(
    CyanPool to CyanBrand,
    Color(0xFFD94F72) to PinkGift,
    Color(0xFF2E9C72) to MintGreen,
    Color(0xFFC58F1A) to Color(0xFFF7C948),
    Color(0xFF3568C4) to LudoBlue,
    Color(0xFF6C5CE7) to Color(0xFF9B8CFF),
    Color(0xFFB25A2A) to Color(0xFFFF8A4C),
)
