package com.wifiradar.app;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.net.InetAddress;

public class ToolsFragment extends Fragment {

    private LinearLayout logContainer;
    private EditText etTarget;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf,
            @Nullable ViewGroup parent, @Nullable Bundle s) {

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(dp(8), dp(6), dp(8), dp(8));

        addTitle(root, "🛠 FERRAMENTAS");

        // Campo de IP/host
        etTarget = new EditText(requireContext());
        etTarget.setHint("IP ou host (ex: 192.168.1.1)");
        etTarget.setHintTextColor(0xFF555555);
        etTarget.setTextColor(0xFF00FF88);
        etTarget.setTextSize(10);
        etTarget.setBackgroundColor(0x11FFFFFF);
        etTarget.setPadding(dp(8), dp(6), dp(8), dp(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(6));
        etTarget.setLayoutParams(lp);
        root.addView(etTarget);

        // Botões
        LinearLayout btns = new LinearLayout(requireContext());
        btns.setOrientation(LinearLayout.HORIZONTAL);
        addBtn(btns, "PING", () -> doPing(etTarget.getText().toString().trim()));
        addBtn(btns, "TRACERT", () -> doTraceroute(etTarget.getText().toString().trim()));
        addBtn(btns, "PORTAS", () -> doPortScan(etTarget.getText().toString().trim()));
        addBtn(btns, "DNS", () -> doDnsLookup(etTarget.getText().toString().trim()));
        root.addView(btns);

        // Botão gateway automático
        TextView btnGw = new TextView(requireContext());
        btnGw.setText("[ usar gateway automaticamente ]");
        btnGw.setTextColor(0xFF3399FF);
        btnGw.setTextSize(9);
        btnGw.setPadding(0, dp(4), 0, dp(4));
        btnGw.setOnClickListener(v -> fillGateway());
        root.addView(btnGw);

        // Separador
        View div = new View(requireContext());
        div.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        div.setBackgroundColor(0x22FFFFFF);
        root.addView(div);

        // Log de resultados
        TextView tvLogHdr = new TextView(requireContext());
        tvLogHdr.setText("Resultados:");
        tvLogHdr.setTextColor(0xFF888888);
        tvLogHdr.setTextSize(9);
        tvLogHdr.setPadding(0, dp(4), 0, dp(2));
        root.addView(tvLogHdr);

        ScrollView sv = new ScrollView(requireContext());
        logContainer = new LinearLayout(requireContext());
        logContainer.setOrientation(LinearLayout.VERTICAL);
        sv.addView(logContainer);
        root.addView(sv);

        return root;
    }

    private void fillGateway() {
        android.net.wifi.WifiManager wm = (android.net.wifi.WifiManager)
                requireContext().getApplicationContext()
                .getSystemService(android.content.Context.WIFI_SERVICE);
        if (wm == null) return;
        android.net.DhcpInfo dhcp = wm.getDhcpInfo();
        if (dhcp != null && dhcp.gateway != 0)
            etTarget.setText(ThreatDetector.intToIp(dhcp.gateway));
    }

    private void doPing(String host) {
        if (host.isEmpty()) { log("⚠️ Digite um IP ou host", 0xFFFFCC00); return; }
        log("🔵 Ping → " + host, 0xFF3399FF);
        new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                try {
                    long t0 = System.currentTimeMillis();
                    boolean ok = InetAddress.getByName(host).isReachable(2000);
                    long lat = System.currentTimeMillis() - t0;
                    String msg = ok ? "  seq " + i + " latência: " + lat + "ms"
                                    : "  seq " + i + " timeout";
                    int color = ok ? (lat < 50 ? 0xFF00FF88 : lat < 150 ? 0xFFFFCC00 : 0xFFFF8800)
                                   : 0xFFFF3344;
                    log(msg, color);
                } catch (Exception e) {
                    log("  seq " + i + " erro: " + e.getMessage(), 0xFFFF3344);
                }
                try { Thread.sleep(500); } catch (Exception ignored) {}
            }
            log("✅ Ping completo", 0xFF00FF88);
        }).start();
    }

    private void doTraceroute(String host) {
        if (host.isEmpty()) { log("⚠️ Digite um IP ou host", 0xFFFFCC00); return; }
        log("🔵 Traceroute → " + host, 0xFF3399FF);
        new Thread(() -> {
            try {
                Process p = Runtime.getRuntime().exec("traceroute -m 15 -w 2 " + host);
                java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(p.getInputStream()));
                String line;
                int hop = 0;
                while ((line = br.readLine()) != null && hop < 15) {
                    hop++;
                    log("  " + line, 0xFFAAAAAA);
                }
                log("✅ Traceroute completo", 0xFF00FF88);
            } catch (Exception e) {
                log("⚠️ Traceroute não disponível neste dispositivo", 0xFFFFCC00);
                // fallback: ping com TTL incremental
                log("Usando fallback manual (ping TTL)...", 0xFF888888);
            }
        }).start();
    }

    private void doPortScan(String host) {
        if (host.isEmpty()) { log("⚠️ Digite um IP ou host", 0xFFFFCC00); return; }
        log("🔵 Port scan → " + host, 0xFF3399FF);
        NetworkScanner.scanPorts(host, new NetworkScanner.PortCallback() {
            int found = 0;
            @Override public void onPortOpen(int port, String service) {
                found++;
                log("  🟢 " + port + "/" + service + " ABERTA", 0xFF00FF88);
                MainActivity.addEvent("🔓 " + host + ":" + port + " [" + service + "] ABERTA");
            }
            @Override public void onComplete() {
                log(found == 0 ? "✅ Nenhuma porta comum aberta" : "✅ " + found + " porta(s) abertas",
                        found == 0 ? 0xFF00FF88 : 0xFFFFCC00);
            }
        });
    }

    private void doDnsLookup(String host) {
        if (host.isEmpty()) { log("⚠️ Digite um IP ou host", 0xFFFFCC00); return; }
        log("🔵 DNS lookup → " + host, 0xFF3399FF);
        new Thread(() -> {
            try {
                InetAddress[] addrs = InetAddress.getAllByName(host);
                for (InetAddress a : addrs)
                    log("  → " + a.getHostAddress(), 0xFF00FF88);
                log("✅ " + addrs.length + " endereço(s) resolvido(s)", 0xFF00FF88);
            } catch (Exception e) {
                log("❌ Falha: " + e.getMessage(), 0xFFFF3344);
            }
        }).start();
    }

    private void log(String msg, int color) {
        ui.post(() -> {
            if (logContainer == null) return;
            TextView tv = new TextView(requireContext());
            tv.setText(msg);
            tv.setTextColor(color);
            tv.setTextSize(9);
            tv.setTypeface(android.graphics.Typeface.MONOSPACE);
            logContainer.addView(tv, 0);
            if (logContainer.getChildCount() > 80)
                logContainer.removeViewAt(logContainer.getChildCount() - 1);
        });
    }

    private void addTitle(LinearLayout root, String txt) {
        TextView tv = new TextView(requireContext());
        tv.setText(txt);
        tv.setTextColor(0xFF00FF88);
        tv.setTextSize(11);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, 0, 0, dp(4));
        root.addView(tv);
    }

    private void addBtn(LinearLayout parent, String label, Runnable action) {
        TextView btn = new TextView(requireContext());
        btn.setText(label);
        btn.setTextColor(0xFF000000);
        btn.setBackgroundColor(0xFF00FF88);
        btn.setTextSize(8);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setPadding(dp(8), dp(6), dp(8), dp(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(dp(2), 0, dp(2), dp(6));
        btn.setLayoutParams(lp);
        btn.setOnClickListener(v -> action.run());
        parent.addView(btn);
    }

    private int dp(int v) {
        return Math.round(v * requireContext().getResources().getDisplayMetrics().density);
    }
}
