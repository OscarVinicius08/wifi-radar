package com.wifiradar.app;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMISSIONS = 101;
    // Frequência do auto-scan (Android 9 limita a ~4 scans / 2 minutos)
    private static final long SCAN_INTERVAL_MS = 30_000L;

    private WifiManager wifiManager;
    private RadarView radarView;
    private TextView txtStatus;
    private TextView txtCount;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean receiverRegistered = false;

    private final BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            boolean ok = intent.getBooleanExtra(
                    WifiManager.EXTRA_RESULTS_UPDATED, false);
            handleScanResults(ok);
        }
    };

    private final Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            triggerScan();
            handler.postDelayed(this, SCAN_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        radarView  = findViewById(R.id.radar_view);
        txtStatus  = findViewById(R.id.txt_status);
        txtCount   = findViewById(R.id.txt_count);

        wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        // toque na tela = força um novo scan
        findViewById(R.id.root).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerScan();
            }
        });

        ensurePermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!receiverRegistered) {
            IntentFilter f = new IntentFilter(
                    WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            registerReceiver(scanReceiver, f);
            receiverRegistered = true;
        }
        handler.post(scanRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(scanRunnable);
        if (receiverRegistered) {
            try { unregisterReceiver(scanReceiver); } catch (Exception ignored) {}
            receiverRegistered = false;
        }
    }

    // ---------- permissões ----------

    private void ensurePermissions() {
        String[] needed = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
        };
        List<String> missing = new ArrayList<>();
        for (String p : needed) {
            if (ContextCompat.checkSelfPermission(this, p)
                    != PackageManager.PERMISSION_GRANTED) {
                missing.add(p);
            }
        }
        if (!missing.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, missing.toArray(new String[0]), REQ_PERMISSIONS);
        } else {
            triggerScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS) {
            boolean allGranted = grantResults.length > 0;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) allGranted = false;
            }
            if (allGranted) {
                triggerScan();
            } else {
                txtStatus.setText(R.string.err_permissions);
                Toast.makeText(this, R.string.err_permissions,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // ---------- scan ----------

    private void triggerScan() {
        if (wifiManager == null) return;
        if (!wifiManager.isWifiEnabled()) {
            txtStatus.setText(R.string.wifi_off);
            // tenta ligar (em Android 10+ exigirá ação do usuário)
            try { wifiManager.setWifiEnabled(true); } catch (Exception ignored) {}
            return;
        }

        txtStatus.setText(R.string.scanning);
        boolean started = false;
        try {
            //noinspection deprecation
            started = wifiManager.startScan();
        } catch (Exception ignored) {}

        // independentemente de startScan ter sido aceito (throttling),
        // pegamos o último resultado conhecido.
        if (!started) {
            handleScanResults(false);
        }
    }

    private void handleScanResults(boolean fresh) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            txtStatus.setText(R.string.err_permissions);
            return;
        }

        List<ScanResult> raw;
        try {
            raw = wifiManager.getScanResults();
        } catch (SecurityException e) {
            txtStatus.setText(R.string.err_permissions);
            return;
        }

        if (raw == null) raw = Collections.emptyList();

        // ordena por força do sinal (mais forte primeiro)
        Collections.sort(raw, new Comparator<ScanResult>() {
            @Override public int compare(ScanResult a, ScanResult b) {
                return Integer.compare(b.level, a.level);
            }
        });

        List<WifiNetwork> networks = new ArrayList<>(raw.size());
        for (ScanResult r : raw) {
            networks.add(WifiNetwork.from(r));
        }

        radarView.setNetworks(networks);
        txtCount.setText(getString(R.string.count_fmt, networks.size()));
        txtStatus.setText(fresh ? R.string.scan_done : R.string.scan_cached);
    }
}
