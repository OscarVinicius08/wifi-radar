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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMISSIONS = 101;
    private static final long SCAN_INTERVAL_MS = 30_000L;

    /** Dados compartilhados com os fragments (acesso na main thread). */
    public static final List<WifiNetwork> sharedNetworks = new ArrayList<>();
    /** Fragments registram aqui para receber notificação de novos dados. */
    public static final List<Runnable> dataListeners = new ArrayList<>();

    private WifiManager wifiManager;
    private ViewPager2 viewPager;
    private LinearLayout dotsContainer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean receiverRegistered = false;

    private final BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            processScanResults(intent.getBooleanExtra(
                    WifiManager.EXTRA_RESULTS_UPDATED, false));
        }
    };

    private final Runnable scanRunnable = new Runnable() {
        @Override public void run() {
            triggerScan();
            handler.postDelayed(this, SCAN_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        viewPager     = findViewById(R.id.view_pager);
        dotsContainer = findViewById(R.id.dots_container);

        viewPager.setAdapter(new WatchPagerAdapter(this));
        viewPager.setOffscreenPageLimit(4);

        buildDots();
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int pos) { updateDots(pos); }
        });

        ensurePermissions();
    }

    private void buildDots() {
        dotsContainer.removeAllViews();
        for (int i = 0; i < 4; i++) {
            TextView dot = new TextView(this);
            dot.setText("●");
            dot.setTextSize(8);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(6, 0, 6, 0);
            dot.setLayoutParams(lp);
            dotsContainer.addView(dot);
        }
        updateDots(0);
    }

    private void updateDots(int selected) {
        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            ((TextView) dotsContainer.getChildAt(i))
                    .setTextColor(i == selected ? 0xFF00FF88 : 0x55FFFFFF);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!receiverRegistered) {
            registerReceiver(scanReceiver,
                    new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
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

    private void ensurePermissions() {
        String[] needed = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
        };
        List<String> missing = new ArrayList<>();
        for (String p : needed)
            if (ContextCompat.checkSelfPermission(this, p)
                    != PackageManager.PERMISSION_GRANTED) missing.add(p);
        if (!missing.isEmpty())
            ActivityCompat.requestPermissions(
                    this, missing.toArray(new String[0]), REQ_PERMISSIONS);
        else
            triggerScan();
    }

    @Override
    public void onRequestPermissionsResult(int req,
            @NonNull String[] perms, @NonNull int[] grants) {
        super.onRequestPermissionsResult(req, perms, grants);
        if (req == REQ_PERMISSIONS) {
            boolean ok = grants.length > 0;
            for (int g : grants) if (g != PackageManager.PERMISSION_GRANTED) ok = false;
            if (ok) triggerScan();
        }
    }

    public void triggerScan() {
        if (wifiManager == null || !wifiManager.isWifiEnabled()) return;
        try { //noinspection deprecation
            boolean started = wifiManager.startScan();
            if (!started) processScanResults(false);
        } catch (Exception e) {
            processScanResults(false);
        }
    }

    private void processScanResults(boolean fresh) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;
        List<ScanResult> raw;
        try { raw = wifiManager.getScanResults(); }
        catch (SecurityException e) { return; }
        if (raw == null) raw = Collections.emptyList();

        Collections.sort(raw, (a, b) -> Integer.compare(b.level, a.level));

        sharedNetworks.clear();
        for (ScanResult r : raw) sharedNetworks.add(WifiNetwork.from(r));

        // Notifica todos os fragments registrados
        for (Runnable l : new ArrayList<>(dataListeners)) l.run();
    }
}
