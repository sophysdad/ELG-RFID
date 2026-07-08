package dngsoftware.elgrfid;

import android.content.Context;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public final class CrealityUtils {

    public static final int CIPHER_ENCRYPT = Cipher.ENCRYPT_MODE;
    public static final int CIPHER_DECRYPT = Cipher.DECRYPT_MODE;

    public static final String[] PRINTER_TYPES = {"K2", "K1", "HI"};

    private static final byte[] KEY_DERIVE_AES = {
            113, 51, 98, 117, 94, 116, 49, 110, 113, 102, 90, 40, 112, 102, 36, 49};
    private static final byte[] DATA_CIPHER_AES = {
            72, 64, 67, 70, 107, 82, 110, 122, 64, 75, 65, 116, 66, 74, 112, 50};

    private CrealityUtils() {
    }

    @NonNull
    public static String dbKeyForPrinter(@NonNull String printerType) {
        return "creality_" + printerType.toLowerCase(Locale.US);
    }

    public static int rawResourceForPrinter(@NonNull String printerType) {
        switch (printerType.toLowerCase(Locale.US)) {
            case "k1":
                return R.raw.creality_k1;
            case "hi":
                return R.raw.creality_hi;
            case "k2":
            default:
                return R.raw.creality_k2;
        }
    }

    public static void populateDatabase(@NonNull Context context, @NonNull FilamentDao db,
                                        @NonNull String printerType) {
        try {
            InputStream stream = context.getResources().openRawResource(rawResourceForPrinter(printerType));
            String json = readStream(stream);
            if (json == null || json.isEmpty()) {
                return;
            }
            JSONObject root = new JSONObject(json);
            JSONObject result = root.getJSONObject("result");
            JSONArray list = result.getJSONArray("list");
            for (int i = 0; i < list.length(); i++) {
                JSONObject item = list.getJSONObject(i);
                JSONObject base = item.getJSONObject("base");
                String currentId = base.getString("id").trim();
                DbFilament existing = db.getFilamentById(currentId);
                DbFilament filament = new DbFilament();
                filament.filamentID = currentId;
                filament.position = i;
                filament.filamentName = base.getString("name").trim();
                filament.filamentVendor = base.getString("brand").trim();
                filament.filamentParam = item.toString();
                if (existing != null) {
                    filament.dbKey = existing.dbKey;
                    db.updateItem(filament);
                } else {
                    db.addItem(filament);
                }
            }
        } catch (Exception ignored) {
        }
    }

    @NonNull
    public static String[] getMaterialBrands(@NonNull FilamentDao db) {
        Set<String> brands = new HashSet<>();
        for (DbFilament item : db.getAllItems()) {
            brands.add(item.filamentVendor);
        }
        String[] result = brands.toArray(new String[0]);
        Arrays.sort(result);
        return result;
    }

    public static String getMaterialLengthCode(@NonNull String materialWeight) {
        switch (materialWeight) {
            case "750 G":
                return "0247";
            case "600 G":
                return "0198";
            case "500 G":
                return "0165";
            case "250 G":
                return "0082";
            case "1 KG":
            default:
                return "0330";
        }
    }

    @NonNull
    public static String getMaterialWeightFromCode(@NonNull String lengthCode) {
        switch (lengthCode) {
            case "0247":
                return "750 G";
            case "0198":
                return "600 G";
            case "0165":
                return "500 G";
            case "0082":
                return "250 G";
            case "0330":
            default:
                return "1 KG";
        }
    }

    @NonNull
    public static byte[] createKey(@NonNull byte[] tagId) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(KEY_DERIVE_AES, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encB = new byte[16];
            int x = 0;
            for (int i = 0; i < 16; i++) {
                if (x >= tagId.length) {
                    x = 0;
                }
                encB[i] = tagId[x];
                x++;
            }
            return Arrays.copyOfRange(cipher.doFinal(encB), 0, 6);
        } catch (Exception ignored) {
            return MifareClassicTransport.KEY_DEFAULT;
        }
    }

    @NonNull
    public static byte[] cipherData(int mode, @NonNull byte[] tagData) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(DATA_CIPHER_AES, "AES");
            cipher.init(mode, secretKeySpec);
            return cipher.doFinal(tagData);
        } catch (Exception ignored) {
            return tagData;
        }
    }

    @NonNull
    private static String readStream(@NonNull InputStream stream) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }
}