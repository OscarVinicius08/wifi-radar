package com.wifiradar.app;

import java.util.HashMap;
import java.util.Map;

/** Base offline de OUI (primeiros 3 octetos do MAC → fabricante). */
public class OuiDatabase {

    private static final Map<String, String> DB = new HashMap<>();

    static {
        // Roteadores / APs comuns no Brasil
        DB.put("00:1A:2B", "Cisco"); DB.put("00:1B:8F", "Cisco");
        DB.put("F8:72:EA", "TP-Link"); DB.put("EC:08:6B", "TP-Link");
        DB.put("50:C7:BF", "TP-Link"); DB.put("54:AF:97", "TP-Link");
        DB.put("C8:D7:19", "TP-Link"); DB.put("00:27:19", "TP-Link");
        DB.put("B0:BE:76", "TP-Link"); DB.put("98:DA:C4", "TP-Link");
        DB.put("A0:F3:C1", "TP-Link"); DB.put("30:DE:4B", "TP-Link");

        DB.put("00:18:E7", "D-Link"); DB.put("1C:7E:E5", "D-Link");
        DB.put("78:54:2E", "D-Link"); DB.put("B0:C5:54", "D-Link");
        DB.put("28:10:7B", "D-Link"); DB.put("C8:BE:19", "D-Link");

        DB.put("00:90:4C", "Asus");  DB.put("04:92:26", "Asus");
        DB.put("10:BF:48", "Asus");  DB.put("2C:FD:A1", "Asus");
        DB.put("50:46:5D", "Asus");  DB.put("74:D0:2B", "Asus");
        DB.put("AC:22:0B", "Asus");

        DB.put("00:26:B8", "Netgear"); DB.put("20:4E:7F", "Netgear");
        DB.put("2C:B0:5D", "Netgear"); DB.put("A0:40:A0", "Netgear");
        DB.put("C0:FF:D4", "Netgear"); DB.put("9C:D3:6D", "Netgear");

        DB.put("00:17:C5", "Huawei"); DB.put("04:C0:6F", "Huawei");
        DB.put("28:6E:D4", "Huawei"); DB.put("54:89:98", "Huawei");
        DB.put("70:72:3C", "Huawei"); DB.put("AC:E8:7B", "Huawei");
        DB.put("E8:CD:2D", "Huawei"); DB.put("48:46:FB", "Huawei");

        DB.put("00:1D:AA", "D-Link"); DB.put("14:D6:4D", "Motorola");
        DB.put("00:24:A5", "Buffalo"); DB.put("10:6F:3F", "Buffalo");

        DB.put("58:6D:8F", "Intelbras"); DB.put("C8:D7:79", "Intelbras");
        DB.put("00:08:A1", "Intelbras"); DB.put("EC:F0:0E", "Intelbras");

        DB.put("00:26:44", "ZTE"); DB.put("20:F4:1B", "ZTE");
        DB.put("2C:26:C5", "ZTE"); DB.put("58:2A:F7", "ZTE");
        DB.put("68:89:C1", "ZTE"); DB.put("A4:39:B3", "ZTE");
        DB.put("BC:F1:71", "ZTE");

        // Celulares e dispositivos
        DB.put("F8:A9:D0", "Apple");  DB.put("AC:CF:85", "Apple");
        DB.put("00:17:F2", "Apple");  DB.put("04:52:F3", "Apple");
        DB.put("3C:22:FB", "Apple");  DB.put("70:70:0D", "Apple");

        DB.put("00:15:5D", "Microsoft"); DB.put("28:18:78", "Microsoft");

        DB.put("3C:5A:B4", "Google"); DB.put("F4:F5:D8", "Google");
        DB.put("54:60:09", "Google"); DB.put("94:95:A0", "Google");

        DB.put("00:1A:11", "Google Nest"); DB.put("D4:F5:47", "Google Nest");

        DB.put("8C:85:90", "Samsung"); DB.put("CC:07:AB", "Samsung");
        DB.put("F4:42:8F", "Samsung"); DB.put("00:12:47", "Samsung");
        DB.put("50:01:BB", "Samsung"); DB.put("78:1F:DB", "Samsung");

        DB.put("34:97:F6", "Xiaomi");  DB.put("58:44:98", "Xiaomi");
        DB.put("64:09:80", "Xiaomi");  DB.put("F8:A4:5F", "Xiaomi");
        DB.put("AC:F7:F3", "Xiaomi");  DB.put("00:9E:C8", "Xiaomi");

        DB.put("00:BB:3A", "Amazon"); DB.put("40:B4:CD", "Amazon");
        DB.put("74:C2:46", "Amazon"); DB.put("FC:A6:67", "Amazon");

        DB.put("08:00:27", "VirtualBox"); DB.put("00:50:56", "VMware");
        DB.put("00:0C:29", "VMware");

        DB.put("B8:27:EB", "Raspberry Pi"); DB.put("DC:A6:32", "Raspberry Pi");
        DB.put("E4:5F:01", "Raspberry Pi");
    }

    public static String lookup(String mac) {
        if (mac == null || mac.length() < 8) return "Desconhecido";
        String oui = mac.substring(0, 8).toUpperCase().replace("-", ":");
        String vendor = DB.get(oui);
        return vendor != null ? vendor : "Desconhecido (" + oui + ")";
    }
}
