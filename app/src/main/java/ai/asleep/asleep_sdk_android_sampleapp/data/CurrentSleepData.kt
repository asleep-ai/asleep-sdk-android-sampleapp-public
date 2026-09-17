package ai.asleep.asleep_sdk_android_sampleapp.data

import android.os.Parcel
import android.os.Parcelable

/**
 * Wire format for an interim analysis result crossing the process boundary.
 *
 * `Session` is not Parcelable, so only the two values the tracking screen shows are sent:
 * the latest sleep stage and the latest snoring stage. Both are null until the analysis has
 * enough sequences to produce them.
 */
data class CurrentSleepData(
    val lastSleepStage: Int?,
    val lastSnoringStage: Int?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(lastSleepStage)
        parcel.writeValue(lastSnoringStage)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CurrentSleepData> {
        override fun createFromParcel(parcel: Parcel): CurrentSleepData {
            return CurrentSleepData(parcel)
        }

        override fun newArray(size: Int): Array<CurrentSleepData?> {
            return arrayOfNulls(size)
        }
    }
}
