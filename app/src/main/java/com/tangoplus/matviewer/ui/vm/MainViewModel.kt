package com.tangoplus.matviewer.ui.vm


import androidx.lifecycle.ViewModel
import com.tangoplus.facebeautyexpert.domain.vision.pose.PoseLandmarkResult
import com.tangoplus.facebeautyexpert.domain.vision.pose.PoseLandmarkerHelper


class MainViewModel : ViewModel() {

	private var _delegate: Int = PoseLandmarkerHelper.DELEGATE_CPU
	private var _minFaceDetectionConfidence: Float =
		PoseLandmarkerHelper.Companion.DEFAULT_POSE_DETECTION_CONFIDENCE
	private var _minFaceTrackingConfidence: Float = PoseLandmarkerHelper.Companion
		.DEFAULT_POSE_TRACKING_CONFIDENCE
	private var _minFacePresenceConfidence: Float = PoseLandmarkerHelper.Companion
		.DEFAULT_POSE_PRESENCE_CONFIDENCE
	private var _maxFaces: Int = PoseLandmarkerHelper.DEFAULT_NUM_POSES

	val currentDelegate: Int get() = _delegate
	val currentMinFaceDetectionConfidence: Float
		get() =
			_minFaceDetectionConfidence
	val currentMinFaceTrackingConfidence: Float
		get() =
			_minFaceTrackingConfidence
	val currentMinFacePresenceConfidence: Float
		get() =
			_minFacePresenceConfidence
	val currentMaxFaces: Int get() = _maxFaces

	// 💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃

	var pResult : PoseLandmarkResult? = null

}