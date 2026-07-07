package dngsoftware.elgrfid;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public enum PrinterBrand {
    ELEGOO(
            "elegoo",
            R.string.brand_elegoo_name,
            R.string.brand_elegoo_desc,
            R.color.brand_elegoo_accent),
    ANYCUBIC(
            "anycubic",
            R.string.brand_anycubic_name,
            R.string.brand_anycubic_desc,
            R.color.brand_anycubic_accent);

    public final String id;
    @StringRes public final int nameRes;
    @StringRes public final int descRes;
    public final int accentColorRes;

    PrinterBrand(String id, int nameRes, int descRes, int accentColorRes) {
        this.id = id;
        this.nameRes = nameRes;
        this.descRes = descRes;
        this.accentColorRes = accentColorRes;
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