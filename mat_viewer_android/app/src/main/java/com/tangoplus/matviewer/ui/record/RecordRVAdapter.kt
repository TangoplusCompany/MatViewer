package com.tangoplus.matviewer.ui.record

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.matviewer.R
import com.tangoplus.matviewer.data.db.MatRecordSummary
import com.tangoplus.matviewer.databinding.RvRecordSummaryItemBinding
import com.tangoplus.matviewer.domain.util.FileUtil.getFileByName
import com.tangoplus.matviewer.domain.util.InterfaceUtil.setOnSingleClickListener

class RecordRVAdapter(val context : Context, val records: List<MatRecordSummary>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
	var recordSummaryItemClickListener : RecordSummaryItemClickListener? = null
	inner class RecordSummaryItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val clRSI : ConstraintLayout = view.findViewById(R.id.clRSI)
		val tvRSITitle: TextView = view.findViewById(R.id.tvRSITitle)
		val tvRSILocation: TextView = view.findViewById(R.id.tvRSILocation)
		val ivRSIQRCode: ImageView = view.findViewById(R.id.ivRSIQRCode)
		fun bind(currentItem: MatRecordSummary, idx: Int) {

			val rawDate = currentItem.reg_date

			if (rawDate.length >= 15) {
				val year = rawDate.substring(0, 4)
				val month = rawDate.substring(4, 6)
				val day = rawDate.substring(6, 8)

				val hour = rawDate.substring(9, 11)
				val minute = rawDate.substring(11, 13)
				val second = rawDate.substring(13, 15)

				tvRSITitle.text = "${year}년 ${month}월 ${day}일 ${hour}:${minute}:${second}"
			} else {
				tvRSITitle.text = rawDate // 예외 방어
			}
			tvRSILocation.text = "🚩측정 위치: ${currentItem.location ?: ""}"
			if (currentItem.qr_code_name != null) {
				val qrCodeUri = getFileByName(context, currentItem.qr_code_name)
				ivRSIQRCode.setImageURI(Uri.fromFile(qrCodeUri))
			}
			clRSI.setOnSingleClickListener {
				recordSummaryItemClickListener?.onRecordSummaryItemClick(currentItem.sn)
			}

		}
	}
	override fun onCreateViewHolder(
		p0: ViewGroup,
		p1: Int
	): RecyclerView.ViewHolder {
		val inflater = LayoutInflater.from(p0.context)
		val bd = RvRecordSummaryItemBinding.inflate(inflater, p0, false)
		return RecordSummaryItemViewHolder(bd.root)
	}

	override fun onBindViewHolder(
		p0: RecyclerView.ViewHolder,
		p1: Int
	) {
		if (p0 is RecordSummaryItemViewHolder) {
			p0.bind(records[p1], p1)
		}
	}

	override fun getItemCount(): Int {
		return records.size
	}
}