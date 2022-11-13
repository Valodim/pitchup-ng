package pink.nora.pitchupcore

import android.media.AudioRecord.RECORDSTATE_RECORDING
import be.tarsos.dsp.pitch.Yin
import pink.nora.pitchupcore.pitch.PitchHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.lang.Thread.yield

fun createTunerFlow(
    pitchAudioRecord: PitchAudioRecorder,
    instrumentType: InstrumentType = InstrumentType.GUITAR
) = flow {
    val torsoYin = Yin(pitchAudioRecord.sampleRateInHz.toFloat(), pitchAudioRecord.readSize)
    val pitchHandler = PitchHandler(instrumentType)

    pitchAudioRecord.startRecording()
    try {
        while (pitchAudioRecord.recordingState == RECORDSTATE_RECORDING) {
            yield()

            val buffer = pitchAudioRecord.read()
            val pitchResult = torsoYin.getPitch(buffer)
            val result = pitchHandler.handlePitch(pitchResult.pitch)

            emit(
                TunerResult(
                    note = result.note,
                    tuningStatus = result.tuningStatus,
                    diffFrequency = result.diffFrequency,
                    expectedFrequency = result.expectedFrequency,
                    diffCents = result.diffCents
                )
            )
        }
    } finally {
        pitchAudioRecord.stopRecording()
    }
}.flowOn(Dispatchers.IO)