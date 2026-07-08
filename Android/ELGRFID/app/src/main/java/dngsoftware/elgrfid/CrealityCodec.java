package dngsoftware.elgrfid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public final class CrealityCodec {

    public static final String VENDOR_ID = "0276";
    public static final String DATE_PREFIX = "AB124";
    public static final String BATCH = "A2";

    public static class Parsed {
        public String materialId;
        public String colorHex;
        public String lengthCode;
        public String printerType;
    }

    private CrealityCodec() {
    }

    @NonNull
    public static String buildPayload(@NonNull String materialId, @NonNull String colorHex,
                                    @NonNull String lengthCode, @NonNull String printerType) {
        String filamentId = "1" + materialId;
        String color = colorHex.length() == 6 ? "0" + colorHex : colorHex;
        String serialNum = "000001";
        String reserve = "00000000000000";
        return DATE_PREFIX + VENDOR_ID + BATCH + filamentId + color + lengthCode
                + serialNum + reserve + printerType.toLowerCase(Locale.US);
    }

    @Nullable
    public static Parsed parse(@Nullable String tagData) {
        if (tagData == null || tagData.length() < 40) {
            return null;
        }
        if (!VENDOR_ID.equals(tagData.substring(5, 9))) {
            return null;
        }
        Parsed parsed = new Parsed();
        parsed.materialId = tagData.substring(12, 17);
        parsed.colorHex = tagData.substring(18, 24);
        parsed.lengthCode = tagData.substring(24, 28);
        if (tagData.length() >= 48) {
            parsed.printerType = tagData.substring(48).trim();
        }
        return parsed;
    }

    public static boolean isCrealityPayload(@Nullable String tagData) {
        return parse(tagData) != null;
    }
}