package dngsoftware.elgrfid;

import android.graphics.Color;
import android.nfc.Tag;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;

import dngsoftware.elgrfid.databinding.ActivityMainBinding;

public class NdefFilamentController implements FilamentPresetController, TagOperations {

    private static final String[] MATERIAL_TYPES = {
            "PLA", "PETG", "ABS", "ASA", "TPU", "PA", "PC", "PVA", "HIPS", "PP", "BVOH"
    };

    private static final String[] MATERIAL_MODIFIERS = {
            "", "Basic", "Matte", "Silk", "CF", "HF", "Plus", "Pro"
    };

    private static final String[] BRANDS = {
            "Generic", "eSUN", "Polymaker", "Overture", "Prusament", "HATCHBOX", "SUNLU", "Custom"
    };

    private final MainActivity activity;
    private final ActivityMainBinding main;
    private final Handler mainHandler;
    private final ExecutorService executorService;
    private final PrinterBrand brand;
    private final TagCodec codec;

    private ArrayAdapter<String> brandAdapter;
    private ArrayAdapter<String> typeAdapter;
    private ArrayAdapter<String> modifierAdapter;
    private String customBrand = "Generic";

    public NdefFilamentController(MainActivity activity, ActivityMainBinding main,
                                  Handler mainHandler, ExecutorService executorService,
                                  PrinterBrand brand, TagCodec codec) {
        this.activity = activity;
        this.main = main;
        this.mainHandler = mainHandler;
        this.executorService = executorService;
        this.brand = brand;
        this.codec = codec;
    }

