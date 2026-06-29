package com.tangoplus.matviewer.ui.vm

import android.content.Context
import android.graphics.Bitmap
import android.location.Geocoder
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangoplus.matviewer.data.supabase.SupabaseManager
import com.tangoplus.matviewer.domain.vo.CenterOfPoint
import com.tangoplus.matviewer.domain.vo.MatRatio
import com.tangoplus.matviewer.ui.MainActivity
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class HeatmapViewModel : ViewModel() {
	var currentAddress: String = "위치 확인 불가 지역"
	var currentLatitude : Double? = null
	var currentLongitude : Double? = null
	// 결과 콜백으로 생성된 파일명(String)을 반환하도록 변경
	fun uploadHeatmap(activity: FragmentActivity, bitmap: Bitmap, onResult: (Boolean, String?) -> Unit) {
		viewModelScope.launch(Dispatchers.IO) {
			try {
				// 1. 현재 시간을 기반으로 유니크한 파일명 생성

				val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
				val deviceId = Settings.Secure.getString(activity.contentResolver, Settings.Secure.ANDROID_ID)
				val fileName = "heatmap_${timeStamp}@${deviceId}.png"

				val baos = ByteArrayOutputStream()
				bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
				val byteArray = baos.toByteArray()

				val bucket = SupabaseManager.client.storage.from("temp_mat_images")

				// 2. Supabase 스토리지에 업로드 실행
				bucket.upload(path = fileName, data = byteArray) {
					upsert = true
				}
				Log.e("이미지 업로드?", "버켓 업로드 ${bucket.bucketId}, ${bucket.supabaseClient} $fileName")
				// 3. 🎯 성공 시 publicUrl 대신 생성된 'fileName'을 그대로 전달
				viewModelScope.launch(Dispatchers.Main) {
					onResult(true, fileName)
				}
			} catch (e: Exception) {
				e.printStackTrace()
				viewModelScope.launch(Dispatchers.Main) {
					onResult(false, e.localizedMessage)
				}
			}
		}
	}

	fun getAddressString(context: Context, latitude: Double, longitude: Double, onResult: (String) -> Unit) {
		val geocoder = Geocoder(context, Locale.KOREA)

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			// API 33 이상 (최신 비동기 방식)
			geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
				if (addresses.isNotEmpty()) {
					// 주소 전체 문자열 가져오기 (예: 대한민국 서울특별시 강남구 역삼동 ...)
					onResult(addresses[0].getAddressLine(0))
				} else {
					onResult("알 수 없는 위치")
				}
			}
		} else {
			// API 33 미만 (과거 동기 방식)
			try {
				val addresses = geocoder.getFromLocation(latitude, longitude, 1)
				if (!addresses.isNullOrEmpty()) {
					onResult(addresses[0].getAddressLine(0))
				} else {
					onResult("알 수 없는 위치")
				}
			} catch (e: Exception) {
				onResult("위치 변환 실패")
			}
		}
	}


	// 좌우 ratio
	var matRatio: MatRatio = MatRatio()
		private set
	var cop : CenterOfPoint = CenterOfPoint()
		private set
	var syncedMatRatio : MatRatio = MatRatio()
	var syncedCop : CenterOfPoint = CenterOfPoint()
	fun calculateRatio(
		leftWeight: Float,
		topWeight: Float,
		ltWeight: Float,
		lbWeight: Float,
		rtWeight: Float,
		rbWeight: Float,
		totalWeight: Float) {
		val left = ((leftWeight / totalWeight) * 100).roundToInt()
		val right = 100 - left
		val top = ((topWeight / totalWeight) * 100).roundToInt()
		val bottom = 100 - top
		val ltPercent = ((ltWeight / totalWeight) * 100).roundToInt()
		val lbPercent = ((lbWeight / totalWeight) * 100).roundToInt()
		val rtPercent = ((rtWeight / totalWeight) * 100).roundToInt()
		val rbPercent = ((rbWeight / totalWeight) * 100).roundToInt()
		matRatio = MatRatio(
			left,
			right,
			top,
			bottom,
			ltPercent,
			lbPercent,
			rtPercent,
			rbPercent
		)
	}

	fun calculateRelativeCoP(
		maxX: Float = 1f,
		maxY: Float = 1f,

		// 기존 데이터들
		ltSumX: Float, ltSumY: Float, ltWeight: Float,
		lbSumX: Float, lbSumY: Float, lbWeight: Float,
		rtSumX: Float, rtSumY: Float, rtWeight: Float,
		rbSumX: Float, rbSumY: Float, rbWeight: Float,
		leftSumX: Float, leftSumY: Float, leftWeight: Float,
		rightSumX: Float, rightSumY: Float, rightWeight: Float,
		totalSumX: Float, totalSumY: Float, totalWeight: Float
	) {

		// 내부 유틸 함수: 무게가 있을 때만 0.0 ~ 1.0 사이의 상대 좌표 생성
		fun toNormalPoint(sX: Float, sY: Float, w: Float): Pair<Float,Float>? {
			return if (w > 0f) {
				val rawX = sX / w
				val rawY = sY / w
				Pair(rawX / maxX, rawY / maxY)
			} else null
		}

		cop = CenterOfPoint(
			leftTop = toNormalPoint(ltSumX, ltSumY, ltWeight),
			leftBottom = toNormalPoint(lbSumX, lbSumY, lbWeight),
			rightTop = toNormalPoint(rtSumX, rtSumY, rtWeight),
			rightBottom = toNormalPoint(rbSumX, rbSumY, rbWeight),
			leftCenter = toNormalPoint(leftSumX, leftSumY, leftWeight),
			rightCenter = toNormalPoint(rightSumX, rightSumY, rightWeight),
			center = toNormalPoint(totalSumX, totalSumY, totalWeight)
		)
	}

	var capturedHitmap : Bitmap? = null

	var isCaptured : Boolean = false

}