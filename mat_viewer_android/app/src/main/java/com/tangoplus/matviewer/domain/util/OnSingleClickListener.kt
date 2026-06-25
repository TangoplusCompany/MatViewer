package com.tangoplus.matviewer.domain.util

import android.util.Log
import android.view.View

class OnSingleClickListener(private val clickListener: View.OnClickListener) :
	View.OnClickListener {
	companion object {
		// ---- 클릭 방지 시간 설정 ----
		const val CLICK_INTERVAL: Long = 600
		const val TAG = "OnSingleClickListener"
	}

	private var clickable = true

	override fun onClick(v: View?) {
		if (clickable) {
			clickable = false
			v?.run {
				postDelayed({ clickable = true }, CLICK_INTERVAL)
				clickListener.onClick(v)
			}
		} else {
			Log.d(TAG, "wait for click, 600ms")
		}
	}
}