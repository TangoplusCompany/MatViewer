package com.tangoplus.matviewer.data.db

import androidx.room.TypeConverter

class CoPPairConverter {

	// Pair를 "x,y" 형태의 String으로 변환 (Room에 저장할 때)
	@TypeConverter
	fun fromPair(pair: Pair<Float, Float>?): String? {
		if (pair == null) return null
		return "${pair.first},${pair.second}"
	}

	// "x,y" String을 다시 Pair로 변환 (Room에서 불러올 때)
	@TypeConverter
	fun toPair(value: String?): Pair<Float, Float>? {
		if (value.isNullOrEmpty()) return null
		val parts = value.split(",")
		if (parts.size != 2) return null

		val x = parts[0].toFloatOrNull() ?: return null
		val y = parts[1].toFloatOrNull() ?: return null
		return Pair(x, y)
	}
}