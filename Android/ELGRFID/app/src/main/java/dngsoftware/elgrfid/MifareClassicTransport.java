package dngsoftware.elgrfid;

import android.nfc.Tag;
import android.nfc.tech.MifareClassic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

public final class MifareClassicTransport {

    public static final byte[] KEY_DEFAULT = MifareClassic.KEY_DEFAULT;
    public static final byte[] KEY_QIDI = {(byte) 0xD3, (byte) 0xF7, (byte) 0xD3,
            (byte) 0xF7, (byte) 0xD3, (byte) 0xF7};

    private static final int CREALITY_SECTOR_1 = 1;
    private static final int CREALITY_SECTOR_2 = 2;
    private static final int QIDI_SECTOR = 1;

    private MifareClassicTransport() {
    }

    @Nullable
    public static MifareClassic connect(@NonNull Tag tag) throws Exception {
        MifareClassic mfc = MifareClassic.get(tag);
        if (mfc == null || mfc.getType() != MifareClassic.TYPE_CLASSIC) {
            return null;
        }
        mfc.connect();
        return mfc;
    }

    public static boolean isClassic(@Nullable Tag tag) {
        if (tag == null) {
            return false;
        }
        MifareClassic mfc = MifareClassic.get(tag);
        return mfc != null && mfc.getType() == MifareClassic.TYPE_CLASSIC;
    }

    @NonNull
    public static String describeTag(@Nullable Tag tag) {
        if (tag == null) {
            return "";
        }
        MifareClassic mfc = MifareClassic.get(tag);
        if (mfc == null) {
            return "";
        }
        switch (mfc.getType()) {
            case MifareClassic.TYPE_CLASSIC:
                if (mfc.getSize() == MifareClassic.SIZE_4K) {
                    return "MIFARE Classic 4K";
                }
                return "MIFARE Classic 1K";
            case MifareClassic.TYPE_PLUS:
                return "MIFARE Plus";
            case MifareClassic.TYPE_PRO:
                return "MIFARE Pro";
            default:
                return "MIFARE Classic";
        }
    }

