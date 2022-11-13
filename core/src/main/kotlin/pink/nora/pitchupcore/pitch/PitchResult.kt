package pink.nora.pitchupcore.pitch

import pink.nora.pitchupcore.TuningStatus

data class PitchResult(
    val note: String,
    val tuningStatus: TuningStatus,
    val expectedFrequency: Double,
    val diffFrequency: Double,
    val diffCents: Double
)