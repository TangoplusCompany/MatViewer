package com.tangoplus.matviewer.domain.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Geocoder
import android.os.Build
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.util.Locale

object QRCodeUtil {

	fun generateQrCode(text: String, size: Int = 500): Bitmap? {
		return try {
			val bitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
			val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
			for (x in 0 until size) {
				for (y in 0 until size) {
					bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
				}
			}
			bitmap
		} catch (e: Exception) {
			e.printStackTrace()
			null
		}
	}

	fun getCurrentAddress(context: Context, latitude: Double, longitude: Double, callback: (String) -> Unit) {
		val geocoder = Geocoder(context, Locale.KOREA)

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			// API 33 이상 (비동기 방식)
			geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
				if (addresses.isNotEmpty()) {
					callback(addresses[0].getAddressLine(0)) // 전체 주소 문자열 반환
				} else {
					callback("주소를 찾을 수 없습니다.")
				}
			}
		} else {
			// API 33 미만 (동기 방식)
			try {
				val addresses = geocoder.getFromLocation(latitude, longitude, 1)
				if (!addresses.isNullOrEmpty()) {
					callback(addresses[0].getAddressLine(0))
				} else {
					callback("주소를 찾을 수 없습니다.")
				}
			} catch (e: Exception) {
				callback("주소 변환 실패")
			}
		}
	}
}