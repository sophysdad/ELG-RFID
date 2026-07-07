package dngsoftware.elgrfid;

import static dngsoftware.elgrfid.Utils.GetSetting;
import static dngsoftware.elgrfid.Utils.setThemeMode;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import dngsoftware.elgrfid.databinding.ActivityBrandSelectionBinding;

public class BrandSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeMode(GetSetting(this, "enabledm", false));

        ActivityBrandSelectionBinding binding = ActivityBrandSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.brandList.setLayoutManager(new LinearLayoutManager(this));
        binding.brandList.setAdapter(new brandCardAdapter(BrandRegistry.getAvailableBrands(), brand -> {
            BrandPreferences.saveSelectedBrand(this, brand);
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }));
    }
}