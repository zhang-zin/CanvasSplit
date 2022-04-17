package com.example.canvassplit

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.ceil
import kotlin.math.pow

class SplitView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val mPaint: Paint = Paint()
    private val mBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.mipmap.pic)
    private val d = 3f //粒子直径
    private val mAnimator: ValueAnimator
    private val mBalls: MutableList<Ball> = ArrayList()

    init {
        for (i in 0 until mBitmap.width) {
            for (j in 0 until mBitmap.height) {
                //速度(-20,20)
                val vX = ((-1.0).pow(ceil(Math.random() * 1000)) * 20 * Math.random()).toFloat()
                val vY = rangInt(-15, 35)
                val ball = Ball(
                    mBitmap.getPixel(i, j),
                    i * d + d / 2,
                    j * d + d / 2,
                    d / 2,
                    vX,
                    vY,
                    0f,
                    0.98f
                )
                mBalls.add(ball)
            }
        }
        mAnimator = ValueAnimator.ofFloat(0f, 1f)
        //mAnimator.repeatCount = -1
        mAnimator.duration = 2000
        mAnimator.interpolator = LinearInterpolator()
        mAnimator.interpolator = LinearInterpolator()
        mAnimator.addUpdateListener {
            //it.reverse()
            updateBall()
            invalidate()
        }
//        mAnimator.addListener(onEnd = {
//            mAnimator.reverse()
//        })
    }


    private fun rangInt(i: Int, j: Int): Float {
        val max = i.coerceAtLeast(j)
        val min = i.coerceAtMost(j) - 1
        //在0到(max - min)范围内变化，取大于x的最小整数 再随机
        return (min + ceil(Math.random() * (max - min))).toFloat()
    }

    private fun updateBall() {
        //更新粒子的位置
        for (ball in mBalls) {
            ball.x += ball.vX
            ball.y += ball.vY
            ball.vX += ball.aX
            ball.vY += ball.aY
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.translate(500f, 500f)
        for (ball in mBalls) {
            mPaint.color = ball.color
            canvas.drawCircle(ball.x, ball.y, ball.r, mPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            //执行动画
            mAnimator.start()
        }
        return super.onTouchEvent(event)
    }

}