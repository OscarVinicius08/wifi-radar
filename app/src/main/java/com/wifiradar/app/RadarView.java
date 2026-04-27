package com.wifiradar.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * Radar circular para smartwatch (~466x466 px).
 *
 * - Centro do radar = o smartwatch (você).
 * - Cada anel concêntrico = um nível de distância (5m, 15m, 30m, 60m+).
 * - Cada AP é um ponto colorido pela força do sinal:
 *      verde = forte, amarelo = médio, laranja = fraco, vermelho = muito fraco.
 * - O ângulo é DERIVADO DO BSSID (estável entre scans, mas não é direção real).
 * - Linha de varredura animada (estilo radar militar).
 *
 * Otimizado para tela AMOLED pequena: fundo preto puro (economia de bateria),
 * traços finos, fonte legível.
 */
public class RadarView extends View {

    private final Paint ringPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint crossPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sweepPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<WifiNetwork> networks = new ArrayList<>();

    // Distâncias dos anéis em metros (ordem crescente).
    // O último valor define o "raio máximo" mostrado.
    private static final double[] RING_METERS = { 5, 15, 30, 60 };

    private float sweepAngle = 0f;
    private final ValueAnimator sweepAnim;

    public RadarView(Context c) { this(c, null); }
    public RadarView(Context c, AttributeSet a) { this(c, a, 0); }
    public RadarView(Context c, AttributeSet a, int s) {
        super(c, a, s);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(dp(1f));
        ringPaint.setColor(0x55_00FF88);

        crossPaint.setStyle(Paint.Style.STROKE);
        crossPaint.setStrokeWidth(dp(0.5f));
        crossPaint.setColor(0x33_00FF88);

        sweepPaint.setStyle(Paint.Style.FILL);

        dotPaint.setStyle(Paint.Style.FILL);
        glowPaint.setStyle(Paint.Style.FILL);

        labelPaint.setColor(0xFF_AAAAAA);
        labelPaint.setTextSize(dp(8f));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        centerPaint.setColor(0xFF_00FF88);
        centerPaint.setStyle(Paint.Style.FILL);

        // Animação do "feixe" do radar — uma volta a cada 4s.
        sweepAnim = ValueAnimator.ofFloat(0f, 360f);
        sweepAnim.setDuration(4000);
        sweepAnim.setRepeatCount(ValueAnimator.INFINITE);
        sweepAnim.setInterpolator(new LinearInterpolator());
        sweepAnim.addUpdateListener(a2 -> {
            sweepAngle = (float) a2.getAnimatedValue();
            invalidate();
        });
        sweepAnim.start();
    }

    public void setNetworks(List<WifiNetwork> list) {
        networks.clear();
        if (list != null) networks.addAll(list);
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        sweepAnim.cancel();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(w, h) / 2f - dp(4f);

        // Anéis concêntricos
        int rings = RING_METERS.length;
        for (int i = 1; i <= rings; i++) {
            float r = radius * i / rings;
            canvas.drawCircle(cx, cy, r, ringPaint);
        }

        // Cruz central (norte/sul/leste/oeste — meramente decorativa)
        canvas.drawLine(cx, cy - radius, cx, cy + radius, crossPaint);
        canvas.drawLine(cx - radius, cy, cx + radius, cy, crossPaint);

        // Feixe do radar (gradiente que vai do verde transparente ao opaco)
        Shader shader = new RadialGradient(
                cx, cy, radius,
                new int[] { 0x66_00FF88, 0x00_00FF88 },
                new float[] { 0f, 1f },
                Shader.TileMode.CLAMP);
        sweepPaint.setShader(shader);

        canvas.save();
        canvas.rotate(sweepAngle, cx, cy);
        // setor angular de ~30°
        android.graphics.Path sector = new android.graphics.Path();
        sector.moveTo(cx, cy);
        android.graphics.RectF box = new android.graphics.RectF(
                cx - radius, cy - radius, cx + radius, cy + radius);
        sector.arcTo(box, -15f, 30f);
        sector.close();
        canvas.drawPath(sector, sweepPaint);
        canvas.restore();

        // Ponto central = você
        canvas.drawCircle(cx, cy, dp(3f), centerPaint);

        // Pontos de cada rede
        double maxRing = RING_METERS[RING_METERS.length - 1];
        for (WifiNetwork n : networks) {
            // converte distância -> raio na tela (clipa em maxRing)
            double d = Math.min(n.distanceM, maxRing);
            float pr = (float) (radius * d / maxRing);
            double angle = n.bearingRadians();
            float x = cx + (float) (pr * Math.cos(angle));
            float y = cy + (float) (pr * Math.sin(angle));

            int color = colorForBars(n.signalBars);
            // halo
            glowPaint.setColor((color & 0x00FFFFFF) | 0x40000000);
            canvas.drawCircle(x, y, dp(6f), glowPaint);
            // ponto
            dotPaint.setColor(color);
            canvas.drawCircle(x, y, dp(3f), dotPaint);
        }

        // Legendas dos anéis (5m, 15m...)
        for (int i = 0; i < rings; i++) {
            float r = radius * (i + 1) / rings;
            String label = ((int) RING_METERS[i]) + "m";
            canvas.drawText(label, cx + dp(2f),
                    cy - r + dp(10f), labelPaint);
        }
    }

    private static int colorForBars(int bars) {
        switch (bars) {
            case 4: return 0xFF_00FF88; // verde forte
            case 3: return 0xFF_BBFF33; // verde-amarelo
            case 2: return 0xFF_FFCC00; // amarelo
            case 1: return 0xFF_FF8800; // laranja
            default: return 0xFF_FF3344; // vermelho
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
