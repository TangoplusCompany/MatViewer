package com.tangoplus.matviewer.domain.util

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

object PermissionUtil {
	private var permissionLauncher: ActivityResultLauncher<String>? = null
	private var onGranted: (() -> Unit)? = null
	private var onDenied: (() -> Unit)? = null

	fun register(
		activity: ComponentActivity,
		onPermissionGranted: () -> Unit,
		onPermissionDenied: () -> Unit
	) {
		onGranted = onPermissionGranted
		onDenied = onPermissionDenied

		permissionLauncher = activity.registerForActivityResult(
			ActivityResultContracts.RequestPermission()
		) { isGranted ->
			if (isGranted) {
				onGranted?.invoke()
			} else {
				onDenied?.invoke()
			}
		}
	}

	fun requestPermission(activity: ComponentActivity) {
		when {
			ContextCompat.checkSelfPermission(
				activity, Manifest.permission.CAMERA
			) == PackageManager.PERMISSION_GRANTED -> {
				onGranted?.invoke()
			}
			activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
				showRationaleDialog(activity)
			}
			else -> {
				permissionLauncher?.launch(Manifest.permission.CAMERA)
			}
		}
	}

	private fun showRationaleDialog(activity: ComponentActivity) {
		AlertDialog.Builder(activity)
			.setTitle("카메라 권한 필요")
			.setMessage("사진 촬영을 위해 카메라 권한이 필요합니다.")
			.setPositiveButton("확인") { _, _ ->
				permissionLauncher?.launch(Manifest.permission.CAMERA)
			}
			.setNegativeButton("취소", null)
			.show()
	}

	fun showPermissionDeniedDialog(activity: ComponentActivity) {
		AlertDialog.Builder(activity)
			.setTitle("권한 거부됨")
			.setMessage("카메라 권한이 거부되었습니다. 설정에서 권한을 허용해주세요.")
			.setPositiveButton("설정으로 이동") { _, _ ->
				val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
					data = Uri.fromParts("package", activity.packageName, null)
				}
				activity.startActivity(intent)
			}
			.setNegativeButton("취소", null)
			.show()
	}
}