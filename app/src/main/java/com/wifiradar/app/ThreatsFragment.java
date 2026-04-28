package com.wifiradar.app;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ThreatsFragment extends Fragment {

    private LinearLayout container;
    private final Runnable listener = this::refresh;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf,
            @Nullable ViewGroup parent, @Nullable Bundle s) {
        ScrollView sv = new ScrollView(requireContext());
        sv.setBackgroundColor(Color.BLACK);
        container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(8), dp(6), dp(8), dp(24));
        sv.addView(container);
        return sv;
    }

    @Override public void onResume() {
        super.onResume();
        MainActivity.dataListeners.add(listener);
        listener.run();
    }

    @Override public void onPause() {
        super.onPause();
        MainActivity.dataListeners.remove(listener);
    }

    private void refresh() {
        if (container == null) return;
        container.removeAllViews();

        // Header
        TextView hdr = new TextView(requireContext());
        hdr.setText("🛡 AMEAÇAS DETECTADAS");
        hdr.setTextColor(0xFF00FF88);
        hdr.setTextSize(11);
        hdr.setTypeface(null, Typeface.BOLD);
        hdr.setPadding(0, 0, 0, dp(6));
        container.addView(hdr);

        // Score de ameaça
        int threatCount = ThreatDetector.activeThreats.size();
        int riskCount = 0;
        for (WifiNetwork n : MainActivity.sharedNetworks)
            if (n.signalBars >= 2 && (n.security.equals("Open") || n.security.equals("WEP")))
                riskCount++;

        addCard("Score de Risco",
                threatCount == 0 ? "✅ AMBIENTE LIMPO" :
                threatCount <= 2 ? "⚠️ RISCO MODERADO (" + threatCount + " ameaças)" :
                        "🚨 RISCO ALTO (" + threatCount + " ameaças)",
                threatCount == 0 ? 0xFF00FF88 : threatCount <= 2 ? 0xFFFFCC00 : 0xFFFF3344);

        addCard("Redes vulneráveis próximas",
                riskCount + " (Open/WEP com sinal ≥ médio)",
                riskCount == 0 ? 0xFF00FF88 : 0xFFFF8800);

        addDivider();

        if (ThreatDetector.activeThreats.isEmpty()) {
            addCard("Status", "Nenhuma ameaça ativa detectada.\nMonitorando...", 0xFF555555);
        } else {
            for (String t : ThreatDetector.activeThreats) {
                addCard("ALERTA", t, t.contains("🚨") ? 0xFFFF3344 : 0xFFFFCC00);
            }
        }

        addDivider();

        // Info sobre o que está sendo monitorado
        TextView info = new TextView(requireContext());
        info.setText("Monitorando: Evil Twin · ARP Spoof · DNS · Honeypot · SSID Oculto");
        info.setTextColor(0xFF444444);
        info.setTextSize(8);
        container.addView(info);
    }

    private void addCard(String title, String body, int color) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0x11FFFFFF);
        int p = dp(8);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(6));
        card.setLayoutParams(lp);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(title);
        tvTitle.setTextColor(0xFF888888);
        tvTitle.setTextSize(8);
        card.addView(tvTitle);

        TextView tvBody = new TextView(requireContext());
        tvBody.setText(body);
        tvBody.setTextColor(color);
        tvBody.setTextSize(10);
        tvBody.setTypeface(null, Typeface.BOLD);
        card.addView(tvBody);

        container.addView(card);
    }

    private void addDivider() {
        View d = new View(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(0, dp(4), 0, dp(8));
        d.setLayoutParams(lp);
        d.setBackgroundColor(0x22FFFFFF);
        container.addView(d);
    }

    private int dp(int v) {
        return Math.round(v * requireContext().getResources().getDisplayMetrics().density);
    }
}
