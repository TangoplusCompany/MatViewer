package com.tangoplus.matviewer.ui.vm


import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tangoplus.matviewer.domain.vision.PoseLandmarkResult
import com.tangoplus.facebeautyexpert.domain.vision.pose.PoseLandmarkerHelper
import com.tangoplus.matviewer.domain.vision.PoseEmaFilter
import com.tangoplus.matviewer.domain.vo.ButtonState


class MainViewModel : ViewModel() {

	private var _delegate: Int = PoseLandmarkerHelper.DELEGATE_CPU
	private var _minPoseDetectionConfidence: Float =
		PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE
	private var _minPoseTrackingConfidence: Float = PoseLandmarkerHelper.Companion
		.DEFAULT_POSE_TRACKING_CONFIDENCE
	private var _minPosePresenceConfidence: Float = PoseLandmarkerHelper.Companion
		.DEFAULT_POSE_PRESENCE_CONFIDENCE
	private var _maxFaces: Int = PoseLandmarkerHelper.DEFAULT_NUM_POSES

	val currentDelegate: Int get() = _delegate
	val currentMinFaceDetectionConfidence: Float
		get() =
			_minPoseDetectionConfidence
	val currentMinFaceTrackingConfidence: Float
		get() =
			_minPoseTrackingConfidence
	val currentMinFacePresenceConfidence: Float
		get() =
			_minPosePresenceConfidence
	val currentMaxFaces: Int get() = _maxFaces

	// 💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃

	var pResult : PoseLandmarkResult? = null
	val poseFilter = PoseEmaFilter(alpha = 0.4f)

	val btns = ButtonState.entries.toList()
	val currentBtnState = MutableLiveData(ButtonState.CENTER)
}