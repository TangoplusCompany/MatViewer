package com.tangoplus.matviewer.domain.supabase

import android.content.Context
import com.tangoplus.matviewer.R
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage

object SupabaseManager {
	lateinit var client: SupabaseClient
		private set

	fun initialize(context: Context) {
		if (::client.isInitialized) return // 이미 초기화됐다면 무시

		client = createSupabaseClient(
			supabaseUrl = context.getString(R.string.supabase_url),
			supabaseKey = context.getString(R.string.supabase_anon_key)
		) {
			install(Storage)
		}
	}
}