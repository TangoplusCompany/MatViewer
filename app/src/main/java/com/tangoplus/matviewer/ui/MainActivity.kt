package com.tangoplus.matviewer.ui

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.Ch34xSerialDriver
import com.hoho.android.usbserial.driver.Cp21xxSerialDriver
import com.hoho.android.usbserial.driver.FtdiSerialDriver
import com.hoho.android.usbserial.driver.ProlificSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import com.tangoplus.matviewer.R
import com.tangoplus.matviewer.databinding.ActivityMainBinding
import com.tangoplus.matviewer.domain.uil.HexUtil.applyAdaptiveDilation
import com.tangoplus.matviewer.domain.uil.HexUtil.applyGaussianBlur
import com.tangoplus.matviewer.domain.uil.HexUtil.applyWideBlur
import com.tangoplus.matviewer.domain.uil.HexUtil.colorLUT
import com.tangoplus.matviewer.domain.uil.HexUtil.extractBatteryCapacity
import com.tangoplus.matviewer.domain.uil.HexUtil.findHeaderIndex
import com.tangoplus.matviewer.domain.uil.HexUtil.normalizeData
import androidx.core.graphics.scale
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.tangoplus.facebeautyexpert.domain.vision.pose.PoseLandmarkerHelper
import com.tangoplus.matviewer.ui.vm.MainViewModel

class MainActivity : AppCompatActivity(), PoseLandmarkerHelper.LandmarkerListener {
	private lateinit var bd: ActivityMainBinding
	private val vm: MainViewModel by viewModels()
	//  💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃
	private lateinit var plh : PoseLandmarkerHelper

	//  👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣
	private val ACTION_USB_PERMISSION = "com.tangoplus.matexample.USB_PERMISSION"
	private var usbIoManager: SerialInputOutputManager? = null
	private var port: UsbSerialPort? = null
	private var connection : UsbDeviceConnection? = null
	private var isRunning = false // 현재 수신 중인지 체크용 플래그
	private var byteBuffer = byteArrayOf()
	private val PACKET_SIZE = 71 // 구분자(4) + 헤더(3) + 데이터(64)
	private val currentFrameData = mutableMapOf<Int, IntArray>()
	private var currentBattery = 0 // 배터리 저장용




