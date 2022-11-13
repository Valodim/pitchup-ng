package pink.nora.pitchup.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors

val Blue = Color(0xFF01579b)
val DarkCerulean = Color(0xFF014378)
val LightBlue = Color(0xFF0894b2)

internal val wearColorPalette: Colors = Colors(
    primary = Blue,
    primaryVariant = DarkCerulean,
    secondary = LightBlue,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onError = Color.Black
)