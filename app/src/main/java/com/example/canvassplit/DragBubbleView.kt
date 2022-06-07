package com.example.canvassplit

import android.animation.PointFEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.animation.doOnEnd
import kotlin.math.hypot

class DragBubbleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val BUBBLE_STATE_DEFAULT = 0  //气泡默认状态--静止
        private const val BUBBLE_STATE_CONNECT = 1  //气泡相连
        private const val BUBBLE_STATE_APART = 2    //气泡分离
        private const val BUBBLE_STATE_DISMISS = 3  //气泡消失
    }

    private var mBubbleState = BUBBLE_STATE_DEFAULT

    private val mBubFixedCenter: PointF = PointF()    //不动气泡的圆心
    private var mBubMovableCenter: PointF = PointF()  //可动气泡的圆心

    private var mBubbleRadius = 0f              // 气泡半径
    private var mBubbleColor = Color.RED        // 气泡颜色
    private var mBubbleText = ""                // 气泡文字
    private var mBubbleTextSize = 0f            // 气泡文字大小
    private var mBubbleTextColor = Color.WHITE  // 气泡文字大小
    private val mTextRect = Rect()
    private var mDist: Float = 0f
    private val mMaxDist: Float
    private val MOVE_OFFSET: Float

    private var mBubFixedRadius = 0f     //不可动气泡半径
    private var mBubMovableRadius = 0f   //可动气泡半径

    private val mBurstBitmapsArray: Array<Bitmap?>  // 气泡爆炸的bitmap数组
    private var mCurDrawableIndex = 0              // 当前气泡爆炸图片index
    private val mBurstRect: Rect = Rect()         //爆炸绘制区域

    /**
     * 气泡爆炸的图片id数组
     */
    private val mBurstDrawablesArray = intArrayOf(
        R.mipmap.burst_1,
        R.mipmap.burst_2,
        R.mipmap.burst_3,
        R.mipmap.burst_4,
        R.mipmap.burst_5
    )


    private val mBubblePaint by lazy {
        // 气泡画笔
        Paint(Paint.ANTI_ALIAS_FLAG)
    }
    private val mBubbleTextPaint by lazy {
        // 气泡文字画笔
        Paint(Paint.ANTI_ALIAS_FLAG)
    }
    private val mBurstPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
        }
    }

    private val mBezierPath = Path()   //贝塞尔曲线路径

    init {
        val array =
            context.obtainStyledAttributes(attrs, R.styleable.DragBubbleView, defStyleAttr, 0)
        mBubbleRadius =
            array.getDimension(R.styleable.DragBubbleView_bubble_radius, 12.dp.toFloat())
        mBubbleColor = array.getColor(R.styleable.DragBubbleView_bubble_color, Color.RED)
        mBubbleText = array.getString(R.styleable.DragBubbleView_bubble_text).toString()
        mBubbleTextSize =
            array.getDimension(R.styleable.DragBubbleView_bubble_textSize, 12.dp.toFloat())
        mBubbleTextColor = array.getColor(R.styleable.DragBubbleView_bubble_textColor, Color.WHITE)
        array.recycle()
        mMaxDist = mBubbleRadius * 8
        MOVE_OFFSET = mMaxDist / 4

        mBubFixedRadius = mBubbleRadius.apply {
            mBubMovableRadius = this
        }

        mBubblePaint.color = mBubbleColor
        mBubblePaint.style = Paint.Style.FILL
        mBubbleTextPaint.color = mBubbleTextColor
        mBubbleTextPaint.textSize = mBubbleTextSize
        mBubbleTextPaint.textAlign = Paint.Align.CENTER
        mBurstBitmapsArray = arrayOfNulls(mBurstDrawablesArray.size)
        for (i in mBurstDrawablesArray.indices) {
            //将气泡爆炸的drawable转为bitmap
            val bitmap = BitmapFactory.decodeResource(resources, mBurstDrawablesArray[i])
            mBurstBitmapsArray[i] = bitmap
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mBubFixedCenter.set(w / 2f, h / 2f)
        mBubMovableCenter.set(w / 2f, h / 2f)

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //1，静止状态，一个气泡加消息数据

        //2, 连接状态，一个气泡加消息数据，贝塞尔曲线，本身位置上气泡，大小可变化

        //3，分离状态，一个气泡加消息数据

        //4，消失状态，爆炸效果
        if (mBubbleState == BUBBLE_STATE_CONNECT) {
            //绘制不动气泡
            canvas.drawCircle(
                mBubFixedCenter.x,
                mBubFixedCenter.y,
                mBubFixedRadius,
                mBubblePaint
            )
            //绘制贝塞尔曲线
            //控制点坐标
            val iAnchorX = ((mBubFixedCenter.x + mBubMovableCenter.x) / 2)
            val iAnchorY = ((mBubFixedCenter.y + mBubMovableCenter.y) / 2)

            val sinTheta = (mBubMovableCenter.y - mBubFixedCenter.y) / mDist
            val cosTheta = (mBubMovableCenter.x - mBubFixedCenter.x) / mDist
            //B
            val iBubMovableStartX = mBubMovableCenter.x + sinTheta * mBubMovableRadius
            val iBubMovableStartY = mBubMovableCenter.y - cosTheta * mBubMovableRadius
            //A
            val iBubFixedEndX = mBubFixedCenter.x + mBubFixedRadius * sinTheta
            val iBubFixedEndY = mBubFixedCenter.y - mBubFixedRadius * cosTheta
            //D
            val iBubFixedStartX = mBubFixedCenter.x - mBubFixedRadius * sinTheta
            val iBubFixedStartY = mBubFixedCenter.y + mBubFixedRadius * cosTheta
            //C
            val iBubMovableEndX = mBubMovableCenter.x - mBubMovableRadius * sinTheta
            val iBubMovableEndY = mBubMovableCenter.y + mBubMovableRadius * cosTheta

            mBezierPath.reset()
            mBezierPath.moveTo(iBubFixedStartX, iBubFixedStartY)
            mBezierPath.quadTo(iAnchorX, iAnchorY, iBubMovableEndX, iBubMovableEndY)
            //移动到B点
            mBezierPath.lineTo(iBubMovableStartX, iBubMovableStartY)
            mBezierPath.quadTo(iAnchorX, iAnchorY, iBubFixedEndX, iBubFixedEndY)
            mBezierPath.close()
            canvas.drawPath(mBezierPath, mBubblePaint)
        }
        if (mBubbleState != BUBBLE_STATE_DISMISS) {
            canvas.drawCircle(
                mBubMovableCenter.x,
                mBubMovableCenter.y,
                mBubMovableRadius,
                mBubblePaint
            )
            mBubbleTextPaint.getTextBounds(mBubbleText, 0, mBubbleText.length, mTextRect)
            canvas.drawText(
                mBubbleText,
                mBubMovableCenter.x,
                mBubMovableCenter.y + mTextRect.height() / 2,
                mBubbleTextPaint
            )
        }

        if (mBubbleState == BUBBLE_STATE_DISMISS && mCurDrawableIndex < mBurstBitmapsArray.size) {
            mBurstRect.set(
                (mBubMovableCenter.x - mBubMovableRadius).toInt(),
                (mBubMovableCenter.y - mBubMovableRadius).toInt(),
                (mBubMovableCenter.x + mBubMovableRadius).toInt(),
                (mBubMovableCenter.y + mBubMovableRadius).toInt()
            )
            mBurstBitmapsArray[mCurDrawableIndex]?.run {
                canvas.drawBitmap(
                    this,
                    null,
                    mBurstRect,
                    mBubblePaint
                )
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (mBubbleState != BUBBLE_STATE_DISMISS) {
                    mDist = hypot(
                        (event.x - mBubFixedCenter.x).toDouble(),
                        (event.y - mBubFixedCenter.y).toDouble()
                    ).toFloat()
                    mBubbleState = if (mDist < mMaxDist + MOVE_OFFSET) {
                        BUBBLE_STATE_CONNECT
                    } else {
                        BUBBLE_STATE_DEFAULT
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (mBubbleState != BUBBLE_STATE_DEFAULT) {
                    mDist = hypot(
                        (event.x - mBubFixedCenter.x).toDouble(),
                        (event.y - mBubFixedCenter.y).toDouble()
                    ).toFloat()
                    mBubMovableCenter.x = event.x
                    mBubMovableCenter.y = event.y
                    if (mBubbleState == BUBBLE_STATE_CONNECT) {
                        if (mDist < mMaxDist - MOVE_OFFSET) { //当拖拽的距离在指定范围内，那么调整不动气泡的半径
                            mBubFixedRadius = mBubbleRadius - mDist / 8
                        } else {
                            mBubbleState = BUBBLE_STATE_APART //当拖拽的距离超过指定范围，那么改成分离状态
                        }
                    }
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (mBubbleState == BUBBLE_STATE_CONNECT) {
                    //橡皮筋动画效果
                    startBubbleRestAnim()
                } else if (mBubbleState == BUBBLE_STATE_APART) {
                    if (mDist < 2 * mBubbleRadius) {
                        startBubbleRestAnim()
                    } else {
                        //爆炸效果
                        startBubbleBurstAnim()
                    }
                }
            }
        }
        return true
    }

    private fun startBubbleBurstAnim() {
        mBubbleState = BUBBLE_STATE_DISMISS
        val anim = ValueAnimator.ofInt(0, mBurstBitmapsArray.size)
        anim.duration = 500
        anim.interpolator = LinearInterpolator()
        anim.addUpdateListener { animation ->
            mCurDrawableIndex = animation.animatedValue as Int
            invalidate()
        }
        anim.start()
    }

    private fun startBubbleRestAnim() {
        val animator = ValueAnimator.ofObject(PointFEvaluator(), mBubMovableCenter, mBubFixedCenter)
        animator.duration = 200L
        animator.interpolator = OvershootInterpolator(5f)
        animator.addUpdateListener {
            mBubMovableCenter = animator.getAnimatedValue() as PointF
            invalidate()
        }
        animator.doOnEnd {
            mBubbleState = BUBBLE_STATE_DEFAULT
        }
        animator.start()
    }

    val Int.dp: Int
        get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            Resources.getSystem().displayMetrics
        ).toInt()
}