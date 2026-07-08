package dngsoftware.elgrfid;

import android.graphics.Color;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class BambuUtils {

    private static final byte[] MASTER_KEY = {
            (byte) 0x9a, (byte) 0x75, (byte) 0x9c, (byte) 0xf2,
            (byte) 0xc4, (byte) 0xf7, (byte) 0xca, (byte) 0xff,
            (byte) 0x22, (byte) 0x2c, (byte) 0xb9, (byte) 0x76,
            (byte) 0x9b, (byte) 0x41, (byte) 0xbc, (byte) 0x96};
    private static final byte[] HKDF_CONTEXT = "RFID-A\u0000".getBytes(StandardCharsets.UTF_8);
    private static final byte[] TRAILER_ACCESS_BITS = {
            (byte) 0x87, (byte) 0x87, (byte) 0x87, 0x69};

    private static final Map<String, int[]> COLOR_NAMES = new LinkedHashMap<>();

    static {
        COLOR_NAMES.put("Jade White", new int[]{255, 255, 255});
        COLOR_NAMES.put("Beige", new int[]{247, 230, 222});
        COLOR_NAMES.put("Gold", new int[]{228, 189, 104});
        COLOR_NAMES.put("Silver", new int[]{166, 169, 170});
        COLOR_NAMES.put("Gray", new int[]{142, 144, 137});
        COLOR_NAMES.put("Bronze", new int[]{132, 125, 72});
        COLOR_NAMES.put("Brown", new int[]{157, 67, 44});
        COLOR_NAMES.put("Red", new int[]{193, 46, 31});
        COLOR_NAMES.put("Magenta", new int[]{236, 0, 140});
        COLOR_NAMES.put("Pink", new int[]{245, 90, 116});
        COLOR_NAMES.put("Orange", new int[]{255, 106, 19});
        COLOR_NAMES.put("Yellow", new int[]{244, 238, 42});
        COLOR_NAMES.put("Bambu Green", new int[]{0, 174, 66});
        COLOR_NAMES.put("Mistletoe Green", new int[]{63, 142, 67});
        COLOR_NAMES.put("Cyan", new int[]{0, 134, 214});
        COLOR_NAMES.put("Blue", new int[]{10, 41, 137});
        COLOR_NAMES.put("Purple", new int[]{94, 67, 183});
        COLOR_NAMES.put("Blue Gray", new int[]{91, 101, 121});
        COLOR_NAMES.put("Light Gray", new int[]{209, 211, 213});
        COLOR_NAMES.put("Dark Gray", new int[]{84, 84, 84});
        COLOR_NAMES.put("Black", new int[]{0, 0, 0});
    }

    private BambuUtils() {
    }

    @NonNull
    public static byte[][] deriveKeys(@NonNull byte[] uid, int sectorCount) {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
        hkdf.init(new HKDFParameters(uid, MASTER_KEY, HKDF_CONTEXT));
        byte[] derived = new byte[sectorCount * 6];
        hkdf.generateBytes(derived, 0, derived.length);
        byte[][] keys = new byte[sectorCount][6];
        for (int i = 0; i < sectorCount; i++) {
            keys[i] = Arrays.copyOfRange(derived, i * 6, i * 6 + 6);
        }
        return keys;
    }

    public static boolean isBambuTrailer(@NonNull byte[] trailerBlock) {
        if (trailerBlock.length < 10) {
            return false;
        }
        return trailerBlock[6] == TRAILER_ACCESS_BITS[0]
                && trailerBlock[7] == TRAILER_ACCESS_BITS[1]
                && trailerBlock[8] == TRAILER_ACCESS_BITS[2]
                && trailerBlock[9] == TRAILER_ACCESS_BITS[3];
    }

    public static boolean isBambuTag(@NonNull Tag tag) {
        byte[] uid = tag.getId();
        if (uid.length != 4) {
            return false;
        }
        MifareClassic mfc = null;
        try {
            mfc = MifareClassicTransport.connect(tag);
            if (mfc == null || mfc.getType() != MifareClassic.TYPE_CLASSIC) {
                return false;
            }
            byte[][] keys = deriveKeys(uid, mfc.getSectorCount());
            if (!MifareClassicTransport.authenticateSectorA(mfc, 0, keys[0])) {
                return false;
            }
            byte[] trailer = mfc.readBlock(3);
            return isBambuTrailer(trailer);
        } catch (Exception ignored) {
            return false;
        } finally {
            MifareClassicTransport.closeQuietly(mfc);
        }
    }

    @Nullable
    public static byte[] readDump(@NonNull Tag tag) throws Exception {
        byte[] uid = tag.getId();
        if (uid.length != 4) {
            throw new IllegalArgumentException("Bambu tags need a 4-byte UID");
        }
        MifareClassic mfc = MifareClassicTransport.connect(tag);
        if (mfc == null) {
            throw new IllegalStateException("Not a MIFARE Classic tag");
        }
        try {
            byte[][] keys = deriveKeys(uid, mfc.getSectorCount());
            byte[] dump = new byte[mfc.getSize()];
            for (int sector = 0; sector < mfc.getSectorCount(); sector++) {
                if (!MifareClassicTransport.authenticateSectorA(mfc, sector, keys[sector])) {
                    throw new IllegalStateException("Authentication failed for sector " + sector);
                }
                int firstBlock = mfc.sectorToBlock(sector);
                int blockCount = mfc.getBlockCountInSector(sector);
                for (int block = 0; block < blockCount; block++) {
                    int absolute = firstBlock + block;
                    byte[] blockData = mfc.readBlock(absolute);
                    System.arraycopy(blockData, 0, dump, absolute * 16, 16);
                }
            }
            return dump;
        } finally {
            MifareClassicTransport.closeQuietly(mfc);
        }
    }

    public static void writeClone(@NonNull Tag tag, @NonNull byte[] dump) throws Exception {
        MifareClassic mfc = MifareClassicTransport.connect(tag);
        if (mfc == null) {
            throw new IllegalStateException("Tag is not MIFARE Classic");
        }
        byte[] uid = tag.getId();
        if (uid == null || uid.length != 4) {
            throw new IllegalStateException("Clone target needs a 4-byte UID blank tag");
        }
        if (mfc.getType() != MifareClassic.TYPE_CLASSIC || mfc.getSize() != MifareClassic.SIZE_1K) {
            throw new IllegalStateException("Clone target must be MIFARE Classic 1K");
        }
        if (dump.length < mfc.getSize()) {
            throw new IllegalStateException("Dump is too small");
        }
        try {
            if (!MifareClassicTransport.authenticateSectorA(mfc, 0, MifareClassicTransport.KEY_DEFAULT)) {
                throw new IllegalStateException("Blank tag auth failed — use a Gen2/FUID magic tag");
            }
            mfc.writeBlock(0, Arrays.copyOfRange(dump, 0, 16));
            mfc.close();
            mfc.connect();
            for (int sector = 0; sector < mfc.getSectorCount(); sector++) {
                if (!MifareClassicTransport.authenticateSectorA(mfc, sector, MifareClassicTransport.KEY_DEFAULT)) {
                    throw new IllegalStateException("Failed to authenticate sector " + sector + " on blank tag");
                }
                int firstBlock = mfc.sectorToBlock(sector);
                int blockCount = mfc.getBlockCountInSector(sector);
                for (int block = 0; block < blockCount; block++) {
                    int absolute = firstBlock + block;
                    mfc.writeBlock(absolute, Arrays.copyOfRange(dump, absolute * 16, absolute * 16 + 16));
                }
            }
        } finally {
            MifareClassicTransport.closeQuietly(mfc);
        }
    }

    @NonNull
    public static String getColorName(@NonNull byte[] rgba) {
        if (rgba.length < 3) {
            return "Unknown";
        }
        int red = rgba[0] & 0xFF;
        int green = rgba[1] & 0xFF;
        int blue = rgba[2] & 0xFF;
        String bestName = "Unknown";
        double bestDistance = Double.MAX_VALUE;
        for (Map.Entry<String, int[]> entry : COLOR_NAMES.entrySet()) {
            int[] rgb = entry.getValue();
            double distance = Math.sqrt(
                    Math.pow(rgb[0] - red, 2)
                            + Math.pow(rgb[1] - green, 2)
                            + Math.pow(rgb[2] - blue, 2));
            if (distance < bestDistance) {
                bestDistance = distance;
                bestName = entry.getKey();
            }
        }
        return bestName;
    }

    public static int getColorArgb(@NonNull String colorName) {
        for (Map.Entry<String, int[]> entry : COLOR_NAMES.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(colorName)) {
                int[] rgb = entry.getValue();
                return Color.rgb(rgb[0], rgb[1], rgb[2]);
            }
        }
        return Color.rgb(128, 128, 128);
    }

    @NonNull
    public static String readAscii(@NonNull byte[] data, int offset, int length) {
        if (offset < 0 || offset + length > data.length) {
            return "";
        }
        return new String(data, offset, length, StandardCharsets.UTF_8).trim().replace("\0", "");
    }

    public static int readUint16Le(@NonNull byte[] data, int offset) {
        if (offset + 2 > data.length) {
            return 0;
        }
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }

    public static float readFloatLe(@NonNull byte[] data, int offset) {
        if (offset + 4 > data.length) {
            return 0f;
        }
        return ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    @NonNull
    public static String rgbaToHex(@NonNull byte[] rgba) {
        if (rgba.length < 3) {
            return "808080";
        }
        return String.format(Locale.US, "%02X%02X%02X", rgba[0] & 0xFF, rgba[1] & 0xFF, rgba[2] & 0xFF);
    }
}