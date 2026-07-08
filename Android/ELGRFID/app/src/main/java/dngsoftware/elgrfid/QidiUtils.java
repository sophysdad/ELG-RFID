package dngsoftware.elgrfid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class QidiUtils {

    public static final class MaterialEntry {
        public final int code;
        public final String name;

        MaterialEntry(int code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    public static final class ColorEntry {
        public final int code;
        public final String name;
        public final String rgb;

        ColorEntry(int code, String name, String rgb) {
            this.code = code;
            this.name = name;
            this.rgb = rgb;
        }
    }

    public static final MaterialEntry[] MATERIALS = {
            new MaterialEntry(1, "PLA"),
            new MaterialEntry(2, "PLA Matte"),
            new MaterialEntry(3, "PLA Metal"),
            new MaterialEntry(4, "PLA Silk"),
            new MaterialEntry(5, "PLA-CF"),
            new MaterialEntry(6, "PLA-Wood"),
            new MaterialEntry(7, "PLA Basic"),
            new MaterialEntry(8, "PLA Matte Basic"),
            new MaterialEntry(11, "ABS"),
            new MaterialEntry(12, "ABS-GF"),
            new MaterialEntry(13, "ABS-Metal"),
            new MaterialEntry(14, "ABS-Odorless"),
            new MaterialEntry(18, "ASA"),
            new MaterialEntry(19, "ASA-AERO"),
            new MaterialEntry(24, "UltraPA"),
            new MaterialEntry(25, "PA-CF"),
            new MaterialEntry(26, "UltraPA-CF25"),
            new MaterialEntry(27, "PA12-CF"),
            new MaterialEntry(30, "PAHT-CF"),
            new MaterialEntry(31, "PAHT-GF"),
            new MaterialEntry(32, "Support For PAHT"),
            new MaterialEntry(33, "Support For PET/PA"),
            new MaterialEntry(34, "PC/ABS-FR"),
            new MaterialEntry(37, "PET-CF"),
            new MaterialEntry(38, "PET-GF"),
            new MaterialEntry(39, "PETG Basic"),
            new MaterialEntry(40, "PETG Tough"),
            new MaterialEntry(41, "PETG Rapido"),
            new MaterialEntry(42, "PETG-CF"),
            new MaterialEntry(43, "PETG-GF"),
            new MaterialEntry(44, "PPS-CF"),
            new MaterialEntry(45, "PETG Translucent"),
            new MaterialEntry(47, "PVA"),
            new MaterialEntry(49, "TPU-Aero"),
            new MaterialEntry(50, "TPU"),
    };

    public static final ColorEntry[] COLORS = {
            new ColorEntry(1, "White", "#FAFAFA"),
            new ColorEntry(2, "Black", "#060606"),
            new ColorEntry(3, "Light Gray", "#D9E3ED"),
            new ColorEntry(4, "Lime Green", "#5CF30F"),
            new ColorEntry(5, "Mint", "#63E492"),
            new ColorEntry(6, "Blue", "#2850FF"),
            new ColorEntry(7, "Pink", "#FE98FE"),
            new ColorEntry(8, "Yellow", "#DFD628"),
            new ColorEntry(9, "Forest Green", "#228332"),
            new ColorEntry(10, "Light Blue", "#99DEFF"),
            new ColorEntry(11, "Dark Blue", "#1714B0"),
            new ColorEntry(12, "Lavender", "#CEC0FE"),
            new ColorEntry(13, "Yellow Green", "#CADE4B"),
            new ColorEntry(14, "Navy Blue", "#1353AB"),
            new ColorEntry(15, "Sky Blue", "#5EA9FD"),
            new ColorEntry(16, "Purple", "#A878FF"),
            new ColorEntry(17, "Light Red", "#FE717A"),
            new ColorEntry(18, "Red", "#FF362D"),
            new ColorEntry(19, "Beige", "#E2DFCD"),
            new ColorEntry(20, "Gray", "#898F9B"),
            new ColorEntry(21, "Brown", "#6E3812"),
            new ColorEntry(22, "Tan", "#CAC59F"),
            new ColorEntry(23, "Orange", "#F28636"),
            new ColorEntry(24, "Gold", "#B87F2B"),
    };

    private QidiUtils() {
    }

    @Nullable
    public static MaterialEntry getMaterial(int code) {
        for (MaterialEntry entry : MATERIALS) {
            if (entry.code == code) {
                return entry;
            }
        }
        return null;
    }

    @Nullable
    public static ColorEntry getColor(int code) {
        for (ColorEntry entry : COLORS) {
            if (entry.code == code) {
                return entry;
            }
        }
        return null;
    }

    @NonNull
    public static String getMaterialName(int code) {
        MaterialEntry entry = getMaterial(code);
        return entry == null ? "Unknown (" + code + ")" : entry.name;
    }

    @NonNull
    public static String getColorName(int code) {
        ColorEntry entry = getColor(code);
        return entry == null ? "Unknown (" + code + ")" : entry.name;
    }

    @NonNull
    public static String getColorRgb(int code) {
        ColorEntry entry = getColor(code);
        return entry == null ? "000000" : entry.rgb.replace("#", "");
    }

    public static int indexOfMaterial(int code) {
        for (int i = 0; i < MATERIALS.length; i++) {
            if (MATERIALS[i].code == code) {
                return i;
            }
        }
        return 0;
    }

    public static int indexOfColor(int code) {
        for (int i = 0; i < COLORS.length; i++) {
            if (COLORS[i].code == code) {
                return i;
            }
        }
        return 0;
    }
}