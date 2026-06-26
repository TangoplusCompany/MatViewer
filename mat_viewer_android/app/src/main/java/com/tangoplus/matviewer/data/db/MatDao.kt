package com.tangoplus.matviewer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MatDao {

	@Insert
	suspend fun insertMatRecord(record: MatRecord) : Long

	@Query("SELECT sn, reg_date, location, qr_code_name FROM t_mat_record ORDER BY reg_date DESC")
	suspend fun getAllMatRecordSummary() : List<MatRecordSummary>

	@Query("SELECT * FROM t_mat_record WHERE sn = :sn")
	suspend fun getMatRecord(sn: Int) : MatRecord


}