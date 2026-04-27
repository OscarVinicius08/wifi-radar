package com.wifiradar.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class RadarFragment extends Fragment {

    private RadarView radarView;

    private final Runnable listener = () -> {
        if (radarView != null)
            radarView.setNetworks(MainActivity.sharedNetworks);
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf,
            @Nullable ViewGroup container, @Nullable Bundle saved) {
        radarView = new RadarView(requireContext());
        // tap = força scan
        radarView.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).triggerScan();
        });
        return radarView;
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity.dataListeners.add(listener);
        // mostra dados já disponíveis imediatamente
        listener.run();
    }

    @Override
    public void onPause() {
        super.onPause();
        MainActivity.dataListeners.remove(listener);
    }
}
