package dngsoftware.elgrfid;

import static dngsoftware.elgrfid.Utils.getContrastColor;

import android.graphics.Color;
import android.nfc.Tag;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;

import dngsoftware.elgrfid.databinding.ActivityMainBinding;

public class QidiController implements FilamentPresetController, TagOperations {

    private final MainActivity activity;
    private final ActivityMainBinding main;
    private final Handler mainHandler;
    private final ExecutorService executorService;

    private ArrayAdapter<String> materialAdapter;
    private ArrayAdapter<String> colorAdapter;

    private int materialCode = 1;
    private int colorCode = 1;

    public QidiController(MainActivity activity, ActivityMainBinding main,
                          Handler mainHandler, ExecutorService executorService) {
        this.activity = activity;
        this.main = main;
        this.mainHandler = mainHandler;
        this.executorService = executorService;
    }

    public void initialize() {
        String[] materialNames = new String[QidiUtils.MATERIALS.length];
        for (int i = 0; i < QidiUtils.MATERIALS.length; i++) {
            materialNames[i] = QidiUtils.MATERIALS[i].name;
        }
        String[] colorNames = new String[QidiUtils.COLORS.length];
        for (int i = 0; i < QidiUtils.COLORS.length; i++) {
            colorNames[i] = QidiUtils.COLORS[i].name;
        }

        materialAdapter = new ArrayAdapter<>(activity, R.layout.spinner_item, materialNames);
        colorAdapter = new ArrayAdapter<>(activity, R.layout.spinner_item, colorNames);

        main.type.setAdapter(materialAdapter);
        main.subtype.setAdapter(colorAdapter);

        main.type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                materialCode = QidiUtils.MATERIALS[position].code;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        main.subtype.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                colorCode = QidiUtils.COLORS[position].code;
                applyColorPreview(colorCode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        applyColorPreview(colorCode);
    }

    public void configureUi() {
        int visible = View.VISIBLE;
        int gone = View.GONE;

        main.vendor.setVisibility(gone);
        main.vendorborder.setVisibility(gone);
        main.lblvendorMain.setVisibility(gone);
        main.vendorTapTarget.setVisibility(gone);

        main.material.setVisibility(gone);
        main.materialborder.setVisibility(gone);
        main.lblmaterial.setVisibility(gone);
        main.infotext.setVisibility(gone);

        main.type.setVisibility(visible);
        main.typeborder.setVisibility(visible);
        main.lbltype.setVisibility(visible);
        main.lbltype.setText(R.string.material);

        main.subtype.setVisibility(visible);
        main.subtypeborder.setVisibility(visible);
        main.lblsubtype.setVisibility(visible);
        main.lblsubtype.setText(R.string.color);

        main.lblnozzletemps.setVisibility(gone);
        main.extminborder.setVisibility(gone);
        main.extmin.setVisibility(gone);
        main.lblnozzlemin.setVisibility(gone);
        main.extmaxborder.setVisibility(gone);
        main.extmax.setVisibility(gone);
        main.lblnozzlemax.setVisibility(gone);
        main.lblbedtemps.setVisibility(gone);
        main.bedminborder.setVisibility(gone);
        main.bedmin.setVisibility(gone);
        main.lblbedmin.setVisibility(gone);
        main.bedmaxborder.setVisibility(gone);
        main.bedmax.setVisibility(gone);
        main.lblbedmax.setVisibility(gone);
        main.otherSettingsHeader.setVisibility(gone);
        main.spoolsize.setVisibility(gone);
        main.sizeborder.setVisibility(gone);
        main.lblsize.setVisibility(gone);
        main.lbldiameter.setVisibility(gone);
        main.diameterborder.setVisibility(gone);
        main.diameter.setVisibility(gone);
        main.lblproddate.setVisibility(gone);
        main.proddateborder.setVisibility(gone);
        main.proddate.setVisibility(gone);
    }

    @Override
    public void readTag(Tag tag) {
        if (tag == null) {
            activity.showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
            return;
        }
        executorService.execute(() -> {
            try {
                byte[] block = MifareClassicTransport.readQidiPayload(tag);
                QidiCodec.Payload payload = QidiCodec.decode(block);
                if (payload == null) {
                    activity.showToast(R.string.unknown_or_empty_tag, Toast.LENGTH_SHORT);
                    return;
                }
                mainHandler.post(() -> {
                    materialCode = payload.materialCode;
                    colorCode = payload.colorCode;
                    main.type.setSelection(findMaterialIndex(materialCode));
                    main.subtype.setSelection(findColorIndex(colorCode));
                    applyColorPreview(colorCode);
                    Utils.playBeep();
                    activity.showToast(R.string.data_read_from_tag, Toast.LENGTH_SHORT);
                });
            } catch (Exception e) {
                activity.showToast(R.string.error_reading_tag, Toast.LENGTH_SHORT);
            }
        });
    }

    @Override
    public void writeTag(Tag tag) {
        if (tag == null) {
            activity.showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
            return;
        }
        executorService.execute(() -> {
            try {
                MifareClassicTransport.writeQidiPayload(tag, (byte) materialCode,
                        (byte) colorCode, (byte) 1);
                Utils.playBeep();
                activity.showToast(R.string.data_written_to_tag, Toast.LENGTH_SHORT);
            } catch (Exception e) {
                activity.showToast(R.string.error_writing_to_tag, Toast.LENGTH_SHORT);
            }
        });
    }

    public void formatTag(Tag tag) {
        if (tag == null) {
            activity.showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
            return;
        }
        executorService.execute(() -> {
            try {
                MifareClassicTransport.writeQidiPayload(tag, (byte) 0, (byte) 0, (byte) 0);
                Utils.playBeep();
                activity.showToast(R.string.tag_formatted, Toast.LENGTH_SHORT);
            } catch (Exception e) {
                activity.showToast(R.string.failed_to_format_tag_for_writing, Toast.LENGTH_SHORT);
            }
        });
    }

    public void readTagMemory() {
        activity.showToast(R.string.mifare_memory_coming_soon, Toast.LENGTH_SHORT);
    }

    @Override
    public void openAddDialog(boolean edit) {
        activity.showToast(R.string.qidi_no_custom_filaments, Toast.LENGTH_SHORT);
    }

    @Override
    public void confirmDeleteFilament() {
        activity.showToast(R.string.qidi_no_custom_filaments, Toast.LENGTH_SHORT);
    }

    private void applyColorPreview(int code) {
        String rgb = QidiUtils.getColorRgb(code);
        activity.setMaterialColor(rgb);
        main.txtcolor.setText(rgb);
        main.colorview.setBackgroundColor(Color.parseColor("#" + rgb));
        main.txtcolor.setTextColor(getContrastColor(Color.parseColor("#" + rgb)));
    }

    private int findMaterialIndex(int code) {
        for (int i = 0; i < QidiUtils.MATERIALS.length; i++) {
            if (QidiUtils.MATERIALS[i].code == code) {
                return i;
            }
        }
        return 0;
    }

    private int findColorIndex(int code) {
        for (int i = 0; i < QidiUtils.COLORS.length; i++) {
            if (QidiUtils.COLORS[i].code == code) {
                return i;
            }
        }
        return 0;
    }
}