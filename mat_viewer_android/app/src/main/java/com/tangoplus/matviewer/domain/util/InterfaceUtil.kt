package com.tangoplus.matviewer.domain.util

import android.view.View

object InterfaceUtil {
	fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
		val listener = View.OnClickListener { action(it) }
		setOnClickListener(OnSingleClickListener(listener))
	}
}