package dngsoftware.elgrfid;

import android.content.Context;
import android.content.SharedPreferences;

public final class BrandPreferences {

    private static final String PREFS = "spooltag_prefs";
    private static final String KEY_BRAND = "selected_brand";

    private BrandPreferences() {
    }

    public static void saveSelectedBrand(Context context, PrinterBrand brand) {
        prefs(context).edit().putString(KEY_BRAND, brand.id).apply();
    }

    public static PrinterBrand getSelectedBrand(Context context) {
        return PrinterBrand.fromId(prefs(context).getString(KEY_BRAND, null));
    }

    public static void clearSelectedBrand(Context context) {
        prefs(context).edit().remove(KEY_BRAND).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}