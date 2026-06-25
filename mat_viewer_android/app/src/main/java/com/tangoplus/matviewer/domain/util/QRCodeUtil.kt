package com.tangoplus.matviewer.domain.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

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

}