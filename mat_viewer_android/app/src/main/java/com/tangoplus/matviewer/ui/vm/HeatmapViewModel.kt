package com.tangoplus.matviewer.ui.vm

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangoplus.matviewer.domain.supabase.SupabaseManager
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HeatmapViewModel : ViewModel() {

	// 결과 콜백에 파일명 대신 이미지 URL(String)을 반환하도록 변경
	fun uploadHeatmap(bitmap: Bitmap, onResult: (Boolean, String?) -> Unit) {
		viewModelScope.launch(Dispatchers.IO) {
			try {
				val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
				val fileName = "heatmap_$timeStamp.png"

				val baos = ByteArrayOutputStream()
				bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
				val byteArray = baos.toByteArray()

				val bucket = SupabaseManager.client.storage.from("temp_mat_images")

				// 1. 이미지 업로드 실행
				bucket.upload(path = fileName, data = byteArray) {
					upsert = true
				}

				// 2. 업로드된 파일의 Public URL 직접 가져오기
				val publicUrl = bucket.publicUrl(fileName)

				// 성공 시 URL 전달
				viewModelScope.launch(Dispatchers.Main) {
					onResult(true, publicUrl)
				}
			} catch (e: Exception) {
				e.printStackTrace()
				viewModelScope.launch(Dispatchers.Main) {
					onResult(false, e.localizedMessage)
				}
			}
		}
	}

	private fun create
}