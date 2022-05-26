package com.example.canvassplit

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.pow
import kotlin.random.Random

class BezierView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val mPaint = Paint()
    private val mLinePoint = Paint()
    private val mPath = Path()
    private val mControlPoints = mutableListOf<PointF>() // 控制点集合

    init {
        mPaint.apply {
            isAntiAlias = true
            strokeWidth = 4f
            style = Paint.Style.STROKE
            color = Color.RED
        }
        mLinePoint.apply {
            isAntiAlias = true
            strokeWidth = 4f
            style = Paint.Style.STROKE
            color = Color.GRAY
        }
        init()
    }

    private fun init() {
        mControlPoints.clear()
        for (i in 0 until 9) {
            val x = Random.nextInt(800) + 200f
            val y = Random.nextInt(800) + 200f
            val pointF = PointF(x, y)
            mControlPoints.add(pointF)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mControlPoints.forEachIndexed { index, pointF ->
            if (index > 0) {
                mLinePoint.color = Color.GRAY
                // 绘制控制点连线
                canvas.drawLine(
                    mControlPoints[index - 1].x,
                    mControlPoints[index - 1].y,
                    pointF.x,
                    pointF.y,
                    mLinePoint
                )
            }
            // 起点、终点换颜色
            if (index == 0) {
                mLinePoint.color = Color.RED
            } else if (index == mControlPoints.size - 1) {
                mLinePoint.color = Color.BLUE
            }
            canvas.drawCircle(pointF.x, pointF.y, 10f, mLinePoint)
        }
        // 曲线连接
        //buildBezierPoints()
        calculate()
        canvas.drawPath(mPath, mPaint)
    }

    private fun calculate() {
        mPath.reset()
        val number = mControlPoints.size // 控制点个数
        if (number < 2) {
            return
        }
        val pointFs = mutableListOf<PointF>()
        // 计算杨辉三角
        val mi = IntArray(number)
        mi[0] = 1.also { mi[1] = it }
        for (i in 3..number) {
            // 得到上一层数据
            val t = IntArray(i - 1)
            for (j in t.indices) {
                t[j] = mi[j]
            }
            // 计算当前行的数据
            mi[0] = 1.also { mi[i - 1] = 1 }
            for (j in 0 until i - 2) {
                mi[j + 1] = t[j] + t[j + 1]
            }
        }
        // 计算坐标点
        for (i in 0 until 1000) {
            val t :Float = i / 1000f
            // 分别计算x、y
            // 计算各项和(n)
            val pointF = PointF()
            for (j in 0 until number) {
                pointF.x += mi[j] * mControlPoints[j].x * (1 - t).pow(number - 1 - j) * t.pow(j)
                pointF.y += mi[j] * mControlPoints[j].y * (1 - t).pow(number - 1 - j) * t.pow(j)
            }
            pointFs.add(pointF)
            if (i == 0) {
                mPath.moveTo(pointFs[0].x, pointFs[0].y)
            } else {
                mPath.lineTo(pointF.x, pointF.y)
            }
        }
    }

    private fun buildBezierPoints() {
        mPath.reset()
        val pointFs = mutableListOf<PointF>()
        val order = mControlPoints.size - 1 // 阶数
        val delta = 1.0f / 1000 // 份数
        var t = 0f
        while (t <= 1) {
            t += delta
            // bezier点集
            val pointF = PointF(deCastelJau(order, 0, t, true), deCastelJau(order, 0, t, false))
            pointFs.add(pointF)
            if (pointFs.size == 1) {
                mPath.moveTo(pointFs[0].x, pointFs[0].y)
            } else {
                mPath.lineTo(pointF.x, pointF.y)
            }
        }

    }

    /**
     * 计算在曲线上的坐标
     * p(i,j) = (1-t) * p(i-1,j) + t * p(i-1,j+1)
     * [i] 阶数
     * [j] 控制点
     * [t] 时间
     * [calculateX] 计算那个坐标值 true = x
     */
    private fun deCastelJau(i: Int, j: Int, t: Float, calculateX: Boolean): Float {
        return if (i == 1) {
            if (calculateX) {
                (1 - t) * mControlPoints[j].x + t * mControlPoints[j + 1].x
            } else {
                (1 - t) * mControlPoints[j].y + t * mControlPoints[j + 1].y
            }
        } else {
            (1 - t) * deCastelJau(i - 1, j, t, calculateX) +
                    t * deCastelJau(i - 1, j + 1, t, calculateX)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            init()
            invalidate()
        }
        return super.onTouchEvent(event)
    }
}