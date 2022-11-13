package pink.nora.pitchup

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresPermission
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pink.nora.pitchup.ui.theme.PitchUp2Theme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import pink.nora.pitchupcore.PitchAudioRecorder
import pink.nora.pitchupcore.TunerResult
import pink.nora.pitchupcore.TuningStatus
import pink.nora.pitchupcore.createTunerFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

val EMPTY_TUNER_RESULT = TunerResult("", TuningStatus.DEFAULT, 0.0, 0.0, 0.0)

@RequiresPermission(Manifest.permission.RECORD_AUDIO)
fun getAudioRecorder() = PitchAudioRecorder(
    AudioRecord(
        MediaRecorder.AudioSource.DEFAULT,
        44100,
        AudioFormat.CHANNEL_IN_DEFAULT,
        AudioFormat.ENCODING_PCM_16BIT,
        AudioRecord.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_IN_DEFAULT,
            AudioFormat.ENCODING_PCM_16BIT
        )
    )
)

fun tunerResultFlow(audioRecorder: PitchAudioRecorder) =
    createTunerFlow(audioRecorder)
        .distinctUntilChanged { old, new -> old.note == new.note && old.tuningStatus == new.tuningStatus }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)

        setContent {
            MainScreen()
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalLifecycleComposeApi::class)
@Composable
fun MainScreen() {
    PitchUp2Theme {
        PitchUpScaffolding {
            val recordPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
            if (recordPermissionState.status.isGranted) {
                val tunerResultFlow = remember {
                    @SuppressLint("MissingPermission") // we check with isGranted
                    val audioRecorder = getAudioRecorder()
                    tunerResultFlow(audioRecorder)
                }
                val tunerResult = tunerResultFlow.collectAsStateWithLifecycle(EMPTY_TUNER_RESULT)
                Tuner(tunerResult.value)
            } else {
                AskPermission(recordPermissionState::launchPermissionRequest)
            }
        }
    }
}

@Composable
fun Tuner(tunerResult: TunerResult) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        val angle = animateFloatAsState(targetValue = (-tunerResult.diffCents.toFloat()) / 100f)
        PitchGauge(pitchAngle = angle.value)
        TunerCurrentFrequency(tunerResult)
        Spacer(Modifier.height(8.dp))
        TunerDiffCents(tunerResult)
        Spacer(Modifier.height(16.dp))
        TunerNote(tunerResult)
    }
}

@Composable
fun AskPermission(askPermission: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Button(onClick = askPermission) {
            Text(text = "Request Permission")
        }
    }
}

@Composable
fun PitchUpScaffolding(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Image(
                painterResource(R.drawable.pitchup),
                "Logo",
                modifier = Modifier
                    .padding(vertical = 32.dp)
                    .align(Alignment.CenterHorizontally)
            )
            content()
        }
    }
}

@Composable
fun TunerCurrentFrequency(
    tunerResult: TunerResult,
) {
    val currentFrequency = (tunerResult.expectedFrequency - tunerResult.diffFrequency).roundToInt()
    Text(
        "$currentFrequency Hz",
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
fun TunerDiffCents(
    tunerResult: TunerResult,
) {
    Text(
        "${tunerResult.diffCents.roundToInt()} cents",
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
fun TunerNote(
    tunerResult: TunerResult,
) {
    Row {
        val leftIndicator = when (tunerResult.tuningStatus) {
            TuningStatus.TOO_LOW -> ">"
            TuningStatus.WAY_TOO_LOW -> ">>"
            else -> ""
        }
        val rightIndicator = when (tunerResult.tuningStatus) {
            TuningStatus.TOO_HIGH -> "<"
            TuningStatus.WAY_TOO_HIGH -> "<<"
            else -> ""
        }
        val text = when (tunerResult.note) {
            "" -> "?"
            else -> tunerResult.note
        }

        Text(
            leftIndicator,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Right,
            modifier = Modifier.width(60.dp),
        )
        Text(
            text,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            rightIndicator,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Left,
            modifier = Modifier.width(60.dp),
        )
    }
}

@Composable
fun PitchGauge(pitchAngle: Float) {
    Box {
        val canvasSize = Modifier.size(300.dp, 200.dp)
        Canvas(
            modifier = canvasSize
                .clipToBounds()
        ) {
            val radius = size.width / 2 - 10
            val arcColor = Color(0xFFc6c6c6)
            drawCircle(
                arcColor,
                radius = radius,
                center = Offset(center.x, size.height),
                style = Stroke(10f),
            )
        }

        Canvas(modifier = canvasSize) {
            val radius = size.width / 2 - 10
            val x = sin(pitchAngle * 0.5 * PI).toFloat()
            val y = cos(pitchAngle * 0.5 * PI).toFloat()
            drawLine(
                Color.Green,
                start = Offset(center.x, size.height),
                end = Offset(center.x + x * radius, size.height - y * radius),
                strokeWidth = 10f,
            )
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_5",
)
@Composable
fun DefaultPreview() {
    PitchUp2Theme {
        PitchUpScaffolding {
            Tuner(tunerResult = TunerResult("A", TuningStatus.TOO_LOW, 440.0, 10.0, 10.0))
        }
    }
}