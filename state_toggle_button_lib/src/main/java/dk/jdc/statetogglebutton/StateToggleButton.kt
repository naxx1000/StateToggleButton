package dk.jdc.statetogglebutton

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.res.ResourcesCompat
import dk.jdc.libs.statetogglebutton.R

class StateToggleButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val defaultCornerRadius = 200f
    private val animationDuration = 700L

    private var buttonItemList: List<ButtonItem> = emptyList()
    private var titleWidth = 0

    // Background
    private val paint = Paint()

    // Selection
    private val selectionPaint = Paint()
    private var selectedIndex = 0f
    private var selectionRect = RectF()
    private val selectionPath = Path()
    private var actionDownIndex = 0
    private var cornerRadiusLeft = 200f
    private var cornerRadiusRight = 0f
    private var valueAnimator = ValueAnimator()
    private var valueAnimatorLeft = ValueAnimator()
    private var valueAnimatorRight = ValueAnimator()

    // Divider
    private val dividerPaint = Paint()
    private var isDividerEnabled = false

    // Text
    private val textPaint = Paint()
    private var textFontId: Int = -1
    private var fontMetrics = textPaint.fontMetrics
    private var fontHeight = 0f

    data class ButtonItem(
        val title: String,
        val action: () -> Unit
    )

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.MultiStateToggleButton, 0, 0)
            .apply {
                try {
                    paint.color =
                        getColor(R.styleable.MultiStateToggleButton_stb_backgroundColor, Color.DKGRAY)
                    selectionPaint.color =
                        getColor(R.styleable.MultiStateToggleButton_stb_selectionColor, Color.LTGRAY)
                    textPaint.color =
                        getColor(R.styleable.MultiStateToggleButton_stb_textColor, Color.BLACK)
                    textPaint.textSize =
                        getDimension(R.styleable.MultiStateToggleButton_stb_textSize, 30f)
                    dividerPaint.color =
                        getColor(R.styleable.MultiStateToggleButton_stb_dividerColor, Color.WHITE)
                    isDividerEnabled =
                        getBoolean(R.styleable.MultiStateToggleButton_stb_isDividerEnabled, false)
                    dividerPaint.strokeWidth =
                        getDimension(R.styleable.MultiStateToggleButton_stb_dividerWidth, 0f)
                    textFontId = getResourceId(R.styleable.MultiStateToggleButton_stb_textFont, -1)
                } finally {
                    recycle()
                }
            }

        paint.isAntiAlias = true
        selectionPaint.isAntiAlias = true
        dividerPaint.isAntiAlias = true
        textPaint.typeface = ResourcesCompat.getFont(context, textFontId)
        textPaint.isAntiAlias = true
        textPaint.textAlign = Paint.Align.CENTER
        fontMetrics = textPaint.fontMetrics
        fontHeight = (fontMetrics.descent - fontMetrics.ascent) / 4
    }

    fun setButtons(buttonItemList: List<ButtonItem>) {
        post {
            this.buttonItemList = buttonItemList
            if (buttonItemList.size == 1) {
                cornerRadiusRight = defaultCornerRadius
            }
            measure(measuredWidth, measuredHeight)
            calculateSelectionRect()
            invalidate()
        }
    }

    fun selectIndex(index: Int) {
        try {
            animateByIndex(index)
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (buttonItemList.isNotEmpty()) titleWidth = width / buttonItemList.size
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                actionDown(event)
                return true
            }
            MotionEvent.ACTION_UP -> {
                actionUp(event)
                return false
            }

        }
        return false
    }

    private fun actionDown(event: MotionEvent) {
        buttonItemList.forEachIndexed { index, _ ->
            if (event.x > index * titleWidth && event.x < (index + 1) * titleWidth) {
                actionDownIndex = index
            }
        }
    }

    private fun actionUp(event: MotionEvent) {
        buttonItemList.forEachIndexed { index, buttonItem ->
            if (event.x > index * titleWidth && event.x < (index + 1) * titleWidth && event.y > 0 && event.y < height) {
                if (actionDownIndex != index) return@forEachIndexed

                buttonItem.action.invoke()

                animateByIndex(index)
            }
        }
    }

    private fun animateByIndex(index: Int) {
        when (index.toFloat()) {
            0f -> {
                valueAnimatorLeft =
                    ValueAnimator.ofFloat(cornerRadiusLeft, defaultCornerRadius)
                valueAnimatorLeft.duration = animationDuration
                valueAnimatorLeft.addUpdateListener {
                    cornerRadiusLeft = it.animatedValue as Float
                }

                valueAnimatorRight = ValueAnimator.ofFloat(
                    cornerRadiusRight,
                    if (buttonItemList.size == 1) defaultCornerRadius else 0f
                )
                valueAnimatorRight.duration = animationDuration
                valueAnimatorRight.addUpdateListener {
                    cornerRadiusRight = it.animatedValue as Float
                }

                valueAnimatorLeft.start()
                valueAnimatorRight.start()
            }
            buttonItemList.lastIndex.toFloat() -> {
                valueAnimatorLeft = ValueAnimator.ofFloat(cornerRadiusLeft, 0f)
                valueAnimatorLeft.duration = animationDuration
                valueAnimatorLeft.addUpdateListener {
                    cornerRadiusLeft = it.animatedValue as Float
                }

                valueAnimatorRight =
                    ValueAnimator.ofFloat(cornerRadiusRight, defaultCornerRadius)
                valueAnimatorRight.duration = animationDuration
                valueAnimatorRight.addUpdateListener {
                    cornerRadiusRight = it.animatedValue as Float
                }

                valueAnimatorLeft.start()
                valueAnimatorRight.start()
            }
            else -> {
                valueAnimatorLeft = ValueAnimator.ofFloat(cornerRadiusLeft, 0f)
                valueAnimatorLeft.duration = animationDuration
                valueAnimatorLeft.addUpdateListener {
                    cornerRadiusLeft = it.animatedValue as Float
                }

                valueAnimatorRight = ValueAnimator.ofFloat(cornerRadiusRight, 0f)
                valueAnimatorRight.duration = animationDuration
                valueAnimatorRight.addUpdateListener {
                    cornerRadiusRight = it.animatedValue as Float
                }

                valueAnimatorLeft.start()
                valueAnimatorRight.start()
            }
        }

        valueAnimator = ValueAnimator.ofFloat(selectedIndex, index.toFloat())
        valueAnimator.interpolator = DecelerateInterpolator()
        valueAnimator.duration = animationDuration
        valueAnimator.addUpdateListener {
            selectedIndex = it.animatedValue as Float
            calculateSelectionRect()
            invalidate()
        }
        valueAnimator.start()
    }

    private fun calculateSelectionRect() {
        selectionRect = RectF(
            selectedIndex * titleWidth.toFloat(),
            0f,
            (selectedIndex + 1) * titleWidth.toFloat(),
            height.toFloat()
        )
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), 200f, 200f, paint)

        selectionPath.addRoundRect(
            selectionRect,
            floatArrayOf(
                cornerRadiusLeft,
                cornerRadiusLeft,
                cornerRadiusRight,
                cornerRadiusRight,
                cornerRadiusRight,
                cornerRadiusRight,
                cornerRadiusLeft,
                cornerRadiusLeft
            ),
            Path.Direction.CW
        )
        canvas.drawPath(selectionPath, selectionPaint)
        selectionPath.reset()

        buttonItemList.forEachIndexed { index, buttonItem ->

            if (index != 0 && isDividerEnabled) {
                val dividerMargin = height / 8f
                canvas.drawLine(
                    index * titleWidth.toFloat(),
                    dividerMargin,
                    index * titleWidth.toFloat(),
                    height - dividerMargin,
                    dividerPaint
                )
            }

            val position = titleWidth / 2
            canvas.drawText(
                buttonItem.title,
                position + (index * titleWidth).toFloat(),
                fontHeight + height / 2,
                textPaint
            )
        }
    }
}