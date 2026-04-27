package com.wifiradar.app;

import android.net.wifi.ScanResult;

public class WifiNetwork {

    public final String ssid;
    public final String bssid;
    public final int rssi;
    public final int frequency;
    public final double distanceM;
    public final int signalBars;
    public final int channel;
    public final String security;
    public final String band;

    private WifiNetwork(String ssid, String bssid, int rssi, int freq,
                        double dist, int bars, int ch, String sec) {
        this.ssid = ssid; this.bssid = bssid; this.rssi = rssi;
        this.frequency = freq; this.distanceM = dist; this.signalBars = bars;
        this.channel = ch; this.security = sec;
        this.band = freq >= 5000 ? "5G" : "2.4G";
    }

    public static WifiNetwork from(ScanResult r) {
        String name = (r.SSID == null || r.SSID.isEmpty()) ? "<oculta>" : r.SSID;
        String mac  = r.BSSID == null ? "00:00:00:00:00:00" : r.BSSID;
        return new WifiNetwork(name, mac, r.level, r.frequency,
                estimateDistanceMeters(r.level, r.frequency),
                signalBarsFromRssi(r.level),
                channelFromFreq(r.frequency),
                parseSecurityFromCaps(r.capabilities));
    }

    public static double estimateDistanceMeters(int rssi, int freqMHz) {
        if (freqMHz <= 0) freqMHz = 2412;
        double exp = (27.55 - (20.0 * Math.log10(freqMHz)) + Math.abs(rssi)) / 20.0;
        double d = Math.pow(10.0, exp);
        return Math.max(0.3, Math.min(200, d));
    }

    public static int signalBarsFromRssi(int rssi) {
        if (rssi >= -50) return 4;
        if (rssi >= -60) return 3;
        if (rssi >= -70) return 2;
        if (rssi >= -80) return 1;
        return 0;
    }

    public static int channelFromFreq(int freq) {
        if (freq == 2484) return 14;
        if (freq >= 2412 && freq <= 2472) return (freq - 2407) / 5;
        if (freq >= 5160 && freq <= 5885) return (freq - 5000) / 5;
        if (freq >= 5955) return (freq - 5955) / 5 + 1;
        return 0;
    }

    public static String parseSecurityFromCaps(String caps) {
        if (caps == null || caps.isEmpty()) return "Open";
        if (caps.contains("SAE"))  return "WPA3";
        if (caps.contains("WPA2")) return "WPA2";
        if (caps.contains("WPA"))  return "WPA";
        if (caps.contains("WEP"))  return "WEP";
        return "Open";
    }

    public double bearingRadians() {
        int hash = bssid.hashCode();
        return ((hash & 0xFFFF) / 65535.0) * 2.0 * Math.PI;
    }
}
