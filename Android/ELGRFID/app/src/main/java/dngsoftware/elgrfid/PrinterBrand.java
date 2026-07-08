package dngsoftware.elgrfid;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public enum PrinterBrand {
    ELEGOO(
            "elegoo",
            R.string.brand_elegoo_name,
            R.string.brand_elegoo_desc,
            R.color.brand_elegoo_accent,
            false,
            false,
            false),
    ANYCUBIC(
            "anycubic",
            R.string.brand_anycubic_name,
            R.string.brand_anycubic_desc,
            R.color.brand_anycubic_accent,
            false,
            false,
            false),
    OPEN_SPOOL(
            "openspool",
            R.string.brand_openspool_name,
            R.string.brand_openspool_desc,
            R.color.brand_openspool_accent,
            true,
            false,
            false),
    OPEN_TAG3D(
            "opentag3d",
            R.string.brand_opentag3d_name,
            R.string.brand_opentag3d_desc,
            R.color.brand_opentag3d_accent,
            true,
            false,
            false),
    CREALITY(
            "creality",
            R.string.brand_creality_name,
            R.string.brand_creality_desc,
            R.color.brand_creality_accent,
            false,
            true,
            false),
    QIDI(
            "qidi",
            R.string.brand_qidi_name,
            R.string.brand_qidi_desc,
            R.color.brand_qidi_accent,
            false,
            true,
            false),
    BAMBU(
            "bambu",
            R.string.brand_bambu_name,
            R.string.brand_bambu_desc,
            R.color.brand_bambu_accent,
            false,
            true,
            true);

    public final String id;
    @StringRes public final int nameRes;
    @StringRes public final int descRes;
    public final int accentColorRes;
    public final boolean openStandard;
    public final boolean mifareClassic;
    public final boolean readOnly;

    PrinterBrand(String id, int nameRes, int descRes, int accentColorRes,
                 boolean openStandard, boolean mifareClassic, boolean readOnly) {
        this.id = id;
        this.nameRes = nameRes;
        this.descRes = descRes;
        this.accentColorRes = accentColorRes;
        this.openStandard = openStandard;
        this.mifareClassic = mifareClassic;
        this.readOnly = readOnly;
    }

    public static PrinterBrand fromId(String id) {
        if (id == null) {
            return null;
        }
        for (PrinterBrand brand : values()) {
            if (brand.id.equals(id)) {
                return brand;
            }
        }
        return null;
    }

    @NonNull
    @Override
    public String toString() {
        return id;
    }
}