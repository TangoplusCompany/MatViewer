package com.tangoplus.matviewer.ui.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.matviewer.R
import com.tangoplus.matviewer.databinding.RvButtonItemBinding
import com.tangoplus.matviewer.domain.vo.ButtonState
import com.tangoplus.matviewer.ui.MainActivity
import com.tangoplus.matviewer.ui.listener.OnButtonClickListener
import com.tangoplus.matviewer.ui.vm.MainViewModel

class ButtonRVAdapter(private val activity: MainActivity, private val vm: MainViewModel) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
	var onButtonClickListener : OnButtonClickListener? = null
	inner class ButtonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val tvBI: TextView = view.findViewById(R.id.tvBI)

		fun bind(currentItem: ButtonState) {
			tvBI.text = currentItem.displayName
			tvBI.setOnClickListener {
				if (vm.currentBtnState.value == currentItem) return@setOnClickListener
				onButtonClickListener?.onButtonClick(currentItem)
				notifyDataSetChanged()
			}

			tvBI.backgroundTintList  = if (currentItem == vm.currentBtnState.value)
				ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.white20)) else null
		}
	}

	override fun onCreateViewHolder(
		p0: ViewGroup,
		p1: Int
	): RecyclerView.ViewHolder {
		val inflater = LayoutInflater.from(p0.context)
		val bd = RvButtonItemBinding.inflate(inflater, p0, false)
		return ButtonViewHolder(bd.root)
	}

	override fun onBindViewHolder(
		p0: RecyclerView.ViewHolder,
		p1: Int
	) {
		if (p0 is ButtonViewHolder) {
			p0.bind(vm.btns[p1])
		}
	}


	override fun getItemCount(): Int {
		return vm.btns.size
	}
}