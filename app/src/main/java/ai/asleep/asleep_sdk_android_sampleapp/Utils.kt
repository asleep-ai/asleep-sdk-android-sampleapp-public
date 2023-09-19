package ai.asleep.asleep_sdk_android_sampleapp

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun changeTimeFormat(time: String?): String? {
    if (time != null) {
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
        val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        val offsetDateTime = OffsetDateTime.parse(time, inputFormatter)
        val systemZone = ZoneId.systemDefault()
        val localDateTime = offsetDateTime.atZoneSameInstant(systemZone).toLocalDateTime()

        return localDateTime.format(outputFormatter)
    }
    return null
}
