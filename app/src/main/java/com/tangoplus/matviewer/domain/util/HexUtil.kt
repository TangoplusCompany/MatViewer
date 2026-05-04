package com.tangoplus.matviewer.domain.util

import android.graphics.Color

object HexUtil {
	private val PREAMBLE = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0xFF.toByte(), 0xFE.toByte())
	fun findHeaderIndex(source: ByteArray, startIndex: Int): Int {
		if (startIndex > source.size - PREAMBLE.size) return -1
		for (i in startIndex..source.size - PREAMBLE.size) {
			if (source[i] == PREAMBLE[0] && source[i + 1] == PREAMBLE[1] &&
				source[i + 2] == PREAMBLE[2] && source[i + 3] == PREAMBLE[3]) {
				return i
			}
		}
		return -1
	}

	fun applyGaussianBlur(data: Array<IntArray>): Array<IntArray> {
		val height = data.size
		val width = data[0].size
		val result = Array(height) { IntArray(width) }

		// 3x3 가우시안 커널 (가중치 총합 16)
		val kernel = arrayOf(
			intArrayOf(1, 2, 1),
			intArrayOf(2, 4, 2),
			intArrayOf(1, 2, 1)
		)

		for (y in 0 until height) {
			for (x in 0 until width) {
				var sum = 0
				var weightSum = 0

				for (ky in -1..1) {
					for (kx in -1..1) {
						val ny = y + ky
						val nx = x + kx
						if (ny in 0 until height && nx in 0 until width) {
							val weight = kernel[ky + 1][kx + 1]
							sum += data[ny][nx] * weight
							weightSum += weight
						}
					}
				}
				result[y][x] = sum / weightSum
			}
		}
		return result
	}

	val colorLUT: IntArray = IntArray(256) { i ->
		// [조절 파라미터] 구간 시작 지점 (0.0 ~ 1.0)
		val NOISE_CUT = 10         // 10 미만은 무조건 투명 (끄트머리 투명화 반영)
		val GREEN_START = 0.08f   // 옅고 흐릿한 녹색 시작점
		val SOLID_GREEN_START = 0.22f // 쨍한 녹색 시작점
		val YELLOW_START = 0.375f  // 노란색 시작점 (기존 0.4에서 0.5로 넓혀서 녹색->노랑 구간 확보)
		val RED_START = 0.9f     // 빨간색 시작점 (노란색 주변 그라데이션 추가)

		if (i < NOISE_CUT) return@IntArray Color.TRANSPARENT

		val v = i / 255f // Grayscale 값(0~255)을 0~1 비율로 변환

		return@IntArray when {
			// [구간 1] NOISE_CUT ~ GREEN_START: 투명 -> 아주 옅고 흐릿한 녹색 (Fade In)

			v < GREEN_START -> {
				val f = (v - (NOISE_CUT / 255f)) / (GREEN_START - (NOISE_CUT / 255f))
				// interpolateColor(Color1, Color2, fraction): Color1에서 Color2로 f만큼 이동
				interpolateColor(Color.argb(0, 0, 255, 0), Color.argb(100, 0, 255, 0), f)
			}
			// [구간 2] GREEN_START ~ SOLID_GREEN_START: 흐릿 녹색 -> 쨍한 녹색
			v < SOLID_GREEN_START -> {
				val f = (v - GREEN_START) / (SOLID_GREEN_START - GREEN_START)
				interpolateColor(Color.argb(100, 0, 255, 0), Color.argb(255, 0, 255, 0), f)
			}
			// [구간 3] SOLID_GREEN_START ~ YELLOW_START: 쨍한 녹색 -> 노란색
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
	private fun interpolateColor(color1: Int, color2: Int, fraction: Float): Int {
		val a = (Color.alpha(color1) + (Color.alpha(color2) - Color.alpha(color1)) * fraction).toInt()
		val r = (Color.red(color1) + (Color.red(color2) - Color.red(color1)) * fraction).toInt()
		val g = (Color.green(color1) + (Color.green(color2) - Color.green(color1)) * fraction).toInt()
		val b = (Color.blue(color1) + (Color.blue(color2) - Color.blue(color1)) * fraction).toInt()
		return Color.argb(a, r, g, b)
	}

	fun extractBatteryCapacity(packet: ByteArray): Int {
		// 패킷 크기 예외 처리 (헤더 4 + 배터리 2 최소 6바이트 필요)
		if (packet.size < 6) return 0

		// 인덱스 4: 배터리 Low byte, 인덱스 5: 배터리 High byte
		val batLow = packet[4].toInt() and 0xFF
		val batHigh = packet[5].toInt() and 0xFF

		// 2바이트를 결합하여 10진수 정수로 반환
		return (batHigh shl 8) or batLow
	}
	// 섬과 섬 사이의 빈 공간(0)을 메워주기 위한 넓은 블러 (5x5)
	fun applyWideBlur(data: Array<IntArray>): Array<IntArray> {
		val height = data.size
		val width = data[0].size
		val result = Array(height) { IntArray(width) }

		// 5x5 커널 (가중치 총합 256) - 주변으로 데이터를 확 퍼트립니다.
		val kernel = arrayOf(
			intArrayOf(1, 4, 6, 4, 1),
			intArrayOf(4, 16, 24, 16, 4),
			intArrayOf(6, 24, 36, 24, 6),
			intArrayOf(4, 16, 24, 16, 4),
			intArrayOf(1, 4, 6, 4, 1)
		)

		for (y in 0 until height) {
			for (x in 0 until width) {
				var sum = 0
				var weightSum = 0

				for (ky in -2..2) {
					for (kx in -2..2) {
						val ny = y + ky
						val nx = x + kx
						if (ny in 0 until height && nx in 0 until width) {
							val weight = kernel[ky + 2][kx + 2]
							sum += data[ny][nx] * weight
							weightSum += weight
						}
					}
				}
				if (weightSum > 0) result[y][x] = sum / weightSum
			}
		}
		return result
	}


	fun applyAdaptiveDilation(data: Array<IntArray>, radius: Int): Array<IntArray> {
		val height = data.size
		val width = data[0].size
		val result = Array(height) { IntArray(width) }

		for (y in 0 until height) {
			for (x in 0 until width) {
				val myVal = data[y][x]
				var maxNeighbor = 0
				var activeNeighborCount = 0

				// 1. 주변 문맥(Context) 파악: 활성화된 이웃 픽셀 개수 세기
				for (dy in -radius..radius) {
					for (dx in -radius..radius) {
						if (dy == 0 && dx == 0) continue // 내 위치는 제외

						val ny = y + dy
						val nx = x + dx

						if (ny in 0 until height && nx in 0 until width) {
							val nVal = data[ny][nx]

							if (nVal > 30) {
								activeNeighborCount++
							}
							// 가장 센 이웃의 압력값 기억
							if (nVal > maxNeighbor) {
								maxNeighbor = nVal
							}
						}
					}
				}

				// 2. 인과관계에 따른 번짐 강도(Strength) 동적 결정
				val dynamicStrength = when (activeNeighborCount) {
					// 이웃 중 밟힌 곳이 0~1개뿐임 -> 고립된 점 (발등 찍힘이나 노이즈)
					// 강도를 0.15로 확 낮춰서 원형으로 뚱뚱해지는 것을 방지함
					0, 1 -> 0.15f

					// 이웃 중 밟힌 곳이 2~3개임 -> 발날 같은 선(Line) 형태
					// 강도를 중간(0.4)으로 주어 선의 길쭉한 형태를 유지하면서 살짝만 두꺼워지게 함
					2, 3 -> 0.325f

					// 이웃 중 밟힌 곳이 4개 이상임 -> 발뒷꿈치/앞꿈치 같은 뭉친 면(Area) 형태
					// 꽉 찬 압력이므로 강도를 높여(0.8) 넓고 부드러운 원형 그라데이션 형성
					else -> 0.75f
				}

				// 3. 최종 값 결정 (내 원래 값 vs 이웃 영향력 중 큰 값)
				val dilatedVal = (maxNeighbor * dynamicStrength).toInt()
				result[y][x] = maxOf(myVal, dilatedVal)
			}
		}
		return result
	}

	fun normalizeData(data: Array<IntArray>): Array<FloatArray> {
		val height = data.size
		val width = data[0].size
		val result = Array(height) { FloatArray(width) }

		// 스무딩된 데이터 중 가장 큰 값 찾기
		var maxVal = 1
		for (row in data) {
			val rowMax = row.maxOrNull() ?: 0
			if (rowMax > maxVal) maxVal = rowMax
		}

		for (y in 0 until height) {
			for (x in 0 until width) {
				val normalized = (data[y][x].toFloat() / maxVal).coerceIn(0f, 1f)
				// 제곱근을 씌워 낮은 압력 데이터를 증폭(넓어 보이게 함)
				result[y][x] = kotlin.math.sqrt(normalized)
			}
		}
		return result
	}
}