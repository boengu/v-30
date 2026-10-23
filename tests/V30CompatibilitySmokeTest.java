import com.example.phonehud.HudHardwareProfile;

public class V30CompatibilitySmokeTest {
    private static int passed;
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError("FAIL: " + label);
        passed++;
    }
    public static void main(String[] args) {
        check(HudHardwareProfile.isV30("LGE", "LG-V300S", "joan"), "Korean V30");
        check(HudHardwareProfile.isV30("LGE", "LG-H930", "joan"), "International V30");
        check(HudHardwareProfile.isV30("LGE", "LG V30", "anything"), "Marketing model");
        check(!HudHardwareProfile.isV30("SAMSUNG", "LG-V300", "joan"), "Wrong manufacturer");
        check(!HudHardwareProfile.isV30("LGE", "LG-V350", "judy"), "V35 not V30");
        check(HudHardwareProfile.brightness(true, 0) == .16f, "OLED default low");
        check(HudHardwareProfile.brightness(true, 2) < .8f, "OLED max moderated");
        check(HudHardwareProfile.brightness(false, 2) == 1f, "NonOLED max");
        check(HudHardwareProfile.brightness(true, -42) == .16f, "Clamped low");
        check(HudHardwareProfile.brightness(false, 20) == 1f, "Clamped high");
        System.out.println("V30_COMPAT_PASS " + passed);
    }
}
