package com.tangoplus.matviewer.ui.listener

import com.tangoplus.matviewer.domain.vo.ButtonState

interface OnButtonClickListener {
	fun onButtonClick(currentItem: ButtonState)
}