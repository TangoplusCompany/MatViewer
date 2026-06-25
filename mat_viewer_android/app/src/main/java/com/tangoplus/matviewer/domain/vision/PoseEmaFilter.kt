package com.tangoplus.matviewer.domain.vision

class PoseEmaFilter(private val alpha: Float = 0.4f) {
	// MediaPipe Pose는 보통 33개의 랜드마크를 반환합니다.
	private val smoothedX = FloatArray(33)
	private val smoothedY = FloatArray(33)
	private val isInitialized = BooleanArray(33)

	fun applyFilter(landmarks: List<PoseLandmarkResult.PoseLandmark>) {
		// 안전장치: 혹시라도 33개를 초과하거나 적게 들어올 경우를 대비
		val count = minOf(landmarks.size, 33)

		for (i in 0 until count) {
			val lm = landmarks[i]

			if (!isInitialized[i]) {
				// 첫 프레임은 원본 좌표를 그대로 배열에 저장
				smoothedX[i] = lm.x
				smoothedY[i] = lm.y
				isInitialized[i] = true
			} else {
				// EMA(지수 이동 평균) 계산
				smoothedX[i] = smoothedX[i] + alpha * (lm.x - smoothedX[i])
				smoothedY[i] = smoothedY[i] + alpha * (lm.y - smoothedY[i])

				// 💡 var로 선언된 원본 객체의 x, y 값에 정제된 결과를 즉시 덮어씀
				lm.x = smoothedX[i]
				lm.y = smoothedY[i]
			}
		}
	}
}