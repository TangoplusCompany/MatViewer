package com.tangoplus.matviewer.domain.vo



// 7개의 CoP 상대 좌표를 모아둔 클래스
data class CenterOfPoint(
	val leftTop: Pair<Float, Float>? = null,
	val leftBottom: Pair<Float , Float>? = null,
	val rightTop: Pair<Float , Float>? = null,
	val rightBottom: Pair<Float , Float>? = null,
	val leftCenter: Pair<Float , Float>? = null,
	val rightCenter: Pair<Float , Float>? = null,
	val center: Pair<Float , Float>? = null
) {
	fun clear(): CenterOfPoint = CenterOfPoint()
}