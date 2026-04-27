package com.wifiradar.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ChannelFragment extends Fragment {

    private ChannelView channelView;

    private final Runnable listener = () -> {
        if (channelView != null)
            channelView.setNetworks(MainActivity.sharedNetworks);
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf,
            @Nullable ViewGroup container, @Nullable Bundle saved) {
        channelView = new ChannelView(requireContext());
        return channelView;
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
