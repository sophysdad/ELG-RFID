package dngsoftware.elgrfid;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * OpenSpool NDEF JSON format — https://openspool.io/rfid.html
 */
public final class OpenSpoolCodec implements TagCodec {

    public static final String MIME_TYPE = "application/json";
    public static final String PROTOCOL = "openspool";

    @NonNull
    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    @NonNull
    @Override
    public FilamentTagData decode(@NonNull byte[] payload) throws Exception {
        JSONObject json = new JSONObject(new String(payload, StandardCharsets.UTF_8));
        if (!PROTOCOL.equalsIgnoreCase(json.optString("protocol", ""))) {
            throw new IllegalArgumentException("Not an OpenSpool tag");
        }
        FilamentTagData data = new FilamentTagData();
        data.brand = json.optString("brand", "Generic");
        data.materialType = json.optString("type", "PLA");
        data.materialModifier = json.optString("subtype", "");
        data.colorHex = normalizeColor(json.optString("color_hex", "FF0000"));
        data.nozzleMin = parseInt(json.optString("min_temp", "190"), 190);
        data.nozzleMax = parseInt(json.optString("max_temp", "230"), 230);
        data.bedMin = parseInt(json.optString("bed_min_temp", "0"), 0);
        data.bedMax = parseInt(json.optString("bed_max_temp", "60"), 60);
        if (json.has("diameter")) {
            double diameter = json.optDouble("diameter", 1.75d);
            data.diameterMicrons = (int) Math.round(diameter * 1000);
        }
        if (json.has("weight")) {
            data.weightGrams = json.optInt("weight", data.weightGrams);
        }
        return data;
    }

    @NonNull
    @Override
    public byte[] encode(@NonNull FilamentTagData data, int ntagType) throws Exception {
        JSONObject json = new JSONObject();
        json.put("protocol", PROTOCOL);
        json.put("version", "1.0");
        json.put("type", data.materialType);
        json.put("color_hex", stripHash(data.colorHex));
        json.put("brand", data.brand == null || data.brand.isEmpty() ? "Generic" : data.brand);
        json.put("min_temp", String.valueOf(data.nozzleMin));
        json.put("max_temp", String.valueOf(data.nozzleMax));
        if (data.materialModifier != null && !data.materialModifier.trim().isEmpty()) {
            json.put("subtype", data.materialModifier.trim());
        }
        if (data.bedMin > 0 || data.bedMax > 0) {
            json.put("bed_min_temp", String.valueOf(data.bedMin));
            json.put("bed_max_temp", String.valueOf(data.bedMax));
        }
        json.put("diameter", data.diameterMicrons / 1000.0d);
        json.put("weight", data.weightGrams);
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }

    static boolean isOpenSpool(@NonNull JSONObject json) {
        return PROTOCOL.equalsIgnoreCase(json.optString("protocol", ""));
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    @NonNull
    static String normalizeColor(String color) {
        String hex = stripHash(color);
        if (hex.length() == 6) {
            return hex.toUpperCase(Locale.US);
        }
        if (hex.length() == 8) {
            return hex.substring(2).toUpperCase(Locale.US);
        }
        return "FF0000";
    }

    @NonNull
    static String stripHash(String color) {
        if (color == null) {
            return "FF0000";
        }
        return color.replace("#", "").trim();
    }
}