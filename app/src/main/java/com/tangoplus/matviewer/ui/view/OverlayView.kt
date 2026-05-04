package com.tangoplus.matviewer.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt
import com.tangoplus.matviewer.domain.util.MathUtil.calculateSlope
import com.tangoplus.matviewer.domain.vision.PoseLandmarkResult
import com.tangoplus.matviewer.domain.vo.ButtonState
import kotlin.math.hypot

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
	// 선 흔들림 감소
	private val smoothedLandmarks = mutableMapOf<Int, PointF>()
	private val ALPHA = 0.4f
	private fun getSmoothedPoint(index: Int, rawX: Float, rawY: Float): PointF {
		val prev = smoothedLandmarks[index]

		if (prev == null) {
			// 첫 프레임이면 일단 그대로 저장
			val firstPoint = PointF(rawX, rawY)
			smoothedLandmarks[index] = firstPoint
			return firstPoint
		}

		// 핵심 로직: 이전 값과 현재 값의 비율을 섞어서 부드럽게 렌더링
		val smoothedX = prev.x + ALPHA * (rawX - prev.x)
		val smoothedY = prev.y + ALPHA * (rawY - prev.y)

		prev.set(smoothedX, smoothedY) // 다음 프레임을 위해 저장
		return prev
	}

	private var horizonWidth = 0f
	private var results: PoseLandmarkResult? = null

	private var lineBgPaint = Paint()
	private var lineAccentPaint = Paint()
	private var circleBgPaint = Paint()
	private var circleAccentPaint = Paint()

	private val panelPaint = Paint()
	private val textPaint = Paint()
	private val normalStatusPaint = Paint()
	private val warningStatusPaint = Paint()

	private var scaleFactorX: Float = 1f
	private var scaleFactorY : Float = 1f
	private var imageWidth: Int = 1
	private var imageHeight: Int = 1
	private var currentRunningMode: RunningMode = RunningMode.IMAGE
	private var currentBtnState : ButtonState = ButtonState.CENTER
	init {
		initPaints()
	}

	@SuppressLint("ResourceAsColor")
	private fun initPaints() {
		// -----! 연결선 색 !-----
		lineBgPaint.apply {
			color = "#FFFFFF".toColorInt()
			strokeWidth = 7f
			style = Paint.Style.STROKE
		}
		lineAccentPaint.apply {
			color = "#0DFF00".toColorInt()
			strokeWidth = 3f
			style = Paint.Style.STROKE
		}
		circleBgPaint.apply {
			color = "#FFFFFF".toColorInt()
			strokeWidth = 3f
			style = Paint.Style.STROKE
		}
		circleAccentPaint.apply {
			color = "#0DFF00".toColorInt()
			strokeWidth = 3f
			style = Paint.Style.FILL_AND_STROKE
		}
		panelPaint.apply {
			color = Color.BLACK
			alpha = 80 // 0~255 사이 값. 128은 약 50% 투명도
			style = Paint.Style.FILL
			isAntiAlias = true
		}
		textPaint.apply {
			color = Color.WHITE
			textSize = 36f // 태블릿 화면에 맞춘 크기 (필요시 조절)
			typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
			isAntiAlias = true
		}
		normalStatusPaint.apply {
			color = "#00E676".toColorInt() // 트렌디한 민트 그린
			textSize = 32f // 태블릿 화면에 맞춘 크기 (필요시 조절)
			typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
			isAntiAlias = true
		}
		warningStatusPaint.apply {
			color = "#FF5252".toColorInt() // 시인성 높은 코랄 레드
			textSize = 32f // 태블릿 화면에 맞춘 크기 (필요시 조절)
			typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
			isAntiAlias = true
		}
	}

	fun setResults(
		poseLandmarkResult: PoseLandmarkResult,
		imageWidth: Int,
		imageHeight: Int,
		runningMode: RunningMode = RunningMode.IMAGE,
	) {
		results = poseLandmarkResult
		this.imageHeight = imageHeight
		this.imageWidth = imageWidth
		currentRunningMode = runningMode
		scaleFactorX = maxOf(width * 1f / imageWidth, height * 1f / imageHeight)
		scaleFactorY = maxOf(width * 1f / imageWidth, height * 1f / imageHeight)
		invalidate()
	}

	override fun draw(canvas: Canvas) {
		super.draw(canvas)
		results?.let { poseLandmarkResult ->
			drawLandmarks(canvas, poseLandmarkResult.landmarks)
		}
	}
	enum class RunningMode {
		IMAGE, VIDEO, LIVE_STREAM
	}

	private fun drawLandmarks(canvas: Canvas, landmarks: List<PoseLandmarkResult.PoseLandmark>) {
		if (landmarks.isEmpty()) {
			return
		}

		when (currentBtnState) {
			ButtonState.CENTER -> {
				val offsetX = (width - imageWidth * scaleFactorX) / 2
				val offsetY = (height - imageHeight * scaleFactorY) / 2

				val l11 = landmarks.getOrNull(11)
				val l12 = landmarks.getOrNull(12)
				val l15 = landmarks.getOrNull(15)
				val l16 = landmarks.getOrNull(16)
				val l23 = landmarks.getOrNull(23)
				val l24 = landmarks.getOrNull(24)


				if (l11 != null && l12 != null) {
					val midShoulderX = ((l11.x + l12.x) / 2) * imageWidth * scaleFactorX + offsetX
					val midShoulderY = ((l11.y + l12.y) / 2) * imageHeight * scaleFactorY + offsetY
					val l11X = l11.x * imageWidth * scaleFactorX + offsetX
					val l11Y = l11.y * imageHeight * scaleFactorY + offsetY
					val l12X = l12.x * imageWidth * scaleFactorX + offsetX
					val l12Y = l12.y * imageHeight * scaleFactorY + offsetY
					drawExtendedLine(
						canvas,
						l11X,
						l11Y,
						l12X,
						l12Y,
						100f,
						100f,
						lineBgPaint
					)
					drawExtendedLine(
						canvas,
						l11X + 200,
						midShoulderY,
						l11X + 100,
						midShoulderY,
						0f,
						0f,
						lineBgPaint
					)
					drawExtendedLine(
						canvas,
						l12X - 100,
						midShoulderY,
						l12X - 200,
						midShoulderY,
						0f,
						0f,
						lineBgPaint
					)
					canvas.drawCircle(midShoulderX, midShoulderY, 12f, circleAccentPaint)
				}

				if (l11 != null && l12 != null) {
					val midShoulderX = ((l11.x + l12.x) / 2) * imageWidth * scaleFactorX + offsetX
					val midShoulderY = ((l11.y + l12.y) / 2) * imageHeight * scaleFactorY + offsetY
					val l11X = l11.x * imageWidth * scaleFactorX + offsetX
					val l11Y = l11.y * imageHeight * scaleFactorY + offsetY
					val l12X = l12.x * imageWidth * scaleFactorX + offsetX
					val l12Y = l12.y * imageHeight * scaleFactorY + offsetY
					drawExtendedLine(
						canvas,
						l11X,
						l11Y,
						l12X,
						l12Y,
						100f,
						100f,
						lineBgPaint
					)
					drawExtendedLine(
						canvas,
						l11X + 200,
						midShoulderY,
						l11X + 100,
						midShoulderY,
						0f,
						0f,
						lineBgPaint
					)
					drawExtendedLine(
						canvas,
						l12X - 100,
						midShoulderY,
						l12X - 200,
						midShoulderY,
						0f,
						0f,
						lineBgPaint
					)
				}

				if (l15 != null && l16 != null) {
					val midWristX = ((l15.x + l16.x) / 2) * imageWidth * scaleFactorX + offsetX
					val midWristY = ((l15.y + l16.y) / 2) * imageHeight * scaleFactorY + offsetY
					val l15X = l15.x * imageWidth * scaleFactorX + offsetX
					val l15Y = l15.y * imageHeight * scaleFactorY + offsetY
					val l16X = l16.x * imageWidth * scaleFactorX + offsetX
					val l16Y = l16.y * imageHeight * scaleFactorY + offsetY
					drawExtendedLine(
						canvas,
						l15X,
						l15Y,
						l16X,
						l16Y,
						100f,
						100f,
						lineBgPaint
					)
					drawExtendedLine(
						canvas,
						l15X + 200,
						midWristY,
						l15X + 100,
						midWristY,
						0f,
						0f,
						lineBgPaint
					)
					drawExtendedLine(
						canvas,
						l16X - 100,
						midWristY,
						l16X - 200,
						midWristY,
						0f,
						0f,
						lineBgPaint
					)
				}

				if (l23 != null && l24 != null) {
					val midHipX = ((l23.x + l24.x) / 2) * imageWidth * scaleFactorX + offsetX
					val midHipY = ((l23.y + l24.y) / 2) * imageHeight * scaleFactorY + offsetY
					val l23X = l23.x * imageWidth * scaleFactorX + offsetX
					val l23Y = l23.y * imageHeight * scaleFactorY + offsetY
					val l24X = l24.x * imageWidth * scaleFactorX + offsetX
					val l24Y = l24.y * imageHeight * scaleFactorY + offsetY
					drawExtendedLine(
						canvas,
						l23X,
						l23Y,
						l24X,
						l24Y,
						100f,
						100f,
						lineBgPaint
					)
					drawExtendedLine(
						canvas,
						l23X + 200,
						midHipY,
						l23X + 100,
						midHipY,
						0f,
						0f,
						lineBgPaint
					)
					drawExtendedLine(
						canvas,
						l24X - 100,
						midHipY,
						l24X - 200,
						midHipY,
						0f,
						0f,
						lineBgPaint
					)
				}
				if (l11 != null && l12 != null && l15 != null && l16 != null && l23 != null && l24 != null) {
					val wrist = calculateSlope(l15.x, l15.y, l16.x, l16.y)
					val shoulder = calculateSlope(l11.x, l11.y, l12.x, l12.y)
					val hip = calculateSlope(l23.x, l23.y, l24.x, l24.y)

					drawSidePanel(canvas, wrist, shoulder ,hip )
				}

			}

			ButtonState.LEFT -> {

			}
			ButtonState.RIGHT -> {

			}
		}
	}

	fun setCurrentBtnState(btnState: ButtonState) {
		currentBtnState = btnState
	}

	fun drawExtendedLine(
		canvas: Canvas,
		x0: Float,
		y0: Float,
		x1: Float,
		y1: Float,
		startExtension: Float,
		endExtension: Float,
		paint: Paint,
	) {
		val dx = x1 - x0
		val dy = y1 - y0
		val length = hypot(dx.toDouble(), dy.toDouble()).toFloat()

		if (length == 0f) return

		val nx = dx / length
		val ny = dy / length

		val extendedStartX = x0 - nx * startExtension
		val extendedStartY = y0 - ny * startExtension
		val extendedEndX = x1 + nx * endExtension
		val extendedEndY = y1 + ny * endExtension

		paint.strokeCap = Paint.Cap.ROUND
		canvas.drawLine(extendedStartX, extendedStartY, extendedEndX, extendedEndY, paint)
	}

	fun drawSidePanel(
		canvas: Canvas,
		wristAngle: Float,
		shoulderAngle: Float,
		pelvisAngle: Float
	) {
		val canvasWidth = canvas.width.toFloat()
		val canvasHeight = canvas.height.toFloat()

		// --- 1. 패널 레이아웃 계산 ---
		val panelWidth = 450f
		val panelHeight = 350f
		val margin = 20f

		// 우측 상단 쪽에 배치 (정중앙보다 살짝 위가 시선 이동에 좋습니다)
		val left = canvasWidth - panelWidth - margin
		val top = margin + 0f // 화면 맨 위에서 살짝 내림
		val right = canvasWidth - margin
		val bottom = top + panelHeight
		val cornerRadius = 20f

		// --- 2. 반투명 배경 그리기 ---
		canvas.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, panelPaint)

		// --- 3. 텍스트 레이아웃 설정 ---
		val startX = left + 40f  // 패널 왼쪽 안쪽 여백
		var currentY = top + 80f // 첫 번째 줄 Y 좌표
		val lineSpacing = 100f   // 줄 간격

		fun drawRow(label: String, angle: Float, yPos: Float) {
			canvas.drawText(label, startX, yPos, textPaint)
			val threshold = 3f
			val statusText: String
			val statusPaint: Paint

			when {
				angle in -threshold..threshold -> {
					statusText = "✅ 정상"
					statusPaint = normalStatusPaint
				}
				angle < -threshold -> {
					statusText = "↙️ 좌측 내려감"
					statusPaint = warningStatusPaint
				}
				else -> {
					statusText = "우측 내려감 ↘️"
					statusPaint = warningStatusPaint
				}
			}

			canvas.drawText(statusText, startX + 160f, yPos, statusPaint)
		}

		// --- 5. 각 부위별 행 그리기 ---
		drawRow("손목", wristAngle, currentY)
		currentY += lineSpacing

		drawRow("어깨", shoulderAngle, currentY)
		currentY += lineSpacing

		drawRow("골반", pelvisAngle, currentY)
	}
}