package com.tangoplus.matviewer.ui

import android.app.Application
import com.tangoplus.matviewer.data.supabase.SupabaseManager

class MyApp : Application() {
	override fun onCreate() {
		super.onCreate()
		// 앱이 켜질 때 본인의 context를 넘겨서 안전하게 딱 한 번만 만듭니다.
		SupabaseManager.initialize(this)
	}
}