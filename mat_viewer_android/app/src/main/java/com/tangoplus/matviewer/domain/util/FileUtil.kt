package com.tangoplus.matviewer.domain.util

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

object FileUtil {
	private const val FOLDER_NAME = "mat_images"
	fun saveBitmapToInternal(context: Context, bitmap: Bitmap, fileName: String): Boolean {
		val directory = File(context.filesDir, FOLDER_NAME)
		if (!directory.exists()) directory.mkdirs() // 폴더가 없으면 생성

		val file = File(directory, fileName)
		return try {
			FileOutputStream(file).use { out ->
				bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) // PNG 원본 압축 저장
			}
			true
		} catch (e: Exception) {
			e.printStackTrace()
			false
		}
	}

	// 2. 저장된 파일명으로 물리적 File 객체 가져오기 (이미지 뷰 로드용)
	fun getFileByName(context: Context, fileName: String): File {
		val directory = File(context.filesDir, FOLDER_NAME)
		return File(directory, fileName)
	}

	// 3. 물리적으로 내부 저장소에서 이미지 삭제하기
	fun deleteImageFile(context: Context, fileName: String): Boolean {
		val file = getFileByName(context, fileName)
		return if (file.exists()) {
			file.delete()
		} else {
			false
		}
	}


}