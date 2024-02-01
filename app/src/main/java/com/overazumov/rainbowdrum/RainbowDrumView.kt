package com.overazumov.rainbowdrum

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.graphics.drawable.toBitmap
import com.squareup.picasso.Picasso
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class RainbowDrumView(
    context: Context,
    attributeSet: AttributeSet
) : View(context, attributeSet) {
    private val colors = listOf(
        Color.rgb(128, 0, 128),
        Color.BLUE,
        Color.rgb(0, 191, 255),
        Color.GREEN,
        Color.YELLOW,
        Color.rgb(255, 165, 0),
        Color.RED,
    )
    private val words = listOf(
        "Believe", "Challenge", "Determination", "Growth", "Hope", "Inspiration", "Motivation",
        "Perseverance", "Resilience", "Optimism", "Ambition", "Tenacity", "Courage", "Excellence"
    )

    private val startAngle = 90f
    private val sectorAngle = 360f / colors.size

    private val paint = Paint()
    private val strokePaint = Paint()

    private var executor: ExecutorService? = null
    private var bitmap: Bitmap? = null
    private var text: String? = null

    private var alpha = 0.5f
    private var isRotates = false
    private var currentAngeSpeed = 0f
    private var currentAngle = startAngle


    init {
        initClickListeners()
        initPaint()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColoredCircle(
            width / 2f,
            height / 2f,
            minOf(width, height) / 2f
        )
        canvas.drawArrow(
            width / 2f,
            minOf(width, height) / 16f
        )
        if (text != null) {
            canvas.drawCircularText(
                width / 2f,
                height / 2f,
                minOf(width, height) / 5f,
                minOf(width, height) / 16f
            )
        }
        if (bitmap != null) {
            canvas.drawCircularBitmap(
                width / 2f,
                height / 2f,
                minOf(width, height) / 5f
            )
        }
        if (isRotates) {
            currentAngle = (currentAngle + currentAngeSpeed) % 360
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = getBitmap().width + paddingLeft + paddingRight
        val height = getBitmap().height + paddingTop + paddingBottom
        setMeasuredDimension(
            resolveSizeAndState(
                (width * alpha).toInt(),
                (widthMeasureSpec * alpha).toInt(),
                0
            ),
            resolveSizeAndState(
                (height * alpha).toInt(),
                (maxOf(widthMeasureSpec, heightMeasureSpec) * alpha).toInt(),
                0
            )
        )
    }

    fun changeSize(progress: Int) {
        alpha = progress / 100f
        requestLayout()
    }

    private fun initClickListeners() {
        setOnClickListener {
            if (!isRotates) {
                rotateDrum(Random.nextInt(4, 12))
                invalidate()
            }
        }
        setOnLongClickListener {
            reset()
            true
        }
    }

    private fun rotateDrum(angleSpeed: Int) {
        isRotates = true
        executor = Executors.newSingleThreadExecutor()
        executor?.execute {
            for (speed in (angleSpeed * 10) downTo 0 step 1) {
                if (!executor!!.isShutdown) {
                    Thread.sleep(100)
                    currentAngeSpeed = speed / 10f
                }
            }
            if (!executor!!.isShutdown) performSectorResponse()
            isRotates = false
        }
    }

    private fun performSectorResponse() {
        val sectorIndex = ((currentAngle - startAngle) / sectorAngle).toInt()
        if (sectorIndex % 2 == 0) initText() else initBitmap()
    }

    private fun reset() {
        if (executor != null) executor?.shutdown()
        text = null
        bitmap = null
        invalidate()
    }

    private fun initText() {
        text = getText()
//        text = "Determination"
        bitmap = null
        invalidate()
    }

    private fun initBitmap() {
        bitmap = getBitmap()
        text = null
        invalidate()
    }

    private fun getText() = words.random()

    private fun getBitmap() = try {
        Picasso.get().load("https://loremflickr.com/320/320").get()
    } catch (e: Exception) {
        BitmapFactory.decodeResource(resources, R.drawable.image)
    }

    private fun initPaint() {
        paint.style = Paint.Style.FILL
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 2f
        strokePaint.color = Color.BLACK
    }

    private fun Canvas.drawColoredCircle(cx: Float, cy: Float, radius: Float) {
        for (i in colors.indices) {
            paint.color = colors[i]
            drawArc(
                cx - radius,
                cy - radius,
                cx + radius,
                cy + radius,
                sectorAngle * i + currentAngle,
                sectorAngle,
                true,
                paint
            )
            drawArc(
                cx - radius + 1,
                cy - radius + 1,
                cx + radius - 1,
                cy + radius - 1,
                sectorAngle * i + currentAngle,
                sectorAngle,
                true,
                strokePaint
            )
        }
    }

    private fun Canvas.drawArrow(cx: Float, arrowHeight: Float) {
        val height = (height + minOf(width, height)) / 2
        val path = Path().apply{
            moveTo(cx, height - arrowHeight)
            lineTo(cx - arrowHeight, height - 1f)
            lineTo(cx, height - arrowHeight / 5)
            lineTo(cx + arrowHeight, height - 1f)
            close()
        }
        paint.color = Color.WHITE
        drawPath(path, paint)
        drawPath(path, strokePaint)
    }

    private fun Canvas.drawCircularText(cx: Float, cy: Float, radius: Float, textSize: Float) {
        drawCircle(cx, cy, radius, paint)
        paint.color = Color.BLACK
        paint.textSize = textSize
        val textWidth = paint.measureText(text)
        drawText(text!!, cx - textWidth / 2, cy + paint.textSize / 2, paint)
        drawCircle(cx, cy, radius, strokePaint)
    }

    private fun Canvas.drawCircularBitmap(cx: Float, cy: Float, radius: Float) {
        val roundedBitmap = RoundedBitmapDrawableFactory.create(resources, bitmap)
        val dst = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        roundedBitmap.isCircular = true
        drawBitmap(roundedBitmap.toBitmap(), null, dst, paint)
        drawCircle(cx, cy, radius, strokePaint)
    }
}


