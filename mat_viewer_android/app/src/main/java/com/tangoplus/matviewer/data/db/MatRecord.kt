package com.tangoplus.matviewer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "t_mat_record")
data class MatRecord(
	@PrimaryKey(autoGenerate = true) val sn: Int = 0,
	val reg_date : String = getCurrentTime(),
	val mat_image_name: String? = null,
	val qr_code_name: String? = null,
	val location : String? = null,
	val latitude: Double? = null,
	val longitude : Double? = null,
	val p_left: Int? = null,
	val p_right: Int? = null,
	val p_top: Int? = null,
	val p_bottom: Int? = null,
	val p_left_top : Int? = null,
	val p_left_bottom: Int? = null,
	val p_right_top : Int? = null,
	val p_right_bottom: Int? = null
) {
	companion object {
		fun getCurrentTime() : String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
	}
}
