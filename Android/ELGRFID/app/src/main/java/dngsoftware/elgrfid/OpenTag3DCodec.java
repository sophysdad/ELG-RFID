package dngsoftware.elgrfid;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * OpenTag3D core memory map — https://opentag3d.info/spec.json
 */
public final class OpenTag3DCodec implements TagCodec {

    public static final String MIME_TYPE = "application/opentag3d";
    private static final int CORE_SIZE = 0x70;
    private static final int EXTENDED_SIZE = 0xBB;
    private static final int TAG_VERSION = 1000;

    @NonNull
    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    @NonNull
    @Override
    public FilamentTagData decode(@NonNull byte[] payload) throws Exception {
        if (payload.length < CORE_SIZE) {
            throw new IllegalArgumentException("OpenTag3D payload too small");
        }
        FilamentTagData data = new FilamentTagData();
        data.materialType = readUtf8(payload, 0x02, 5).trim();
        data.materialModifier = readUtf8(payload, 0x07, 5).trim();
        data.brand = readUtf8(payload, 0x1B, 16).trim();
        data.colorName = readUtf8(payload, 0x2B, 32).trim();
        int r = payload[0x4B] & 0xFF;
        int g = payload[0x4C] & 0xFF;
        int b = payload[0x4D] & 0xFF;
        data.colorHex = String.format(Locale.US, "%02X%02X%02X", r, g, b);
        data.diameterMicrons = readUInt16(payload, 0x5C);
        data.weightGrams = readUInt16(payload, 0x5E);
        int printTemp = (payload[0x60] & 0xFF) * 5;
        int bedTemp = (payload[0x61] & 0xFF) * 5;
        data.nozzleMax = printTemp;
        data.nozzleMin = Math.max(0, printTemp - 20);
        data.bedMax = bedTemp;
        data.bedMin = Math.max(0, bedTemp - 10);
        data.densityMilliGramsPerCc = readUInt16(payload, 0x62);

        if (payload.length >= EXTENDED_SIZE) {
            int minPrint = (payload[0xB4] & 0xFF) * 5;
            int maxPrint = (payload[0xB5] & 0xFF) * 5;
            int minBed = (payload[0xB6] & 0xFF) * 5;
            int maxBed = (payload[0xB7] & 0xFF) * 5;
            if (maxPrint > 0) {
                data.nozzleMax = maxPrint;
            }
            if (minPrint > 0) {
                data.nozzleMin = minPrint;
            }
            if (maxBed > 0) {
                data.bedMax = maxBed;
            }
            if (minBed > 0) {
                data.bedMin = minBed;
            }
            int measuredWeight = readUInt16(payload, 0xAE);
            if (measuredWeight > 0) {
                data.weightGrams = measuredWeight;
            }
        }
        if (data.materialType.isEmpty()) {
            data.materialType = "PLA";
        }
        if (data.brand.isEmpty()) {
            data.brand = "Generic";
        }
        return data;
    }

    @NonNull
    @Override
    public byte[] encode(@NonNull FilamentTagData data, int ntagType) throws Exception {
        boolean extended = ntagType == 215 || ntagType == 216;
        int size = extended ? EXTENDED_SIZE : CORE_SIZE;
        byte[] payload = new byte[size];
        writeUInt16(payload, 0x00, TAG_VERSION);
        writeUtf8(payload, 0x02, 5, data.materialType);
        writeUtf8(payload, 0x07, 5, data.materialModifier);
        writeUtf8(payload, 0x1B, 16, data.brand);
        writeUtf8(payload, 0x2B, 32, data.colorName);
        int[] rgb = parseRgb(data.colorHex);
        payload[0x4B] = (byte) rgb[0];
        payload[0x4C] = (byte) rgb[1];
        payload[0x4D] = (byte) rgb[2];
        payload[0x4E] = (byte) 0xFF;
        writeUInt16(payload, 0x5C, data.diameterMicrons > 0 ? data.diameterMicrons : 1750);
        writeUInt16(payload, 0x5E, data.weightGrams > 0 ? data.weightGrams : 1000);
        int printTemp = ((data.nozzleMin + data.nozzleMax) / 2) / 5;
        int bedTemp = ((data.bedMin + data.bedMax) / 2) / 5;
        payload[0x60] = (byte) Math.max(0, Math.min(255, printTemp));
        payload[0x61] = (byte) Math.max(0, Math.min(255, bedTemp));
        int density = data.densityMilliGramsPerCc > 0
                ? data.densityMilliGramsPerCc
                : defaultDensity(data.materialType);
        writeUInt16(payload, 0x62, density);

        if (extended) {
            writeUtf8(payload, 0x90, 16, "");
            payload[0xB4] = (byte) Math.max(0, Math.min(255, data.nozzleMin / 5));
            payload[0xB5] = (byte) Math.max(0, Math.min(255, data.nozzleMax / 5));
            payload[0xB6] = (byte) Math.max(0, Math.min(255, data.bedMin / 5));
            payload[0xB7] = (byte) Math.max(0, Math.min(255, data.bedMax / 5));
            writeUInt16(payload, 0xAE, data.weightGrams > 0 ? data.weightGrams : 1000);
        }
        return payload;
    }

    private static int defaultDensity(String materialType) {
        if (materialType == null) {
            return 1240;
        }
        String type = materialType.toUpperCase(Locale.US);
        if (type.contains("PETG")) {
            return 1270;
        }
        if (type.contains("ABS") || type.contains("ASA")) {
            return 1040;
        }
        if (type.contains("TPU")) {
            return 1200;
        }
        return 1240;
    }

    @NonNull
    private static String readUtf8(byte[] payload, int offset, int length) {
        int end = Math.min(payload.length, offset + length);
        int actual = end - offset;
        if (actual <= 0) {
            return "";
        }
        String value = new String(payload, offset, actual, StandardCharsets.UTF_8);
        int zero = value.indexOf('\0');
        return zero >= 0 ? value.substring(0, zero) : value;
    }

    private static void writeUtf8(byte[] payload, int offset, int length, String value) {
        byte[] bytes = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
        int copy = Math.min(length, bytes.length);
        System.arraycopy(bytes, 0, payload, offset, copy);
    }

    private static int readUInt16(byte[] payload, int offset) {
        return ((payload[offset] & 0xFF) << 8) | (payload[offset + 1] & 0xFF);
    }

    private static void writeUInt16(byte[] payload, int offset, int value) {
        payload[offset] = (byte) ((value >> 8) & 0xFF);
        payload[offset + 1] = (byte) (value & 0xFF);
    }

    private static int[] parseRgb(String colorHex) {
        String hex = OpenSpoolCodec.stripHash(colorHex);
        if (hex.length() == 8) {
            hex = hex.substring(2);
        }
        while (hex.length() < 6) {
            hex = hex + "0";
        }
        if (hex.length() > 6) {
            hex = hex.substring(0, 6);
        }
        int rgb = (int) Long.parseLong(hex, 16);
        return new int[]{(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF};
    }
}