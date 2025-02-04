package ai.asleep.asleep_sdk_android_sampleapp.ui.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

data class Slice(
    val value: Float,
    val color: Int,
    val target: Boolean,
    val isTransparent: Boolean = false
)

class StackedBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    var slices: List<Slice> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    var onStartWidth: (Float) -> Unit = {}
    var onEndWidth: (Float) -> Unit = {}

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val canvasWidth = width.toFloat()
        val canvasHeight = height.toFloat()

        var currentX = 0f

        slices.forEachIndexed { index, slice ->
            val width = (slice.value / 100f) * canvasWidth

            if (index == 0) {
                if (slice.target) {
                    onStartWidth(0f)
                } else {
                    onStartWidth(width)
                }
            } else if (index == slices.size - 1) {
                if (slice.target) {
                    onEndWidth(0f)
                } else {
                    onEndWidth(width)
                }
            }

            paint.color = slice.color
            canvas.drawRect(currentX, 0f, currentX + width, canvasHeight, paint)
            currentX += width
        }
    }
}

fun makeSlice(
    stages: List<Int>?,
    targetValue: Int,
    mainColor: Int,
    otherColor: Int
): List<Slice> {
    val slice = mutableListOf<Slice>()
    var firstItemCnt = 0f
    var secondItemCnt = 0f

    stages?.let { stages ->
        stages.forEachIndexed { _, stage ->
            if (stage == targetValue) {
                if (secondItemCnt > 0) {
                    slice.add(
                        Slice((secondItemCnt / stages.size.toFloat()) * 100, otherColor, target = false)
                    )
                    secondItemCnt = 0f
                }
                firstItemCnt++
            } else {
                if (firstItemCnt > 0) {
                    slice.add(
                        Slice(
                            (firstItemCnt / stages.size.toFloat()) * 100,
                            mainColor,
                            target = true
                        )
                    )
                    firstItemCnt = 0f
                }
                secondItemCnt++
            }
        }
        if (firstItemCnt != 0f) {
            slice.add(Slice((firstItemCnt / stages.size.toFloat()) * 100, mainColor, target = true))
            firstItemCnt = 0f
        } else if (secondItemCnt != 0f) {
            slice.add(
                Slice(
                    (secondItemCnt / stages.size.toFloat()) * 100,
                    otherColor,
                    target = false,
                    true
                )
            )
            secondItemCnt = 0f
        }
    }

    return slice
}