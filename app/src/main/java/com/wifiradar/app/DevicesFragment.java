package com.wifiradar.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class DevicesFragment extends Fragment {

    private LinearLayout container;
    private TextView tvStatus;
    private ProgressBar progressBar;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean scanning = false;
    private final List<NetworkScanner.Device> devices = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf,
            @Nullable ViewGroup parent, @Nullable Bundle s) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(dp(8), dp(6), dp(8), dp(8));

        // Header
        TextView hdr = new TextView(requireContext());
        hdr.setText("📱 DISPOSITIVOS NA REDE");
        hdr.setTextColor(0xFF00FF88);
        hdr.setTextSize(11);
        hdr.setTypeface(null, Typeface.BOLD);
        root.addView(hdr);

        // Status + botão
        tvStatus = new TextView(requireContext());
        tvStatus.setText("Toque para escanear a rede local");
        tvStatus.setTextColor(0xFFAAAAAA);
        tvStatus.setTextSize(9);
        tvStatus.setPadding(0, dp(4), 0, dp(4));
        root.addView(tvStatus);

        progressBar = new ProgressBar(requireContext(),
                null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(254);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.GONE);
        root.addView(progressBar);

        // Botão scan
        TextView btn = new TextView(requireContext());
        btn.setText("[ ESCANEAR REDE ]");
        btn.setTextColor(0xFF00FF88);
        btn.setTextSize(10);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setPadding(dp(8), dp(6), dp(8), dp(6));
        btn.setBackgroundColor(0x1100FF88);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        blp.setMargins(0, dp(4), 0, dp(8));
        btn.setLayoutParams(blp);
        btn.setOnClickListener(v -> startScan());
        root.addView(btn);

        // Lista
        ScrollView sv = new ScrollView(requireContext());
        container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        sv.addView(container);
        root.addView(sv);

        return root;
    }

    private void startScan() {
        if (scanning) return;
        scanning = true;
        devices.clear();
        container.removeAllViews();
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("Escaneando 254 hosts...");

        WifiManager wm = (WifiManager) requireContext()
                .getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        NetworkScanner.scanSubnet(wm, new NetworkScanner.ScanCallback() {
            @Override public void onDeviceFound(NetworkScanner.Device d) {
                devices.add(d);
                MainActivity.addEvent("📱 Dispositivo: " + d.ip + " [" + d.vendor + "]");
                ui.post(() -> addDeviceCard(d));
            }
            @Override public void onProgress(int done, int total) {
                ui.post(() -> {
                    progressBar.setProgress(done);
                    tvStatus.setText("Escaneando " + done + "/" + total + " — " +
                            devices.size() + " encontrado(s)");
                });
            }
            @Override public void onComplete(List<NetworkScanner.Device> all) {
                scanning = false;
                ui.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText(all.size() + " dispositivo(s) encontrado(s) na rede");
                });
            }
        });
    }

    private void addDeviceCard(NetworkScanner.Device d) {
        if (container == null) return;
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0x11FFFFFF);
        int p = dp(8);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(4));
        card.setLayoutParams(lp);

        // IP + latência
        TextView tvIp = new TextView(requireContext());
        tvIp.setText(d.ip + "  (" + d.latencyMs + "ms)");
        tvIp.setTextColor(0xFF00FF88);
        tvIp.setTextSize(11);
        tvIp.setTypeface(null, Typeface.BOLD);
        card.addView(tvIp);

        // Fabricante
        TextView tvVendor = new TextView(requireContext());
        tvVendor.setText("🏭 " + d.vendor);
        tvVendor.setTextColor(0xFFDDDDDD);
        tvVendor.setTextSize(9);
        card.addView(tvVendor);

        // MAC
        TextView tvMac = new TextView(requireContext());
        tvMac.setText(d.mac);
        tvMac.setTextColor(0xFF666666);
        tvMac.setTextSize(8);
        card.addView(tvMac);

        // Botão port scan inline
        TextView btnPorts = new TextView(requireContext());
        btnPorts.setText("[ ver portas ]");
        btnPorts.setTextColor(0xFF3399FF);
        btnPorts.setTextSize(8);
        btnPorts.setPadding(0, dp(4), 0, 0);
        card.addView(btnPorts);

        TextView tvPorts = new TextView(requireContext());
        tvPorts.setTextColor(0xFFFFCC00);
        tvPorts.setTextSize(8);
        card.addView(tvPorts);

        btnPorts.setOnClickListener(v -> {
            btnPorts.setText("escaneando portas...");
            NetworkScanner.scanPorts(d.ip, new NetworkScanner.PortCallback() {
                StringBuilder sb = new StringBuilder();
                @Override public void onPortOpen(int port, String service) {
                    sb.append("• ").append(port).append("/").append(service).append("\n");
                    MainActivity.addEvent("🔓 " + d.ip + ":" + port + " [" + service + "] ABERTA");
                }
                @Override public void onComplete() {
                    String result = sb.length() > 0 ? sb.toString() : "nenhuma porta comum aberta";
                    ui.post(() -> {
                        tvPorts.setText(result.trim());
                        btnPorts.setVisibility(View.GONE);
                    });
                }
            });
        });

        container.addView(card, 0); // mais novo no topo
    }

    private int dp(int v) {
        return Math.round(v * requireContext().getResources().getDisplayMetrics().density);
    }
}
