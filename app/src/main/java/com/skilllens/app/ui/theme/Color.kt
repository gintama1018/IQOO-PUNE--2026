package com.skilllens.app.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// SkillLens Design System — iQOO-inspired Performance Dark Palette
// Feeling: Engineering precision + real-time intelligence + technical HUD
// ─────────────────────────────────────────────────────────────────────────────

// ── Core Backgrounds ──────────────────────────────────────────────────────────
val ColorBackground        = Color(0xFF050810)   // Near-black with blue cast
val ColorSurface           = Color(0xFF0D1117)   // Card surface
val ColorSurfaceVariant    = Color(0xFF161B27)   // Elevated card / modal surface
val ColorSurfaceContainer  = Color(0xFF1C2135)   // Container within a card

// ── Primary Accent — Electric Blue ────────────────────────────────────────────
val ColorPrimary           = Color(0xFF00A8FF)   // Electric blue — CTA / active
val ColorPrimaryDim        = Color(0xFF0070CC)   // Dimmed primary
val ColorPrimaryGlow       = Color(0x3300A8FF)   // Glow / ambient

// ── Secondary Accent — Cyan / Teal ────────────────────────────────────────────
val ColorSecondary         = Color(0xFF00E5FF)   // Cyan highlight
val ColorSecondaryDim      = Color(0xFF009BB5)

// ── Status Colors ─────────────────────────────────────────────────────────────
val ColorCorrect           = Color(0xFF00E676)   // Verified / correct state
val ColorCorrectDim        = Color(0xFF00963F)
val ColorCorrectGlow       = Color(0x3300E676)

val ColorError             = Color(0xFFFF3D5A)   // Error / wrong state
val ColorErrorDim          = Color(0xFFB00028)
val ColorErrorGlow         = Color(0x33FF3D5A)

val ColorWarning           = Color(0xFFFFAB00)   // Caution / medium confidence
val ColorWarningGlow       = Color(0x33FFAB00)

val ColorUnknown           = Color(0xFF607080)   // Unknown / low confidence

// ── Typography ────────────────────────────────────────────────────────────────
val ColorOnBackground      = Color(0xFFEBEFF5)   // Primary text
val ColorOnSurface         = Color(0xFFCDD5E0)   // Secondary text on card
val ColorTextMuted         = Color(0xFF607080)   // Tertiary / caption text
val ColorTextDisabled      = Color(0xFF3A4455)

// ── Overlay / HUD ─────────────────────────────────────────────────────────────
val ColorHudLine           = Color(0x4000A8FF)   // Camera guide lines
val ColorHudCorner         = Color(0xCC00A8FF)   // Corner brackets
val ColorBoundingBoxActive = Color(0xFF00E676)   // Detected & verified
val ColorBoundingBoxIdle   = Color(0xFF00A8FF)   // Detected & observing
val ColorBoundingBoxError  = Color(0xFFFF3D5A)   // Detected & wrong
val ColorOverlayDark       = Color(0xCC050810)   // Camera overlay scrim

// ── Dividers / Borders ────────────────────────────────────────────────────────
val ColorBorder            = Color(0xFF1E2A3D)
val ColorBorderActive      = Color(0xFF00A8FF)
