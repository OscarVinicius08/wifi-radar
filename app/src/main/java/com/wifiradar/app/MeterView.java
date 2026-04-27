package com.wifiradar.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

/**
 * Medidor analógico (gauge) de sinal Wi-Fi.
 * Mostra o RSSI da rede mais forte detectada.
 * Arco de -100 dBm (esquerda, vermelho) a 0 dBm (direita, verde).
 */
public class MeterView extends View {

    private final Paint arcPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int   rssi    = -100;
    private String ssid   = "—";
    private String band   = "";

    // O arco vai de 210° a 330° (150° de span, centrado em baixo)
    private static final float START_ANGLE = 150f;
    private static final float SWEEP_TOTAL = 240f;

    public MeterView(Context c) { this(c, null); }
    public MeterView(Context c, AttributeSet a) { this(c, a, 0); }
    public MeterView(Context c, AttributeSet a, int s) {
        super(c, a, s);

        bgArcPaint.setStyle(Paint.Style.STROKE);
        bgArcPaint.setStrokeCap(Paint.Cap.ROUND);
        bgArcPaint.setColor(0x22FFFFFF);

        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        needlePaint.setStyle(Paint.Style.FILL);
        needlePaint.setStrokeWidth(dp(2));

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        subPaint.setTextAlign(Paint.Align.CENTER);
        subPaint.setColor(0xFFAAAAAA);

        dotPaint.setColor(0xFFFFFFFF);
        dotPaint.setStyle(Paint.Style.FILL);
    }

    public void setData(List<WifiNetwork> networks) {
        if (networks == null || networks.isEmpty()) {
            rssi = -100; ssid = "—"; band = "";
        } else {
            WifiNetwork best = networks.get(0); // já ordenado por RSSI
            rssi = best.rssi;
            ssid = best.ssid.length() > 14 ? best.ssid.substring(0, 13) + "…" : best.ssid;
            band = best.band + " CH" + best.channel;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f + dp(10f);
        float r  = Math.min(w, h) * 0.38f;
        float thick = dp(10f);

        bgArcPaint.setStrokeWidth(thick);
        arcPaint.setStrokeWidth(thick);

        RectF oval = new RectF(cx - r, cy - r, cx + r, cy + r);

        // Arco de fundo
        canvas.drawArc(oval, START_ANGLE, SWEEP_TOTAL, false, bgArcPaint);

        // Arco colorido por gradiente (vermelho → amarelo → verde)
        // Calcula quantos graus preencher baseado no RSSI (-100 = 0%, 0 = 100%)
        float fraction = Math.max(0f, Math.min(1f, (rssi + 100f) / 100f));
        float sweepFilled = SWEEP_TOTAL * fraction;

        // Usa SweepGradient para colorir o arco
        SweepGradient sg = new SweepGradient(cx, cy,
                new int[]{0xFFFF3344, 0xFFFF3344, 0xFFFFCC00, 0xFF00FF88, 0xFF00FF88},
                new float[]{0f, START_ANGLE/360f,
                        (START_ANGLE + SWEEP_TOTAL*0.5f)/360f,
                        (START_ANGLE + SWEEP_TOTAL)/360f, 1f});
        arcPaint.setShader(sg);
        if (sweepFilled > 0)
            canvas.drawArc(oval, START_ANGLE, sweepFilled, false, arcPaint);

        // Marcações nos extremos
        subPaint.setTextSize(dp(7f));
        // -100 dBm (extremo esquerdo)
        double angleL = Math.toRadians(START_ANGLE);
        canvas.drawText("-100", cx + (float)(r * 1.18 * Math.cos(angleL)),
                                cy + (float)(r * 1.18 * Math.sin(angleL)) + dp(4f), subPaint);
        // 0 dBm (extremo direito)
        double angleR = Math.toRadians(START_ANGLE + SWEEP_TOTAL);
        canvas.drawText("0",   cx + (float)(r * 1.18 * Math.cos(angleR)),
                                cy + (float)(r * 1.18 * Math.sin(angleR)) + dp(4f), subPaint);

        // Ponteiro (needle)
        double needleAngle = Math.toRadians(START_ANGLE + sweepFilled);
        float nx = cx + (float)((r - thick/2f) * Math.cos(needleAngle));
        float ny = cy + (float)((r - thick/2f) * Math.sin(needleAngle));
        needlePaint.setColor(0xFFFFFFFF);
        needlePaint.setStyle(Paint.Style.STROKE);
        needlePaint.setStrokeWidth(dp(2f));
        canvas.drawLine(cx, cy, nx, ny, needlePaint);
        // ponto central
        dotPaint.setColor(0xFFFFFFFF);
        canvas.drawCircle(cx, cy, dp(4f), dotPaint);

        // Valor em dBm
        int color = colorForRssi(rssi);
        textPaint.setColor(color);
        textPaint.setTextSize(dp(22f));
        canvas.drawText(rssi + " dBm", cx, cy - r * 0.25f, textPaint);

        // SSID
        subPaint.setTextSize(dp(10f));
        subPaint.setColor(0xFFDDDDDD);
        canvas.drawText(ssid, cx, cy + r * 0.55f, subPaint);

        // Banda / Canal
        subPaint.setTextSize(dp(8f));
        subPaint.setColor(0xFF777777);
        canvas.drawText(band, cx, cy + r * 0.7f, subPaint);

        // Label "rede mais forte"
        subPaint.setTextSize(dp(8f));
        subPaint.setColor(0xFF555555);
        canvas.drawText("rede mais forte", cx, dp(16f), subPaint);
    }

    private static int colorForRssi(int rssi) {
        if (rssi >= -50) return 0xFF00FF88;
        if (rssi >= -60) return 0xFFBBFF33;
        if (rssi >= -70) return 0xFFFFCC00;
        if (rssi >= -80) return 0xFFFF8800;
        return 0xFFFF3344;
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
