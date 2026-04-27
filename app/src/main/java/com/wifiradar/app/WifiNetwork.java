package com.wifiradar.app;

import android.net.wifi.ScanResult;

/**
 * Representa uma rede Wi-Fi detectada com:
 *  - SSID (nome)
 *  - BSSID (MAC do AP) — usado como semente para posicionamento angular estável
 *  - RSSI (dBm)
 *  - distância estimada em metros (modelo log-distance path loss)
 *  - frequência (MHz) — usada para ajustar o cálculo
 *
 * IMPORTANTE: a distância é uma ESTIMATIVA. RSSI é altamente afetado por
 * paredes, multipath, interferência e potência de transmissão real do AP
 * (que assumimos ~ -40 dBm a 1 metro). Erros de ±50% são comuns.
 */
public class WifiNetwork {

    public final String ssid;
    public final String bssid;
    public final int rssi;          // em dBm (ex.: -55)
    public final int frequency;     // em MHz
    public final double distanceM;  // estimativa em metros
    public final int signalBars;    // 0..4

    private WifiNetwork(String ssid, String bssid, int rssi, int freq,
                        double distance, int bars) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.rssi = rssi;
        this.frequency = freq;
        this.distanceM = distance;
        this.signalBars = bars;
    }

    public static WifiNetwork from(ScanResult r) {
        String name = (r.SSID == null || r.SSID.isEmpty()) ? "<oculta>" : r.SSID;
        String mac  = r.BSSID == null ? "00:00:00:00:00:00" : r.BSSID;
        double dist = estimateDistanceMeters(r.level, r.frequency);
        int bars = signalBarsFromRssi(r.level);
        return new WifiNetwork(name, mac, r.level, r.frequency, dist, bars);
    }

    /**
     * Modelo Free Space Path Loss (FSPL):
     *   distância(m) = 10 ^ ((27.55 - 20*log10(freqMHz) - rssi) / 20)
     *
     * Boa aproximação em ambiente aberto. Em ambientes fechados a distância
     * real costuma ser MENOR (paredes atenuam o sinal, fazendo o RSSI parecer
     * "mais longe" do que realmente está).
     */
    public static double estimateDistanceMeters(int rssi, int freqMHz) {
        if (freqMHz <= 0) freqMHz = 2412; // fallback canal 1 / 2.4 GHz
        double exp = (27.55 - (20.0 * Math.log10(freqMHz)) + Math.abs(rssi)) / 20.0;
        double d = Math.pow(10.0, exp);
        // sanidade: limita entre 0.3m e 200m
        if (d < 0.3) d = 0.3;
        if (d > 200) d = 200;
        return d;
    }

    public static int signalBarsFromRssi(int rssi) {
        if (rssi >= -50) return 4; // excelente
        if (rssi >= -60) return 3; // bom
        if (rssi >= -70) return 2; // ok
        if (rssi >= -80) return 1; // fraco
        return 0;                  // péssimo
    }

    /**
     * Gera um ângulo determinístico (0..2π) a partir do BSSID, para que cada
     * rede sempre apareça no MESMO ponto do radar entre scans (não fica
     * pulando aleatoriamente). NÃO representa direção real do sinal.
     */
    public double bearingRadians() {
        int hash = bssid.hashCode();
        return ((hash & 0xFFFF) / 65535.0) * 2.0 * Math.PI;
    }
}
