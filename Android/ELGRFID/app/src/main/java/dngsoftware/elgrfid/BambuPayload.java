package dngsoftware.elgrfid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BambuPayload {

    public String materialShort;
    public String materialDetailed;
    public String colorName;
    public String colorHex;
    public String trayUid;
    public String materialId;
    public String variantId;
    public int nozzleMin;
    public int nozzleMax;
    public int bedTemp;
    public int dryTemp;
    public int dryHours;
    public int weightGrams;
    public float diameterMm;
    public int lengthMeters;
    @Nullable
    public byte[] rawDump;

    @NonNull
    public String getDisplayMaterial() {
        if (materialDetailed != null && !materialDetailed.trim().isEmpty()) {
            return materialDetailed.trim();
        }
        if (materialShort != null && !materialShort.trim().isEmpty()) {
            return materialShort.trim();
        }
        return "Unknown";
    }
}