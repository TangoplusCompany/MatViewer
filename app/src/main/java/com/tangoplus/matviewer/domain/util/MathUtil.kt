package com.tangoplus.matviewer.domain.util

import java.lang.Math.toDegrees
import kotlin.math.atan2

object MathUtil {
	fun calculateSlope(x1: Float, y1: Float, x2: Float, y2: Float): Float {
		val radians = atan2(y1 - y2, x1 - x2)
		val degrees = toDegrees(radians.toDouble()).toFloat()
		val result = if (degrees > 180) degrees % 180 else degrees
		return "%.4f".format(result).toFloat()
	}

}