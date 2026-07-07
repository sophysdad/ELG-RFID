package dngsoftware.elgrfid;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public final class AnycubicUtils {

    public static final String PRESET_VENDOR = "AC";

    private AnycubicUtils() {
    }

    public static int getMaterialLength(String materialWeight) {
        switch (materialWeight) {
            case "750 G": return 247;
            case "600 G": return 198;
            case "500 G": return 165;
            case "250 G": return 82;
            default: return 330;
        }
    }

    public static String getMaterialWeightLabel(int materialLength) {
        switch (materialLength) {
            case 247: return "750 G";
            case 198: return "600 G";
            case 165: return "500 G";
            case 82: return "250 G";
            default: return "1 KG";
        }
    }

    public static String[] getAllMaterials(FilamentDao db) {
        List<DbFilament> items = db.getAllItems();
        String[] materials = new String[items.size()];
        for (int i = 0; i < items.size(); i++) {
            materials[i] = items.get(i).filamentName;
        }
        return materials;
    }

    public static int[] getTemps(FilamentDao db, String materialName) {
        DbFilament item = db.getFilamentByName(materialName);
        if (item == null) {
            return new int[]{200, 210, 50, 60};
        }
        return FilamentCatalog.getTemps(item);
    }

    public static boolean isPreset(DbFilament item) {
        return item != null && PRESET_VENDOR.equalsIgnoreCase(item.filamentVendor);
    }

    public static boolean needsStructuredParams(FilamentDao db) {
        for (DbFilament item : db.getAllItems()) {
            if (item.filamentParam == null || item.filamentParam.split("\\|", -1).length < 6) {
                return true;
            }
        }
        return false;
    }

    public static void resetDatabase(FilamentDao db) {
        for (DbFilament item : db.getAllItems()) {
            db.deleteItem(item);
        }
        populateDatabase(db);
    }

    public static String[] parseTypeSubtype(String name) {
        java.util.List<String> sortedTypes = new java.util.ArrayList<>(java.util.Arrays.asList(FILAMENT_TYPES));
        sortedTypes.sort((a, b) -> Integer.compare(b.length(), a.length()));
        for (String type : sortedTypes) {
            if (name.equals(type)) {
                return new String[]{type, type};
            }
            if (name.startsWith(type + " ")) {
                return new String[]{type, name.substring(type.length() + 1)};
            }
        }
        return new String[]{name, name};
    }

    public static byte[] getSku(FilamentDao db, String materialName) {
        byte[] skuData = new byte[20];
        Arrays.fill(skuData, (byte) 0);
        DbFilament item = db.getFilamentByName(materialName);
        if (item != null && item.filamentID != null && !item.filamentID.isEmpty()) {
            System.arraycopy(item.filamentID.getBytes(StandardCharsets.UTF_8), 0, skuData, 0,
                    item.filamentID.getBytes(StandardCharsets.UTF_8).length);
        }
        return skuData;
    }

    public static byte[] getBrand(FilamentDao db, String materialName) {
        byte[] brandData = new byte[20];
        Arrays.fill(brandData, (byte) 0);
        DbFilament item = db.getFilamentByName(materialName);
        if (item != null && item.filamentVendor != null && !item.filamentVendor.isEmpty()) {
            System.arraycopy(item.filamentVendor.getBytes(StandardCharsets.UTF_8), 0, brandData, 0,
                    item.filamentVendor.getBytes(StandardCharsets.UTF_8).length);
        }
        return brandData;
    }

    public static byte[] numToBytes(int value) {
        return revArray(new byte[]{(byte) (value >> 8), (byte) value});
    }

    public static int parseNumber(byte[] byteArray) {
        int result = 0;
        for (byte b : revArray(byteArray)) {
            result = (result << 8) | (b & 0xFF);
        }
        return result;
    }

    public static byte[] revArray(byte[] array) {
        if (array == null || array.length <= 1) {
            return array;
        }
        int left = 0;
        int right = array.length - 1;
        while (left < right) {
            byte temp = array[left];
            array[left] = array[right];
            array[right] = temp;
            left++;
            right--;
        }
        return array;
    }

    public static byte[] parseColorHex(final String hexString) {
        int length = hexString.length();
        byte[] byteArray = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            byteArray[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return revArray(byteArray);
    }

    public static String parseColorBytes(byte[] byteArray) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : revArray(byteArray)) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    public static byte[] combineArrays(byte[] array1, byte[] array2) {
        byte[] combined = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, combined, 0, array1.length);
        System.arraycopy(array2, 0, combined, array1.length, array2.length);
        return combined;
    }

    public static String rgbToHexA(int r, int g, int b, int a) {
        return String.format("%02X%02X%02X%02X", a, r, g, b);
    }

    public static void populateDatabase(FilamentDao db) {
        addPreset(db, 0, "SHABBK-102", "ABS", "220|250|90|100");
        addPreset(db, -1, "", "ASA", "240|280|90|100");
        addPreset(db, -1, "", "PC", "260|300|100|110");
        addPreset(db, -1, "", "PEBA", "225|255|45|90");
        addPreset(db, -1, "", "PETG", "230|250|70|90");
        addPreset(db, -1, "", "PETG-CF", "240|270|65|75");
        addPreset(db, -1, "AHPLBK-101", "PLA", "190|230|50|60");
        addPreset(db, -1, "", "PLA Galaxy", "190|230|50|60");
        addPreset(db, -1, "", "PLA Glow", "190|230|50|60");
        addPreset(db, -1, "AHHSBK-103", "PLA High Speed", "190|230|50|60");
        addPreset(db, -1, "", "PLA Marble", "200|230|50|60");
        addPreset(db, -1, "HYGBK-102", "PLA Matte", "190|230|55|65");
        addPreset(db, -1, "", "PLA Metal", "190|230|35|60");
        addPreset(db, -1, "", "PLA SE", "190|230|55|65");
        addPreset(db, -1, "AHSCWH-102", "PLA Silk", "200|230|55|65");
        addPreset(db, -1, "AHPLPBK-102", "PLA+", "205|215|50|60");
        addPreset(db, -1, "", "PLA-CF", "210|240|45|65");
        addPreset(db, -1, "STPBK-101", "TPU", "210|230|25|60");
    }

    private static void addPreset(FilamentDao db, int position, String sku, String name,
                                  String tempCsv) {
        String[] temps = tempCsv.split("\\|");
        int extMin = Integer.parseInt(temps[0]);
        int extMax = Integer.parseInt(temps[1]);
        int bedMin = Integer.parseInt(temps[2]);
        int bedMax = Integer.parseInt(temps[3]);
        String[] typeSubtype = parseTypeSubtype(name);
        DbFilament filament = new DbFilament();
        filament.position = position >= 0 ? position : db.getItemCount();
        filament.filamentID = sku;
        filament.filamentName = name;
        filament.filamentVendor = PRESET_VENDOR;
        filament.filamentParam = FilamentCatalog.buildParams(
                typeSubtype[0], typeSubtype[1], extMin, extMax, bedMin, bedMax);
        db.addItem(filament);
    }

    public static final String[] FILAMENT_VENDORS = {
            "3Dgenius", "3DJake", "3DXTECH", "3D BEST-Q", "3D Hero", "3D-Fuel", "Aceaddity",
            "AddNorth", "Amazon Basics", "AMOLEN", "Ankermake", "Anycubic", "Atomic", "AzureFilm",
            "BASF", "Bblife", "BCN3D", "Beyond Plastic", "California Filament", "Capricorn", "CC3D",
            "colorFabb", "Comgrow", "Cookiecad", "Creality", "CERPRiSE", "Das Filament", "DO3D",
            "DOW", "DSM", "Duramic", "ELEGOO", "Eryone", "Essentium", "eSUN", "Extrudr", "Fiberforce",
            "Fiberlogy", "FilaCube", "Filamentive", "Fillamentum", "FLASHFORGE", "Formfutura",
            "Francofil", "FilamentOne", "Fil X", "GEEETECH", "Giantarm", "Gizmo Dorks", "GreenGate3D",
            "HATCHBOX", "Hello3D", "IC3D", "IEMAI", "IIID Max", "INLAND", "iProspect", "iSANMATE",
            "Justmaker", "Keene Village Plastics", "Kexcelled", "LDO", "MakerBot", "MatterHackers",
            "MIKA3D", "NinjaTek", "Nobufil", "Novamaker", "OVERTURE", "OVVNYXE", "Polymaker", "Priline",
            "Printed Solid", "Protopasta", "Prusament", "Push Plastic", "R3D", "Re-pet3D", "Recreus",
            "Regen", "Sain SMART", "SliceWorx", "Snapmaker", "SnoLabs", "Spectrum", "SUNLU", "TTYT3D",
            "Tianse", "UltiMaker", "Valment", "Verbatim", "VO3D", "Voxelab", "VOXELPLA", "YOOPAI",
            "Yousu", "Ziro", "Zyltech"
    };

    public static final String[] FILAMENT_TYPES = {
            "ABS", "ASA", "HIPS", "PA", "PA-CF", "PC", "PLA", "PLA-CF", "PVA", "PP", "TPU", "PETG",
            "BVOH", "PET-CF", "PETG-CF", "PA6-CF", "PAHT-CF", "PPS", "PPS-CF", "PET", "ASA-CF",
            "PA-GF", "PETG-GF", "PP-CF", "PCTG", "PEBA", "PBT"
    };

    public static int[] getDefaultTemps(String materialType) {
        switch (materialType) {
            case "ABS": return new int[]{220, 250, 90, 100};
            case "ASA": return new int[]{240, 280, 90, 100};
            case "HIPS": return new int[]{230, 245, 80, 100};
            case "PA": return new int[]{220, 250, 70, 90};
            case "PA-CF": return new int[]{260, 280, 80, 100};
            case "PC": return new int[]{260, 300, 100, 110};
            case "PETG": return new int[]{230, 250, 70, 90};
            case "PLA": return new int[]{190, 230, 50, 60};
            case "PLA-CF": return new int[]{210, 240, 45, 65};
            case "PVA": return new int[]{215, 225, 45, 60};
            case "PP": return new int[]{225, 245, 80, 105};
            case "TPU": return new int[]{210, 230, 25, 60};
            case "BVOH": return new int[]{200, 220, 55, 65};
            case "PETG-CF": return new int[]{240, 270, 65, 75};
            case "PEBA": return new int[]{225, 255, 45, 90};
            default: return new int[]{190, 230, 50, 60};
        }
    }
}