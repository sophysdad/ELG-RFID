package dngsoftware.elgrfid;

import android.nfc.Tag;

public interface TagOperations {
    void readTag(Tag tag);

    void writeTag(Tag tag);
}