    public static boolean authenticateSectorA(@NonNull MifareClassic mfc, int sector,
                                              @NonNull byte[]... keys) {
        for (byte[] key : keys) {
            try {
                if (mfc.authenticateSectorWithKeyA(sector, key)) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public static boolean isCrealityEncrypted(@NonNull Tag tag, @NonNull byte[] uid) {
        MifareClassic mfc = null;
        try {
            mfc = connect(tag);
            if (mfc == null) {
                return false;
            }
            byte[] encKey = CrealityUtils.createKey(uid);
            return authenticateSectorA(mfc, CREALITY_SECTOR_1, encKey);
        } catch (Exception ignored) {
            return false;
        } finally {
            closeQuietly(mfc);
        }
    }

    @Nullable
    public static String readCrealityPayload(@NonNull Tag tag, @NonNull byte[] uid,
                                             boolean encrypted) {
        MifareClassic mfc = null;
        try {
            mfc = connect(tag);
            if (mfc == null) {
                return null;
            }
            byte[] encKey = CrealityUtils.createKey(uid);
            byte[] keyS1 = encrypted ? encKey : KEY_DEFAULT;
            if (!authenticateSectorA(mfc, CREALITY_SECTOR_1, keyS1)) {
                return null;
            }
            byte[] s1Data = readSectorDataBlocks(mfc, CREALITY_SECTOR_1);
            if (!authenticateSectorA(mfc, CREALITY_SECTOR_2, KEY_DEFAULT)) {
                return null;
            }
            byte[] s2Data = readSectorDataBlocks(mfc, CREALITY_SECTOR_2);
            String part1;
            if (encrypted) {
                part1 = new String(CrealityUtils.cipherData(CrealityUtils.CIPHER_DECRYPT, s1Data),
                        StandardCharsets.UTF_8);
            } else {
                part1 = new String(s1Data, StandardCharsets.UTF_8);
            }
            String part2 = new String(s2Data, StandardCharsets.UTF_8);
            return part1 + part2;
        } catch (Exception ignored) {
            return null;
        } finally {
            closeQuietly(mfc);
        }
    }

    public static void writeCrealityPayload(@NonNull Tag tag, @NonNull byte[] uid,
                                            @NonNull String payload, boolean encrypted) throws Exception {
        MifareClassic mfc = connect(tag);
        if (mfc == null) {
            throw new IllegalStateException("Not a MIFARE Classic tag");
        }
        try {
            String padded = String.format(Locale.US, "%-96s", payload);
            byte[] fullData = padded.getBytes(StandardCharsets.UTF_8);
            byte[] encKey = CrealityUtils.createKey(uid);
            byte[] keyS1 = encrypted ? encKey : KEY_DEFAULT;
            if (!authenticateSectorA(mfc, CREALITY_SECTOR_1, keyS1)) {
                throw new IllegalStateException("Sector 1 authentication failed");
            }
            byte[] s1Raw = Arrays.copyOfRange(fullData, 0, 48);
            byte[] s1ToDisk = CrealityUtils.cipherData(CrealityUtils.CIPHER_ENCRYPT, s1Raw);
            writeSectorDataBlocks(mfc, CREALITY_SECTOR_1, s1ToDisk);
            if (!encrypted) {
                byte[] trailer = mfc.readBlock(mfc.sectorToBlock(CREALITY_SECTOR_1) + 3);
                System.arraycopy(encKey, 0, trailer, 0, 6);
                System.arraycopy(encKey, 0, trailer, 10, 6);
                mfc.writeBlock(mfc.sectorToBlock(CREALITY_SECTOR_1) + 3, trailer);
            }
            if (!authenticateSectorA(mfc, CREALITY_SECTOR_2, KEY_DEFAULT)) {
                throw new IllegalStateException("Sector 2 authentication failed");
            }
            byte[] s2ToDisk = Arrays.copyOfRange(fullData, 48, 96);
            writeSectorDataBlocks(mfc, CREALITY_SECTOR_2, s2ToDisk);
        } finally {
            closeQuietly(mfc);
        }
    }

    public static void formatCrealityTag(@NonNull Tag tag, @NonNull byte[] uid,
                                         boolean encrypted) throws Exception {
        MifareClassic mfc = connect(tag);
        if (mfc == null) {
            throw new IllegalStateException("Not a MIFARE Classic tag");
        }
        try {
            byte[] encKey = CrealityUtils.createKey(uid);
            byte[] currentAuthKey = encrypted ? encKey : KEY_DEFAULT;
            byte[] zeroBlock = new byte[16];
            if (authenticateSectorA(mfc, CREALITY_SECTOR_1, currentAuthKey)) {
                int base = mfc.sectorToBlock(CREALITY_SECTOR_1);
                mfc.writeBlock(base, zeroBlock);
                mfc.writeBlock(base + 1, zeroBlock);
                mfc.writeBlock(base + 2, zeroBlock);
                if (encrypted) {
                    byte[] trailer = mfc.readBlock(base + 3);
                    System.arraycopy(KEY_DEFAULT, 0, trailer, 0, 6);
                    System.arraycopy(KEY_DEFAULT, 0, trailer, 10, 6);
                    mfc.writeBlock(base + 3, trailer);
                }
            }
            if (authenticateSectorA(mfc, CREALITY_SECTOR_2, KEY_DEFAULT)) {
                int base = mfc.sectorToBlock(CREALITY_SECTOR_2);
                mfc.writeBlock(base, zeroBlock);
                mfc.writeBlock(base + 1, zeroBlock);
                mfc.writeBlock(base + 2, zeroBlock);
            }
        } finally {
            closeQuietly(mfc);
        }
    }

    @Nullable
    public static byte[] readQidiPayload(@NonNull Tag tag) {
        MifareClassic mfc = null;
        try {
            mfc = connect(tag);
            if (mfc == null) {
                return null;
            }
            if (!authenticateSectorA(mfc, QIDI_SECTOR, KEY_QIDI, KEY_DEFAULT)) {
                return null;
            }
            return mfc.readBlock(mfc.sectorToBlock(QIDI_SECTOR));
        } catch (Exception ignored) {
            return null;
        } finally {
            closeQuietly(mfc);
        }
    }

    public static void writeQidiPayload(@NonNull Tag tag, byte materialCode, byte colorCode,
                                        byte manufacturerCode) throws Exception {
        MifareClassic mfc = connect(tag);
        if (mfc == null) {
            throw new IllegalStateException("Not a MIFARE Classic tag");
        }
        try {
            if (!authenticateSectorA(mfc, QIDI_SECTOR, KEY_QIDI, KEY_DEFAULT)) {
                throw new IllegalStateException("QIDI sector authentication failed");
            }
            byte[] block = new byte[16];
            block[0] = materialCode;
            block[1] = colorCode;
            block[2] = manufacturerCode;
            mfc.writeBlock(mfc.sectorToBlock(QIDI_SECTOR), block);
            byte[] verify = mfc.readBlock(mfc.sectorToBlock(QIDI_SECTOR));
            if (verify[0] != materialCode || verify[1] != colorCode || verify[2] != manufacturerCode) {
                throw new IllegalStateException("QIDI write verification failed");
            }
        } finally {
            closeQuietly(mfc);
        }
    }

    public static boolean looksLikeQidi(@NonNull byte[] block) {
        if (block.length < 3) {
            return false;
        }
        int material = block[0] & 0xFF;
        int color = block[1] & 0xFF;
        if (material < 1 || material > 50 || color < 1 || color > 24) {
            return false;
        }
        for (int i = 3; i < Math.min(block.length, 16); i++) {
            if (block[i] != 0) {
                return false;
            }
        }
        return true;
    }

    @NonNull
    public static byte[] readSectorDataBlocks(@NonNull MifareClassic mfc, int sector) throws Exception {
        int firstBlock = mfc.sectorToBlock(sector);
        ByteBuffer buffer = ByteBuffer.allocate(48);
        buffer.put(mfc.readBlock(firstBlock));
        buffer.put(mfc.readBlock(firstBlock + 1));
        buffer.put(mfc.readBlock(firstBlock + 2));
        return buffer.array();
    }

    public static void writeSectorDataBlocks(@NonNull MifareClassic mfc, int sector,
                                               @NonNull byte[] data48) throws Exception {
        int firstBlock = mfc.sectorToBlock(sector);
        for (int i = 0; i < 48; i += 16) {
            mfc.writeBlock(firstBlock + (i / 16), Arrays.copyOfRange(data48, i, i + 16));
        }
    }

    public static void closeQuietly(@Nullable MifareClassic mfc) {
        if (mfc == null) {
            return;
        }
        try {
            mfc.close();
        } catch (Exception ignored) {
        }
    }
}