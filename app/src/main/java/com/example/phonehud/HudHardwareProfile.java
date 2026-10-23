package com.example.phonehud;

import java.util.Locale;

/** Pure Java for unit testing old-LG device identification and OLED brightness. */
public final class HudHardwareProfile {
    private HudHardwareProfile() { }
    public static boolean isV30(String manufacturer, String model, String device) {
        String make = manufacturer == null ? "" : manufacturer.toUpperCase(Locale.ROOT);
        String name = model == null ? "" : model.toUpperCase(Locale.ROOT);
        String codename = device == null ? "" : device.toUpperCase(Locale.ROOT);
        boolean lg = make.equals("LGE") || make.equals("LG ELECTRONICS") || make.equals("LG");
        return lg && (name.contains("V30") || codename.contains("JOAN")
                || name.startsWith("LG-V300") || name.startsWith("LGM-V300")
                || name.startsWith("LM-V300"));
    }
    public static float brightness(boolean oled, int level) {
        int safe = Math.max(0, Math.min(2, level));
        if (oled) return safe == 0 ? 0.16f : safe == 2 ? 0.76f : 0.45f;
        return safe == 0 ? 0.22f : safe == 2 ? 1.0f : 0.72f;
    }
}
