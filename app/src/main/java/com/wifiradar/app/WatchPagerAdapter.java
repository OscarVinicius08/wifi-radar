package com.wifiradar.app;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class WatchPagerAdapter extends FragmentStateAdapter {
    public WatchPagerAdapter(FragmentActivity fa) { super(fa); }
    @Override public int getItemCount() { return 9; }
    @NonNull
    @Override
    public Fragment createFragment(int pos) {
        switch (pos) {
            case 0: return new RadarFragment();
            case 1: return new ListFragment();
            case 2: return new MeterFragment();
            case 3: return new ChannelFragment();
            case 4: return new ThreatsFragment();
            case 5: return new DevicesFragment();
            case 6: return new AuditFragment();
            case 7: return new ToolsFragment();
            case 8: return new ReportFragment();
            default: return new RadarFragment();
        }
    }
}
