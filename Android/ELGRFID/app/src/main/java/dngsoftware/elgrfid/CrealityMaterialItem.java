package dngsoftware.elgrfid;

import androidx.annotation.NonNull;

public class CrealityMaterialItem {

    private final String materialName;
    private final String materialId;

    public CrealityMaterialItem(@NonNull String materialName, @NonNull String materialId) {
        this.materialName = materialName;
        this.materialId = materialId;
    }

    @NonNull
    public String getMaterialName() {
        return materialName;
    }

    @NonNull
    public String getMaterialId() {
        return materialId;
    }

    @NonNull
    @Override
    public String toString() {
        return materialName;
    }
}