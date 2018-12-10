package com.thebrodyaga.carmovement

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.os.Parcelable
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.arellomobile.mvp.MvpDelegate
import com.arellomobile.mvp.presenter.InjectPresenter
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.roundToInt


/**
 * Created by admin
 *         on 07/12/2018.
 */
class AnimatedPathView : View, MoxyView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    private lateinit var mParentDelegate: MvpDelegate<*>
    private val mMvpDelegate: MvpDelegate<*> by lazy {
        MvpDelegate(this).apply { setParentDelegate(mParentDelegate, id.toString()) }
    }

    @InjectPresenter
    lateinit var presenter: Presenter

    fun init(parentDelegate: MvpDelegate<*>) {
        mParentDelegate = parentDelegate
        mMvpDelegate.onCreate()
        mMvpDelegate.onAttach()
    }

    fun setCar(carModel: CarModel) {
        this.stateModel.carModel = carModel
        checkCar(this.stateModel.carModel)
    }

    override fun onDetachedFromWindow() {
        presenter.updateState(stateModel)
        super.onDetachedFromWindow()
        mMvpDelegate.onSaveInstanceState()
        mMvpDelegate.onDetach()
    }

    private val paint = Paint().apply {
        isAntiAlias = true
        color = Color.RED
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }
    private val bitmap: Bitmap = getBitmapFromVectorDrawable(context, R.drawable.ic_car_top)

    private val path1: Path = Path()
    private val pathMeasure1: PathMeasure = PathMeasure(path1, false)
    private val carModel1: CarModel = CarModel()

    private var stateModel = StateModel(Pair(530f, 600f), Path(), (bitmap.width / 2).toFloat(),
            (bitmap.height / 2).toFloat(), PathMeasure(path1, false),
            pathMeasure1.length, 1f, 0f, 0f, 0f, CarModel(),
            carModel1.carAngle + 90f, carModel1.carAngle + 90f,
            FloatArray(2), FloatArray(2), Matrix(), false)

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        stateModel.apply {
            //            checkCar(carModel, left, top, right, bottom)
            val angle = curAngle - 90f
            curY = carModel.center.second -
                    (Math.sin(Math.toRadians(angle.toDouble())) * carModel.radius).toFloat() -
                    offsetY
            curX = carModel.center.first -
                    (Math.cos(Math.toRadians(angle.toDouble())) * carModel.radius).toFloat() -
                    offsetX
            curMatrix.postRotate(curAngle, offsetX, offsetY)
            curMatrix.postTranslate(curX, curY)
        }
    }

    private var needSwipe = false
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        stateModel.apply {
            if (path.isEmpty) {
                canvas.drawBitmap(bitmap, curMatrix, null)
                canvas.drawCircle(carModel.center.first, carModel.center.second, carModel.radius, paint)
                return
            }

            canvas.drawPath(path, paint)
            curMatrix.reset()

            //проверка на задний ход
            needSwipe = ((curAngle - targetAngle).roundToInt().absoluteValue - 180).absoluteValue < 5
            curAngle = if (!needSwipe) targetAngle else targetAngle - 180

            if (distance < pathLength) {
                pathMeasure.getPosTan(distance, position, tan)

                targetAngle = (Math.atan2(tan[1].toDouble(), tan[0].toDouble()) * 180.0 / Math.PI)
                        .toFloat() + 180f
                curMatrix.postRotate(curAngle, offsetX, offsetY)

                curX = position[0] - offsetX
                curY = position[1] - offsetY

                curMatrix.postTranslate(curX, curY)

                canvas.drawBitmap(bitmap, curMatrix, null)

                distance += step

                invalidate()
            } else {
                isDrawing = false
                curMatrix.postRotate(curAngle, offsetX, offsetY)
                curMatrix.postTranslate(curX, curY)
                canvas.drawBitmap(bitmap, curMatrix, null)
                carModel.carAngle = curAngle - 90f
                val deltaY = carModel.radius * Math.sin(Math.toRadians(360.0 - curAngle - 90.0))
                val deltaX = carModel.radius * Math.cos(Math.toRadians(((360.0 - curAngle - 90.0))))
                pathMeasure.getPosTan(distance, position, tan)
                carModel.center = carModel.center.copy((position[0] - deltaX).toFloat(), (position[1] + deltaY).toFloat())

                val newPath = Path()
                        .apply { addCircle(carModel.center.first, carModel.center.second, carModel.radius, Path.Direction.CW) }
                canvas.drawPath(newPath, paint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        performClick()
        stateModel.apply {
            val action = event.action
            if (isDrawing) return true
            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    targetPoint = targetPoint.copy(event.x, event.y)
                    val isInCircle = checkPoint()
                    if (isInCircle) {//TODO
                        Toast.makeText(context, "Точка внутри окружности", Toast.LENGTH_SHORT).show()
                        isDrawing = false
                        return true
                    }
                    buildPath(carModel, targetPoint, path)
                    pathMeasure = PathMeasure(path, false)
                    pathLength = pathMeasure.length
                    curAngle = carModel.carAngle + 90f
                    targetAngle = carModel.carAngle + 90f
                    isDrawing = true
                    invalidate()
                }
            }
        }
        return true
    }

    override fun updateState(stateModel: StateModel) {
        Log.d("Tag", "updateState")
        this.stateModel = stateModel
    }

    override fun performClick(): Boolean = super.performClick()

    private fun checkPoint(): Boolean {
        val tarX = stateModel.targetPoint.first
        val tarY = stateModel.targetPoint.second
        val centerX = stateModel.carModel.center.first
        val centerY = stateModel.carModel.center.second
        return (tarX - centerX).pow(2) + (tarY - centerY).pow(2) <= stateModel.carModel.radius.pow(2)
    }

    private fun checkCar(carModel: CarModel, left: Int = 0, top: Int = 0, right: Int = width, bottom: Int = height) {
        if (right <= 0 || bottom <= 0) return
        val x = carModel.center.first
        val y = carModel.center.second
        val r = carModel.radius
        var newX = carModel.center.first
        var newY = carModel.center.second
        when {
            x < r - left -> newX = left + r
            x > right - r -> newX = right - r
            y < r - top -> newY = top + r
            y > bottom - r -> newY = bottom - r
        }
        carModel.center = carModel.center.copy(newX, newY)
    }

    data class CarModel(
            var carAngle: Float = 60f,
            var radius: Float = 150.0f,
            var center: Pair<Float, Float> = Pair(radius + 50, radius + 50))

    companion object {
        private const val STATE_FLAG = "STATE_FLAG"
        fun buildPath(carModel: CarModel, targetPoint: Pair<Float, Float>, path: Path/*, circlePath: Path? = null*/) {
            val carCenter = Pair(carModel.center.first.toDouble(), carModel.center.second.toDouble())
            val target = Pair(targetPoint.first.toDouble(), targetPoint.second.toDouble())
            val radius = carModel.radius
            val carAngle = carModel.carAngle
            //длина от цели до центра окружности авто
            val pointToCenter = Math.sqrt(
                    Math.pow(Math.abs(carCenter.first - target.first), 2.0) +
                            Math.pow(Math.abs(carCenter.second - target.second), 2.0))
            //длина кассательных
            val tangent = Math.sqrt(Math.pow(pointToCenter, 2.0) - Math.pow(radius.toDouble(), 2.0))
            //тангенс внутреннего угла
            val tgInnerCorner = radius.toDouble() / tangent
            //тангенс внутреннего угла и угла между ОХ и первой кассательной
            val tgFirstAndInner = (carCenter.second - target.second) / (carCenter.first - target.first)
            //внутренний угол в градусах
            val innerAngle = Math.toDegrees(Math.atan(tgInnerCorner))
            val firstAndInner = Math.toDegrees(Math.atan(tgFirstAndInner))
            //углы касательных
            val firstAngle = firstAndInner - innerAngle
            val secondAngle = firstAndInner + innerAngle
            //угловые коф. кассательных
            val firstTg = Math.tan(Math.toRadians(firstAngle))
            val secondTg = Math.tan(Math.toRadians(secondAngle))
            //коф. отступа касательных
            val b1 = target.second - firstTg * target.first
            val b2 = target.second - secondTg * target.first
            //точки касания
            val firstTangent = tangentPint(carCenter, firstTg, b1)
            val secondTangent = tangentPint(carCenter, secondTg, b2)

            val firstTangentAngle = getTangentAngle(firstTangent, carCenter, radius.toDouble())
            val secondTangentAngle = getTangentAngle(secondTangent, carCenter, radius.toDouble())
            val innerArc = doubleArrayOf(firstTangentAngle, secondTangentAngle).apply { sort() }


            //проверка на внутреннюю дугу
            val delta = Math.abs(innerArc[0] - innerArc[1])
            val isInArc = if (delta < 180.0)
                carAngle.toDouble() in innerArc[0]..innerArc[1]
            else {
                innerArc.reverse()
                carAngle.toDouble() in 0.0..innerArc[1] || carAngle.toDouble() in innerArc[0]..360.0
            }

            path.arcTo(carModel.center.first - radius, carModel.center.second - radius,
                    carModel.center.first + radius, carModel.center.second + radius, 180F + carAngle,
                    if (isInArc) {
                        val targetAngle = innerArc[0].toFloat()
                        -1 * when {
                            carAngle < targetAngle -> 360 - targetAngle + carAngle
                            carAngle > targetAngle -> carAngle - targetAngle
                            else -> 0f
                        }
                    } else {
                        val targetAngle = innerArc[0].toFloat()
                        when {
                            carAngle > targetAngle -> 360 - carAngle + targetAngle
                            carAngle < targetAngle -> targetAngle - carAngle
                            else -> 0f
                        }
                    }, false)
            path.lineTo(target.first.toFloat(), target.second.toFloat())
        }

        private fun getTangentAngle(tangentPoint: Pair<Double, Double>, carCenter: Pair<Double, Double>,
                                    radius: Double): Double {
            val x = tangentPoint.first
            val y = tangentPoint.second
            val carX = carCenter.first
            val carY = carCenter.second

            return when {
                x == carX && y > carY -> 270.0
                x == carX && y < carY -> 90.0
                y == carY && x > carX -> 180.0
                y == carY && x < carX -> 0.0

                x > carX && y > carY -> 180 + Math.toDegrees(Math.asin(Math.abs(y - carY) / radius))
                x < carX && y > carY -> 180 + Math.toDegrees(Math.PI - Math.asin(Math.abs(y - carY) / radius))

                x > carX && y < carY -> {
                    val result = Math.toDegrees(Math.asin(Math.abs(y - carY) / radius))
                    if (result.isNaN()) 179.999999
                    else 180 - result
                }
                x < carX && y < carY -> {
                    val result = Math.toDegrees(Math.PI - Math.asin(Math.abs(y - carY) / radius))
                    if (result.isNaN()) 359.9999999
                    else 180 - result
                }
                else -> 0.0
            }
        }

        /**
         * Решение системы уравнений(окружности и касательной)
         * [tg] - угловой коф. уравнения кас., [bKaf] - коф. смещения уравнения кас.
         */
        private fun tangentPint(carCenter: Pair<Double, Double>, tg: Double, bKaf: Double): Pair<Double, Double> {
            //коф. а квадратного уравнения
            val a = 1 + Math.pow(tg, 2.0)
            //коф. b квадратного уравнения
            val b = -2 * (carCenter.first - tg * bKaf + tg * carCenter.second)
            //корень один тк касательная
            val x = (-1 * b) / (2 * a)
            val y = x * tg + bKaf
            return Pair(x, y)
        }

        fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {
            val drawable = ContextCompat.getDrawable(context, drawableId)

            val bitmap = Bitmap.createBitmap(drawable!!.intrinsicWidth,
                    drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            return bitmap
        }
    }

    data class StateModel(
            var targetPoint: Pair<Float, Float>,
            var path: Path,
            var offsetX: Float,
            var offsetY: Float,
            var pathMeasure: PathMeasure,
            var pathLength: Float,
            var step: Float,
            var distance: Float,
            var curX: Float,
            var curY: Float,
            var carModel: CarModel,
            var curAngle: Float,
            var targetAngle: Float,
            var position: FloatArray,
            var tan: FloatArray,
            var curMatrix: Matrix,
            var isDrawing: Boolean) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as StateModel

            if (path != other.path) return false
            if (offsetX != other.offsetX) return false
            if (offsetY != other.offsetY) return false
            if (pathMeasure != other.pathMeasure) return false
            if (pathLength != other.pathLength) return false
            if (step != other.step) return false
            if (distance != other.distance) return false
            if (curX != other.curX) return false
            if (curY != other.curY) return false
            if (carModel != other.carModel) return false
            if (curAngle != other.curAngle) return false
            if (targetAngle != other.targetAngle) return false
            if (!Arrays.equals(position, other.position)) return false
            if (!Arrays.equals(tan, other.tan)) return false
            if (curMatrix != other.curMatrix) return false
            if (isDrawing != other.isDrawing) return false

            return true
        }

        override fun hashCode(): Int {
            var result = path.hashCode()
            result = 31 * result + offsetX.hashCode()
            result = 31 * result + offsetY.hashCode()
            result = 31 * result + pathMeasure.hashCode()
            result = 31 * result + pathLength.hashCode()
            result = 31 * result + step.hashCode()
            result = 31 * result + distance.hashCode()
            result = 31 * result + curX.hashCode()
            result = 31 * result + curY.hashCode()
            result = 31 * result + carModel.hashCode()
            result = 31 * result + curAngle.hashCode()
            result = 31 * result + targetAngle.hashCode()
            result = 31 * result + Arrays.hashCode(position)
            result = 31 * result + Arrays.hashCode(tan)
            result = 31 * result + curMatrix.hashCode()
            result = 31 * result + isDrawing.hashCode()
            return result
        }

        fun update(stateModel: StateModel) {
            targetPoint = stateModel.targetPoint
            path = stateModel.path
            offsetX = stateModel.offsetX
            offsetY = stateModel.offsetY
            pathMeasure = stateModel.pathMeasure
            pathLength = stateModel.pathLength
            step = stateModel.step
            distance = stateModel.distance
            curX = stateModel.curX
            curY = stateModel.curY
            carModel = stateModel.carModel
            curAngle = stateModel.curAngle
            targetAngle = stateModel.targetAngle
            position = stateModel.position
            tan = stateModel.tan
            curMatrix = stateModel.curMatrix
            isDrawing = stateModel.isDrawing
        }
    }
}