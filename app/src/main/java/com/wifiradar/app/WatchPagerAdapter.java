package com.wifiradar.app;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class WatchPagerAdapter extends FragmentStateAdapter {

    public WatchPagerAdapter(FragmentActivity fa) { super(fa); }

    @Override public int getItemCount() { return 4; }

    @NonNull
    @Override
    public Fragment createFragment(int pos) {
        switch (pos) {
            case 0: return new RadarFragment();
            case 1: return new ListFragment();
            case 2: return new MeterFragment();
            case 3: return new ChannelFragment();
            default: return new RadarFragment();
        }
    }
}
