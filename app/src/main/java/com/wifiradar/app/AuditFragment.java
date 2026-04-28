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

import java.util.List;

public class AuditFragment extends Fragment {

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

        List<WifiNetwork> nets = MainActivity.sharedNetworks;

        addHeader("🔐 AUDITORIA DE SEGURANÇA");

        // Score geral
        int score = calcScore(nets);
        String scoreLabel = score >= 80 ? "SEGURO" : score >= 50 ? "MODERADO" : "CRÍTICO";
        int scoreColor = score >= 80 ? 0xFF00FF88 : score >= 50 ? 0xFFFFCC00 : 0xFFFF3344;
        addCard("Score do Ambiente", score + "/100 — " + scoreLabel, scoreColor);

        addSectionTitle("Por Rede (toque para detalhe)");

        if (nets.isEmpty()) {
            addCard("Status", "Aguardando scan...", 0xFF555555);
            return;
        }

        for (WifiNetwork n : nets) {
            addNetworkAuditCard(n);
        }
    }

    private void addNetworkAuditCard(WifiNetwork n) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0x0FFFFFFF);
        int p = dp(8);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(4));
        card.setLayoutParams(lp);

        // SSID + risco
        int risk = networkRisk(n);
        String riskLabel = risk == 0 ? "✅" : risk == 1 ? "⚠️" : risk == 2 ? "🔴" : "🚨";
        TextView tvName = new TextView(requireContext());
        tvName.setText(riskLabel + " " + n.ssid);
        tvName.setTextColor(risk == 0 ? 0xFF00FF88 : risk == 1 ? 0xFFFFCC00 : 0xFFFF3344);
        tvName.setTextSize(10);
        tvName.setTypeface(null, Typeface.BOLD);
        card.addView(tvName);

        // Segurança
        String secInfo = "Segurança: " + n.security;
        if (n.security.equals("Open")) secInfo += " ⚠️ TRÁFEGO EXPOSTO";
        if (n.security.equals("WEP"))  secInfo += " 🚨 QUEBRADO/INSEGURO";
        if (n.security.equals("WPA3")) secInfo += " ✅ Excelente";
        TextView tvSec = new TextView(requireContext());
        tvSec.setText(secInfo);
        tvSec.setTextColor(0xFFAAAAAA);
        tvSec.setTextSize(8);
        card.addView(tvSec);

        // OUI / fabricante
        String vendor = OuiDatabase.lookup(n.bssid);
        TextView tvVendor = new TextView(requireContext());
        tvVendor.setText("Fabricante: " + vendor);
        tvVendor.setTextColor(0xFF777777);
        tvVendor.setTextSize(8);
        card.addView(tvVendor);

        // WPS
        TextView tvWps = new TextView(requireContext());
        boolean hasWps = n.capabilities != null && n.capabilities.contains("WPS");
        tvWps.setText("WPS: " + (hasWps ? "⚠️ ATIVO (vulnerável)" : "✅ Inativo"));
        tvWps.setTextColor(hasWps ? 0xFFFF8800 : 0xFF00FF88);
        tvWps.setTextSize(8);
        card.addView(tvWps);

        // Canal / banda
        TextView tvCh = new TextView(requireContext());
        tvCh.setText("Canal: " + n.channel + " | " + n.band +
                " | " + n.frequency + " MHz | " + n.rssi + " dBm");
        tvCh.setTextColor(0xFF555555);
        tvCh.setTextSize(7);
        card.addView(tvCh);

        // BSSID
        TextView tvBssid = new TextView(requireContext());
        tvBssid.setText(n.bssid);
        tvBssid.setTextColor(0xFF444444);
        tvBssid.setTextSize(7);
        card.addView(tvBssid);

        container.addView(card);
    }

    /** 0=seguro, 1=atenção, 2=risco, 3=crítico */
    private int networkRisk(WifiNetwork n) {
        if (n.security.equals("Open")) return 3;
        if (n.security.equals("WEP"))  return 3;
        boolean wps = n.capabilities != null && n.capabilities.contains("WPS");
        if (n.security.equals("WPA") && wps) return 2;
        if (n.security.equals("WPA"))  return 1;
        if (n.security.equals("WPA2") && wps) return 1;
        if (n.security.equals("WPA2")) return 0;
        if (n.security.equals("WPA3")) return 0;
        return 1;
    }

    private int calcScore(List<WifiNetwork> nets) {
        if (nets.isEmpty()) return 100;
        int threats = ThreatDetector.activeThreats.size();
        int open = 0, wep = 0, wps = 0;
        for (WifiNetwork n : nets) {
            if (n.security.equals("Open") && n.signalBars >= 2) open++;
            if (n.security.equals("WEP")) wep++;
            if (n.capabilities != null && n.capabilities.contains("WPS")) wps++;
        }
        int score = 100;
        score -= threats * 15;
        score -= open   * 10;
        score -= wep    * 20;
        score -= wps    * 5;
        return Math.max(0, Math.min(100, score));
    }

    private void addHeader(String txt) {
        TextView tv = new TextView(requireContext());
        tv.setText(txt); tv.setTextColor(0xFF00FF88);
        tv.setTextSize(11); tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, 0, 0, dp(6));
        container.addView(tv);
    }

    private void addSectionTitle(String txt) {
        TextView tv = new TextView(requireContext());
        tv.setText(txt); tv.setTextColor(0xFF888888);
        tv.setTextSize(9);
        tv.setPadding(0, dp(4), 0, dp(4));
        container.addView(tv);
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

        TextView t = new TextView(requireContext());
        t.setText(title); t.setTextColor(0xFF777777); t.setTextSize(8);
        card.addView(t);
        TextView b = new TextView(requireContext());
        b.setText(body); b.setTextColor(color); b.setTextSize(10);
        b.setTypeface(null, Typeface.BOLD);
        card.addView(b);
        container.addView(card);
    }

    private int dp(int v) {
        return Math.round(v * requireContext().getResources().getDisplayMetrics().density);
    }
}
