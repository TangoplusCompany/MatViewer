package com.tangoplus.matexample

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.tangoplus.facebeautyexpert.domain.vision.pose.PoseLandmarkResult

object PoseLandmarkAdapter {
	fun toCustomPoseLandmarkResult(poseLandmarkerResult: PoseLandmarkerResult): PoseLandmarkResult {
		val landmarks = poseLandmarkerResult.landmarks().firstOrNull()?.map { landmark ->
			PoseLandmarkResult.PoseLandmark(
				x = landmark.x(),
				y = landmark.y(),

				)
		} ?: emptyList()

		return PoseLandmarkResult(landmarks)
	}
}