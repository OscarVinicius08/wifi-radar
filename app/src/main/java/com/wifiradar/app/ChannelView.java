package com.wifiradar.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Gráfico de canais Wi-Fi estilo spectrum analyzer.
 * Curvas de sino (Gaussian) por rede, coloridas individualmente.
 * Toque para alternar entre 2.4 GHz e 5 GHz.
 */
public class ChannelView extends View {

    private static final int[] COLORS = {
        0xFF00FF88, 0xFF3399FF, 0xFFFFCC00, 0xFFFF5566,
        0xFFAA88FF, 0xFFFF9900, 0xFF00DDCC, 0xFFFF55AA
    };

    private List<WifiNetwork> networks = new ArrayList<>();
    private boolean show5G = false; // false = 2.4GHz, true = 5GHz

    private final Paint axisPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint curvePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hdrPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final GestureDetector gd;

    public ChannelView(Context c) { this(c, null); }
    public ChannelView(Context c, AttributeSet a) { this(c, a, 0); }
    public ChannelView(Context c, AttributeSet a, int s) {
        super(c, a, s);

        axisPaint.setColor(0x44FFFFFF);
        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setStrokeWidth(dp(0.8f));

        curvePaint.setStyle(Paint.Style.STROKE);
        curvePaint.setStrokeWidth(dp(1.5f));
        curvePaint.setAntiAlias(true);

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(dp(8f));
        labelPaint.setColor(0xFFAAAAAA);

        hdrPaint.setTextAlign(Paint.Align.CENTER);
        hdrPaint.setTextSize(dp(10f));
        hdrPaint.setColor(0xFF00FF88);
        hdrPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        gd = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                show5G = !show5G;
                invalidate();
                return true;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        gd.onTouchEvent(e);
        return true;
    }

    public void setNetworks(List<WifiNetwork> nets) {
        networks = nets != null ? nets : new ArrayList<>();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        int w = getWidth(), h = getHeight();

        float padL = dp(28f), padR = dp(8f);
        float padT = dp(24f), padB = dp(28f);
        float plotW = w - padL - padR;
        float plotH = h - padT - padB;

        // Faixa atual
        boolean hasBand = false;
        List<WifiNetwork> filtered = new ArrayList<>();
        for (WifiNetwork n : networks) {
            if (show5G ? n.band.equals("5G") : n.band.equals("2.4G")) {
                filtered.add(n);
                hasBand = true;
            }
        }

        // Header com toggle
        String bandLabel = show5G ? "5 GHz  (toque = 2.4G)" : "2.4 GHz  (toque = 5G)";
        canvas.drawText(bandLabel, w / 2f, dp(16f), hdrPaint);

        // Defini canais X
        int chMin, chMax;
        if (show5G) { chMin = 36; chMax = 165; }
        else        { chMin = 1;  chMax = 14;  }

        // Eixo X (canais)
        canvas.drawLine(padL, h - padB, w - padR, h - padB, axisPaint);
        // Eixo Y (RSSI)
        canvas.drawLine(padL, padT, padL, h - padB, axisPaint);

        // Linhas horizontais de referência: -40, -60, -80, -100
        int[] refRssi = {-40, -60, -80, -100};
        for (int ref : refRssi) {
            float yRef = rssiToY(ref, padT, plotH);
            axisPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{dp(4f), dp(4f)}, 0));
            canvas.drawLine(padL, yRef, w - padR, yRef, axisPaint);
            axisPaint.setPathEffect(null);
            // label Y
            labelPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(ref + "", padL - dp(3f), yRef + dp(4f), labelPaint);
        }
        labelPaint.setTextAlign(Paint.Align.CENTER);

        // Labels de canal X
        int step = show5G ? 16 : 2;
        for (int ch = chMin; ch <= chMax; ch += step) {
            float xCh = chToX(ch, chMin, chMax, padL, plotW);
            canvas.drawLine(xCh, h - padB, xCh, h - padB + dp(3f), axisPaint);
            canvas.drawText("" + ch, xCh, h - padB + dp(12f), labelPaint);
        }

        if (!hasBand) {
            labelPaint.setTextAlign(Paint.Align.CENTER);
            labelPaint.setTextSize(dp(10f));
            canvas.drawText("Nenhuma rede " + (show5G ? "5 GHz" : "2.4 GHz"),
                    w / 2f, h / 2f, labelPaint);
            labelPaint.setTextSize(dp(8f));
            return;
        }

        // Desenha curvas
        int colorIdx = 0;
        for (WifiNetwork n : filtered) {
            int color = COLORS[colorIdx % COLORS.length];
            colorIdx++;

            // Largura de canal (2.4G: ~20MHz = 4ch; 5G: ~20-80MHz)
            float chWidth = show5G ? 8f : 4f;
            float cx = chToX(n.channel, chMin, chMax, padL, plotW);
            float peakY = rssiToY(n.rssi, padT, plotH);
            float baseY = rssiToY(-100, padT, plotH);

            // Curva Gaussiana: y = rssi * exp(-((ch - center)^2) / (2*sigma^2))
            // Desenhamos usando path com pontos
            Path fillPath = new Path();
            Path curvePath = new Path();
            boolean first = true;
            int steps = 80;
            float chSpan = chMax - chMin;
            for (int i = 0; i <= steps; i++) {
                float chF = chMin + (chSpan * i / steps);
                float xF = chToX(chF, chMin, chMax, padL, plotW);
                double sigma = chWidth / 2.0;
                double gauss = Math.exp(-Math.pow(chF - n.channel, 2) / (2 * sigma * sigma));
                float rssiF = (float) (-100 + (n.rssi + 100) * gauss);
                float yF = rssiToY(rssiF, padT, plotH);

                if (first) {
                    fillPath.moveTo(xF, baseY);
                    fillPath.lineTo(xF, yF);
                    curvePath.moveTo(xF, yF);
                    first = false;
                } else {
                    fillPath.lineTo(xF, yF);
                    curvePath.lineTo(xF, yF);
                }
            }
            // fecha o fill
            float lastX = chToX(chMax, chMin, chMax, padL, plotW);
            fillPath.lineTo(lastX, baseY);
            fillPath.close();

            // Fill semitransparente
            fillPaint.setColor((color & 0x00FFFFFF) | 0x33000000);
            canvas.drawPath(fillPath, fillPaint);

            // Contorno
            curvePaint.setColor(color);
            canvas.drawPath(curvePath, curvePaint);

            // Label SSID no pico
            labelPaint.setColor(color);
            labelPaint.setTextSize(dp(7.5f));
            String lbl = n.ssid.length() > 8 ? n.ssid.substring(0, 7) + "…" : n.ssid;
            canvas.drawText(lbl, cx, peakY - dp(5f), labelPaint);
        }
    }

    private float chToX(float ch, int chMin, int chMax, float padL, float plotW) {
        return padL + (ch - chMin) / (float)(chMax - chMin) * plotW;
    }

    private float rssiToY(float rssi, float padT, float plotH) {
        // -100 dBm = baixo (plotH), 0 dBm = topo (0)
        float frac = (rssi + 100f) / 100f; // 0..1
        return padT + plotH * (1f - frac);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
