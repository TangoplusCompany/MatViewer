package com.tangoplus.matviewer.ui


import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangoplus.matviewer.R
import com.tangoplus.matviewer.databinding.ActivityMainBinding
import com.tangoplus.matviewer.ui.connect.ConnectFragment
import com.tangoplus.matviewer.ui.record.RecordFragment
import com.tangoplus.matviewer.ui.vm.FragmentViewModel

class MainActivity : AppCompatActivity() {
	private lateinit var bd: ActivityMainBinding
	private var selectedTabId = R.id.main
	private val fvm : FragmentViewModel by viewModels()
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		bd = ActivityMainBinding.inflate(layoutInflater)

//		val isTablet = resources.configuration.smallestScreenWidthDp >= 600
//		requestedOrientation = if (isTablet) ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT

		setContentView(bd.root)
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}
		// 상단, 하단 상태표시줄 넣기
		val controller = WindowCompat.getInsetsController(window, window.decorView)
		controller.apply {
			hide(WindowInsetsCompat.Type.systemBars())
			systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
		}
		checkMemoryUsage()
//		SoundManager.init(this@MainActivity)

		onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
		if (savedInstanceState == null) {
			supportFragmentManager.beginTransaction()
				.replace(R.id.flMain, ConnectFragment())
				.commit()
		}

		bd.bnv.setOnItemSelectedListener {
			setCurrentFragment(it.itemId)
			true
		}
		bd.bnv.setOnItemReselectedListener { setCurrentFragment(it.itemId) }
	}


	private var backPressedOnce = false
	private val backPressHandler = Handler(Looper.getMainLooper())
	private val backPressRunnable = Runnable { backPressedOnce = false }

	private val onBackPressedCallback = object : OnBackPressedCallback(true) {
		override fun handleOnBackPressed() {
			val fragmentManager = supportFragmentManager

			// 현재 표시 중인 Fragment 확인
			val currentFragment = fragmentManager.findFragmentById(R.id.flMain)

			if (currentFragment is ConnectFragment) {
				// MainFragment일 때만 앱 종료 로직
				if (backPressedOnce) {
					finishAffinity()
				} else {
					backPressedOnce = true
					Toast.makeText(this@MainActivity, "한 번 더 누르시면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show()
					backPressHandler.postDelayed(backPressRunnable, 1000)
				}
			} else {
				// 그 외 Fragment일 경우 -> 기본 back stack 동작 실행
				fragmentManager.popBackStack()
			}
		}
	}

	private fun checkMemoryUsage() {
		val activityManager = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
		val memInfo = ActivityManager.MemoryInfo()
		activityManager.getMemoryInfo(memInfo)

		val usagePercent =
			((memInfo.totalMem - memInfo.availMem).toFloat() / memInfo.totalMem * 100).toInt()

		if (usagePercent >= 89) {
			MaterialAlertDialogBuilder(
				this@MainActivity,
				R.style.ThemeOverlay_App_MaterialAlertDialog
			).apply {
				setTitle("경고")
				setMessage("메모리 사용량이 90%가 넘었습니다\n원활한 사용을 위해 앱 종료 후 재실행을 권장드립니다\n앱을 종료하시겠습니까?")
				setPositiveButton("예") { _, _ ->
					finishAffinity()
				}
				setNegativeButton("아니오") { _, _ -> }
			}
		}
	}
	private var previousTabId = R.id.nav_connect

	fun navigateToSubFragment(fragment: Fragment) {
		supportFragmentManager.beginTransaction().apply {
			// 부드러운 Fade In / Fade Out 효과
			// 뒤로가기 했을 때의 애니메이션까지 4개를 적어주는 것이 좋습니다.
			setCustomAnimations(
				R.anim.fade_in,
				android.R.anim.fade_out,
			)
			replace(R.id.flMain, fragment)
			// 핵심: 뒤로 가기 버튼을 누르면 이전 화면(바텀 탭 화면)으로 돌아오게 만듦
			addToBackStack(null)
			commit()
		}
	}

	private fun setCurrentFragment(itemId: Int) {
		val tabOrder = mapOf(
			R.id.nav_connect to 0,
			R.id.nav_history to 1,
		)

		val currentPosition = tabOrder[itemId] ?: 0
		val previousPosition = tabOrder[previousTabId] ?: 0

		// 방향 결정 (오른쪽으로 가는지, 왼쪽으로 가는지)
		val isMovingRight = currentPosition > previousPosition

		// 새로운 프래그먼트 생성
		val fragment = when (itemId) {
			R.id.nav_connect -> ConnectFragment()
			R.id.nav_history -> RecordFragment()
			else -> ConnectFragment()
		}

		when (fragment) {
			is ConnectFragment -> fvm.setCurrentFragment(FragmentViewModel.FragmentType.CONNECT)
			is RecordFragment -> fvm.setCurrentFragment(FragmentViewModel.FragmentType.RECORD)
		}

		supportFragmentManager.beginTransaction().apply {
			// 방향에 따라 애니메이션 설정
			if (isMovingRight) {
				// 오른쪽으로 이동 (0->1, 1->2)
				setCustomAnimations(
					R.anim.fade_in_right,  // 새 프래그먼트 enter
					R.anim.fade_out_left,  // 기존 프래그먼트 exit
				)
			} else {
				// 왼쪽으로 이동 (2->1, 1->0)
				setCustomAnimations(
					R.anim.fade_in_left,   // 새 프래그먼트 enter
					R.anim.fade_out_right, // 기존 프래그먼트 exit
				)
			}

			replace(R.id.flMain, fragment)
			commit()
		}

		// 현재 상태 저장
		previousTabId = itemId
		selectedTabId = itemId
	}


}