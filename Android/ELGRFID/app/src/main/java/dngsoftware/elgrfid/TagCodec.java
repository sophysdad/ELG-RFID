package dngsoftware.elgrfid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface TagCodec {

    @NonNull
    String getMimeType();

    @NonNull
    FilamentTagData decode(@NonNull byte[] payload) throws Exception;

    @NonNull
    byte[] encode(@NonNull FilamentTagData data, int ntagType) throws Exception;

    @Nullable
    default FilamentTagData tryDecode(@Nullable NdefMimeRecord[] records) {
        NdefMimeRecord record = NdefMimeRecord.find(records, getMimeType());
        if (record == null) {
            return null;
        }
        try {
            return decode(record.payload);
        } catch (Exception ignored) {
            return null;
        }
    }
}