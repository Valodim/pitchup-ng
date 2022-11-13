package pink.nora.pitchup.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme
import pink.nora.pitchup.theme.Typography
import pink.nora.pitchup.theme.wearColorPalette

@Composable
fun PitchUpTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = wearColorPalette,
        typography = Typography,
        content = content
    )
}