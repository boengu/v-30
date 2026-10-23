package com.example.phonehud;

/** Only locally-routed private IPv4 addresses may be configured as the HUD sender. */
public final class LanAddress {
    private LanAddress() { }
    public static boolean allowed(String ip) {
        if (ip == null || ip.length() < 7 || ip.length() > 15) return false;
        String[] parts = ip.split("\\.", -1);
        if (parts.length != 4) return false;
        int[] octet = new int[4];
        for (int i = 0; i < 4; i++) {
            String p = parts[i];
            if (p.isEmpty() || p.length() > 3) return false;
            for (int j = 0; j < p.length(); j++) if (p.charAt(j) < '0' || p.charAt(j) > '9') return false;
            // Leading zeroes are confusing when copying a network address.
            if (p.length() > 1 && p.charAt(0) == '0') return false;
            try { octet[i] = Integer.parseInt(p); } catch (NumberFormatException e) { return false; }
            if (octet[i] > 255) return false;
        }
        int a = octet[0], b = octet[1], c = octet[2], d = octet[3];
        boolean privateNet = a == 10 || a == 172 && b >= 16 && b <= 31
                || a == 192 && b == 168 || a == 100 && b >= 64 && b <= 127;
        // Avoid broadcast/network addresses in the usual /24 mobile hotspot setup.
        return privateNet && d != 0 && d != 255 && !(a == 10 && b == 255 && c == 255);
    }
}