    public void initialize() {
        brandAdapter = new ArrayAdapter<>(activity, R.layout.spinner_item, BRANDS);
        typeAdapter = new ArrayAdapter<>(activity, R.layout.spinner_item, MATERIAL_TYPES);
        modifierAdapter = new ArrayAdapter<>(activity, R.layout.spinner_item, MATERIAL_MODIFIERS);
        main.vendor.setAdapter(brandAdapter);
        main.type.setAdapter(typeAdapter);
        main.subtype.setAdapter(modifierAdapter);
        main.vendor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = brandAdapter.getItem(position);
                if ("Custom".equals(selected)) {
                    promptCustomBrand();
                } else {
                    customBrand = selected;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    public void configureUi() {
        int visible = View.VISIBLE;
        int gone = View.GONE;

        main.vendor.setVisibility(visible);
        main.vendorborder.setVisibility(visible);
        main.lblvendorMain.setVisibility(visible);
        main.lblvendorMain.setText(R.string.brand);
        main.vendorTapTarget.setVisibility(gone);

        main.type.setVisibility(visible);
        main.typeborder.setVisibility(visible);
        main.lbltype.setVisibility(visible);

        main.subtype.setVisibility(visible);
        main.subtypeborder.setVisibility(visible);
        main.lblsubtype.setVisibility(visible);
        main.lblsubtype.setText(R.string.variant);

        main.material.setVisibility(gone);
        main.materialborder.setVisibility(gone);
        main.lblmaterial.setVisibility(gone);
        main.infotext.setVisibility(gone);

        main.lblnozzletemps.setVisibility(visible);
        main.extminborder.setVisibility(visible);
        main.extmin.setVisibility(visible);
        main.lblnozzlemin.setVisibility(visible);
        main.extmaxborder.setVisibility(visible);
        main.extmax.setVisibility(visible);
        main.lblnozzlemax.setVisibility(visible);
        main.lblbedtemps.setVisibility(visible);
        main.bedminborder.setVisibility(visible);
        main.bedmin.setVisibility(visible);
        main.lblbedmin.setVisibility(visible);
        main.bedmaxborder.setVisibility(visible);
        main.bedmax.setVisibility(visible);
        main.lblbedmax.setVisibility(visible);
        main.otherSettingsHeader.setVisibility(visible);
        main.spoolsize.setVisibility(visible);
        main.sizeborder.setVisibility(visible);
        main.lblsize.setVisibility(visible);
        main.lbldiameter.setVisibility(visible);
        main.diameterborder.setVisibility(visible);
        main.diameter.setVisibility(visible);
        main.lblproddate.setVisibility(gone);
        main.proddateborder.setVisibility(gone);
        main.proddate.setVisibility(gone);

        if (activity.getFilamentDiameterStored() <= 0) {
            activity.setFilamentDiameterStored(175);
        }
        applyColorToUi(OpenSpoolCodec.normalizeColor(activity.getMaterialColor()));
    }

    @Override
    public void readTag(Tag tag) {
        if (tag == null) {
            activity.showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
            return;
        }
        executorService.execute(() -> {
            try {
                NdefMimeRecord[] records = NdefTransport.readMimeRecords(tag);
                FilamentTagData data = codec.tryDecode(records);
                if (data == null) {
                    activity.showToast(R.string.unknown_or_empty_tag, Toast.LENGTH_SHORT);
                    return;
                }
                mainHandler.post(() -> applyToUi(data));
                Utils.playBeep();
                activity.showToast(R.string.data_read_from_tag, Toast.LENGTH_SHORT);
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
        if (!activity.syncTagSettingsFromUi()) {
            return;
        }
        FilamentTagData data = collectFromUi();
        executorService.execute(() -> {
            try {
                int ntagType = Utils.getNtagType(android.nfc.tech.NfcA.get(tag));
                if (brand == PrinterBrand.OPEN_SPOOL && ntagType < 215) {
                    activity.showToast(R.string.openspool_ntag_required, Toast.LENGTH_LONG);
                    return;
                }
                byte[] payload = codec.encode(data, ntagType);
                NdefTransport.writeMimeRecord(tag, codec.getMimeType(), payload);
                Utils.playBeep();
                activity.showToast(R.string.data_written_to_tag, Toast.LENGTH_SHORT);
            } catch (Exception e) {
                activity.showToast(R.string.error_writing_to_tag, Toast.LENGTH_SHORT);
            }
        });
    }

    @Override
    public void openAddDialog(boolean edit) {
        activity.showToast(R.string.open_standard_no_presets, Toast.LENGTH_SHORT);
    }

    @Override
    public void confirmDeleteFilament() {
        activity.showToast(R.string.open_standard_no_presets, Toast.LENGTH_SHORT);
    }

    private FilamentTagData collectFromUi() {
        FilamentTagData data = new FilamentTagData();
        data.brand = customBrand;
        data.materialType = getSelected(typeAdapter, main.type.getSelectedItemPosition(), "PLA");
        String modifier = getSelected(modifierAdapter, main.subtype.getSelectedItemPosition(), "");
        data.materialModifier = modifier == null ? "" : modifier.trim();
        data.colorHex = OpenSpoolCodec.normalizeColor(activity.getMaterialColor());
        data.nozzleMin = activity.getExtMin();
        data.nozzleMax = activity.getExtMax();
        data.bedMin = activity.getBedMin();
        data.bedMax = activity.getBedMax();
        data.diameterMicrons = activity.getFilamentDiameterStored() * 10;
        data.weightGrams = Utils.GetMaterialIntWeight(activity.getMaterialWeight());
        return data;
    }

    private void applyToUi(FilamentTagData data) {
        selectBrand(data.brand);
        selectSpinner(typeAdapter, main.type, data.materialType);
        selectSpinner(modifierAdapter, main.subtype, data.materialModifier);
        activity.setTagTemps(data.nozzleMin, data.nozzleMax, data.bedMin, data.bedMax);
        if (data.diameterMicrons > 0) {
            activity.setFilamentDiameterStored(data.diameterMicrons / 10);
        }
        String weight = Utils.GetMaterialWeight(Math.max(0, data.weightGrams));
        int weightIndex = activity.getWeightAdapter().getPosition(weight);
        if (weightIndex >= 0) {
            main.spoolsize.setSelection(weightIndex);
        }
        applyColorToUi(data.colorHex);
        activity.applyOpenMaterial(data.materialType, data.materialModifier,
                data.nozzleMin, data.nozzleMax, data.bedMin, data.bedMax);
    }

    private void applyColorToUi(String colorHex) {
        String color = OpenSpoolCodec.normalizeColor(colorHex);
        activity.setMaterialColor(color);
        main.txtcolor.setText(color);
        main.colorview.setBackgroundColor(Color.parseColor("#" + color));
        main.txtcolor.setTextColor(Utils.getContrastColor(Color.parseColor("#" + color)));
    }

    private void selectBrand(String brandName) {
        if (brandName == null || brandName.trim().isEmpty()) {
            customBrand = "Generic";
            main.vendor.setSelection(0);
            return;
        }
        int index = brandAdapter.getPosition(brandName);
        if (index >= 0) {
            customBrand = brandName;
            main.vendor.setSelection(index);
        } else {
            customBrand = brandName;
            int customIndex = brandAdapter.getPosition("Custom");
            if (customIndex >= 0) {
                main.vendor.setSelection(customIndex);
            }
        }
    }

    private void selectSpinner(ArrayAdapter<String> adapter, android.widget.Spinner spinner,
                               String value) {
        if (value == null || value.isEmpty()) {
            spinner.setSelection(0);
            return;
        }
        int index = adapter.getPosition(value);
        spinner.setSelection(index >= 0 ? index : 0);
    }

    private String getSelected(ArrayAdapter<String> adapter, int position, String fallback) {
        String value = adapter.getItem(position);
        return value == null ? fallback : value;
    }

    private void promptCustomBrand() {
        activity.promptTextInput(activity.getString(R.string.brand), customBrand, value -> {
            if (value != null && !value.trim().isEmpty()) {
                customBrand = value.trim();
            }
        });
    }
}