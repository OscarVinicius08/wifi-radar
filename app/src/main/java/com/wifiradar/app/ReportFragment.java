package com.wifiradar.app;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportFragment extends Fragment {

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

        addTitle("📋 RELATÓRIO & TIMELINE");

        // Estatísticas gerais
        List<WifiNetwork> nets = MainActivity.sharedNetworks;
        int open = 0, wpa3 = 0, wps = 0;
        for (WifiNetwork n : nets) {
            if (n.security.equals("Open")) open++;
            if (n.security.equals("WPA3")) wpa3++;
            if (n.capabilities != null && n.capabilities.contains("WPS")) wps++;
        }

        addStatCard("Redes detectadas", "" + nets.size());
        addStatCard("Redes abertas", "" + open,
                open > 0 ? 0xFFFF8800 : 0xFF00FF88);
        addStatCard("Com WPA3", "" + wpa3, wpa3 > 0 ? 0xFF00FF88 : 0xFF888888);
        addStatCard("Com WPS ativo", "" + wps, wps > 0 ? 0xFFFFCC00 : 0xFF00FF88);
        addStatCard("Ameaças ativas", "" + ThreatDetector.activeThreats.size(),
                ThreatDetector.activeThreats.isEmpty() ? 0xFF00FF88 : 0xFFFF3344);
        addStatCard("Eventos no log", "" + MainActivity.eventLog.size());

        // Botão exportar
        addBtn("[ 📤 EXPORTAR RELATÓRIO TXT ]", () -> exportReport());

        addDivider();
        addSubTitle("🕐 TIMELINE DE EVENTOS");

        if (MainActivity.eventLog.isEmpty()) {
            addText("Nenhum evento registrado ainda.\nO log é preenchido automaticamente.", 0xFF555555);
        } else {
            for (String event : MainActivity.eventLog) {
                addText(event, event.contains("🚨") ? 0xFFFF3344 :
                               event.contains("⚠️") ? 0xFFFFCC00 :
                               event.contains("🔓") ? 0xFFFF8800 : 0xFF888888);
            }
        }
    }

    private void exportReport() {
        try {
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            StringBuilder sb = new StringBuilder();
            sb.append("=== WiFi Radar — Relatório de Segurança ===\n");
            sb.append("Gerado: ").append(new Date()).append("\n\n");

            sb.append("--- REDES DETECTADAS (").append(MainActivity.sharedNetworks.size()).append(") ---\n");
            for (WifiNetwork n : MainActivity.sharedNetworks) {
                sb.append("SSID: ").append(n.ssid).append("\n");
                sb.append("  BSSID: ").append(n.bssid).append("\n");
                sb.append("  Segurança: ").append(n.security).append("\n");
                sb.append("  Canal: ").append(n.channel).append(" | Banda: ").append(n.band).append("\n");
                sb.append("  RSSI: ").append(n.rssi).append(" dBm | Distância ~")
                  .append(String.format("%.0f", n.distanceM)).append("m\n");
                sb.append("  Fabricante: ").append(OuiDatabase.lookup(n.bssid)).append("\n");
                sb.append("  WPS: ").append(n.capabilities != null && n.capabilities.contains("WPS")
                        ? "SIM ⚠️" : "não").append("\n\n");
            }

            sb.append("--- AMEAÇAS DETECTADAS ---\n");
            if (ThreatDetector.activeThreats.isEmpty()) {
                sb.append("Nenhuma ameaça detectada\n");
            } else {
                for (String t : ThreatDetector.activeThreats)
                    sb.append("• ").append(t).append("\n");
            }

            sb.append("\n--- TIMELINE DE EVENTOS ---\n");
            for (String e : MainActivity.eventLog)
                sb.append(e).append("\n");

            File dir = requireContext().getExternalFilesDir(null);
            if (dir == null) dir = requireContext().getFilesDir();
            File f = new File(dir, "wifi_radar_" + ts + ".txt");
            try (FileWriter fw = new FileWriter(f)) { fw.write(sb.toString()); }

            // Compartilhar via Intent
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            try {
                android.net.Uri uri = FileProvider.getUriForFile(requireContext(),
                        "com.wifiradar.app.provider", f);
                share.putExtra(Intent.EXTRA_STREAM, uri);
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                share.putExtra(Intent.EXTRA_SUBJECT, "WiFi Radar — Relatório " + ts);
            } catch (Exception ex) {
                share.putExtra(Intent.EXTRA_TEXT, sb.toString());
            }
            startActivity(Intent.createChooser(share, "Compartilhar relatório"));
            MainActivity.addEvent("📤 Relatório exportado: wifi_radar_" + ts + ".txt");

        } catch (Exception e) {
            MainActivity.addEvent("❌ Erro ao exportar: " + e.getMessage());
        }
    }

    private void addTitle(String txt) {
        TextView tv = new TextView(requireContext());
        tv.setText(txt); tv.setTextColor(0xFF00FF88);
        tv.setTextSize(11); tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, 0, 0, dp(6)); container.addView(tv);
    }

    private void addSubTitle(String txt) {
        TextView tv = new TextView(requireContext());
        tv.setText(txt); tv.setTextColor(0xFF00CCFF);
        tv.setTextSize(10); tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, dp(4), 0, dp(4)); container.addView(tv);
    }

    private void addStatCard(String label, String value) {
        addStatCard(label, value, 0xFFFFFFFF);
    }

    private void addStatCard(String label, String value, int color) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(3));
        row.setLayoutParams(lp);

        TextView tvL = new TextView(requireContext());
        tvL.setText(label); tvL.setTextColor(0xFF888888);
        tvL.setTextSize(9);
        tvL.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 2));
        row.addView(tvL);

        TextView tvV = new TextView(requireContext());
        tvV.setText(value); tvV.setTextColor(color);
        tvV.setTextSize(10); tvV.setTypeface(null, Typeface.BOLD);
        tvV.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(tvV);
        container.addView(row);
    }

    private void addBtn(String label, Runnable action) {
        TextView btn = new TextView(requireContext());
        btn.setText(label);
        btn.setTextColor(0xFF000000);
        btn.setBackgroundColor(0xFF00FF88);
        btn.setTextSize(9); btn.setTypeface(null, Typeface.BOLD);
        btn.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        btn.setLayoutParams(lp);
        btn.setOnClickListener(v -> action.run());
        container.addView(btn);
    }

    private void addText(String txt, int color) {
        TextView tv = new TextView(requireContext());
        tv.setText(txt); tv.setTextColor(color);
        tv.setTextSize(8); tv.setTypeface(Typeface.MONOSPACE);
        tv.setPadding(0, dp(1), 0, dp(1));
        container.addView(tv);
    }

    private void addDivider() {
        View d = new View(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(0, dp(6), 0, dp(8));
        d.setLayoutParams(lp); d.setBackgroundColor(0x22FFFFFF);
        container.addView(d);
    }

    private int dp(int v) {
        return Math.round(v * requireContext().getResources().getDisplayMetrics().density);
    }
}
