package com.tangoplus.matviewer.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * SVG 기반 BalanceGaugeView
 * - 둥근 사각형 테두리 + 화살표 아이콘 (circle check 없음)
 * - setProgress(0f~1f): 회색 테두리가 녹색으로 차오름
 * - 1f 도달 시 화살표도 녹색 전환
 *
 * SVG viewBox: 170x133
 *   LR 화살표 path: "M47.55 69.36 ..."
 *   TB 화살표 path: "M83.52 29.55 ..."
 */
class BalanceGaugeView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyle: Int = 0
) : View(context, attrs, defStyle) {

	enum class Type { LR, TB }

	var gaugeType: Type = Type.LR
		set(value) { field = value; buildPaths(); invalidate() }

	private var progress = 0f

	// SVG 원본 viewBox
	private val VW = 170f
	private val VH = 133f

	// Paint
	private val grayStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.STROKE
		strokeCap = Paint.Cap.ROUND
		strokeJoin = Paint.Join.ROUND
		color = Color.parseColor("#D9D9D9")
		strokeWidth = 10f
	}
	private val greenStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.STROKE
		strokeCap = Paint.Cap.ROUND
		strokeJoin = Paint.Join.ROUND
		color = Color.parseColor("#00C853")
		strokeWidth = 10f
	}
	private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.FILL
		color = Color.parseColor("#D9D9D9")
	}

	// Paths
	private val borderPath = Path()
	private val arrowPath  = Path()

	private val borderMeasure = PathMeasure()
	private val segPath = Path()

	fun setProgress(p: Float) {
		progress = p.coerceIn(0f, 1f)
		invalidate()
	}
	fun reset() = setProgress(0f)
	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val dp = resources.displayMetrics.density
		setMeasuredDimension((170 * dp).toInt(), (133 * dp).toInt())
	}
	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		buildPaths()
	}

	private fun buildPaths() {
		val vw = width.toFloat().takeIf { it > 0 } ?: VW
		val vh = height.toFloat().takeIf { it > 0 } ?: VH
		val sx = vw / VW
		val sy = vh / VH

		// 둥근 사각형: rect x=5 y=5 width=160 height=123 rx=15, stroke-width=10
		// stroke는 중앙 기준이라 실제 rect 중심선: x=5+5=10, y=5+5=10 ... 하지만
		// Path로 그릴 때는 SVG stroke 중심 그대로 사용
		borderPath.reset()
		val rect = RectF(5 * sx, 5 * sy, (5 + 160) * sx, (5 + 123) * sy)
		borderPath.addRoundRect(rect, 15 * sx, 15 * sy, Path.Direction.CW)
		borderMeasure.setPath(borderPath, false)

		// strokeWidth 스케일
		grayStrokePaint.strokeWidth  = 10f * sx
		greenStrokePaint.strokeWidth = 10f * sx

		// 화살표 path (SVG fill path 그대로)
		arrowPath.reset()
		when (gaugeType) {
			Type.LR -> {
				// M47.5571 69.3647 C46.8143 68.1893 46.8143 66.6916 47.5571 65.5161
				// L61.2534 43.8423 C62.2177 42.3165 64.5776 42.9993 64.5776 44.8042
				// L64.5776 58.4351 L104.88 58.4351 L104.88 44.8081
				// C104.881 43.0036 107.239 42.3208 108.204 43.8462
				// L121.901 65.52 C122.644 66.6955 122.644 68.1942 121.901 69.3696
				// L108.204 91.0435 C107.239 92.5688 104.88 91.8854 104.88 90.0806
				// L104.88 76.4468 L64.5776 76.4468 L64.5776 90.0767
				// C64.5775 91.8815 62.2177 92.5643 61.2534 91.0386
				// L47.5571 69.3647 Z
				arrowPath.moveTo(47.5571f * sx, 69.3647f * sy)
				arrowPath.cubicTo(46.8143f*sx, 68.1893f*sy, 46.8143f*sx, 66.6916f*sy, 47.5571f*sx, 65.5161f*sy)
				arrowPath.lineTo(61.2534f*sx, 43.8423f*sy)
				arrowPath.cubicTo(62.2177f*sx, 42.3165f*sy, 64.5776f*sx, 42.9993f*sy, 64.5776f*sx, 44.8042f*sy)
				arrowPath.lineTo(64.5776f*sx, 58.4351f*sy)
				arrowPath.lineTo(104.88f*sx, 58.4351f*sy)
				arrowPath.lineTo(104.88f*sx, 44.8081f*sy)
				arrowPath.cubicTo(104.881f*sx, 43.0036f*sy, 107.239f*sx, 42.3208f*sy, 108.204f*sx, 43.8462f*sy)
				arrowPath.lineTo(121.901f*sx, 65.52f*sy)
				arrowPath.cubicTo(122.644f*sx, 66.6955f*sy, 122.644f*sx, 68.1942f*sy, 121.901f*sx, 69.3696f*sy)
				arrowPath.lineTo(108.204f*sx, 91.0435f*sy)
				arrowPath.cubicTo(107.239f*sx, 92.5688f*sy, 104.88f*sx, 91.8854f*sy, 104.88f*sx, 90.0806f*sy)
				arrowPath.lineTo(104.88f*sx, 76.4468f*sy)
				arrowPath.lineTo(64.5776f*sx, 76.4468f*sy)
				arrowPath.lineTo(64.5776f*sx, 90.0767f*sy)
				arrowPath.cubicTo(64.5775f*sx, 91.8815f*sy, 62.2177f*sx, 92.5643f*sy, 61.2534f*sx, 91.0386f*sy)
				arrowPath.lineTo(47.5571f*sx, 69.3647f*sy)
				arrowPath.close()
			}
			Type.TB -> {
				// M83.5206 29.557 C84.696 28.8143 86.1939 28.8143 87.3693 29.557
				// L109.043 43.2543 C110.569 44.2186 109.886 46.5785 108.081 46.5785
				// L94.4503 46.5785 L94.4503 86.8803 L108.076 86.8803
				// C109.881 86.8803 110.565 89.2402 109.039 90.2045
				// L87.3654 103.902 C86.1899 104.645 84.6912 104.645 83.5157 103.902
				// L61.8419 90.2045 C60.3162 89.2402 60.9999 86.8803 62.8048 86.8803
				// L76.4386 86.8803 L76.4386 46.5785 L62.8087 46.5785
				// C61.0038 46.5785 60.321 44.2186 61.8468 43.2543
				// L83.5206 29.557 Z
				arrowPath.moveTo(83.5206f*sx, 29.557f*sy)
				arrowPath.cubicTo(84.696f*sx, 28.8143f*sy, 86.1939f*sx, 28.8143f*sy, 87.3693f*sx, 29.557f*sy)
				arrowPath.lineTo(109.043f*sx, 43.2543f*sy)
				arrowPath.cubicTo(110.569f*sx, 44.2186f*sy, 109.886f*sx, 46.5785f*sy, 108.081f*sx, 46.5785f*sy)
				arrowPath.lineTo(94.4503f*sx, 46.5785f*sy)
				arrowPath.lineTo(94.4503f*sx, 86.8803f*sy)
				arrowPath.lineTo(108.076f*sx, 86.8803f*sy)
				arrowPath.cubicTo(109.881f*sx, 86.8803f*sy, 110.565f*sx, 89.2402f*sy, 109.039f*sx, 90.2045f*sy)
				arrowPath.lineTo(87.3654f*sx, 103.902f*sy)
				arrowPath.cubicTo(86.1899f*sx, 104.645f*sy, 84.6912f*sx, 104.645f*sy, 83.5157f*sx, 103.902f*sy)
				arrowPath.lineTo(61.8419f*sx, 90.2045f*sy)
				arrowPath.cubicTo(60.3162f*sx, 89.2402f*sy, 60.9999f*sx, 86.8803f*sy, 62.8048f*sx, 86.8803f*sy)
				arrowPath.lineTo(76.4386f*sx, 86.8803f*sy)
				arrowPath.lineTo(76.4386f*sx, 46.5785f*sy)
				arrowPath.lineTo(62.8087f*sx, 46.5785f*sy)
				arrowPath.cubicTo(61.0038f*sx, 46.5785f*sy, 60.321f*sx, 44.2186f*sy, 61.8468f*sx, 43.2543f*sy)
				arrowPath.lineTo(83.5206f*sx, 29.557f*sy)
				arrowPath.close()
			}
		}
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)

		// 1. 회색 테두리 (항상)
		canvas.drawPath(borderPath, grayStrokePaint)

		// 2. 녹색 테두리 + 화살표 (progress만큼 클립)
		if (progress > 0f) {
			val clipRight = width * progress
			canvas.save()
			canvas.clipRect(0f, 0f, clipRight, height.toFloat())
			canvas.drawPath(borderPath, greenStrokePaint)
			arrowPaint.color = Color.parseColor("#00C853")
			canvas.drawPath(arrowPath, arrowPaint)
			canvas.restore()
		}

		// 3. 회색 화살표 (항상 — 녹색 위에 덮어씌워지는 부분은 클립으로 가려짐)
		val clipLeft = width * progress
		canvas.save()
		canvas.clipRect(clipLeft, 0f, width.toFloat(), height.toFloat())
		arrowPaint.color = Color.parseColor("#D9D9D9")
		canvas.drawPath(arrowPath, arrowPaint)
		canvas.restore()
	}
}