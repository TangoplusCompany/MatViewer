package com.tangoplus.matviewer.ui

import android.annotation.SuppressLint
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
import android.util.Size
import android.view.Surface
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
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
import com.tangoplus.matviewer.domain.util.HexUtil.applyAdaptiveDilation
import com.tangoplus.matviewer.domain.util.HexUtil.applyGaussianBlur
import com.tangoplus.matviewer.domain.util.HexUtil.applyWideBlur
import com.tangoplus.matviewer.domain.util.HexUtil.colorLUT
import com.tangoplus.matviewer.domain.util.HexUtil.extractBatteryCapacity
import com.tangoplus.matviewer.domain.util.HexUtil.findHeaderIndex
import com.tangoplus.matviewer.domain.util.HexUtil.normalizeData
import androidx.core.graphics.scale
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.tangoplus.facebeautyexpert.domain.vision.pose.PoseLandmarkerHelper
import com.tangoplus.matexample.PoseLandmarkAdapter
import com.tangoplus.matviewer.domain.util.PermissionUtil.register
import com.tangoplus.matviewer.domain.util.PermissionUtil.requestPermission
import com.tangoplus.matviewer.domain.util.PermissionUtil.showPermissionDeniedDialog
import com.tangoplus.matviewer.ui.view.OverlayView
import com.tangoplus.matviewer.ui.vm.MainViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.min

class MainActivity : AppCompatActivity(), PoseLandmarkerHelper.LandmarkerListener {
	private lateinit var bd: ActivityMainBinding
	private val vm: MainViewModel by viewModels()
	//  💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃💃
	private lateinit var plh : PoseLandmarkerHelper
	private var preview: Preview? = null
	private var imageAnalyzer: ImageAnalysis? = null
	private var camera: Camera? = null
	private var cameraProvider: ProcessCameraProvider? = null
	private var cameraFacing = CameraSelector.LENS_FACING_FRONT
	private lateinit var backgroundExecutor: ExecutorService

