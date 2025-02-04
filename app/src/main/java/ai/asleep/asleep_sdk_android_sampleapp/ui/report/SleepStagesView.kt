package ai.asleep.asleep_sdk_android_sampleapp.ui.report

import ai.asleep.asleep_sdk_android_sampleapp.R
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView

class SleepStagesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val awakeBar: StackedBarView
    private val remBar: StackedBarView
    private val lightBar: StackedBarView
    private val deepBar: StackedBarView
    private val startTime: TextView
    private val endTime: TextView

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_sleep_stages, this, true)
        awakeBar = findViewById(R.id.awake_bar)
        remBar = findViewById(R.id.rem_bar)
        lightBar = findViewById(R.id.light_bar)
        deepBar = findViewById(R.id.deep_bar)
        startTime = findViewById(R.id.tv_start_time)
        endTime = findViewById(R.id.tv_end_time)
    }

    fun setStackedBarData(index: Int, slices: List<Slice>) {
        when (index) {
            0 -> awakeBar.slices = slices
            1 -> remBar.slices = slices
            2 -> lightBar.slices = slices
            3 -> deepBar.slices = slices
            else -> throw IllegalArgumentException("Invalid index: $index")
        }
    }

    fun setOnStartWidthListener(index: Int, listener: (Float) -> Unit) {
        when (index) {
            0 -> awakeBar.onStartWidth = listener
            1 -> remBar.onStartWidth = listener
            2 -> lightBar.onStartWidth = listener
            3 -> deepBar.onStartWidth = listener
            else -> throw IllegalArgumentException("Invalid index: $index")
        }
    }

    fun setOnEndWidthListener(index: Int, listener: (Float) -> Unit) {
        when (index) {
            0 -> awakeBar.onEndWidth = listener
            1 -> remBar.onEndWidth = listener
            2 -> lightBar.onEndWidth = listener
            3 -> deepBar.onEndWidth = listener
            else -> throw IllegalArgumentException("Invalid index: $index")
        }
    }

    fun setStartTime(time: String) {
        startTime.text = time
    }

    fun setEndTime(time: String) {
        endTime.text = time
    }
}