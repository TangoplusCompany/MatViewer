package com.tangoplus.matviewer.ui

import android.app.DialogFragment
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tangoplus.matviewer.R
import com.tangoplus.matviewer.databinding.FragmentGuideDialogBinding

class GuideDialogFragment : DialogFragment() {
	private lateinit var bd : FragmentGuideDialogBinding

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		bd = FragmentGuideDialogBinding.inflate(inflater)
		return bd.root
	}

	@Deprecated("Deprecated in Java")
	override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

	}
}