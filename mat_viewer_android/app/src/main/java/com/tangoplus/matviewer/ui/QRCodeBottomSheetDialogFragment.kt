package com.tangoplus.matviewer.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tangoplus.matviewer.R
import com.tangoplus.matviewer.databinding.FragmentQRCodeBottomSheetDialogBinding

class QRCodeBottomSheetDialogFragment : BottomSheetDialogFragment() {
	private lateinit var bd : FragmentQRCodeBottomSheetDialogBinding

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		bd = FragmentQRCodeBottomSheetDialogBinding.inflate(inflater)
		return bd.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		bd.btnQrExit.setOnClickListener { dismiss() }
	}

}