package pink.nora.pitchupcore

data class TunerResult(
    val note: String,
    val tuningStatus: TuningStatus,
    val expectedFrequency: Double,
    val diffFrequency: Double,
    val diffCents: Double
)