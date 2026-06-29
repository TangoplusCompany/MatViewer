package com.tangoplus.matviewer.ui

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.tangoplus.matviewer.data.supabase.SupabaseManager

class MyApp : Application() {
	override fun onCreate() {
		super.onCreate()
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
		SupabaseManager.initialize(this)
	}
}