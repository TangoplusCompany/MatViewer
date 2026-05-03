package com.tangoplus.matviewer.ui.vm


import androidx.lifecycle.ViewModel
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

	private var isStartCountDown = false

	private var isGuideTextAnimationFinished = false

	private var isGuideTextChanged = false

	private var isSeqFinished = false

	private var isContourSave = false
	fun setContourSave(contourSave: Boolean) {
		isContourSave = contourSave
	}

//	fun getContourSave(): Boolean = isContourSave
//
//
//	fun setDelegate(delegate: Int) {
//		_delegate = delegate
//	}
//
//	fun setMinFaceDetectionConfidence(confidence: Float) {
//		_minFaceDetectionConfidence = confidence
//	}
//
//	fun setMinFaceTrackingConfidence(confidence: Float) {
//		_minFaceTrackingConfidence = confidence
//	}
//
//	fun setMinFacePresenceConfidence(confidence: Float) {
//		_minFacePresenceConfidence = confidence
//	}
//
//	fun setMaxFaces(maxResults: Int) {
//		_maxFaces = maxResults
//	}

	fun setCountDownFlag(isStart: Boolean) {
		isStartCountDown = isStart
	}

	fun getCountDownFlag(): Boolean {
		return isStartCountDown
	}

	fun setTextAnimationFlag(isStart: Boolean) {
		isGuideTextAnimationFinished = isStart
	}

	fun getTextAnimationFlag(): Boolean {
		return isGuideTextAnimationFinished
	}

	fun setGuideTextFlag(isStart: Boolean) {
		isGuideTextChanged = isStart
	}

	fun getGuideTextFlag(): Boolean {
		return isGuideTextChanged
	}

	fun setSeqFinishedFlag(isStart: Boolean) {
		isSeqFinished = isStart
	}

	fun getSeqFinishedFlag(): Boolean {
		return isSeqFinished
	}


}