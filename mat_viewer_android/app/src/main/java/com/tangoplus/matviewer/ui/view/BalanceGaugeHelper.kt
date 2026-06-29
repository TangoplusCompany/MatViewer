package com.tangoplus.matviewer.ui.view

/**
 * displayHeatmap() 안에서 호출하는 게이지 업데이트 헬퍼
 *
 * 사용 예시 (Fragment/Activity):
 *
 *   private val lrHelper = BalanceGaugeHelper(targetMs = 5000L)
 *   private val tbHelper = BalanceGaugeHelper(targetMs = 5000L)
 *   private var lastFrameTime = System.currentTimeMillis()
 *
 *   // displayHeatmap() 안에서:
 *   val now = System.currentTimeMillis()
 *   val delta = now - lastFrameTime
 *   lastFrameTime = now
 *
 *   val lrProgress = lrHelper.update(isLrInRange, delta)
 *   val tbProgress = tbHelper.update(isTbInRange, delta)
 *
 *   requireActivity().runOnUiThread {
 *       binding.gaugeLr.setProgress(lrProgress)
 *       binding.gaugeTb.setProgress(tbProgress)
 *   }
 */
class BalanceGaugeHelper(
	private val targetMs: Long = 5000L   // 목표 유지 시간 (ms)
) {
	private var holdMs = 0L

	/**
	 * @param inRange 이번 프레임에 정상 범위 안에 있는지
	 * @param deltaMs 이전 프레임과의 시간 차이 (ms)
	 * @return progress 0f ~ 1f
	 */
	fun update(inRange: Boolean, deltaMs: Long): Float {
		holdMs = if (inRange) (holdMs + deltaMs).coerceAtMost(targetMs)
		else 0L
		return holdMs / targetMs.toFloat()
	}

	fun reset() { holdMs = 0L }
	val progress get() = holdMs / targetMs.toFloat()
}


// ──────────────────────────────────────────────
// displayHeatmap() 안에서 정상 범위 판단하는 부분
// ──────────────────────────────────────────────
//
// val lrRatio = leftWeight / totalWeight * 100f   // 좌측 %
// val tbRatio = topWeight  / totalWeight * 100f   // 앞쪽 %
//
// val lrInRange = kotlin.math.abs(lrRatio - 50f) <= 3f   // 목표 50%, ±3%
// val tbInRange = kotlin.math.abs(tbRatio - 40f) <= 3f   // 목표 40%, ±3%