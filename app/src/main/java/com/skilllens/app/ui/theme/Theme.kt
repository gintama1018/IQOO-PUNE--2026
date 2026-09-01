package com.skilllens.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// SkillLens always uses dark mode — this is a performance, engineering product.
// ─────────────────────────────────────────────────────────────────────────────

private val SkillLensDarkColorScheme = darkColorScheme(
    primary            = ColorPrimary,
    onPrimary          = Color(0xFF000D1A),
    primaryContainer   = ColorPrimaryDim,
    onPrimaryContainer = ColorOnBackground,

    secondary          = ColorSecondary,
    onSecondary        = Color(0xFF00131A),
    secondaryContainer = ColorSecondaryDim,
    onSecondaryContainer = ColorOnBackground,

    tertiary           = ColorCorrect,
    onTertiary         = Color(0xFF00130A),
    tertiaryContainer  = ColorCorrectDim,
    onTertiaryContainer = ColorOnBackground,

    error              = ColorError,
    onError            = Color(0xFF200008),
    errorContainer     = ColorErrorDim,
    onErrorContainer   = ColorOnBackground,

    background         = ColorBackground,
    onBackground       = ColorOnBackground,

    surface            = ColorSurface,
    onSurface          = ColorOnSurface,
    surfaceVariant     = ColorSurfaceVariant,
    onSurfaceVariant   = ColorTextMuted,

    outline            = ColorBorder,
    outlineVariant     = ColorBorderActive,
    scrim              = ColorOverlayDark,
    inverseSurface     = ColorOnBackground,
    inverseOnSurface   = ColorBackground,
    inversePrimary     = ColorPrimaryDim,
)

// ─────────────────────────────────────────────────────────────────────────────
// Extended color tokens accessible via LocalSkillLensColors
// ─────────────────────────────────────────────────────────────────────────────

data class SkillLensExtendedColors(
    val correct: Color,
    val correctDim: Color,
    val correctGlow: Color,
    val error: Color,
    val errorDim: Color,
    val errorGlow: Color,
    val warning: Color,
    val warningGlow: Color,
    val unknown: Color,
    val primaryGlow: Color,
    val hudLine: Color,
    val hudCorner: Color,
    val boundingBoxActive: Color,
    val boundingBoxIdle: Color,
    val boundingBoxError: Color,
    val surfaceContainer: Color,
    val textMuted: Color,
    val textDisabled: Color,
)

val LocalSkillLensColors = staticCompositionLocalOf {
    SkillLensExtendedColors(
        correct             = ColorCorrect,
        correctDim          = ColorCorrectDim,
        correctGlow         = ColorCorrectGlow,
        error               = ColorError,
        errorDim            = ColorErrorDim,
        errorGlow           = ColorErrorGlow,
        warning             = ColorWarning,
        warningGlow         = ColorWarningGlow,
        unknown             = ColorUnknown,
        primaryGlow         = ColorPrimaryGlow,
        hudLine             = ColorHudLine,
        hudCorner           = ColorHudCorner,
        boundingBoxActive   = ColorBoundingBoxActive,
        boundingBoxIdle     = ColorBoundingBoxIdle,
        boundingBoxError    = ColorBoundingBoxError,
        surfaceContainer    = ColorSurfaceContainer,
        textMuted           = ColorTextMuted,
        textDisabled        = ColorTextDisabled,
    )
}

// Convenience accessor
object SkillLensThemeTokens {
    val colors @Composable get() = LocalSkillLensColors.current
}

@Composable
fun SkillLensTheme(
    // Force dark; ignore system setting for this product
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val extendedColors = SkillLensExtendedColors(
        correct             = ColorCorrect,
        correctDim          = ColorCorrectDim,
        correctGlow         = ColorCorrectGlow,
        error               = ColorError,
        errorDim            = ColorErrorDim,
        errorGlow           = ColorErrorGlow,
        warning             = ColorWarning,
        warningGlow         = ColorWarningGlow,
        unknown             = ColorUnknown,
        primaryGlow         = ColorPrimaryGlow,
        hudLine             = ColorHudLine,
        hudCorner           = ColorHudCorner,
        boundingBoxActive   = ColorBoundingBoxActive,
        boundingBoxIdle     = ColorBoundingBoxIdle,
        boundingBoxError    = ColorBoundingBoxError,
        surfaceContainer    = ColorSurfaceContainer,
        textMuted           = ColorTextMuted,
        textDisabled        = ColorTextDisabled,
    )

    CompositionLocalProvider(LocalSkillLensColors provides extendedColors) {
        MaterialTheme(
            colorScheme = SkillLensDarkColorScheme,
            typography  = SkillLensTypography,
            content     = content,
        )
    }
}
