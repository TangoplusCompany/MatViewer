package com.tangoplus.matviewer.ui


import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.AnimatedVectorDrawable
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.provider.Settings
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import androidx.core.graphics.createBitmap
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.Ch34xSerialDriver
import com.hoho.android.usbserial.driver.Cp21xxSerialDriver
import com.hoho.android.usbserial.driver.FtdiSerialDriver
import com.hoho.android.usbserial.driver.ProlificSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialDriver
import androidx.core.graphics.toColorInt
import kotlin.math.roundToInt
import kotlin.math.sqrt
import androidx.core.graphics.scale
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.tangoplus.matviewer.R
import com.tangoplus.matviewer.databinding.ActivityMainBinding
import com.tangoplus.matviewer.domain.util.InterfaceUtil.setOnSingleClickListener
import com.tangoplus.matviewer.domain.util.QRCodeUtil.generateQrCode
import com.tangoplus.matviewer.ui.vm.HeatmapViewModel

class MainActivity : AppCompatActivity() {
	private lateinit var bd: ActivityMainBinding
	private val ACTION_USB_PERMISSION = "com.tangoplus.matexample.USB_PERMISSION"
	private val hvm : HeatmapViewModel by viewModels()
	private lateinit var finalHeatmap : Bitmap
	private lateinit var fusedLocationClient: FusedLocationProviderClient

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
		val filter = IntentFilter().apply {
			addAction(ACTION_USB_PERMISSION) // 기존 권한 요청 액션
			addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED) // USB 꽂힘 감지
			addAction(UsbManager.ACTION_USB_DEVICE_DETACHED) // USB 뽑힘 감지
		}
		Log.v("커넥트시작", "connect시작")
		val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

		Log.v("deviceId", "$deviceId")
		// 위치 정보
		fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

		// 2. 위치 권한이 있는지 확인 후 위도/경도 가져오기 함수 실행
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			preloadDeviceLocation()
		} else {
			ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1000)
		}

		// Android 14(API 34) 이상 타겟팅 시 RECEIVER_NOT_EXPORTED 플래그 필요할 수 있음
		ContextCompat.registerReceiver(
			this,
			usbReceiver,
			filter,
			ContextCompat.RECEIVER_NOT_EXPORTED
		)
		connectUsbAndRead()

		bd.btnStop.setOnSingleClickListener {
			androidx.appcompat.app.AlertDialog.Builder(this)
				.setTitle("종료 확인")
				.setMessage("정말 읽기를 중단하시겠습니까?")
				.setPositiveButton("확인") { _, _ ->
					stopReading() // 사용자가 확인을 눌렀을 때만 실행
				}
				.setNegativeButton("취소", null) // 취소를 누르면 창만 닫힘
				.show()
		}

		bd.btnDownload.setOnSingleClickListener {
			setUploadAndDownloadProcess(hvm.currentAddress)
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
							runOnUiThread { hideWaitedUI() }
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
					showWaitedUI()
				}
			}
		}
	}
	private var usbIoManager: SerialInputOutputManager? = null
	private var port: UsbSerialPort? = null
	private var connection : UsbDeviceConnection? = null
	private var isRunning = false // 현재 수신 중인지 체크용 플래그
	private var byteBuffer = byteArrayOf()
	private val PREAMBLE = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0xFF.toByte(), 0xFE.toByte())
	private val PACKET_SIZE = 71 // 구분자(4) + 헤더(3) + 데이터(64)

	private val currentFrameData = mutableMapOf<Int, IntArray>()
	private var currentBattery = 0 // 배터리 저장용

	private fun connectWithDevice(device: UsbDevice) {
		val manager = getSystemService(USB_SERVICE) as UsbManager
		Log.v("유에스비", "connect device: ${manager.deviceList}")

		// Mass Storage인지 확인
		Log.d("USB_DEBUG", "Device Class: ${device.deviceClass}")
		Log.d("USB_DEBUG", "Product Name: ${device.productName}")
		runOnUiThread {
			hideWaitedUI()
			bd.tvContent.text = "장치정보\nproductName: ${device.productName}, productId: ${device.productId} deviceClass: ${device.deviceClass}, deviceId: ${device.deviceId}\nmanufactureName: ${device.manufacturerName}, deviceProtocol: ${device.deviceProtocol}"
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
				usbIoManager = SerialInputOutputManager(port, object:
					SerialInputOutputManager.Listener {
					override fun onNewData(data: ByteArray) {
						byteBuffer += data

						while (true) {
							// 1. 현재 헤더 (FF FE FF FE) 인덱스 탐색 및 버퍼 정렬
							val syncIdx = findHeaderIndex(byteBuffer, 0)
							if (syncIdx == -1) {
								if (byteBuffer.size > 3) byteBuffer = byteBuffer.takeLast(3).toByteArray()
								break
							}
							if (syncIdx > 0) byteBuffer = byteBuffer.copyOfRange(syncIdx, byteBuffer.size)

							val nextSyncIdx = findHeaderIndex(byteBuffer, 4)

							if (nextSyncIdx != -1 && nextSyncIdx < PACKET_SIZE) {
								val fullPacket = ByteArray(PACKET_SIZE)
								System.arraycopy(byteBuffer, 0, fullPacket, 0, nextSyncIdx)
								parsePacketData(fullPacket)
								byteBuffer = byteBuffer.copyOfRange(nextSyncIdx, byteBuffer.size)
								continue
							}
							if (byteBuffer.size >= PACKET_SIZE) {
								val fullPacket = byteBuffer.copyOfRange(0, PACKET_SIZE)
								parsePacketData(fullPacket)
								byteBuffer = byteBuffer.copyOfRange(PACKET_SIZE, byteBuffer.size)
							} else {
								break
							}
						}
					}
					override fun onRunError(e: java.lang.Exception) {
						runOnUiThread { bd.tvContent.append("에러: ${e.message}") }
						Log.getStackTraceString(e)
					}
				})
//				usbIoManager = SerialInputOutputManager(port, object : SerialInputOutputManager.Listener {
//					override fun onNewData(data: ByteArray) {
////						Log.v("data", data.toHexString())
//						byteBuffer += data
//
//						while (true) {
//							// 1. 현재 헤더 (FF FE FF FE) 인덱스 탐색 및 버퍼 정렬
//							val syncIdx = findHeaderIndex(byteBuffer, 0)
//							if (syncIdx == -1) {
//								if (byteBuffer.size > 3) byteBuffer = byteBuffer.takeLast(3).toByteArray()
//								break
//							}
//							if (syncIdx > 0) byteBuffer = byteBuffer.copyOfRange(syncIdx, byteBuffer.size)
//
//							// 2. [핵심 로직] '다음 헤더'가 어디 있는지 탐색 (현재 헤더 길이 4바이트 이후부터)
//							val nextSyncIdx = findHeaderIndex(byteBuffer, 4)
//
//							// 3. 데이터 유실 감지: 다음 헤더가 PACKET_SIZE(71)보다 일찍 등장한 경우!
//							if (nextSyncIdx != -1 && nextSyncIdx < PACKET_SIZE) {
//								// 유실된 데이터 프레임입니다. 사용자가 제안한 대로 남은 부분을 0으로 채웁니다.
//								val fullPacket = ByteArray(PACKET_SIZE) // 기본적으로 모두 0으로 초기화됨
//								// 들어온 만큼만 복사 (나머지는 0으로 유지됨)
//								System.arraycopy(byteBuffer, 0, fullPacket, 0, nextSyncIdx)
//
//								// 데이터 파싱 진행 (0으로 채워진 불완전 패킷 처리)
//								parsePacketData(fullPacket)
//
//								// 버퍼를 '다음 헤더' 위치로 즉시 이동시켜 다음 프레임이 정상 작동하게 함
//								byteBuffer = byteBuffer.copyOfRange(nextSyncIdx, byteBuffer.size)
//								continue
//							}
//
//							// 4. 정상적인 경우: PACKET_SIZE 이상 데이터가 쌓였을 때
//							if (byteBuffer.size >= PACKET_SIZE) {
//								val fullPacket = byteBuffer.copyOfRange(0, PACKET_SIZE)
//
//								// 정상 데이터 파싱 진행
//								parsePacketData(fullPacket)
//
//								// 처리된 패킷 버퍼에서 제거
//								byteBuffer = byteBuffer.copyOfRange(PACKET_SIZE, byteBuffer.size)
//
//							} else {
//								// 아직 데이터가 다 안 들어왔으므로 다음 수신을 기다림
//								break
//							}
//						}
//					}
//					override fun onRunError(e: Exception) {
//						runOnUiThread { bd.tvContent.append("\n에러: ${e.message}") }
//						Log.getStackTraceString(e)
//					}
//				})
				usbIoManager?.start()
			}
		} catch (e: Exception) {
			bd.tvContent.text = "연결 실패: ${e.message}"
		}
	}

	fun connectUsbAndRead() {
		val manager = getSystemService(USB_SERVICE) as UsbManager

		val deviceList = manager.deviceList
		var targetDevice: UsbDevice? = null
		var selectedDriver: UsbSerialDriver? = null
		Log.d("USB_DEBUG", "deviceList: ${deviceList})")

		val customTable = UsbSerialProber.getDefaultProbeTable().apply {
			addProduct(0x6820, 0x3254, Ch34xSerialDriver::class.java)
			addProduct(0x6820, 0x3254, CdcAcmSerialDriver::class.java)
			addProduct(0x6820, 0x3254, Cp21xxSerialDriver::class.java)
			addProduct(0x6820, 0x3254, FtdiSerialDriver::class.java)
			addProduct(0x6820, 0x3254, ProlificSerialDriver::class.java)
		}
		val customProber = UsbSerialProber(customTable)

		for ((_, device) in deviceList) {
			var isMassStorage = false
			for (i in 0 until device.interfaceCount) {
				if (device.getInterface(i).interfaceClass == 8) {
					isMassStorage = true
					break
				}
			}

			if (isMassStorage) {
				Log.d("USB_DEBUG", "저장장치 스킵: ${device.productName} (VID:${device.vendorId})")
				continue
			}

			var driver = customProber.probeDevice(device)
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
				showWaitedUI()
				Log.v("targetDevice ","targetDevice is null")
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


	override fun onDestroy() {
		super.onDestroy()
		stopReading()
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
		} catch (e: Exception) {
			Log.e("UsbData", "정지 중 에러: ${e.message}")
		}
	}

	private fun findHeaderIndex(source: ByteArray, startIndex: Int): Int {
		if (startIndex > source.size - PREAMBLE.size) return -1
		for (i in startIndex..source.size - PREAMBLE.size) {
			if (source[i] == PREAMBLE[0] && source[i + 1] == PREAMBLE[1] &&
				source[i + 2] == PREAMBLE[2] && source[i + 3] == PREAMBLE[3]) {
				return i
			}
		}
		return -1
	}

	private fun displayFullFrame() {
		val sb = StringBuilder()
		for (i in 0..17) {
			val rowData = currentFrameData[i]
			if (rowData != null) {
				// 4095가 최대값이므로 최소 4자리의 공간(padStart(4))을 확보합니다.
				sb.append(rowData.joinToString(" ") { it.toString().padStart(4, ' ') })
			} else {
				// 빈 데이터 영역도 4자리 표기(----)로 맞춥니다.
				sb.append(Array(32) { "----" }.joinToString(" "))
			}
			if (i < 17) {
				sb.append("\n")
			}
		}
		runOnUiThread {
			bd.tvContent.text = sb.toString()
		}
	}

	private fun displayHeatmap() {
		val rawWidth = 32
		val rawHeight = 18
		val squareSize = 32

		val stretchedData = Array(squareSize) { FloatArray(squareSize) }
		var maxVal = 1f // 정규화를 위한 최대값

		for (y in 0 until squareSize) {
			val srcY = (y * (rawHeight - 1).toFloat() / (squareSize - 1)).toInt()
			for (x in 0 until squareSize) {
				val value = currentFrameData[srcY]?.get(x) ?: 0
				val safeValue = if (value > 4096) 0f else value.toFloat()
				stretchedData[y][x] = safeValue
				if (safeValue > maxVal) maxVal = safeValue
			}
		}

		val squareBitmap = createBitmap(squareSize, squareSize)
		val squarePixels = IntArray(squareSize * squareSize)

		for (y in 0 until squareSize) {
			for (x in 0 until squareSize) {
				val normalized = (stretchedData[y][x] / maxVal).coerceIn(0f, 1f)

				val boosted = sqrt(normalized)

				val gray = (boosted * 255).toInt().coerceIn(0, 255)
				squarePixels[y * squareSize + x] = Color.argb(255, gray, gray, gray)
			}
		}
		squareBitmap.setPixels(squarePixels, 0, squareSize, 0, 0, squareSize, squareSize)

		val midSize = 128
		val midBitmap = squareBitmap.scale(midSize, midSize)
		val blurredMid = blurBitmap(midBitmap, 15f)

		val targetSize = if (bd.ivHeatmap.width > 0) bd.ivHeatmap.width else 640
		val finalScaled = blurredMid.scale(targetSize, targetSize)
		val blurredFinal = blurBitmap(finalScaled, 10f) // 픽셀 깨짐만 한 번 더 잡아줌

		val finalPixels = IntArray(targetSize * targetSize)
		blurredFinal.getPixels(finalPixels, 0, targetSize, 0, 0, targetSize, targetSize)

		for (i in finalPixels.indices) {
			val grayValue = Color.red(finalPixels[i])
			finalPixels[i] = colorLUT[grayValue]
		}

		finalHeatmap = createBitmap(targetSize, targetSize)
		finalHeatmap.setPixels(finalPixels, 0, targetSize, 0, 0, targetSize, targetSize)

		// 🥩🥩🥩🥩🥩🥩🥩🥩🥩 COP
		var minX = squareSize.toFloat(); var maxX = -1f
		var minY = squareSize.toFloat(); var maxY = -1f
		var totalWeight = 0f

		for (y in 0 until squareSize) {
			for (x in 0 until squareSize) {
				val weight = stretchedData[y][x]
				if (weight > 50f) {
					if (x < minX) minX = x.toFloat()
					if (x > maxX) maxX = x.toFloat()
					if (y < minY) minY = y.toFloat()
					if (y > maxY) maxY = y.toFloat()
					totalWeight += weight
				}
			}
		}

		// 데이터가 있을 때만 실행
		if (totalWeight > 0f) {
			val centerX = (minX + maxX) / 2f
			val centerY = (minY + maxY) / 2f

			var ltSumX = 0f; var ltSumY = 0f; var ltWeight = 0f // 좌상
			var lbSumX = 0f; var lbSumY = 0f; var lbWeight = 0f // 좌하
			var rtSumX = 0f; var rtSumY = 0f; var rtWeight = 0f // 우상
			var rbSumX = 0f; var rbSumY = 0f; var rbWeight = 0f // 우하

			for (y in 0 until squareSize) {
				for (x in 0 until squareSize) {
					val weight = stretchedData[y][x]
					if (weight > 50f) {
						if (x <= centerX) {
							if (y <= centerY) {
								ltSumX += x * weight; ltSumY += y * weight; ltWeight += weight
							} else {
								lbSumX += x * weight; lbSumY += y * weight; lbWeight += weight
							}
						} else {
							if (y <= centerY) {
								rtSumX += x * weight; rtSumY += y * weight; rtWeight += weight
							} else {
								rbSumX += x * weight; rbSumY += y * weight; rbWeight += weight
							}
						}
					}
				}
			}

			val leftSumX = ltSumX + lbSumX; val leftSumY = ltSumY + lbSumY; val leftWeight = ltWeight + lbWeight; val topWeight = ltWeight + rtWeight
			val rightSumX = rtSumX + rbSumX; val rightSumY = rtSumY + rbSumY; val rightWeight = rtWeight + rbWeight; val bottomWeight = lbWeight + rbWeight
			val totalSumX = leftSumX + rightSumX; val totalSumY = leftSumY + rightSumY

			val canvas = Canvas(finalHeatmap)
			val scaleFactor = targetSize / squareSize.toFloat()

			// --- [그리기 파트 1] CoP 점 7개 그리기 ---
			val paint = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }
			val drawCoP = { sX: Float, sY: Float, w: Float, radius: Float, colorHex: String ->
				if (w > 0f) {
					val cx = (sX / w) * scaleFactor
					val cy = (sY / w) * scaleFactor
					paint.color = Color.WHITE
					canvas.drawCircle(cx, cy, radius * 1.3f, paint) // 흰색 테두리
					paint.color = colorHex.toColorInt()
					canvas.drawCircle(cx, cy, radius, paint)        // 내부 색상
				}
			}

			val baseRadius = targetSize / 60f

			// 4사분면(앞/뒤꿈치) - 하늘색
			val rQuad = baseRadius * 0.7f
			drawCoP(ltSumX, ltSumY, ltWeight, rQuad, "#00FFFF")
			drawCoP(lbSumX, lbSumY, lbWeight, rQuad, "#00FFFF")
			drawCoP(rtSumX, rtSumY, rtWeight, rQuad, "#00FFFF")
			drawCoP(rbSumX, rbSumY, rbWeight, rQuad, "#00FFFF")

			// 좌우 각각의 중심 - 핑크색
			val rFoot = baseRadius * 0.7f
			drawCoP(leftSumX, leftSumY, leftWeight, rFoot, "#FF4081")
			drawCoP(rightSumX, rightSumY, rightWeight, rFoot, "#FF4081")

			// 전체 중심 - 노란색
			drawCoP(totalSumX, totalSumY, totalWeight, baseRadius * 1.0f, "#FFEB3B")

			// --- [그리기 파트 2] 4사분면 퍼센트(%) 텍스트 그리기 ---
			val ltPercent = ((ltWeight / totalWeight) * 100).roundToInt()
			val lbPercent = ((lbWeight / totalWeight) * 100).roundToInt()
			val rtPercent = ((rtWeight / totalWeight) * 100).roundToInt()
			val rbPercent = ((rbWeight / totalWeight) * 100).roundToInt()
			val ts = targetSize / 20f
			val textPaint = Paint().apply {
				color = Color.WHITE
				textSize = ts
				textAlign = Paint.Align.CENTER
				isAntiAlias = true
				setShadowLayer(8f, 0f, 0f, Color.parseColor("#B3000000")) // 그림자 추가
			}

			// 텍스트 위치: 각 구역의 중앙
//			val textLtX = (centerX / 2) * scaleFactor
//			val textLtY = (centerY / 2) * scaleFactor
//			val textLbX = (centerX / 2) * scaleFactor
//			val textLbY = (centerY + (centerY / 2)) * scaleFactor
//
//			val textRtX = (centerX + (centerX / 2)) * scaleFactor
//			val textRtY = (centerY / 2) * scaleFactor
//			val textRbX = (centerX + (centerX / 2)) * scaleFactor
//			val textRbY = (centerY + (centerY / 2)) * scaleFactor
			val textLtX = targetSize * 0.25f
			val textLtY = targetSize * 0.25f

			val textLbX = targetSize * 0.25f
			val textLbY = targetSize * 0.75f

			val textRtX = targetSize * 0.75f
			val textRtY = targetSize * 0.25f

			val textRbX = targetSize * 0.75f
			val textRbY = targetSize * 0.75f

			canvas.drawText("${ltPercent}%", textLtX, textLtY, textPaint)
			canvas.drawText("${rtPercent}%", textRtX, textRtY, textPaint)
			canvas.drawText("${lbPercent}%", textLbX, textLbY, textPaint)
			canvas.drawText("${rbPercent}%", textRbX, textRbY, textPaint)

			val yellowTextPaint = Paint(textPaint).apply {
				color = Color.WHITE
				textSize = targetSize / 18f
			}

			val outerSide0 = targetSize * 0.1f
			val outerSide1 = targetSize * 0.9f
			val midAbsolute = targetSize * 0.5f

			hvm.calculateRatio(leftWeight, topWeight, totalWeight )

			canvas.drawText("${hvm.matRatio.left}%", outerSide0, midAbsolute, yellowTextPaint)
			canvas.drawText("${hvm.matRatio.right}%", outerSide1, midAbsolute, yellowTextPaint)
			canvas.drawText("${hvm.matRatio.top}%", midAbsolute, outerSide0, yellowTextPaint)
			canvas.drawText("${hvm.matRatio.bottom}%", midAbsolute, outerSide1, yellowTextPaint)
		}

		runOnUiThread { bd.ivHeatmap.setImageBitmap(finalHeatmap) }
	}

	private val colorLUT: IntArray = IntArray(256) { i ->
		// [조절 파라미터] 구간 시작 지점 (0.0 ~ 1.0)
		val NOISE_CUT = 3         // 10 미만은 무조건 투명 (끄트머리 투명화 반영)
		val GREEN_START = 0.02f   // 녹색 시작점
		val SOLID_GREEN_START = 0.1f // 쨍한 녹색 시작점
		val YELLOW_START = 0.125f  // 노란색 시작점
		val RED_START = 0.24f     // 빨간색 시작점

		if (i < NOISE_CUT) return@IntArray Color.TRANSPARENT

		val v = i / 255f // Grayscale 값(0~255)을 0~1 비율로 변환

		return@IntArray when {
			v < GREEN_START -> {
				val f = (v - (NOISE_CUT / 255f)) / (GREEN_START - (NOISE_CUT / 255f))
				interpolateColor(Color.argb(0, 0, 255, 0), Color.argb(100, 0, 255, 0), f)
			}
			v < SOLID_GREEN_START -> {
				val f = (v - GREEN_START) / (SOLID_GREEN_START - GREEN_START)
				interpolateColor(Color.argb(100, 0, 255, 0), Color.argb(255, 0, 255, 0), f)
			}
			v < YELLOW_START -> {
				val f = (v - SOLID_GREEN_START) / (YELLOW_START - SOLID_GREEN_START)
				interpolateColor(Color.argb(255, 0, 255, 0), Color.argb(255, 255, 255, 0), f)
			}
			v < RED_START -> {
				val f = (v - YELLOW_START) / (RED_START - YELLOW_START)
				interpolateColor(Color.argb(255, 255, 255, 0), Color.argb(255, 255, 0, 0), f)
			}
			else -> Color.argb(255, 255, 0, 0)
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

	private fun interpolateColor(color1: Int, color2: Int, fraction: Float): Int {
		val a = (Color.alpha(color1) + (Color.alpha(color2) - Color.alpha(color1)) * fraction).toInt()
		val r = (Color.red(color1) + (Color.red(color2) - Color.red(color1)) * fraction).toInt()
		val g = (Color.green(color1) + (Color.green(color2) - Color.green(color1)) * fraction).toInt()
		val b = (Color.blue(color1) + (Color.blue(color2) - Color.blue(color1)) * fraction).toInt()
		return Color.argb(a, r, g, b)
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
				bd.tvBattery.text = "🪫 $battery%"
			}
			if (rowIndex == 0) {
				displayFullFrame()
				displayHeatmap()
			}
		}
	}

	private fun extractBatteryCapacity(packet: ByteArray): Int {
		if (packet.size < 6) return 0

		val batLow = packet[4].toInt() and 0xFF
		val batHigh = packet[5].toInt() and 0xFF
		return (batHigh shl 8) or batLow
	}

	private fun showWaitedUI() {
		bd.clHeatmapGuide.visibility = View.VISIBLE
		bd.llbtn.visibility = View.GONE
		val animatedDrawable = bd.ivUsb.drawable
		if (animatedDrawable is AnimatedVectorDrawable) animatedDrawable.start()
	}

	private fun hideWaitedUI() {
		bd.clHeatmapGuide.visibility = View.GONE
		bd.llbtn.visibility = View.VISIBLE
		val animatedDrawable = bd.ivUsb.drawable
		if (animatedDrawable is AnimatedVectorDrawable) animatedDrawable.stop()
	}

	private fun preloadDeviceLocation() {
		try {
			fusedLocationClient.lastLocation.addOnSuccessListener { location ->
				if (location != null) {
					Log.v("위치 권한", "캐시된 위치 성공: $location")
					hvm.getAddressString(this@MainActivity, location.latitude, location.longitude) { address ->
						// 🎯 성공 시 뷰모델 변수에 주소 저장
						hvm.currentAddress = address
						Log.v("currentAddress", "기존 캐시에서 등록: ${hvm.currentAddress}")
					}
				} else {
					Log.v("위치 권한", "캐시가 null이므로 실시간 위치를 요청합니다.")
					requestCurrentLocation() // 🔴 실시간 요청 함수 호출
				}
			}
		} catch (e: SecurityException) {
			e.printStackTrace()
		}
	}
	private fun requestCurrentLocation() {
		val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
			.setMaxUpdates(1) // 딱 한 번만 받고 업데이트 종료
			.build()

		try {
			fusedLocationClient.requestLocationUpdates(
				locationRequest,
				object : LocationCallback() {
					override fun onLocationResult(locationResult: LocationResult) {

						val currentLoc = locationResult.lastLocation
						Log.v("실시간 위치 콜백", "$currentLoc")
						if (currentLoc != null) {
							hvm.getAddressString(this@MainActivity, currentLoc.latitude, currentLoc.longitude) { address ->
								// 🎯 성공 시 뷰모델 변수에 주소 저장
								hvm.currentAddress = address
								Log.v("currentAddress", "실시간으로 등록: ${hvm.currentAddress}")
							}

						} else {
							Log.e("위치 권한", "실시간 위치 요청도 실패했습니다.")
						}
					}
				},
				mainLooper
			)
		} catch (e: SecurityException) {
			e.printStackTrace()
		}
	}
	// 2. 주소를 인자로 받아서 Supabase 업로드 및 최종 Vercel URL을 조립하는 함수
	private fun setUploadAndDownloadProcess(address: String) {
		hvm.uploadHeatmap(this@MainActivity ,finalHeatmap) { success, fileName ->
			if (success && fileName != null) {
				val vercelBaseUrl = getString(R.string.vercel_base_url)

				// 전달받은 주소를 URL 규격에 맞게 안전하게 인코딩
				val encodedAddress = java.net.URLEncoder.encode(address, "UTF-8")

				// 🔗 최종 Vercel 배포 주소 완성!
				val finalUrl = "$vercelBaseUrl?image=$fileName&location=$encodedAddress?ratio=${hvm.matRatio.toParamString()}"
				val qrBottomSheet = QRCodeBottomSheetDialogFragment.newInstance(finalUrl)
				qrBottomSheet.show(supportFragmentManager, qrBottomSheet.tag)
			} else {
				// 이미지 업로드 실패 처리
				Log.e("이미지 업로드?", "$success")
				Toast.makeText(this, "이미지 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
			}
		}
	}

}