	//  👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣
	private val ACTION_USB_PERMISSION = "com.tangoplus.matviewer.USB_PERMISSION"
	private var usbIoManager: SerialInputOutputManager? = null
	private var port: UsbSerialPort? = null
	private var connection : UsbDeviceConnection? = null
	private var isRunning = false // 현재 수신 중인지 체크용 플래그
	private var byteBuffer = byteArrayOf()
	private val PACKET_SIZE = 67 // 구분자(4) + 헤더(3) + 데이터(64)
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
		// 💃💃💃💃💃💃💃💃
		register(
			activity = this,
			onPermissionGranted = {
				backgroundExecutor = Executors.newSingleThreadExecutor()
				backgroundExecutor.execute { initPose() }
				bd.viewFinder.post {
					setUpCamera()
				}
			},
			onPermissionDenied = {
				// 권한 거부됨
				showPermissionDeniedDialog(this)
			}
		)
		requestPermission(this)
		// 👣👣👣👣👣👣👣👣
		val filter = IntentFilter().apply {
			addAction(ACTION_USB_PERMISSION) // 기존 권한 요청 액션
			addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED) // USB 꽂힘 감지
			addAction(UsbManager.ACTION_USB_DEVICE_DETACHED) // USB 뽑힘 감지
		}
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
		Log.e("PoseLandmarkerHelper", "error: $error")
		Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
	}

	override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
		runOnUiThread {
			vm.pResult = PoseLandmarkAdapter.toCustomPoseLandmarkResult(resultBundle.results.first())
			if (vm.pResult != null) {
				bd.overlay.setResults(
					vm.pResult!!,
					resultBundle.inputImageWidth,
					resultBundle.inputImageHeight,
					OverlayView.RunningMode.LIVE_STREAM
				)
				bd.overlay.invalidate()
			}
		}
	}

	private fun setUpCamera() {
		val cameraProviderFuture =
			ProcessCameraProvider.getInstance(this)
		cameraProviderFuture.addListener(
			{
				// CameraProvider
				cameraProvider = cameraProviderFuture.get()
				// Build and bind the camera use cases
				bindCameraUseCases()
			}, ContextCompat.getMainExecutor(this)
		)
	}
	@SuppressLint("UnsafeOptInUsageError")
	private fun bindCameraUseCases() {
		val cameraProvider = cameraProvider
			?: throw IllegalStateException("Camera initialization failed.")

		val cameraSelector =
			CameraSelector.Builder().requireLensFacing(cameraFacing).build()

		// 회전 정보 가져오기
		val rotation = bd.viewFinder.display?.rotation ?: Surface.ROTATION_0
		Log.d("RotationDebug", "Display Rotation: $rotation")

		// 미리보기 설정
		preview = Preview.Builder()
			.setTargetResolution(Size(1920, 1080))
			.setTargetRotation(rotation)
			.build()

		// 중요: 이 시점에서 surfaceProvider 설정
		preview?.setSurfaceProvider(bd.viewFinder.surfaceProvider)

		// 이미지 분석 설정
		imageAnalyzer = ImageAnalysis.Builder()
			.setTargetResolution(Size(1920, 1080))
			.setTargetRotation(rotation)
			.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
			.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
			.build()
			.also {
				it.setAnalyzer(backgroundExecutor) { image ->
					detectPose(image)
				}
			}
		cameraProvider.unbindAll()

		try {
			camera = cameraProvider.bindToLifecycle(
				this, cameraSelector, imageAnalyzer, preview
			)
		} catch (e: IndexOutOfBoundsException) {
			Log.e("MSCameraIndex", "${e.message}")
		} catch (e: IllegalArgumentException) {
			Log.e("MSCameraIllegal", "${e.message}")
		} catch (e: IllegalStateException) {
			Log.e("MSCameraIllegal", "${e.message}")
		}catch (e: NullPointerException) {
			Log.e("MSCameraNull", "${e.message}")
		} catch (e: java.lang.Exception) {
			Log.e("MSCameraException", "${e.message}")
		}
	}
	private fun detectPose(imageProxy: ImageProxy) {
		if (this::plh.isInitialized) {
			plh.detectLiveStream(
				imageProxy = imageProxy,
				isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
			)
		}
		imageProxy.close()
	}

	//  👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣👣
	private fun displayHeatmap() {
		val rawHeight = 18
		val squareSize = 30 // 모든 연산의 기준 크기

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
						byteBuffer += data

						while (true) {
							// 1. 첫 번째 헤더 찾기
							val sync1 = findHeaderIndex(byteBuffer, 0)

							if (sync1 == -1) {
								// 헤더가 전혀 없으면 쓸모없는 데이터.
								// 단, 헤더가 잘려서 들어오는 중일 수 있으니 마지막 3바이트만 남김
								if (byteBuffer.size > 3) {
									byteBuffer = byteBuffer.takeLast(3).toByteArray()
								}
								break
							}

							// 헤더 앞의 쓰레기값 제거 (버퍼를 헤더 시작점부터로 정렬)
							if (sync1 > 0) {
								byteBuffer = byteBuffer.copyOfRange(sync1, byteBuffer.size)
								continue
							}

							// --- 이제 byteBuffer[0] ~ [3]은 무조건 [FF FE FF FE] ---

							// 2. 두 번째 헤더(다음 패킷의 시작점) 찾기 (최소 4바이트 이후부터 탐색)
							val sync2 = findHeaderIndex(byteBuffer, 4)

							if (sync2 == -1) {
								// 다음 헤더가 아직 안 들어온 경우
								if (byteBuffer.size < PACKET_SIZE) {
									break
								} else {
									val fullPacket = byteBuffer.copyOfRange(0, PACKET_SIZE)
									parsePacketData(fullPacket)
									byteBuffer = byteBuffer.copyOfRange(PACKET_SIZE, byteBuffer.size)
									continue
								}
							}

							// --- 두 번째 헤더가 발견됨 ---
							// sync2의 위치(index)가 곧 현재 패킷의 길이가 됩니다.

							if (sync2 == PACKET_SIZE) {
								val fullPacket = byteBuffer.copyOfRange(0, PACKET_SIZE)
								parsePacketData(fullPacket)

								// 처리한 67바이트를 버퍼에서 날리고, 두 번째 헤더부터 다시 루프 시작
								byteBuffer = byteBuffer.copyOfRange(PACKET_SIZE, byteBuffer.size)

							} else if (sync2 < PACKET_SIZE) {
								// 🚨 데이터 유실 감지! 67바이트가 다 안 모였는데 다음 프레임이 시작됨.
//								Log.e("PacketSync", "패킷 유실 감지: ${sync2}바이트만 수신됨. 폐기!")

								// 절대 억지로 이어붙이지 마세요! 앞부분을 버리고 새 헤더부터 다시 시작합니다.
								byteBuffer = byteBuffer.copyOfRange(sync2, byteBuffer.size)

							} else { // sync2 > PACKET_SIZE
								// 🚨 노이즈 삽입 감지! 중간에 쓰레기 데이터가 끼어들어 패킷이 길어짐.
//								Log.e("PacketSync", "노이즈 감지: 다음 헤더가 ${sync2} 위치에 있음. 폐기!")

								// 역시 오염된 데이터를 버리고 새 헤더부터 다시 시작합니다.
								byteBuffer = byteBuffer.copyOfRange(sync2, byteBuffer.size)
							}
						}
					}
					override fun onRunError(e: Exception) {
						runOnUiThread { bd.tvContent.append("\n에러: ${e.message}") }
						Log.e("에러임", "${e.printStackTrace()}")
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
		if (fullPacket.size != PACKET_SIZE) {
			Log.e("SensorData", "❌ Invalid packet size: ${fullPacket.size}")
			return
		}

		if (fullPacket[0] != 0xFF.toByte() || fullPacket[1] != 0xFE.toByte() ||
			fullPacket[2] != 0xFF.toByte() || fullPacket[3] != 0xFE.toByte()) {
			Log.e("SensorData", "❌ Invalid header")
			return
		}

		val frameData = fullPacket.copyOfRange(4, PACKET_SIZE)

		val batLow = frameData[0].toInt() and 0xFF
		val batHigh = frameData[1].toInt() and 0xFF
		currentBattery = (batHigh shl 8) or batLow

		val rowIndex = frameData[2].toInt() and 0xFF

		if (rowIndex !in 0..17) {
			Log.e("SensorData", "❌ Invalid row index: $rowIndex")
			return
		}

		val sensorValues = IntArray(30)
		var hasNonZeroValue = false
		var nonZeroCount = 0  // 🔍 0이 아닌 값 개수 카운트

		for (i in 0 until 30) {
			val low = frameData[3 + (i * 2)].toInt() and 0xFF
			val high = frameData[3 + (i * 2) + 1].toInt() and 0xFF
			val rawValue = ((high shl 8) or low).toShort().toInt()

			sensorValues[i] = if (rawValue < 0) 0 else rawValue

			if (sensorValues[i] != 0) {
				hasNonZeroValue = true
				nonZeroCount++
			}
		}

		currentFrameData[rowIndex] = sensorValues

		// 🔍 모든 행에 대해 0이 아닌 값 통계 출력
		Log.d("SensorStats", "Row $rowIndex: ${nonZeroCount}/30 non-zero values (Battery: $currentBattery%)")

		if (hasNonZeroValue) {
			Log.w("SensorData", "⚠️ Row $rowIndex has non-zero values:")
			sensorValues.forEachIndexed { index, value ->
				if (value != 0) {
					Log.w("SensorData", "  [Col $index] = $value (0x${value.toString(16).uppercase()})")
				}
			}
		}

		val battery = extractBatteryCapacity(fullPacket)
		runOnUiThread {
			bd.btnStop.text = "🪫 $battery% | 연결중지"
		}

		if (rowIndex == 0) {
			// 🔍 전체 프레임 통계 출력
			var totalNonZero = 0
			currentFrameData.forEach { (rowIndex, row) ->
				totalNonZero += row.count { it != 0 }
			}
			Log.i("FrameStats", "━━━━━ 전체 프레임: $totalNonZero/540 non-zero values ━━━━━")

			displayHeatmap()
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