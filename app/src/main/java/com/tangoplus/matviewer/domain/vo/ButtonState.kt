package com.tangoplus.matviewer.domain.vo

enum class ButtonState(val displayName: String) {
	LEFT("왼쪽"),
	CENTER("정면"),
	RIGHT("오른쪽");

	override fun toString(): String {
		return displayName
	}
}