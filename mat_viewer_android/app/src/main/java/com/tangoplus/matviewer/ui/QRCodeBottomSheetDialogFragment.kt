package com.tangoplus.matviewer.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.tangoplus.matviewer.data.db.MatDao
import com.tangoplus.matviewer.data.db.MatDatabase
import com.tangoplus.matviewer.data.db.MatRecord
import com.tangoplus.matviewer.databinding.FragmentQRCodeBottomSheetDialogBinding
import com.tangoplus.matviewer.domain.util.FileUtil.saveBitmapToInternal
import com.tangoplus.matviewer.ui.vm.HeatmapViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.Int

class QRCodeBottomSheetDialogFragment : BottomSheetDialogFragment() {
	private lateinit var bd: FragmentQRCodeBottomSheetDialogBinding
	private val hvm : HeatmapViewModel by activityViewModels()
	private lateinit var mDao: MatDao
	companion object {
		private const val ARG_URL = "arg_url"

		// 팩토리 메서드를 통해 안전하게 URL 파라미터를 넘겨받음
		fun newInstance(url: String): QRCodeBottomSheetDialogFragment {
			val fragment = QRCodeBottomSheetDialogFragment()
			val args = Bundle().apply {
				putString(ARG_URL, url)
			}
			fragment.arguments = args
			return fragment
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		bd = FragmentQRCodeBottomSheetDialogBinding.inflate(inflater, container, false)
		return bd.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val url = arguments?.getString(ARG_URL)

		if (!url.isNullOrEmpty()) {
			val qrBitmap = generateQRCode(url)
			if (qrBitmap != null) {
				bd.ivQrCode.setImageBitmap(qrBitmap)
			}


			lifecycleScope.launch(Dispatchers.IO) {
				val md = MatDatabase.getDatabase(requireContext())
				mDao = md.MatDao()

				val heatmapFileName = "heatmap_${MatRecord.getCurrentTime()}.png"
				val qrCodeFileName = "qrcode_${MatRecord.getCurrentTime()}.png"
				if (hvm.capturedHitmap != null && qrBitmap != null) {
					val isHeatMapSaved = saveBitmapToInternal(requireContext(), hvm.capturedHitmap!!, heatmapFileName)
					val isQRCodeSaved = saveBitmapToInternal(requireContext(), qrBitmap, qrCodeFileName)
					if (isHeatMapSaved && isQRCodeSaved) {
						val newRecord = MatRecord(
							mat_image_name = heatmapFileName,
							qr_code_name = qrCodeFileName,
							location = hvm.currentAddress,
							latitude = hvm.currentLatitude,
							longitude = hvm.currentLongitude,
							p_left = hvm.matRatio.left,
							p_right = hvm.matRatio.right,
							p_top = hvm.matRatio.top,
							p_bottom = hvm.matRatio.bottom,
							p_left_top = hvm.matRatio.leftTop,
							p_left_bottom = hvm.matRatio.leftBottom,
							p_right_top = hvm.matRatio.rightTop,
							p_right_bottom = hvm.matRatio.rightBottom
						)
						mDao.insertMatRecord(newRecord)
					}
				}
			}
		}

		bd.btnQrExit.setOnClickListener { dismiss() }
	}
	private fun generateQRCode(text: String): Bitmap? {
		return try {
			val width = 512
			val height = 512
			val matrix: BitMatrix = MultiFormatWriter().encode(
				text,
				BarcodeFormat.QR_CODE,
				width,
				height
			)
			val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

			for (x in 0 until width) {
				for (y in 0 until height) {
					// QR 코드는 검은색(#000000), 배경은 흰색(#FFFFFF)으로 픽셀 매핑
					bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
				}
			}
			bitmap
		} catch (e: Exception) {
			e.printStackTrace()
			null
		}
	}
}
