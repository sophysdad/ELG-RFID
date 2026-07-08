package dngsoftware.elgrfid;

import static dngsoftware.elgrfid.Utils.GetSetting;
import static dngsoftware.elgrfid.Utils.setThemeMode;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;

public class TagGuideActivity extends AppCompatActivity {

    private static final class Section {
        @StringRes final int titleRes;
        @ColorRes final int accentRes;
        @StringRes final int chipRes;
        @StringRes final int bodyRes;

        Section(int titleRes, int accentRes, int chipRes, int bodyRes) {
            this.titleRes = titleRes;
            this.accentRes = accentRes;
            this.chipRes = chipRes;
            this.bodyRes = bodyRes;
        }
    }

    private static final Section[] SECTIONS = {
            new Section(R.string.brand_elegoo_name, R.color.brand_elegoo_accent,
                    R.string.tag_guide_elegoo_chip, R.string.tag_guide_elegoo_body),
            new Section(R.string.brand_anycubic_name, R.color.brand_anycubic_accent,
                    R.string.tag_guide_anycubic_chip, R.string.tag_guide_anycubic_body),
            new Section(R.string.brand_openspool_name, R.color.brand_openspool_accent,
                    R.string.tag_guide_openspool_chip, R.string.tag_guide_openspool_body),
            new Section(R.string.brand_opentag3d_name, R.color.brand_opentag3d_accent,
                    R.string.tag_guide_opentag3d_chip, R.string.tag_guide_opentag3d_body),
            new Section(R.string.brand_creality_name, R.color.brand_creality_accent,
                    R.string.tag_guide_creality_chip, R.string.tag_guide_creality_body),
            new Section(R.string.brand_qidi_name, R.color.brand_qidi_accent,
                    R.string.tag_guide_qidi_chip, R.string.tag_guide_qidi_body),
            new Section(R.string.brand_bambu_name, R.color.brand_bambu_accent,
                    R.string.tag_guide_bambu_chip, R.string.tag_guide_bambu_body),
            new Section(R.string.tag_guide_cheatsheet_title, R.color.primary_brand,
                    R.string.tag_guide_cheatsheet_chip, R.string.tag_guide_cheatsheet_body),
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeMode(GetSetting(this, "enabledm", false));
        setContentView(R.layout.activity_tag_guide);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_main_menu) {
                BrandNavigation.openMainMenu(this);
                return true;
            }
            return false;
        });

        LinearLayout container = findViewById(R.id.sections_container);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Section section : SECTIONS) {
            addSection(inflater, container, section);
        }
    }

    private void addSection(@NonNull LayoutInflater inflater, @NonNull LinearLayout container,
                            @NonNull Section section) {
        View item = inflater.inflate(R.layout.item_tag_guide_section, container, false);
        View accent = item.findViewById(R.id.section_accent);
        TextView title = item.findViewById(R.id.section_title);
        TextView chip = item.findViewById(R.id.section_chip);
        TextView body = item.findViewById(R.id.section_body);

        accent.setBackgroundColor(ContextCompat.getColor(this, section.accentRes));
        title.setText(section.titleRes);
        chip.setText(getString(R.string.tag_guide_chip_label, getString(section.chipRes)));
        body.setText(section.bodyRes);
        container.addView(item);
    }
}