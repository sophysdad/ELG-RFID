package dngsoftware.elgrfid;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Class<?> target = BrandPreferences.getSelectedBrand(this) == null
                ? BrandSelectionActivity.class
                : MainActivity.class;
        Intent mainIntent = new Intent(this, target);
        mainIntent.setAction(getIntent().getAction());
        mainIntent.setData(getIntent().getData());
        mainIntent.putExtras(getIntent());
        startActivity(mainIntent);
        finish();
    }

}
