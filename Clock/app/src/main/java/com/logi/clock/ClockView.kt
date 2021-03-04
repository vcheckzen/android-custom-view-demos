package com.logi.clock

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin

private const val CLOCK_RADIUS = 300F

class ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var factor = 0F
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private lateinit var origin: PointF
    private var angleHour: Float = 0.0F
    private var angleMinute: Float = 0.0F
    private var angleSecond: Float = 0.0F
    private var secondPointerColor = 0
    private var contentColor = 0
    private var scaleColor = 0
    private var canvasBackgroundColor = 0

    init {
        context.withStyledAttributes(attrs, R.styleable.ClockView) {
            secondPointerColor = getColor(R.styleable.ClockView_secondPointerColor, Color.RED)
            contentColor = getColor(R.styleable.ClockView_contentColor, Color.WHITE)
            scaleColor = getColor(R.styleable.ClockView_scaleColor, Color.GRAY)
            canvasBackgroundColor = getColor(R.styleable.ClockView_backgroundColor, Color.BLACK)
        }

        fixedRateTimer(initialDelay = 1000, period = 1000) {
            updateTime()
            postInvalidate()
        }
    }

    private fun updateTime() {
        val calendar = GregorianCalendar()
        val secondsPassedToday = calendar.get(Calendar.HOUR) * 3600 +
                calendar.get(Calendar.MINUTE) * 60 +
                calendar.get(Calendar.SECOND) +
                round(
                    (calendar.get(Calendar.MILLISECOND) / 1000).toDouble()
                )

        angleHour = (secondsPassedToday / 120 % 360).toFloat()
        angleMinute = (secondsPassedToday / 10 % 360).toFloat()
        angleSecond = (secondsPassedToday * 6 % 360).toFloat()
    }

    private fun drawCircle(canvas: Canvas, color: Int, radius: Float) {
        paint.color = color
        canvas.drawCircle(origin.x, origin.y, radius * factor, paint)
    }

    private fun drawAndRotateRoundRect(
        canvas: Canvas,
        color: Int,
        width: Float,
        height: Float,
        angle: Float,
        deltaY: Float = 0F,
        rounded: Boolean = false
    ) {
        val realWidth = width * factor
        val realHeight = height * factor
        val radius = realWidth / 2
        val left = origin.x - radius
        val top = origin.y + radius + deltaY * factor
        val right = left + realWidth
        val bottom = top - realHeight
        paint.color = color

        canvas.save()
        canvas.translate(origin.x, origin.y)
        canvas.rotate(angle)
        canvas.translate(-origin.x, -origin.y)

        if (rounded) {
            path.rewind()
            path.addRoundRect(RectF(left, top, right, bottom), radius, radius, Path.Direction.CW)
            canvas.drawPath(path, paint)
        } else {
            canvas.drawRect(left, top, right, bottom, paint)
        }

        canvas.restore()
    }

    private fun positionNum(
        canvas: Canvas,
        color: Int,
        prefix: String,
        num: Int,
        fontSize: Float,
        radius: Float,
        angleUnit: Float
    ) {
        val realFontSize = fontSize * factor
        val realRadius = radius * factor
        val radians = num * angleUnit * Math.PI / 180
        val halfWidth = realFontSize / 2
        val delta = 4
        val x = origin.x - halfWidth - delta + realRadius * sin(radians)
        val y = origin.y + halfWidth - delta - realRadius * cos(radians)
        val text = if (num < 10) prefix + num else num
        paint.textSize = realFontSize
        paint.color = color
        canvas.drawText(text.toString(), x.toFloat(), y.toFloat(), paint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        factor = min(width, height).toFloat() / 2 / (CLOCK_RADIUS + 20)
        origin = PointF(width.toFloat() / 2, height.toFloat() / 2)
        updateTime()
    }

    override fun onDraw(canvas: Canvas) {
        // background
        canvas.drawColor(canvasBackgroundColor, PorterDuff.Mode.CLEAR)

        // scale line
        for (i in 1..60) {
            if (i % 5 != 0) drawAndRotateRoundRect(
                canvas,
                scaleColor,
                3F,
                30F,
                i * 6F,
                CLOCK_RADIUS
            ) else {
                positionNum(canvas, contentColor, "0", i, 35F, 285F, 6F)
            }
        }

        // scale text
        for (i in 1..12) positionNum(canvas, contentColor, " ", i, 70F, 220F, 30F)

        // circle large
        drawCircle(canvas, contentColor, 9F)

        // hour
        drawAndRotateRoundRect(canvas, contentColor, 4F, 40F, angleHour)
        drawAndRotateRoundRect(canvas, contentColor, 16F, 130F, angleHour, -40F, true)

        // minute
        drawAndRotateRoundRect(canvas, contentColor, 4F, 40F, angleMinute)
        drawAndRotateRoundRect(canvas, contentColor, 16F, 210F, angleMinute, -40F, true)

        // circle medium
        drawCircle(canvas, secondPointerColor, 6F)

        // second
        drawAndRotateRoundRect(canvas, secondPointerColor, 4F, CLOCK_RADIUS + 43, angleSecond, 40F)

        // circle small
        drawCircle(canvas, canvasBackgroundColor, 3F)
    }
}