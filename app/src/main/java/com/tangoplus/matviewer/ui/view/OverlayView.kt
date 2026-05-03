package com.tangoplus.matviewer.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.plus
import androidx.core.graphics.times
import androidx.core.graphics.toColorInt
import com.tangoplus.facebeautyexpert.domain.vision.pose.PoseLandmarkResult

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
	companion object {
		const val VIDEO_STROKE_WIDTH = 5F
	}

	private var results: PoseLandmarkResult? = null

	private var linePaint = Paint()
	private var axisPaint = Paint()
	private var axisSubPaint = Paint()
	private var borderPaint = Paint()
	private var fillPaint = Paint()
	private var textPaint = Paint()
	private var circlePaint = Paint()

	private var scaleFactorX: Float = 1f
	private var scaleFactorY : Float = 1f
	private var imageWidth: Int = 1
	private var imageHeight: Int = 1
	private var currentRunningMode: RunningMode = RunningMode.IMAGE
	init {
		initPaints()
	}


	@SuppressLint("ResourceAsColor")
	private fun initPaints() {
		// -----! 연결선 색 !-----
		linePaint.apply {
			color = "#2EE88B".toColorInt()
			strokeWidth = VIDEO_STROKE_WIDTH
			style = Paint.Style.STROKE
		}
		axisPaint.apply {
			color = "#FF5449".toColorInt()
			strokeWidth = 3f
			style = Paint.Style.STROKE
		}
		axisSubPaint.apply {
			color = "#FF981D".toColorInt()
			strokeWidth = 3f
			style = Paint.Style.STROKE
		}
		// ------! 꼭짓점 색 !------
		borderPaint = Paint().apply {
			color = "#2EE88B".toColorInt() // 테두리 색
			strokeWidth = 3f
			style = Paint.Style.STROKE // 테두리만 그리기
			isAntiAlias = true
			setShadowLayer(10f, 0f, 0f, "#1A2EE88B".toColorInt()) // 반지름, x-offset, y-offset, 그림자 색상
		}
		fillPaint = Paint().apply {
			color = "#FFFFFF".toColorInt() // 내부 색
			style = Paint.Style.FILL // 내부만 채우기
		}
		textPaint = Paint().apply {
			color = "#FFFFFF".toColorInt()
			textSize = 48f
			isAntiAlias = true
			textAlign = Paint.Align.CENTER
		}
		circlePaint = Paint().apply {
			color = "#41000000".toColorInt()
			style = Paint.Style.FILL
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

		if (currentRunningMode == RunningMode.LIVE_STREAM) {
			val offsetX = (width - imageWidth * scaleFactorX) / 2
			val offsetY = (height - imageHeight * scaleFactorY) / 2

			landmarks.forEach { landmark ->
				if (landmarks.indexOf(landmark) == 0 ||
					landmarks.indexOf(landmark) in 11 .. 16 ||
					landmarks.indexOf(landmark) in 23 .. 28
				) {
					canvas.drawCircle(
						landmark.x * imageWidth * scaleFactorX + offsetX,
						landmark.y * imageHeight * scaleFactorY + offsetY,
						5f,
						borderPaint
					)
					canvas.drawCircle(
						landmark.x * imageWidth * scaleFactorX + offsetX,
						landmark.y * imageHeight * scaleFactorY + offsetY,
						5f,
						fillPaint
					)
				}
			}

			// 안전하게 특정 랜드마크 접근
			val nose = landmarks.getOrNull(0)
			val leftShoulder = landmarks.getOrNull(11)
			val rightShoulder = landmarks.getOrNull(12)

			// 코와 어깨 중간점 연결선 그리기 (모든 필요한 점이 있을 때만)
			if (nose != null && leftShoulder != null && rightShoulder != null) {
				val noseX = nose.x * imageWidth * scaleFactorX + offsetX
				val noseY = nose.y * imageHeight * scaleFactorY + offsetY
				val midShoulderX = (leftShoulder.x + rightShoulder.x) / 2 * imageWidth * scaleFactorX + offsetX
				val midShoulderY = (leftShoulder.y + rightShoulder.y) / 2 * imageHeight * scaleFactorY + offsetY

				canvas.drawLine(noseX, noseY, midShoulderX, midShoulderY, linePaint)
			}

			val connections = listOf(
				// 몸통 + 팔
				Pair(11, 12), Pair(11, 13), Pair(12, 14), Pair(13, 15), Pair(14, 16),
				// Legs
				Pair(11, 23), Pair(12, 24), Pair(23, 25), Pair(23, 24), Pair(24, 26), Pair(25, 27), Pair(26, 28),
				// 다리
				Pair(27, 31), Pair(28, 32), Pair(27, 29), Pair(28, 30),
			)

			connections.forEach { (start, end) ->
				if (start < landmarks.size && end < landmarks.size) {
					canvas.drawLine(
						landmarks[start].x * imageWidth * scaleFactorX + offsetX,
						landmarks[start].y * imageHeight * scaleFactorY + offsetY,
						landmarks[end].x * imageWidth * scaleFactorX + offsetX,
						landmarks[end].y * imageHeight * scaleFactorY + offsetY,
						linePaint
					)
				}
			}
		}

		else { // video 일 때

			canvas.scale(1f, 1f, width / 2f, 0f)
			val offsetX = (width - imageWidth * scaleFactorX) / 2
			val offsetY = (height - imageHeight * scaleFactorY) / 2

			// 안전하게 특정 랜드마크 접근
			val nose = landmarks.getOrNull(0)
			val leftShoulder = landmarks.getOrNull(11)
			val rightShoulder = landmarks.getOrNull(12)
			val leftIndex = landmarks.getOrNull(19)
			val rightIndex = landmarks.getOrNull(20)

			val leftHip = landmarks.getOrNull(23)
			val rightHip = landmarks.getOrNull(24)

			val leftKnee = landmarks.getOrNull(25)
			val rightKnee = landmarks.getOrNull(26)

			val leftAnkle = landmarks.getOrNull(27)
			val rightAnkle = landmarks.getOrNull(28)
			if (nose != null && leftShoulder != null && rightShoulder != null
				&& leftIndex != null && rightIndex != null
				&& leftHip != null && rightHip != null
				&& leftKnee != null && rightKnee != null
				&& leftAnkle != null && rightAnkle != null) {
				val noseX = nose.x * scaleFactorX + offsetX
				val noseY = nose.y * scaleFactorY + offsetY
				val midShoulderX = (leftShoulder.x + rightShoulder.x) / 2 * scaleFactorX + offsetX
				val midShoulderY = (leftShoulder.y + rightShoulder.y) / 2 * scaleFactorY + offsetY
				val leftIndexX = leftIndex.x * scaleFactorX + offsetX
				val leftIndexY = leftIndex.y * scaleFactorY + offsetY
				val rightIndexX = rightIndex.x * scaleFactorX + offsetX
				val rightIndexY = rightIndex.y * scaleFactorY + offsetY

				val leftHipX = leftHip.x * scaleFactorX + offsetX
				val leftHipY = leftHip.y * scaleFactorY + offsetY
				val rightHipX = rightHip.x * scaleFactorX + offsetX
				val rightHipY = rightHip.y * scaleFactorY + offsetY

				val leftKneeX = leftKnee.x * scaleFactorX + offsetX
				val leftKneeY = leftKnee.y * scaleFactorY + offsetY
				val rightKneeX = rightKnee.x * scaleFactorX + offsetX
				val rightKneeY = rightKnee.y * scaleFactorY + offsetY

				val leftAnkleX = leftAnkle.x * scaleFactorX + offsetX
				val leftAnkleY = leftAnkle.y * scaleFactorY + offsetY
				val rightAnkleX = rightAnkle.x * scaleFactorX + offsetX
				val rightAnkleY = rightAnkle.y * scaleFactorY + offsetY
				canvas.drawLine(noseX, noseY, midShoulderX, midShoulderY, linePaint)
				// 가로축
				val extraLineWidth = 150
				canvas.drawLine(leftIndexX + extraLineWidth  , leftIndexY, rightIndexX - extraLineWidth, rightIndexY, axisPaint)
				canvas.drawLine(leftKneeX + extraLineWidth, leftKneeY, rightKneeX - extraLineWidth, rightKneeY, axisPaint)
				canvas.drawLine(leftHipX + extraLineWidth, leftHipY, rightHipX - extraLineWidth, rightHipY, axisPaint)
				canvas.drawLine(leftKneeX + extraLineWidth, leftKneeY, rightKneeX - extraLineWidth, rightKneeY, axisPaint)
				canvas.drawLine((leftAnkleX + rightAnkleX) / 2, leftAnkleY + extraLineWidth, (leftAnkleX + rightAnkleX) / 2, noseY - extraLineWidth, axisPaint)
				//세로축
				canvas.drawLine(leftHipX, leftHipY - 100, leftHipX, leftAnkleY + 100, axisPaint)
				canvas.drawLine(rightHipX, rightHipY - 100, rightHipX, rightAnkleY + 100, axisPaint)
			}
			val connections = listOf(
				Pair(11, 13), Pair(12, 14), Pair(13, 15), Pair(14, 16),
				Pair(15, 21), Pair(15, 17), Pair(17, 19), Pair(15, 19),
				Pair(16, 22), Pair(16, 18), Pair(18, 20), Pair(16, 20),
				Pair(11, 23), Pair(12, 24), Pair(23, 25), Pair(24, 26), Pair(25, 27), Pair(26, 28),
				Pair(27, 31), Pair(28, 32), Pair(27, 29), Pair(28, 30),
			)
			connections.forEach { (start, end) ->
				if (start < landmarks.size && end < landmarks.size) {
					canvas.drawLine(
						landmarks[start].x * scaleFactorX + offsetX,
						landmarks[start].y  * scaleFactorY + offsetY,
						landmarks[end].x * scaleFactorX + offsetX,
						landmarks[end].y * scaleFactorY + offsetY,
						linePaint
					)
				}
			}

			val subConnections = listOf(
				Pair(11, 12), Pair(23, 24), Pair(25, 26)
			)
			subConnections.forEach { (start, end) ->
				if (start < landmarks.size && end < landmarks.size) {
					canvas.drawLine(
						landmarks[start].x * scaleFactorX + offsetX,
						landmarks[start].y  * scaleFactorY + offsetY,
						landmarks[end].x * scaleFactorX + offsetX,
						landmarks[end].y * scaleFactorY + offsetY,
						axisSubPaint
					)
				}
			}

			val pointAccentRange = listOf(0, 11, 12, 23, 24, 25, 26)
			val pointRange = listOf(13, 14, 15, 16, 27, 28)
			pointAccentRange.forEach { index ->
				val x = landmarks.getOrNull(index)?.x
				val y = landmarks.getOrNull(index)?.y
				if (x != null && y != null) {
					canvas.drawCircle(
						x * scaleFactorX + offsetX,
						y * scaleFactorY + offsetY,
						7f,
						borderPaint)
					canvas.drawCircle(
						x * scaleFactorX + offsetX,
						y* scaleFactorY + offsetY,
						7f, fillPaint)
				}
			}
			pointRange.forEach { index ->
				val x =landmarks.getOrNull(index)?.x
				val y =landmarks.getOrNull(index)?.y
				if (x != null && y != null) {
					canvas.drawCircle(
						x * scaleFactorX + offsetX,
						y * scaleFactorY + offsetY,
						5f,
						borderPaint)
					canvas.drawCircle(
						x * scaleFactorX + offsetX,
						y * scaleFactorY + offsetY,
						5f,
						fillPaint)
				}
			}
		}
	}
}