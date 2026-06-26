package com.tangoplus.matviewer.ui.record

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.tangoplus.matviewer.data.db.MatDao
import com.tangoplus.matviewer.data.db.MatDatabase
import com.tangoplus.matviewer.data.db.MatRecord
import com.tangoplus.matviewer.databinding.FragmentResultDialogBinding
import com.tangoplus.matviewer.domain.util.FileUtil.getFileByName
import com.tangoplus.matviewer.domain.util.InterfaceUtil.setOnSingleClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResultDialogFragment : DialogFragment() {
	private lateinit var bd : FragmentResultDialogBinding
	private lateinit var mDao : MatDao
	private lateinit var mResult : MatRecord
	companion object {
		private const val ARG_SN = "arg_sn"

		// 팩토리 메서드를 통해 안전하게 URL 파라미터를 넘겨받음
		fun newInstance(sn: Int): ResultDialogFragment {
			val fragment = ResultDialogFragment()
			val args = Bundle().apply {
				putInt(ARG_SN, sn)
			}
			fragment.arguments = args
			return fragment
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		bd = FragmentResultDialogBinding.inflate(inflater)
		return bd.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val sn = arguments?.getInt(ARG_SN, 0)

		val md = MatDatabase.getDatabase(requireContext())
		mDao = md.MatDao()

		lifecycleScope.launch(Dispatchers.IO) {
			if (sn != 0 && sn != null) {
				mResult = mDao.getMatRecord(sn)

				withContext(Dispatchers.Main) {
					val rawDate = mResult.reg_date

					if (rawDate.length >= 15) {
						val year = rawDate.substring(0, 4)
						val month = rawDate.substring(4, 6)
						val day = rawDate.substring(6, 8)

						val hour = rawDate.substring(9, 11)
						val minute = rawDate.substring(11, 13)
						val second = rawDate.substring(13, 15)

						bd.tvRDSDate.text = "${year}년 ${month}월 ${day}일 ${hour}:${minute}:${second}"
					} else {
						bd.tvRDSDate.text = rawDate // 예외 방어
					}

					bd.tvRDLocation.text = "${mResult.location ?: ""}"

					bd.btnRDClose.setOnSingleClickListener { dismiss() }
					val safeContext = context
					if (
						safeContext != null &&
						mResult.qr_code_name != null &&
						mResult.mat_image_name != null
						) {
						try {
							// 1. 저장된 파일로부터 비트맵 로드 및 수정 가능한 복사본 생성
							val heatmapUri = getFileByName(requireContext(), mResult.mat_image_name!!)
							val baseBitmap = BitmapFactory.decodeFile(heatmapUri.absolutePath)
							val targetSize = baseBitmap.width

							// 2. 🔴 새로운 정방형 빈 도화지(비트맵)를 생성
							val outputBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
							val canvas = Canvas(outputBitmap)

							// 3. 🔴 새 도화지에 배경색(#2F3034)을 가장 먼저 칠함
							canvas.drawColor("#2F3034".toColorInt())

							// 4. 🔴 그 위에 원본 힛맵 발모양 이미지를 얹어서 그림
							// (만약 검은색 배경을 투명하게 빼고 싶다면 여기에 Paint 속성을 추가할 수 있습니다)
							canvas.drawBitmap(baseBitmap, 0f, 0f, null)
							val baseTextSize = targetSize / 20f

							// 상/하/좌/우 큰 텍스트용 Paint (노란색 혹은 흰색)
							val largeTextPaint = Paint().apply {
								color = Color.WHITE
								textSize = baseTextSize * 1.3f // 🔴 4개 메인은 1.3배 더 크게 설정
								textAlign = Paint.Align.CENTER
								isAntiAlias = true
								setShadowLayer(8f, 0f, 0f, "#B3000000".toColorInt())
							}

							// 4사분면 모서리 소형 텍스트용 Paint
							val smallTextPaint = Paint(largeTextPaint).apply {
								textSize = baseTextSize * 0.9f // 🔴 대각선 모서리는 상대적으로 작게 설정
							}

							// 3. 좌표 배치를 위한 기준선 정의 (정방형 기반)
							val outerSide0 = targetSize * 0.1f // 맨 위, 맨 왼쪽 공간
							val outerSide1 = targetSize * 0.9f // 맨 아래, 맨 오른쪽 공간
							val midAbsolute = targetSize * 0.5f // 가운데 선

							val quarterLeft = targetSize * 0.25f
							val quarterRight = targetSize * 0.75f

							// 4. 큰 텍스트 그리기 (상, 하, 좌, 우)
							mResult.p_top?.let { canvas.drawText("${it}%", midAbsolute, outerSide0, largeTextPaint) }
							mResult.p_bottom?.let { canvas.drawText("${it}%", midAbsolute, outerSide1, largeTextPaint) }
							mResult.p_left?.let { canvas.drawText("${it}%", outerSide0, midAbsolute + (largeTextPaint.textSize / 3), largeTextPaint) }
							mResult.p_right?.let { canvas.drawText("${it}%", outerSide1, midAbsolute + (largeTextPaint.textSize / 3), largeTextPaint) }

							// 5. 작은 텍스트 그리기 (좌상, 좌하, 우상, 우하 대각선 4곳)
							mResult.p_left_top?.let { canvas.drawText("${it}%", quarterLeft, quarterLeft, smallTextPaint) }
							mResult.p_left_bottom?.let { canvas.drawText("${it}%", quarterLeft, quarterRight, smallTextPaint) }
							mResult.p_right_top?.let { canvas.drawText("${it}%", quarterRight, quarterLeft, smallTextPaint) }
							mResult.p_right_bottom?.let { canvas.drawText("${it}%", quarterRight, quarterRight, smallTextPaint) }

							// 6. 만약 7개 CoP 점의 데이터(좌표)도 DB에서 가져왔다면 여기에 drawCoP 로직을 추가해 점을 얹어줍니다.
							/*
							val paint = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }
							// mResult에서 저장해 둔 cx, cy 좌표가 있다면 꺼내서 바로 그림
							canvas.drawCircle(mResult.cop_total_x, mResult.cop_total_y, radius, paint)
							*/

							// 7. 최종 완성된 비트맵을 이미지뷰에 세팅
							bd.ivRDHeatmap.setImageBitmap(outputBitmap)

						} catch (e: Exception) {
							e.printStackTrace()
							// 에러 발생 시 방어 코드로 기본 이미지 세팅
							val heatmapUri = getFileByName(requireContext(), mResult.mat_image_name!!)
							bd.ivRDHeatmap.setImageURI(Uri.fromFile(heatmapUri))
						}
						val qrCodeUri = getFileByName(requireContext(), mResult.qr_code_name!!)
						bd.ivRDQR.setImageURI(Uri.fromFile(qrCodeUri))

					}

				}
			}

		}
	}
}