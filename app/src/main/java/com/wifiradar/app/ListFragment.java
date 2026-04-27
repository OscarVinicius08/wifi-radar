package com.wifiradar.app;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ListFragment extends Fragment {

    private LinearLayout container;

    private final Runnable listener = this::populate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf,
            @Nullable ViewGroup parent, @Nullable Bundle saved) {

        ScrollView sv = new ScrollView(requireContext());
        sv.setBackgroundColor(Color.BLACK);

        container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(6), dp(6), dp(6), dp(24));
        sv.addView(container);

        return sv;
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity.dataListeners.add(listener);
        listener.run();
    }

    @Override
    public void onPause() {
        super.onPause();
        MainActivity.dataListeners.remove(listener);
    }

    private void populate() {
        if (container == null) return;
        container.removeAllViews();

        // Cabeçalho
        TextView header = makeText("SSID  CH  dBm  Seg   ~m", 9, 0xFF00FF88);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        container.addView(header);
        addDivider();

        if (MainActivity.sharedNetworks.isEmpty()) {
            container.addView(makeText("Nenhuma rede encontrada.\nToque para escanear.", 10, 0xFFAAAAAA));
            return;
        }

        for (WifiNetwork n : MainActivity.sharedNetworks) {
            int color = colorForBars(n.signalBars);

            // Linha 1: SSID (truncado)
            String ssidShort = n.ssid.length() > 16 ? n.ssid.substring(0, 15) + "…" : n.ssid;
            TextView tvSsid = makeText(ssidShort, 11, color);
            tvSsid.setTypeface(null, android.graphics.Typeface.BOLD);
            container.addView(tvSsid);

            // Linha 2: detalhes
            String detail = String.format("CH%d %s  %ddBm  %s  ~%.0fm",
                    n.channel, n.band, n.rssi, n.security, n.distanceM);
            container.addView(makeText(detail, 9, 0xFFAAAAAA));

            // Linha 3: BSSID
            container.addView(makeText(n.bssid, 8, 0xFF666666));

            addDivider();
        }
    }

    private TextView makeText(String txt, int sp, int color) {
        TextView tv = new TextView(requireContext());
        tv.setText(txt);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setPadding(0, dp(2), 0, dp(2));
        return tv;
    }

    private void addDivider() {
        View d = new View(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(0, dp(3), 0, dp(3));
        d.setLayoutParams(lp);
        d.setBackgroundColor(0x22FFFFFF);
        container.addView(d);
    }

    private static int colorForBars(int bars) {
        switch (bars) {
            case 4: return 0xFF00FF88;
            case 3: return 0xFFBBFF33;
            case 2: return 0xFFFFCC00;
            case 1: return 0xFFFF8800;
            default: return 0xFFFF3344;
        }
    }

    private int dp(int v) {
        return Math.round(v * requireContext()
                .getResources().getDisplayMetrics().density);
    }
}