	override fun onDestroy() {
		super.onDestroy()
		stopReading()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		bd = ActivityMainBinding.inflate(layoutInflater)
		val isTablet = resources.configuration.smallestScreenWidthDp >= 600
		requestedOrientation = if (isTablet) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
		setContentView(bd.root)
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}
		initPose()
		val filter = IntentFilter().apply {
			addAction(ACTION_USB_PERMISSION) // 기존 권한 요청 액션
			addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED) // USB 꽂힘 감지
			addAction(UsbManager.ACTION_USB_DEVICE_DETACHED) // USB 뽑힘 감지
		}
		Log.v("커넥트시작", "connect시작")
		// Android 14(API 34) 이상 타겟팅 시 RECEIVER_NOT_EXPORTED 플래그 필요할 수 있음
		ContextCompat.registerReceiver(
			this,
			usbReceiver,
			filter,
			ContextCompat.RECEIVER_NOT_EXPORTED
		)
		connectUsbAndRead()

		bd.btnStop.setOnClickListener { stopReading() }

	}

	//  💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃
	private fun initPose() {
		try {
			Log.d("이닛", "initializing PoseLandmarker resources...")
			// 이제 this가 완전히 초기화된 상태
			try {
				plh = PoseLandmarkerHelper(
					context = this, // 이제 this가 완전히 초기화된 상태
					runningMode = RunningMode.LIVE_STREAM,
					minPoseDetectionConfidence = PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE,
					minPoseTrackingConfidence = PoseLandmarkerHelper.DEFAULT_POSE_TRACKING_CONFIDENCE,
					minPosePresenceConfidence = PoseLandmarkerHelper.DEFAULT_POSE_PRESENCE_CONFIDENCE,
					currentDelegate = PoseLandmarkerHelper.DELEGATE_CPU,
					poseLandmarkerHelperListener = this
				)
				Log.d("PoseLandmarker", "Initialization successful!")
			} catch (e: Exception) {
				Log.e("PoseLandmarker", "Failed to initialize: ${e.message}", e)
			}
			Log.d("이닛", "PoseLandmarker initialized.")
		} catch (e: Exception) {
			Log.e("이닛", "Failed to initialze pose landmarker: ${e.message}", e)
		}
	}

	override fun onError(error: String, errorCode: Int) {
		TODO("Not yet implemented")
	}

	override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
		TODO("Not yet implemented")
	}
	//  👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣
	private fun displayHeatmap() {
		val rawHeight = 18
		val squareSize = 32 // 모든 연산의 기준 크기

		val DILATION_RADIUS = 1
		val BLUR_RADIUS = 5f
		val BRIDGE_STRENGTH = 1.2f // Base 레이어(다리)의 강도

		val stretchedData = Array(squareSize) { IntArray(squareSize) }

		for (y in 0 until squareSize) {
			// 현재 픽셀 위치(y)가 원본 데이터의 어떤 인덱스(srcY)에 해당하는지 계산
			// 32 x 18개의 데이터로 매핑
			val srcY = (y * (rawHeight - 1).toFloat() / (squareSize - 1)).toInt()

			for (x in 0 until squareSize) {
				val value = currentFrameData[srcY]?.get(x) ?: 0
				stretchedData[y][x] = if (value > 4096) 0 else value
			}
		}

		// 2. 적응형 팽창 (32x32 데이터 위에서 동작)
		val dilatedData = applyAdaptiveDilation(stretchedData, DILATION_RADIUS)

		// 3. 이중 레이어 스무딩 (끊어진 부위 메우기)
		val peakData = applyGaussianBlur(dilatedData) // 3x3 가우시안 (기존 피크 유지)
		val baseData = applyWideBlur(dilatedData)    // 5x5 가우시안 (넓은 다리 생성)

		// 3-3. 레이어 결합: Peak와 Base 중 큰 값을 선택하여 빈 공간 메움
		val combinedData = Array(squareSize) { IntArray(squareSize) }
		for (y in 0 until squareSize) {
			for (x in 0 until squareSize) {
				val bridgeValue = (baseData[y][x] * BRIDGE_STRENGTH).toInt()
				combinedData[y][x] = maxOf(peakData[y][x], bridgeValue)
			}
		}

		// 4. 동적 정규화
		val normalizedData = normalizeData(combinedData)

		// 5. 비트맵 생성 (데이터가 이미 32x32이므로 루프가 매우 단순해짐)
		val squareBitmap = createBitmap(squareSize, squareSize)
		val squarePixels = IntArray(squareSize * squareSize)

		for (y in 0 until squareSize) {
			for (x in 0 until squareSize) {
				// 정규화된 Float(0~1)를 0~255 Grayscale로 변환
				val gray = (normalizedData[y][x] * 255).toInt().coerceIn(0, 255)
				squarePixels[y * squareSize + x] = Color.argb(255, gray, gray, gray)
			}
		}
		squareBitmap.setPixels(squarePixels, 0, squareSize, 0, 0, squareSize, squareSize)

		// 6. 업스케일 및 최종 렌더링 블러
		val targetSize = if (bd.ivHeatmap.width > 0) bd.ivHeatmap.width else 640
		val scaledBitmap = squareBitmap.scale(targetSize, targetSize)
		val blurredBitmap = blurBitmap(scaledBitmap, BLUR_RADIUS)

		// 7. 최종 색상 매핑 (LUT 적용)
		val finalPixels = IntArray(targetSize * targetSize)
		blurredBitmap.getPixels(finalPixels, 0, targetSize, 0, 0, targetSize, targetSize)

		for (i in finalPixels.indices) {
			val grayValue = Color.red(finalPixels[i]) // R, G, B 값이 모두 같으므로 하나만 추출
			finalPixels[i] = colorLUT[grayValue]
		}

		val finalHeatmap = createBitmap(targetSize, targetSize)
		finalHeatmap.setPixels(finalPixels, 0, targetSize, 0, 0, targetSize, targetSize)

		// UI 업데이트
		runOnUiThread {
			bd.ivHeatmap.setImageBitmap(finalHeatmap)
		}
	}
	private fun stopReading() {
		try {
			usbIoManager?.stop() // 수신 쓰레드 정지
			usbIoManager = null

			// 포트 닫기
			port?.close()
			port = null
			// connection 닫기
			connection?.close()
			connection = null

			isRunning = false
			bd.tvContent.append("\n--- 수신 중단됨 ---", 0, 1)
		} catch (e: Exception) {
			Log.e("UsbData", "정지 중 에러: ${e.message}")
		}
	}


	private val usbReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			val action = intent.action
			Log.d("USB_RECEIVER", "=== onReceive 호출됨 ===")
			Log.d("USB_RECEIVER", "Action: ${intent.action}")
			when (action) {
				ACTION_USB_PERMISSION -> {
					synchronized(this) {
						val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
						val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)

						Log.d("USB_RECEIVER", "디바이스: ${device?.deviceName}")
						Log.d("USB_RECEIVER", "권한 허용 여부: $granted")

						if (granted) {
							bd.tvContent.text = "권한 획득 성공!"
							device?.let {
								connectWithDevice(it)
							}
						} else {
							bd.tvContent.text = "USB 권한이 거부되었습니다."
						}
					}
				}

				UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
					Log.d("USB_RECEIVER", "새로운 USB 디바이스 연결 감지됨!")
					bd.tvContent.text = "USB 연결 감지됨. 장치 탐색 중..."
					connectUsbAndRead()
				}

				UsbManager.ACTION_USB_DEVICE_DETACHED -> {
					val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
					Log.d("USB_RECEIVER", "USB 디바이스 해제됨: ${device?.deviceName}")
					bd.tvContent.text = "USB 연결이 해제되었습니다."

					// 중요: 물리적으로 뽑혔으므로 즉시 포트를 닫고 통신 스레드를 종료해야 함
					stopReading()
				}
			}
		}
	}
	private fun connectWithDevice(device: UsbDevice) {
		val manager = getSystemService(USB_SERVICE) as UsbManager
		Log.v("유에스비", "connect device: ${manager.deviceList}")

		// Mass Storage인지 확인
		Log.d("USB_DEBUG", "Device Class: ${device.deviceClass}")
		Log.d("USB_DEBUG", "Product Name: ${device.productName}")
		runOnUiThread {
			bd.tvContent.text = "productName: ${device.productName}, productId: ${device.productId} deviceClass: ${device.deviceClass}, deviceId: ${device.deviceId}\nmanufactureName: ${device.manufacturerName}, deviceProtocol: ${device.deviceProtocol}"
		}
		Log.d("USB_DEBUG", "Interfaces: ${device.interfaceCount}")
		val driver = UsbSerialProber.getDefaultProber().probeDevice(device) ?: return
		connection = manager.openDevice(driver.device) ?: return
		runOnUiThread {
			Toast.makeText(this@MainActivity, "커넥팅시작", Toast.LENGTH_SHORT).show()
		}
		port = driver.ports[0]
		Log.v("포트", "${port}, ${driver.ports}")
		try {
			port?.let { p ->
				Log.v("포트","${p.isOpen}")
				p.open(connection)
				Log.v("포트","${p.isOpen}")
				p.setParameters(460805, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

				p.dtr = true
				p.rts = true

				usbIoManager = SerialInputOutputManager(port, object : SerialInputOutputManager.Listener {
					override fun onNewData(data: ByteArray) {
//						Log.v("data", data.toHexString())
						byteBuffer += data

						while (true) {
							// 1. 현재 헤더 (FF FE FF FE) 인덱스 탐색 및 버퍼 정렬
							val syncIdx = findHeaderIndex(byteBuffer, 0)
							if (syncIdx == -1) {
								if (byteBuffer.size > 3) byteBuffer = byteBuffer.takeLast(3).toByteArray()
								break
							}
							if (syncIdx > 0) byteBuffer = byteBuffer.copyOfRange(syncIdx, byteBuffer.size)

							// 2. [핵심 로직] '다음 헤더'가 어디 있는지 탐색 (현재 헤더 길이 4바이트 이후부터)
							val nextSyncIdx = findHeaderIndex(byteBuffer, 4)

							// 3. 데이터 유실 감지: 다음 헤더가 PACKET_SIZE(71)보다 일찍 등장한 경우!
							if (nextSyncIdx != -1 && nextSyncIdx < PACKET_SIZE) {
								// 유실된 데이터 프레임입니다. 사용자가 제안한 대로 남은 부분을 0으로 채웁니다.
								val fullPacket = ByteArray(PACKET_SIZE) // 기본적으로 모두 0으로 초기화됨
								// 들어온 만큼만 복사 (나머지는 0으로 유지됨)
								System.arraycopy(byteBuffer, 0, fullPacket, 0, nextSyncIdx)

								// 데이터 파싱 진행 (0으로 채워진 불완전 패킷 처리)
								parsePacketData(fullPacket)

								// 버퍼를 '다음 헤더' 위치로 즉시 이동시켜 다음 프레임이 정상 작동하게 함
								byteBuffer = byteBuffer.copyOfRange(nextSyncIdx, byteBuffer.size)
								continue
							}

							// 4. 정상적인 경우: PACKET_SIZE 이상 데이터가 쌓였을 때
							if (byteBuffer.size >= PACKET_SIZE) {
								val fullPacket = byteBuffer.copyOfRange(0, PACKET_SIZE)

								// 정상 데이터 파싱 진행
								parsePacketData(fullPacket)

								// 처리된 패킷 버퍼에서 제거
								byteBuffer = byteBuffer.copyOfRange(PACKET_SIZE, byteBuffer.size)

							} else {
								// 아직 데이터가 다 안 들어왔으므로 다음 수신을 기다림
								break
							}
						}
					}
					override fun onRunError(e: Exception) {
						runOnUiThread { bd.tvContent.append("\n에러: ${e.message}") }
						Log.getStackTraceString(e)
					}
				})
				usbIoManager?.start()
			}
		} catch (e: Exception) {
			bd.tvContent.text = "연결 실패: ${e.message}"
		}
	}

	fun connectUsbAndRead() {
		Log.d("USB_DEBUG", "=== connectUsbAndRead 시작 ===")

		val manager = getSystemService(USB_SERVICE) as UsbManager
		Log.d("USB_DEBUG", "UsbManager 획득 완료")

		val deviceList = manager.deviceList
		var targetDevice: UsbDevice? = null
		var selectedDriver: UsbSerialDriver? = null
		Log.d("USB_DEBUG", "deviceList: ${deviceList})")
		// 커스텀 ProbeTable
		val customTable = UsbSerialProber.getDefaultProbeTable().apply {
			addProduct(0x6820, 0x3254, Ch34xSerialDriver::class.java)
			addProduct(0x6820, 0x3254, CdcAcmSerialDriver::class.java)
			addProduct(0x6820, 0x3254, Cp21xxSerialDriver::class.java)
			addProduct(0x6820, 0x3254, FtdiSerialDriver::class.java)
			addProduct(0x6820, 0x3254, ProlificSerialDriver::class.java)
		}
		val customProber = UsbSerialProber(customTable)

		for ((_, device) in deviceList) {
			// 1. 핵심 수정: 내부 인터페이스를 뒤져서 Mass Storage(8)가 포함되어 있는지 확인
			var isMassStorage = false
			for (i in 0 until device.interfaceCount) {
				if (device.getInterface(i).interfaceClass == 8) {
					isMassStorage = true
					break
				}
			}

			if (isMassStorage) {
				Log.d("USB_DEBUG", "저장장치 스킵: ${device.productName} (VID:${device.vendorId})")
				continue // Mass Storage면 드라이버 찾지 않고 다음 장치로 넘어감
			}

			// 2. 시리얼 드라이버 찾기 (자동)
			var driver = customProber.probeDevice(device)

			// 3. 시리얼 드라이버 찾기 (수동 - 기존 로직 유지)
			if (driver == null) {
				val fallbackDrivers = listOf(
					Ch34xSerialDriver(device),
					CdcAcmSerialDriver(device),
					Cp21xxSerialDriver(device),
					FtdiSerialDriver(device),
					ProlificSerialDriver(device)
				)
				for (fallback in fallbackDrivers) {
					try {
						if (fallback.ports.isNotEmpty()) {
							driver = fallback
							break
						}
					} catch (e: Exception) {
						Log.e("USB_DEBUG", "${fallback.javaClass.simpleName} 실패: ${e.message}")
					}
				}
			}

			// 드라이버를 찾았으면 연결 대상 확정 후 루프 탈출
			if (driver != null) {
				targetDevice = device
				selectedDriver = driver
				Log.d("USB_DEBUG", "성공! 선택된 드라이버: ${driver.javaClass.simpleName}")
				break
			}
		}

		if (targetDevice == null ) {
			Log.e("USB_DEBUG", "모든 드라이버 시도 실패")
			runOnUiThread {
				bd.tvContent.text = "targetDevice is null"
			}
			return
		}

		Log.d("USB_DEBUG", "선택된 디바이스: ${targetDevice.deviceName}")

		// 권한 확인 및 연결
		if (!manager.hasPermission(targetDevice)) {
			Log.w("USB_DEBUG", "권한 없음 - 권한 요청 시작")
			val flags = PendingIntent.FLAG_MUTABLE
			val intent = Intent(ACTION_USB_PERMISSION).apply { setPackage(packageName) }
			val permissionIntent = PendingIntent.getBroadcast(this, 0, intent, flags)
			manager.requestPermission(targetDevice, permissionIntent)
			Log.d("USB_DEBUG", "권한 요청 완료")
			return
		}

		Log.d("USB_DEBUG", "권한 있음 - 연결 시도")
		val connection = manager.openDevice(targetDevice)

		if (connection == null) {
			bd.tvContent.text = "USB 접근 권한이 없습니다."
			Log.e("USB_DEBUG", "connection null")
			return
		}

		Log.d("USB_DEBUG", "connection 성공 - connectWithDevice 호출")
		connectWithDevice(targetDevice)
	}

	private fun parsePacketData(fullPacket: ByteArray) {
		val frameData = fullPacket.copyOfRange(4, PACKET_SIZE) // 헤더 제외

		val batLow = frameData[0].toInt() and 0xFF
		val batHigh = frameData[1].toInt() and 0xFF
		currentBattery = (batHigh shl 8) or batLow

		val rowIndex = frameData[2].toInt() and 0xFF

		if (rowIndex in 0..17) {
			val sensorValues = IntArray(32)
			for (i in 0 until 32) {
				val low = frameData[3 + (i * 2)].toInt() and 0xFF
				val high = frameData[3 + (i * 2) + 1].toInt() and 0xFF
				val rawValue = ((high shl 8) or low).toShort().toInt()

				sensorValues[i] = if (rawValue < 0) 0 else rawValue
			}

			currentFrameData[rowIndex] = sensorValues
			val battery = extractBatteryCapacity(fullPacket)
			runOnUiThread {
				bd.btnStop.text = "🪫 $battery% | 연결중지"
			}
			if (rowIndex == 0) {
//				displayFullFrame()
				displayHeatmap()
			}
		}
	}

	private fun blurBitmap(bitmap: Bitmap, radius: Float): Bitmap {
		val outBitmap = createBitmap(bitmap.width, bitmap.height)
		val rs = RenderScript.create(this)
		val blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
		val allIn = Allocation.createFromBitmap(rs, bitmap)
		val allOut = Allocation.createFromBitmap(rs, outBitmap)

		blurScript.setRadius(radius)
		blurScript.setInput(allIn)
		blurScript.forEach(allOut)
		allOut.copyTo(outBitmap)

		rs.destroy()
		return outBitmap
	}

}