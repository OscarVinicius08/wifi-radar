package com.wifiradar.app;

import android.net.DhcpInfo;
import android.net.wifi.WifiManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Scanner de dispositivos e portas na rede local. */
public class NetworkScanner {

    public static class Device {
        public final String ip;
        public final String mac;
        public final String vendor;
        public final long latencyMs;
        public Device(String ip, String mac, String vendor, long lat) {
            this.ip = ip; this.mac = mac; this.vendor = vendor; this.latencyMs = lat;
        }
    }

    public interface ScanCallback {
        void onDeviceFound(Device d);
        void onProgress(int done, int total);
        void onComplete(List<Device> devices);
    }

    public static void scanSubnet(WifiManager wm, ScanCallback cb) {
        new Thread(() -> {
            List<Device> found = new ArrayList<>();
            try {
                DhcpInfo dhcp = wm.getDhcpInfo();
                if (dhcp == null || dhcp.ipAddress == 0) { cb.onComplete(found); return; }

                String myIp = ThreatDetector.intToIp(dhcp.ipAddress);
                String subnet = myIp.substring(0, myIp.lastIndexOf('.') + 1);
                int total = 254;
                AtomicInteger done = new AtomicInteger(0);
                ExecutorService pool = Executors.newFixedThreadPool(30);

                for (int i = 1; i <= 254; i++) {
                    final String ip = subnet + i;
                    pool.submit(() -> {
                        try {
                            long t0 = System.currentTimeMillis();
                            InetAddress addr = InetAddress.getByName(ip);
                            boolean reachable = addr.isReachable(400);
                            long lat = System.currentTimeMillis() - t0;
                            if (reachable) {
                                String mac = getMacForIp(ip);
                                String vendor = mac != null ? OuiDatabase.lookup(mac) : "Desconhecido";
                                Device dev = new Device(ip, mac != null ? mac : "??:??:??:??:??:??", vendor, lat);
                                synchronized (found) { found.add(dev); }
                                cb.onDeviceFound(dev);
                            }
                        } catch (Exception ignored) {}
                        cb.onProgress(done.incrementAndGet(), total);
                    });
                }
                pool.shutdown();
                pool.awaitTermination(30, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
            cb.onComplete(found);
        }).start();
    }

    private static String getMacForIp(String ip) {
        // Ping primeiro para popular ARP cache
        try { Runtime.getRuntime().exec("ping -c 1 -W 1 " + ip); Thread.sleep(100); }
        catch (Exception ignored) {}
        return ThreatDetector.getMacFromArp(ip);
    }

    /** Testa portas abertas num IP */
    public interface PortCallback {
        void onPortOpen(int port, String service);
        void onComplete();
    }

    public static void scanPorts(String ip, PortCallback cb) {
        int[] ports = {21, 22, 23, 25, 53, 80, 110, 139, 143, 443,
                        445, 554, 8080, 8443, 8888, 3389, 9000};
        new Thread(() -> {
            for (int port : ports) {
                try {
                    java.net.Socket s = new java.net.Socket();
                    s.connect(new java.net.InetSocketAddress(ip, port), 300);
                    s.close();
                    cb.onPortOpen(port, serviceName(port));
                } catch (Exception ignored) {}
            }
            cb.onComplete();
        }).start();
    }

    public static String serviceName(int port) {
        switch (port) {
            case 21: return "FTP";    case 22:  return "SSH";
            case 23: return "Telnet"; case 25:  return "SMTP";
            case 53: return "DNS";    case 80:  return "HTTP";
            case 110: return "POP3";  case 139: return "NetBIOS";
            case 143: return "IMAP";  case 443: return "HTTPS";
            case 445: return "SMB";   case 554: return "RTSP";
            case 8080: return "HTTP-Alt"; case 8443: return "HTTPS-Alt";
            case 3389: return "RDP";  case 9000: return "SonarQube";
            default: return "porta " + port;
        }
    }
}
