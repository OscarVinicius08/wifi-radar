package com.wifiradar.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MeterFragment extends Fragment {

    private MeterView meterView;

    private final Runnable listener = () -> {
        if (meterView != null)
            meterView.setData(MainActivity.sharedNetworks);
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf,
            @Nullable ViewGroup container, @Nullable Bundle saved) {
        meterView = new MeterView(requireContext());
        meterView.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).triggerScan();
        });
        return meterView;
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
}
