package com.wifiradar.app;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detecta ameaças Wi-Fi passivamente:
 * - Evil Twin (mesmo SSID, BSSID diferente, canal diferente)
 * - ARP Spoofing (MAC do gateway mudou)
 * - DNS suspeito (DNS fora do range esperado)
 * - Redes abertas com mesmo SSID de redes conhecidas
 * - Redes com SSID oculto
 */
public class ThreatDetector {

    public static final List<String> activeThreats = new ArrayList<>();

    // Último MAC conhecido do gateway para detectar ARP spoof
    private static String lastGatewayMac = null;
    private static String lastGatewayIp  = null;

    public static void analyze(List<WifiNetwork> prev,
                                List<WifiNetwork> current,
                                WifiManager wm,
                                Context ctx) {
        activeThreats.clear();

        detectEvilTwin(current);
        detectArpSpoof(wm, ctx);
        detectDnsAnomaly(wm);
        detectHiddenNetworks(current);
        detectOpenImpersonation(current);
    }

    /** Evil Twin: dois APs com mesmo SSID mas BSSIDs/canais diferentes */
    private static void detectEvilTwin(List<WifiNetwork> nets) {
        Map<String, List<WifiNetwork>> bySSID = new HashMap<>();
        for (WifiNetwork n : nets) {
            if (n.ssid.equals("<oculta>")) continue;
            if (!bySSID.containsKey(n.ssid)) bySSID.put(n.ssid, new ArrayList<>());
            bySSID.get(n.ssid).add(n);
        }
        for (Map.Entry<String, List<WifiNetwork>> e : bySSID.entrySet()) {
            if (e.getValue().size() < 2) continue;
            // Verifica se têm canais diferentes (não é só múltiplos APs do mesmo sistema)
            int firstCh = e.getValue().get(0).channel;
            boolean diffCh = false;
            for (WifiNetwork n : e.getValue())
                if (n.channel != firstCh) { diffCh = true; break; }
            if (diffCh) {
                String msg = "⚠️ EVIL TWIN: \"" + e.getKey() + "\" em " + e.getValue().size() + " canais";
                activeThreats.add(msg);
                MainActivity.addEvent(msg);
            }
        }
    }

    /** ARP Spoof: lê /proc/net/arp e verifica se o MAC do gateway mudou */
    private static void detectArpSpoof(WifiManager wm, Context ctx) {
        try {
            DhcpInfo dhcp = wm.getDhcpInfo();
            if (dhcp == null || dhcp.gateway == 0) return;
            String gwIp = intToIp(dhcp.gateway);
            String gwMac = getMacFromArp(gwIp);
            if (gwMac == null) return;

            if (lastGatewayIp != null && lastGatewayIp.equals(gwIp)) {
                if (lastGatewayMac != null && !lastGatewayMac.equals(gwMac)) {
                    String msg = "🚨 ARP SPOOF: gateway " + gwIp +
                            " mudou de " + lastGatewayMac + " → " + gwMac;
                    activeThreats.add(msg);
                    MainActivity.addEvent(msg);
                }
            }
            lastGatewayIp  = gwIp;
            lastGatewayMac = gwMac;
        } catch (Exception ignored) {}
    }

    /** DNS suspeito: DNS server fora do range RFC-1918 pode indicar redirecionamento */
    private static void detectDnsAnomaly(WifiManager wm) {
        try {
            DhcpInfo dhcp = wm.getDhcpInfo();
            if (dhcp == null || dhcp.dns1 == 0) return;
            String dns = intToIp(dhcp.dns1);
            // DNS público conhecido OK: 8.8.8.8, 1.1.1.1, 8.8.4.4, etc.
            boolean isPublicKnown = dns.equals("8.8.8.8") || dns.equals("8.8.4.4") ||
                    dns.equals("1.1.1.1") || dns.equals("1.0.0.1") ||
                    dns.equals("208.67.222.222");
            // DNS privado (mesmo range do gateway) também é normal
            boolean isPrivate = dns.startsWith("192.168.") ||
                    dns.startsWith("10.") || dns.startsWith("172.");
            if (!isPublicKnown && !isPrivate) {
                String msg = "⚠️ DNS SUSPEITO: " + dns + " (não é privado nem público conhecido)";
                activeThreats.add(msg);
                MainActivity.addEvent(msg);
            }
        } catch (Exception ignored) {}
    }

    /** Redes com SSID oculto */
    private static void detectHiddenNetworks(List<WifiNetwork> nets) {
        int hidden = 0;
        for (WifiNetwork n : nets) if (n.ssid.equals("<oculta>")) hidden++;
        if (hidden > 0) {
            activeThreats.add("👁 " + hidden + " rede(s) com SSID oculto detectada(s)");
        }
    }

    /** Rede aberta com mesmo SSID de rede protegida — possível honeypot */
    private static void detectOpenImpersonation(List<WifiNetwork> nets) {
        Map<String, String> ssidSec = new HashMap<>();
        for (WifiNetwork n : nets) {
            if (n.ssid.equals("<oculta>")) continue;
            if (!ssidSec.containsKey(n.ssid)) {
                ssidSec.put(n.ssid, n.security);
            } else {
                String existing = ssidSec.get(n.ssid);
                // Se uma está aberta e outra protegida = suspeito
                boolean oneOpen = existing.equals("Open") || n.security.equals("Open");
                boolean oneProt = !existing.equals("Open") || !n.security.equals("Open");
                if (oneOpen && oneProt) {
                    String msg = "⚠️ HONEYPOT? \"" + n.ssid + "\" existe como Open E " +
                            (existing.equals("Open") ? n.security : existing);
                    activeThreats.add(msg);
                    MainActivity.addEvent(msg);
                }
            }
        }
    }

    /** Lê /proc/net/arp para obter MAC de um IP — funciona sem root */
    public static String getMacFromArp(String ip) {
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"))) {
            String line;
            br.readLine(); // header
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 4 && parts[0].equals(ip)) {
                    String mac = parts[3];
                    if (!mac.equals("00:00:00:00:00:00")) return mac;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static String intToIp(int ip) {
        return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." +
               ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
    }
}
