package dngsoftware.elgrfid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class FilamentCatalog {

    private FilamentCatalog() {
    }

    public static String displayVendor(String vendor, PrinterBrand brand) {
        if (vendor == null || vendor.trim().isEmpty()) {
            return brand == PrinterBrand.ELEGOO ? "ELEGOO" : "Custom";
        }
        if ("ELG".equalsIgnoreCase(vendor)) {
            return "ELEGOO";
        }
        if ("AC".equalsIgnoreCase(vendor)) {
            return "Anycubic";
        }
        return vendor.trim();
    }

    public static String storageVendor(String displayVendor, PrinterBrand brand) {
        if (displayVendor != null
                && ("ELEGOO".equalsIgnoreCase(displayVendor)
                || "Elegoo".equalsIgnoreCase(displayVendor))) {
            return ElegooUtils.PRESET_VENDOR;
        }
        if ("Anycubic".equalsIgnoreCase(displayVendor)) {
            return AnycubicUtils.PRESET_VENDOR;
        }
        return displayVendor == null ? "" : displayVendor.trim();
    }

    public static String getDefaultVendor(PrinterBrand brand) {
        return brand == PrinterBrand.ELEGOO ? "ELEGOO" : "Anycubic";
    }

    public static boolean isPresetVendor(String displayVendor, PrinterBrand brand) {
        if (displayVendor == null) {
            return false;
        }
        if (brand == PrinterBrand.ELEGOO) {
            return "ELEGOO".equalsIgnoreCase(displayVendor)
                    || "Elegoo".equalsIgnoreCase(displayVendor);
        }
        return "Anycubic".equalsIgnoreCase(displayVendor);
    }

    public static String[] parseParams(String filamentParam) {
        if (filamentParam == null || filamentParam.isEmpty()) {
            return new String[]{"PLA", "0", "190", "230", "50", "60"};
        }
        return filamentParam.split("\\|", -1);
    }

    public static String getType(DbFilament item) {
        return parseParams(item.filamentParam)[0];
    }

    public static String getSubtypeKey(DbFilament item) {
        return parseParams(item.filamentParam)[1];
    }

    public static String getSubtypeLabel(DbFilament item, PrinterBrand brand) {
        if (item.filamentName != null && !item.filamentName.trim().isEmpty()) {
            return item.filamentName.trim();
        }
        String subtype = getSubtypeKey(item);
        if (subtype != null && !subtype.isEmpty()) {
            return subtype;
        }
        return "";
    }

    public static int[] getTemps(DbFilament item) {
        String[] parts = parseParams(item.filamentParam);
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

    public static List<String> getVendors(FilamentDao db, PrinterBrand brand) {
        List<String> vendors = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (String vendor : AnycubicUtils.FILAMENT_VENDORS) {
            if (seen.add(vendor)) {
                vendors.add(vendor);
            }
        }

        List<String> customVendors = new ArrayList<>();
        for (DbFilament item : db.getAllItems()) {
            String vendor = displayVendor(item.filamentVendor, brand);
            if (vendor == null || vendor.isEmpty() || "Custom".equalsIgnoreCase(vendor)) {
                continue;
            }
            if (!seen.contains(vendor)) {
                customVendors.add(vendor);
                seen.add(vendor);
            }
        }
        Collections.sort(customVendors, String::compareToIgnoreCase);
        vendors.addAll(customVendors);
        return vendors;
    }

    public static List<String> getTypes(FilamentDao db, PrinterBrand brand, String displayVendor) {
        String vendorKey = storageVendor(displayVendor, brand);
        Set<String> types = new LinkedHashSet<>();
        for (DbFilament item : db.getAllItems()) {
            if (matchesVendor(item, brand, displayVendor, vendorKey)) {
                types.add(getType(item));
            }
        }
        if (types.isEmpty()) {
            if (brand == PrinterBrand.ANYCUBIC) {
                Collections.addAll(types, AnycubicUtils.FILAMENT_TYPES);
            } else {
                Collections.addAll(types, Utils.filamentTypes);
            }
        }
        List<String> sorted = new ArrayList<>(types);
        Collections.sort(sorted, String::compareToIgnoreCase);
        return sorted;
    }

    public static List<DbFilament> getSubtypes(FilamentDao db, PrinterBrand brand,
                                               String displayVendor, String type) {
        String vendorKey = storageVendor(displayVendor, brand);
        List<DbFilament> matches = new ArrayList<>();
        for (DbFilament item : db.getAllItems()) {
            if (matchesVendor(item, brand, displayVendor, vendorKey)
                    && typeMatches(type, item)) {
                matches.add(item);
            }
        }
        if (matches.isEmpty()) {
            matches.addAll(buildFallbackSubtypes(db, brand, displayVendor, type));
        }
        matches.sort((a, b) -> getSubtypeLabel(a, brand)
                .compareToIgnoreCase(getSubtypeLabel(b, brand)));
        return matches;
    }

    public static DbFilament findSubtype(FilamentDao db, PrinterBrand brand,
                                         String displayVendor, String type, String subtypeLabel) {
        if (subtypeLabel == null) {
            return null;
        }
        for (DbFilament item : getSubtypes(db, brand, displayVendor, type)) {
            if (subtypeLabel.equalsIgnoreCase(getSubtypeLabel(item, brand))) {
                return item;
            }
        }
        return null;
    }

    private static boolean typeMatches(String selectedType, DbFilament item) {
        return selectedType != null && selectedType.equalsIgnoreCase(getType(item));
    }

    private static List<DbFilament> buildFallbackSubtypes(FilamentDao db, PrinterBrand brand,
                                                          String displayVendor, String type) {
        List<DbFilament> fallback = new ArrayList<>();
        String vendorCode = vendorCodeForSave(displayVendor, brand);
        Set<String> seen = new LinkedHashSet<>();

        if (brand == PrinterBrand.ANYCUBIC) {
            for (DbFilament preset : db.getAllItems()) {
                if (AnycubicUtils.isPreset(preset) && typeMatches(type, preset)) {
                    String label = getSubtypeLabel(preset, brand);
                    if (seen.add(label)) {
                        fallback.add(cloneForVendor(preset, vendorCode));
                    }
                }
            }
        }

        if (fallback.isEmpty()) {
            int bedMin = Utils.getDefaultBedMin(type);
            int bedMax = Utils.getDefaultBedMax(type);
            for (Filament subtype : Utils.getFilamentSubTypes(type)) {
                String label = subtype.name;
                if (seen.add(label)) {
                    fallback.add(syntheticSubtype(brand, vendorCode, type, subtype, bedMin, bedMax));
                }
            }
        }

        if (fallback.isEmpty()) {
            int[] temps = brand == PrinterBrand.ANYCUBIC
                    ? AnycubicUtils.getDefaultTemps(type)
                    : defaultCatalogTemps(type);
            fallback.add(syntheticNamedSubtype(brand, vendorCode, type, type, temps));
        }
        return fallback;
    }

    private static DbFilament cloneForVendor(DbFilament source, String vendorCode) {
        DbFilament copy = new DbFilament();
        copy.position = source.position;
        copy.filamentID = source.filamentID;
        copy.filamentName = source.filamentName;
        copy.filamentVendor = vendorCode;
        copy.filamentParam = source.filamentParam;
        return copy;
    }

    private static DbFilament syntheticSubtype(PrinterBrand brand, String vendorCode, String type,
                                               Filament subtype, int bedMin, int bedMax) {
        DbFilament item = new DbFilament();
        item.filamentName = subtype.name;
        item.filamentVendor = vendorCode;
        if (brand == PrinterBrand.ELEGOO) {
            item.filamentParam = ElegooUtils.buildParams(type, subtype.id, subtype.minTemp,
                    subtype.maxTemp, bedMin, bedMax);
            item.filamentID = String.valueOf(subtype.id);
        } else {
            item.filamentParam = buildParams(type, String.valueOf(subtype.id), subtype.minTemp,
                    subtype.maxTemp, bedMin, bedMax);
            item.filamentID = "";
        }
        return item;
    }

    private static DbFilament syntheticNamedSubtype(PrinterBrand brand, String vendorCode,
                                                    String type, String name, int[] temps) {
        DbFilament item = new DbFilament();
        item.filamentName = name;
        item.filamentVendor = vendorCode;
        if (brand == PrinterBrand.ELEGOO) {
            item.filamentParam = ElegooUtils.buildParams(type, 0, temps[0], temps[1],
                    temps[2], temps[3]);
            item.filamentID = "0";
        } else {
            item.filamentParam = buildParams(type, type, temps[0], temps[1], temps[2], temps[3]);
            item.filamentID = "";
        }
        return item;
    }

    private static int[] defaultCatalogTemps(String type) {
        if (!Utils.getFilamentSubTypes(type).isEmpty()) {
            Filament first = Utils.getFilamentSubTypes(type).get(0);
            int bedMin = Utils.getDefaultBedMin(type);
            int bedMax = Utils.getDefaultBedMax(type);
            return new int[]{first.minTemp, first.maxTemp, bedMin, bedMax};
        }
        return new int[]{190, 230, 50, 60};
    }

    private static boolean matchesVendor(DbFilament item, PrinterBrand brand,
                                         String displayVendor, String vendorKey) {
        if (displayVendor == null || displayVendor.isEmpty()) {
            return false;
        }
        String itemDisplay = displayVendor(item.filamentVendor, brand);
        if (displayVendor.equalsIgnoreCase(itemDisplay)) {
            return true;
        }
        if (vendorKey != null && !vendorKey.isEmpty()
                && vendorKey.equalsIgnoreCase(item.filamentVendor)) {
            return true;
        }
        if (displayVendor.equalsIgnoreCase(item.filamentVendor)) {
            return true;
        }
        if (vendorKey == null || vendorKey.isEmpty()) {
            return item.filamentVendor == null || item.filamentVendor.trim().isEmpty();
        }
        return false;
    }

    public static String vendorCodeForSave(String displayVendor, PrinterBrand brand) {
        if (isPresetVendor(displayVendor, brand)) {
            return storageVendor(displayVendor, brand);
        }
        return displayVendor == null ? "" : displayVendor.trim();
    }

    public static String buildParams(String type, String subtypeKey, int extMin, int extMax,
                                     int bedMin, int bedMax) {
        return String.format(Locale.US, "%s|%s|%d|%d|%d|%d",
                type, subtypeKey, extMin, extMax, bedMin, bedMax);
    }
}