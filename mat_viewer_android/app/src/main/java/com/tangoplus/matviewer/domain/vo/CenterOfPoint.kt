package com.tangoplus.matviewer.domain.vo

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