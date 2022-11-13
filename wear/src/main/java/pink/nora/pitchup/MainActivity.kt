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
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import pink.nora.pitchup.theme.PitchUpTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.flow.distinctUntilChanged
import pink.nora.pitchupcore.PitchAudioRecorder
import pink.nora.pitchupcore.TunerResult
import pink.nora.pitchupcore.TuningStatus
import pink.nora.pitchupcore.createTunerFlow
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
    PitchUpTheme {
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

@Composable
fun Tuner(tunerResult: TunerResult) {
    Box {
        val angle = animateFloatAsState(targetValue = (-tunerResult.diffCents.toFloat()) / 100f)
        PitchGauge(angle.value)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            TunerCurrentFrequency(tunerResult)
            Spacer(Modifier.height(8.dp))
            TunerDiffCents(tunerResult)
            Spacer(Modifier.height(16.dp))
            TunerNote(tunerResult)
        }
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
fun TunerCurrentFrequency(
    tunerResult: TunerResult,
) {
    val currentFrequency = (tunerResult.expectedFrequency - tunerResult.diffFrequency).roundToInt()
    Text(
        "$currentFrequency Hz",
        style = MaterialTheme.typography.body1,
    )
}

@Composable
fun TunerDiffCents(
    tunerResult: TunerResult,
) {
    Text(
        "${tunerResult.diffCents.roundToInt()} cents",
        style = MaterialTheme.typography.body2,
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
            color = MaterialTheme.colors.primary,
            style = MaterialTheme.typography.title1,
            textAlign = TextAlign.Right,
            modifier = Modifier.width(60.dp),
        )
        Text(
            text,
            color = MaterialTheme.colors.primary,
            style = MaterialTheme.typography.title1,
        )
        Text(
            rightIndicator,
            color = MaterialTheme.colors.primary,
            style = MaterialTheme.typography.title1,
            textAlign = TextAlign.Left,
            modifier = Modifier.width(60.dp),
        )
    }
}

@Composable
fun PitchGauge(pitchAngle: Float) {
    val color = MaterialTheme.colors.secondary
    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = size.width / 2 - 20
        val x = sin(pitchAngle * 0.5 * PI).toFloat()
        val y = cos(pitchAngle * 0.5 * PI).toFloat()
        drawCircle(
            color,
            radius = 10f,
            center = Offset(center.x + x * radius, center.y - y * radius),
        )
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    PitchUpTheme {
        Tuner(tunerResult = TunerResult("A", TuningStatus.TOO_LOW, 440.0, 10.0, 10.0))
    }
}