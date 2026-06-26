package com.tangoplus.matviewer.domain.vo

data class MatRatio(
	val left: Int = 0,
	val right: Int = 0,
	val top: Int = 0,
	val bottom: Int = 0
) {
	// 앞서 정한 규칙대로 URL 파라미터용 문자열을 스스로 만들게 함수를 내장시킵니다.
	fun toParamString(): String = "${left}_${right}-${top}_${bottom}"
}