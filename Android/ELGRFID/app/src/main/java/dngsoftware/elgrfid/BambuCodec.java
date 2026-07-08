package dngsoftware.elgrfid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BambuCodec {

    private static final int BLOCK_SIZE = 16;

    private BambuCodec() {
    }

    @Nullable
    public static BambuPayload decode(@Nullable byte[] dump) {
        if (dump == null || dump.length < BLOCK_SIZE * 10) {
            return null;
        }
        BambuPayload payload = new BambuPayload();
        payload.rawDump = dump;

        payload.variantId = BambuUtils.readAscii(dump, BLOCK_SIZE * 1, 8);
        payload.materialId = BambuUtils.readAscii(dump, BLOCK_SIZE * 1 + 8, 8);
        payload.materialShort = BambuUtils.readAscii(dump, BLOCK_SIZE * 2, BLOCK_SIZE);
        payload.materialDetailed = BambuUtils.readAscii(dump, BLOCK_SIZE * 4, BLOCK_SIZE);

        byte[] rgba = new byte[4];
        System.arraycopy(dump, BLOCK_SIZE * 5, rgba, 0, 4);
        payload.colorName = BambuUtils.getColorName(rgba);
        payload.colorHex = BambuUtils.rgbaToHex(rgba);
        payload.weightGrams = BambuUtils.readUint16Le(dump, BLOCK_SIZE * 5 + 4);
        payload.diameterMm = BambuUtils.readFloatLe(dump, BLOCK_SIZE * 5 + 8);

        payload.dryTemp = BambuUtils.readUint16Le(dump, BLOCK_SIZE * 6);
        payload.dryHours = BambuUtils.readUint16Le(dump, BLOCK_SIZE * 6 + 2);
        payload.bedTemp = BambuUtils.readUint16Le(dump, BLOCK_SIZE * 6 + 6);
        payload.nozzleMax = BambuUtils.readUint16Le(dump, BLOCK_SIZE * 6 + 8);
        payload.nozzleMin = BambuUtils.readUint16Le(dump, BLOCK_SIZE * 6 + 10);
        payload.trayUid = BambuUtils.readAscii(dump, BLOCK_SIZE * 9, BLOCK_SIZE);
        payload.lengthMeters = BambuUtils.readUint16Le(dump, BLOCK_SIZE * 14 + 4);

        if (payload.getDisplayMaterial().isEmpty() && payload.trayUid.isEmpty()) {
            return null;
        }
        return payload;
    }

    public static boolean isBambuDump(@Nullable byte[] dump) {
        if (dump == null || dump.length < BLOCK_SIZE * 4) {
            return false;
        }
        byte[] trailer = new byte[BLOCK_SIZE];
        System.arraycopy(dump, BLOCK_SIZE * 3, trailer, 0, BLOCK_SIZE);
        return BambuUtils.isBambuTrailer(trailer);
    }
}