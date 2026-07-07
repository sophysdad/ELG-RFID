package dngsoftware.elgrfid;

import static dngsoftware.elgrfid.Utils.filamentTypes;
import static dngsoftware.elgrfid.Utils.getDefaultBedMax;
import static dngsoftware.elgrfid.Utils.getDefaultBedMin;
import static dngsoftware.elgrfid.Utils.getFilamentSubTypes;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ElegooUtils {

    public static final String PRESET_VENDOR = "ELG";

    private ElegooUtils() {
    }

    public static String[] getAllMaterials(FilamentDao db) {
        List<DbFilament> items = db.getAllItems();
        String[] materials = new String[items.size()];
        for (int i = 0; i < items.size(); i++) {
            materials[i] = items.get(i).filamentName;
        }
        return materials;
    }

    public static String[] parseParams(String filamentParam) {
        if (filamentParam == null || filamentParam.isEmpty()) {
            return new String[]{"PLA", "0", "190", "230", "50", "60"};
        }
        return filamentParam.split("\\|", -1);
    }

    public static int[] getTemps(FilamentDao db, String materialName) {
        DbFilament item = db.getFilamentByName(materialName);
        String[] parts = parseParams(item != null ? item.filamentParam : null);
        int[] temps = new int[4];
        for (int i = 0; i < 4; i++) {
            try {
                temps[i] = Integer.parseInt(parts[i + 2].trim());
            } catch (Exception ignored) {
                temps[i] = new int[]{190, 230, 50, 60}[i];
            }
        }
        return temps;
    }

    public static String getMaterialType(FilamentDao db, String materialName) {
        DbFilament item = db.getFilamentByName(materialName);
        return parseParams(item != null ? item.filamentParam : null)[0];
    }

    public static int getSubtypeId(FilamentDao db, String materialName) {
        DbFilament item = db.getFilamentByName(materialName);
        try {
            return Integer.parseInt(parseParams(item != null ? item.filamentParam : null)[1].trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    public static String buildParams(String type, int subtypeId, int extMin, int extMax,
                                     int bedMin, int bedMax) {
        return String.format(Locale.US, "%s|%d|%d|%d|%d|%d",
                type, subtypeId, extMin, extMax, bedMin, bedMax);
    }

    public static DbFilament findByTypeAndSubtype(FilamentDao db, String type, int subtypeId) {
        for (DbFilament item : db.getAllItems()) {
            String[] parts = parseParams(item.filamentParam);
            if (parts.length >= 2 && type.equals(parts[0])) {
                try {
                    if (Integer.parseInt(parts[1].trim()) == subtypeId) {
                        return item;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    public static int nextCustomSubtypeId(FilamentDao db) {
        int maxId = 99;
        for (DbFilament item : db.getAllItems()) {
            try {
                maxId = Math.max(maxId, Integer.parseInt(parseParams(item.filamentParam)[1].trim()));
            } catch (Exception ignored) {
            }
        }
        return maxId + 1;
    }

    public static boolean isPreset(DbFilament item) {
        return item != null && PRESET_VENDOR.equalsIgnoreCase(item.filamentVendor);
    }

    public static void populateDatabase(FilamentDao db) {
        int position = 0;
        for (String type : filamentTypes) {
            List<Filament> subtypes = getFilamentSubTypes(type);
            int bedMin = getDefaultBedMin(type);
            int bedMax = getDefaultBedMax(type);
            for (Filament subtype : subtypes) {
                addPreset(db, position++, type, subtype.id, subtype.name,
                        subtype.minTemp, subtype.maxTemp, bedMin, bedMax);
            }
        }
    }

    private static void addPreset(FilamentDao db, int position, String type, int subtypeId,
                                  String name, int extMin, int extMax, int bedMin, int bedMax) {
        DbFilament filament = new DbFilament();
        filament.position = position;
        filament.filamentID = String.valueOf(subtypeId);
        filament.filamentName = name;
        filament.filamentVendor = PRESET_VENDOR;
        filament.filamentParam = buildParams(type, subtypeId, extMin, extMax, bedMin, bedMax);
        db.addItem(filament);
    }

    public static int typeToIndex(String type) {
        int index = Arrays.asList(filamentTypes).indexOf(type);
        return index >= 0 ? index : 0;
    }
}