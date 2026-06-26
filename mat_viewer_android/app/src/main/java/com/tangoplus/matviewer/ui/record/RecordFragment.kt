package com.tangoplus.matviewer.ui.record

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.matviewer.data.db.MatDao
import com.tangoplus.matviewer.data.db.MatDatabase
import com.tangoplus.matviewer.databinding.FragmentRecordBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecordFragment : Fragment(), RecordSummaryItemClickListener
{
	private lateinit var bd : FragmentRecordBinding
	private lateinit var mDao : MatDao
	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		bd = FragmentRecordBinding.inflate(inflater)
		return bd.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val md = MatDatabase.getDatabase(requireContext())
		mDao = md.MatDao()

		lifecycleScope.launch(Dispatchers.IO) {
			val records = mDao.getAllMatRecordSummary()

			withContext(Dispatchers.Main) {
				val rvAdapter = RecordRVAdapter(requireContext(), records)
				rvAdapter.recordSummaryItemClickListener = this@RecordFragment
				val lManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
				bd.rvRecord.apply {
					adapter = rvAdapter
					layoutManager = lManager
				}
			}
		}
	}

	override fun onRecordSummaryItemClick(sn: Int) {
		Log.v("클릭됨", "$sn")
		val dialog = ResultDialogFragment.newInstance(sn)
		dialog.show(requireActivity().supportFragmentManager, "resultDialogFragment")
	}

}