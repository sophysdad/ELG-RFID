package dngsoftware.elgrfid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class NdefMimeRecord {

    public final String mimeType;
    public final byte[] payload;

    public NdefMimeRecord(@NonNull String mimeType, @NonNull byte[] payload) {
        this.mimeType = mimeType;
        this.payload = payload;
    }

    @Nullable
    public static NdefMimeRecord find(@Nullable NdefMimeRecord[] records, @NonNull String mimeType) {
        if (records == null) {
            return null;
        }
        for (NdefMimeRecord record : records) {
            if (record != null && mimeType.equalsIgnoreCase(record.mimeType)) {
                return record;
            }
        }
        return null;
    }
}