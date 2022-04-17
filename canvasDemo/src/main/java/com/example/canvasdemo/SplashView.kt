package com.example.canvasdemo

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.animation.doOnEnd
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class SplashView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)     //旋转园画笔
    private val mHolePaint = Paint(Paint.ANTI_ALIAS_FLAG)     //扩散圆画笔

    private var mValueAnimator: ValueAnimator? = null //属性动画

    private val mBackgroundColor = Color.WHITE     //背景色
    private val mCircleColors = resources.getIntArray(R.array.splash_circle_colors)     //小圆颜色数组

    // 旋转圆中心坐标
    private var mCentertX: Float = 0f
    private var mCentertY: Float = 0f
    private var mDistance: Float = 0f

    private val mCircleRadius = 18f    //6个小球的半径
    private val mRotateRadius = 90f    //旋转大圆的半径

    private val mAnimatorDuration = 600L // 动画时长
    private var mState: SplashState? = null

    private var mCurrentRotateAngle = 0f  //当前大圆旋转角度
    private var mCurrentRotateRadius = mRotateRadius // 当前大圆半径
    private var mCurrentHoleRadius = 0f  //扩散圆半径

    init {
        mHolePaint.style = Paint.Style.STROKE
        mHolePaint.color = mBackgroundColor
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCentertX = w / 2f
        mCentertY = h / 2f
        mDistance = (hypot(w.toDouble(), h.toDouble()) / 2).toFloat()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (mState == null) {
            mState = RotateState()
        }
        mState?.run {
            canvas ?: return
            onDrawState(canvas)
        }
    }

    private fun drawBackground(canvas: Canvas) {
        if (mCurrentHoleRadius > 0f) {
            // 绘制空心圆
            val strokeWidth = mDistance - mCurrentHoleRadius
            val radius = strokeWidth / 2 + mCurrentHoleRadius
            mHolePaint.strokeWidth = strokeWidth
            canvas.drawCircle(mCentertX, mCentertY, radius, mHolePaint)
        } else {
            canvas.drawColor(mBackgroundColor)
        }
    }

    private fun drawCircles(canvas: Canvas) {
        val rotateAngele = (Math.PI * 2 / mCircleColors.size).toFloat()
        for (i in mCircleColors.indices) {
            val angele = rotateAngele * i + mCurrentRotateAngle
            val cx = cos(angele) * mCurrentRotateRadius + mCentertX
            val cy = sin(angele) * mCurrentRotateRadius + mCentertY
            mPaint.color = mCircleColors[i]
            canvas.drawCircle(cx, cy, mCircleRadius, mPaint)
        }
    }

    // 动画三个过程
    // 小圆旋转
    // 扩散聚合
    // 水波纹
    abstract class SplashState {
        abstract fun onDrawState(canvas: Canvas)
    }

    inner class RotateState : SplashState() {
        init {
            mValueAnimator = ValueAnimator.ofFloat(0f, (Math.PI * 2).toFloat()).apply {
                repeatCount = 2
                interpolator = LinearInterpolator()
                duration = mAnimatorDuration
                addUpdateListener {
                    mCurrentRotateAngle = it.animatedValue as Float
                    invalidate()
                }
                doOnEnd {
                    mState = SpreadState()
                }
                start()
            }
        }

        override fun onDrawState(canvas: Canvas) {
            drawBackground(canvas)
            drawCircles(canvas)
        }
    }

    inner class SpreadState : SplashState() {
        init {
            mValueAnimator = ValueAnimator.ofFloat(mCircleRadius, mRotateRadius).apply {
                duration = mAnimatorDuration
                interpolator = OvershootInterpolator(10f)
                addUpdateListener {
                    mCurrentRotateRadius = animatedValue as Float
                    invalidate()
                }
                doOnEnd {
                    mState = ExpandState()
                }
                reverse()
            }
        }

        override fun onDrawState(canvas: Canvas) {
            drawBackground(canvas)
            drawCircles(canvas)
        }
    }

    inner class ExpandState : SplashState() {
        init {
            mValueAnimator = ValueAnimator.ofFloat(mCircleRadius, mDistance).apply {
                duration = mAnimatorDuration
                interpolator = LinearInterpolator()
                addUpdateListener {
                    mCurrentHoleRadius = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }

        override fun onDrawState(canvas: Canvas) {
            drawBackground(canvas)
        }
    }
}