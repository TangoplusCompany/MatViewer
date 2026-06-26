package com.tangoplus.matviewer.data.db

data class MatRecordSummary(
	val sn: Int,
	val reg_date: String,
	val location: String?,
	val qr_code_name: String?
)