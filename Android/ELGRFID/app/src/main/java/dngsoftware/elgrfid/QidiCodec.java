package dngsoftware.elgrfid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class QidiCodec {

    public static class Payload {
        public int materialCode;
        public int colorCode;
        public int manufacturerCode = 1;
    }

    private QidiCodec() {
    }

    @Nullable
    public static Payload decode(@Nullable byte[] block) {
        if (block == null || block.length < 3) {
            return null;
        }
        int material = block[0] & 0xFF;
        int color = block[1] & 0xFF;
        if (material < 1 || material > 50 || color < 1 || color > 24) {
            return null;
        }
        Payload payload = new Payload();
        payload.materialCode = material;
        payload.colorCode = color;
        payload.manufacturerCode = block[2] & 0xFF;
        return payload;
    }

    @NonNull
    public static byte[] encode(int materialCode, int colorCode, int manufacturerCode) {
        byte[] block = new byte[16];
        block[0] = (byte) materialCode;
        block[1] = (byte) colorCode;
        block[2] = (byte) manufacturerCode;
        return block;
    }
}