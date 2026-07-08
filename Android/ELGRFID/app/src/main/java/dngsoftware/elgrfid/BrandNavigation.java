package dngsoftware.elgrfid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public final class BrandNavigation {

    private BrandNavigation() {
    }

    public static void openMainMenu(Context context) {
        Intent intent = new Intent(context, BrandSelectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
    }
}