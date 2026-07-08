package dngsoftware.elgrfid;

import static dngsoftware.elgrfid.Utils.GetSetting;
import static dngsoftware.elgrfid.Utils.SaveSetting;
import static dngsoftware.elgrfid.Utils.bytesToHex;
import static dngsoftware.elgrfid.Utils.getContrastColor;

import android.graphics.Color;
import android.nfc.Tag;
import android.os.Handler;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import dngsoftware.elgrfid.databinding.ActivityMainBinding;

public class CrealityController implements FilamentPresetController, TagOperations {

    private final MainActivity activity;
    private final ActivityMainBinding main;
    private final Handler mainHandler;
    private final ExecutorService executorService;

    private FilamentDao matDb;
    private ArrayAdapter<String> brandAdapter;
    private ArrayAdapter<String> materialAdapter;
    private ArrayAdapter<String> printerAdapter;
    private final List<CrealityMaterialItem> currentMaterials = new ArrayList<>();

    private String printerType = "k2";
    private String materialId = "01001";
    private String materialColor = "FFFFFF";
    private boolean encrypted;
    private boolean suppressPrinterEvents;
    private boolean suppressVendorEvents;
    private boolean adaptersReady;

    public CrealityController(MainActivity activity, ActivityMainBinding main,
                              Handler mainHandler, ExecutorService executorService) {
        this.activity = activity;
        this.main = main;
        this.mainHandler = mainHandler;
        this.executorService = executorService;
    }

    public void initialize() {
        printerType = GetSetting(activity, "creality_printer", "k2").toLowerCase(Locale.US);
        loadCatalogAsync("", (brands, materials) -> finishInitialize(brands, materials));
    }

    public void configureUi() {
        int visible = View.VISIBLE;
        int gone = View.GONE;

        main.bambuActions.setVisibility(gone);
        main.buttonContainer.setVisibility(visible);

        main.vendor.setVisibility(visible);
        main.vendorborder.setVisibility(visible);
        main.lblvendorMain.setVisibility(visible);
        main.lblvendorMain.setText(R.string.brand);
        main.vendorTapTarget.setVisibility(gone);

        main.material.setVisibility(visible);
        main.materialborder.setVisibility(visible);
        main.lblmaterial.setVisibility(visible);

        main.type.setVisibility(visible);
        main.typeborder.setVisibility(visible);
        main.lbltype.setVisibility(visible);
        main.lbltype.setText(R.string.printer_model);

        main.subtype.setVisibility(gone);
        main.subtypeborder.setVisibility(gone);
        main.lblsubtype.setVisibility(gone);
        main.infotext.setVisibility(gone);

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
        main.otherSettingsHeader.setVisibility(visible);
        main.spoolsize.setVisibility(visible);
        main.sizeborder.setVisibility(visible);
        main.lblsize.setVisibility(visible);
        main.lbldiameter.setVisibility(gone);
        main.diameterborder.setVisibility(gone);
        main.diameter.setVisibility(gone);
        main.lblproddate.setVisibility(gone);
        main.proddateborder.setVisibility(gone);
        main.proddate.setVisibility(gone);

        applyColorToUi(materialColor);
    }

    private void finishInitialize(@NonNull String[] brands, @NonNull List<CrealityMaterialItem> materials) {
        brandAdapter = newStringAdapter(brands);
        materialAdapter = newStringAdapter(toMaterialNames(materials));
        printerAdapter = newStringAdapter(CrealityUtils.PRINTER_TYPES);

        suppressVendorEvents = true;
        main.vendor.setAdapter(brandAdapter);
        main.material.setAdapter(materialAdapter);
        main.type.setAdapter(printerAdapter);
        if (brandAdapter.getCount() > 0) {
            main.vendor.setSelection(0, false);
        }
        suppressVendorEvents = false;

        main.vendor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressVendorEvents) {
                    return;
                }
                requestMaterialsForBrand(getSelectedBrand());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        main.type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressPrinterEvents) {
                    return;
                }
                String selected = printerAdapter.getItem(position);
                if (selected != null) {
                    printerType = selected.toLowerCase(Locale.US);
                    SaveSetting(activity, "creality_printer", printerType);
                    String selectedBrand = getSelectedBrand();
                    loadCatalogAsync(selectedBrand, (brands, materials) -> {
                        refreshBrandsOnMain(brands);
                        applyMaterialList(materials);
                    });
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        main.material.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CrealityMaterialItem item = getMaterialAt(position);
                if (item != null) {
                    materialId = item.getMaterialId();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        suppressPrinterEvents = true;
        int printerIndex = getPrinterIndex(printerType);
        main.type.setSelection(printerIndex >= 0 ? printerIndex : 0);
        suppressPrinterEvents = false;

        applyMaterialList(materials);
        adaptersReady = true;
    }

    private interface CatalogLoadedCallback {
        void onLoaded(@NonNull String[] brands, @NonNull List<CrealityMaterialItem> materials);
    }

    private void loadCatalogAsync(@NonNull String selectedBrand, @NonNull CatalogLoadedCallback callback) {
        executorService.execute(() -> {
            loadDatabaseOnWorker();
            String[] brands = queryBrandsOnWorker();
            String brand = selectedBrand;
            if (brand.isEmpty() && brands.length > 0) {
                brand = brands[0];
            }
            List<CrealityMaterialItem> materials = queryMaterialsOnWorker(brand);
            mainHandler.post(() -> callback.onLoaded(brands, materials));
        });
    }

    private void requestMaterialsForBrand(@NonNull String brand) {
        executorService.execute(() -> {
            List<CrealityMaterialItem> materials = queryMaterialsOnWorker(brand);
            mainHandler.post(() -> applyMaterialList(materials));
        });
    }

    private void refreshBrandsOnMain(@NonNull String[] brands) {
        if (brandAdapter == null) {
            return;
        }
        suppressVendorEvents = true;
        brandAdapter.clear();
        brandAdapter.addAll(brands);
        suppressVendorEvents = false;
        if (!getSelectedBrand().isEmpty()) {
            requestMaterialsForBrand(getSelectedBrand());
        }
    }

    private void loadDatabaseOnWorker() {
        FilamentDatabase db = FilamentDatabase.getInstance(activity,
                CrealityUtils.dbKeyForPrinter(printerType));
        matDb = db.filamentDao();
        if (matDb.getItemCount() == 0) {
            CrealityUtils.populateDatabase(activity, matDb, printerType);
        }
    }

    @NonNull
    private String[] queryBrandsOnWorker() {
        if (matDb == null) {
            return new String[0];
        }
        return CrealityUtils.getMaterialBrands(matDb);
    }

    @NonNull
    private List<CrealityMaterialItem> queryMaterialsOnWorker(@NonNull String brand) {
        List<CrealityMaterialItem> items = new ArrayList<>();
        if (matDb == null || brand.isEmpty()) {
            return items;
        }
        for (DbFilament filament : matDb.getFilamentsByVendor(brand)) {
            items.add(new CrealityMaterialItem(filament.filamentName, filament.filamentID));
        }
        return items;
    }

    private void applyMaterialList(@NonNull List<CrealityMaterialItem> items) {
        currentMaterials.clear();
        currentMaterials.addAll(items);
        if (materialAdapter == null) {
            return;
        }
        materialAdapter.clear();
        materialAdapter.addAll(toMaterialNames(items));
        int index = getMaterialIndex(materialId);
        if (index >= 0 && index < materialAdapter.getCount()) {
            main.material.setSelection(index);
        } else if (!items.isEmpty()) {
            main.material.setSelection(0);
            materialId = items.get(0).getMaterialId();
        }
    }

    @NonNull
    private ArrayAdapter<String> newStringAdapter(@NonNull String[] items) {
        List<String> list = new ArrayList<>(items.length);
        Collections.addAll(list, items);
        return new ArrayAdapter<>(activity, R.layout.spinner_item, list);
    }

    @NonNull
    private ArrayAdapter<String> newStringAdapter(@NonNull List<String> items) {
        return new ArrayAdapter<>(activity, R.layout.spinner_item, new ArrayList<>(items));
    }

    @NonNull
    private static List<String> toMaterialNames(@NonNull List<CrealityMaterialItem> items) {
        List<String> names = new ArrayList<>(items.size());
        for (CrealityMaterialItem item : items) {
            names.add(item.getMaterialName());
        }
        return names;
    }

    private CrealityMaterialItem getMaterialAt(int position) {
        if (position < 0 || position >= currentMaterials.size()) {
            return null;
        }
        return currentMaterials.get(position);
    }

    private boolean ensureReady() {
        if (!adaptersReady || matDb == null) {
            activity.showToast(R.string.creality_loading, Toast.LENGTH_SHORT);
            return false;
        }
        return true;
    }

    public void onTagScanned(@NonNull Tag tag) {
        if (!ensureReady()) {
            return;
        }
        byte[] uid = tag.getId();
        if (uid.length > 4) {
            activity.showToast(R.string.creality_uid_required, Toast.LENGTH_SHORT);
            return;
        }
        encrypted = MifareClassicTransport.isCrealityEncrypted(tag, uid);
        mainHandler.post(() -> updateTagIdDisplay(tag, uid));
    }

    @Override
    public void readTag(Tag tag) {
        if (tag == null) {
            activity.showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
            return;
        }
        if (!ensureReady()) {
            return;
        }
        byte[] uid = tag.getId();
        if (uid.length > 4) {
            activity.showToast(R.string.creality_uid_required, Toast.LENGTH_SHORT);
            return;
        }
        executorService.execute(() -> {
            try {
                encrypted = MifareClassicTransport.isCrealityEncrypted(tag, uid);
                String tagData = MifareClassicTransport.readCrealityPayload(tag, uid, encrypted);
                CrealityCodec.Parsed parsed = CrealityCodec.parse(tagData);
                if (parsed == null) {
                    activity.showToast(R.string.unknown_or_empty_tag, Toast.LENGTH_SHORT);
                    return;
                }
                boolean printerChanged = false;
                if (parsed.printerType != null && !parsed.printerType.isEmpty()) {
                    String newType = parsed.printerType.toLowerCase(Locale.US);
                    if (!newType.equals(printerType)) {
                        printerType = newType;
                        SaveSetting(activity, "creality_printer", printerType);
                        loadDatabaseOnWorker();
                        printerChanged = true;
                    }
                }
                DbFilament filament = matDb.getFilamentById(parsed.materialId);
                if (filament == null) {
                    activity.showToast(R.string.unknown_or_empty_tag, Toast.LENGTH_SHORT);
                    return;
                }
                final boolean refreshCatalog = printerChanged;
                final String vendor = filament.filamentVendor;
                final String[] brands = refreshCatalog ? queryBrandsOnWorker() : null;
                final List<CrealityMaterialItem> materials = queryMaterialsOnWorker(vendor);
                mainHandler.post(() -> {
                    if (refreshCatalog && brands != null) {
                        refreshBrandsOnMain(brands);
                        int printerIndex = getPrinterIndex(printerType);
                        if (printerIndex >= 0) {
                            suppressPrinterEvents = true;
                            main.type.setSelection(printerIndex);
                            suppressPrinterEvents = false;
                        }
                    }
                    applyParsedTag(parsed, filament, materials);
                    updateTagIdDisplay(tag, uid);
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
        if (!ensureReady()) {
            return;
        }
        byte[] uid = tag.getId();
        if (uid.length > 4) {
            activity.showToast(R.string.creality_uid_required, Toast.LENGTH_SHORT);
            return;
        }
        String lengthCode = CrealityUtils.getMaterialLengthCode(activity.getMaterialWeight());
        String payload = CrealityCodec.buildPayload(materialId, materialColor, lengthCode, printerType);
        executorService.execute(() -> {
            try {
                encrypted = MifareClassicTransport.isCrealityEncrypted(tag, uid);
                MifareClassicTransport.writeCrealityPayload(tag, uid, payload, encrypted);
                if (!encrypted) {
                    encrypted = true;
                }
                mainHandler.post(() -> updateTagIdDisplay(tag, uid));
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
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogTheme);
        SpannableString titleText = new SpannableString(activity.getString(R.string.format_tag));
        titleText.setSpan(new ForegroundColorSpan(
                ContextCompat.getColor(activity, R.color.primary_brand)), 0, titleText.length(), 0);
        builder.setTitle(titleText);
        builder.setMessage(R.string.this_will_erase_the_data_on_the_tag_and_format_it_for_writing);
        builder.setPositiveButton(R.string.format, (dialog, which) -> {
            byte[] uid = tag.getId();
            executorService.execute(() -> {
                try {
                    MifareClassicTransport.formatCrealityTag(tag, uid, encrypted);
                    encrypted = false;
                    mainHandler.post(() -> {
                        updateTagIdDisplay(tag, uid);
                        Utils.playBeep();
                        activity.showToast(R.string.tag_formatted, Toast.LENGTH_SHORT);
                    });
                } catch (Exception e) {
                    activity.showToast(R.string.failed_to_format_tag_for_writing, Toast.LENGTH_SHORT);
                }
            });
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    public void readTagMemory() {
        activity.showToast(R.string.mifare_memory_coming_soon, Toast.LENGTH_SHORT);
    }

    @Override
    public void openAddDialog(boolean edit) {
        activity.showToast(R.string.creality_no_custom_filaments, Toast.LENGTH_SHORT);
    }

    @Override
    public void confirmDeleteFilament() {
        activity.showToast(R.string.creality_no_custom_filaments, Toast.LENGTH_SHORT);
    }

    private void applyParsedTag(@NonNull CrealityCodec.Parsed parsed, @NonNull DbFilament filament,
                                @NonNull List<CrealityMaterialItem> materials) {
        materialId = parsed.materialId;
        materialColor = parsed.colorHex;
        int brandIndex = brandAdapter.getPosition(filament.filamentVendor);
        if (brandIndex >= 0) {
            suppressVendorEvents = true;
            main.vendor.setSelection(brandIndex);
            suppressVendorEvents = false;
        }
        applyMaterialList(materials);
        int materialIndex = getMaterialIndex(materialId);
        if (materialIndex >= 0) {
            main.material.setSelection(materialIndex);
        }
        String weight = CrealityUtils.getMaterialWeightFromCode(parsed.lengthCode);
        int weightIndex = activity.getWeightAdapter().getPosition(weight);
        if (weightIndex >= 0) {
            main.spoolsize.setSelection(weightIndex);
        }
        applyColorToUi(materialColor);
    }

    private void applyColorToUi(String colorHex) {
        String color = colorHex == null ? "FFFFFF" : colorHex.replace("#", "");
        if (color.length() > 6) {
            color = color.substring(color.length() - 6);
        }
        materialColor = color;
        activity.setMaterialColor(color);
        main.txtcolor.setText(color);
        main.colorview.setBackgroundColor(Color.parseColor("#" + color));
        main.txtcolor.setTextColor(getContrastColor(Color.parseColor("#" + color)));
    }

    private void updateTagIdDisplay(@NonNull Tag tag, @NonNull byte[] uid) {
        String uidHex = bytesToHex(uid, true);
        if (encrypted) {
            main.tagid.setText(String.format(Locale.US, "\uD83D\uDD10 %s", uidHex));
        } else {
            main.tagid.setText(uidHex);
        }
    }

    @NonNull
    private String getSelectedBrand() {
        if (brandAdapter == null || brandAdapter.getCount() == 0) {
            return "";
        }
        int position = main.vendor.getSelectedItemPosition();
        if (position < 0 || position >= brandAdapter.getCount()) {
            position = 0;
        }
        String brand = brandAdapter.getItem(position);
        return brand == null ? "" : brand;
    }

    private int getMaterialIndex(String id) {
        for (int i = 0; i < currentMaterials.size(); i++) {
            CrealityMaterialItem item = currentMaterials.get(i);
            if (item != null && Objects.equals(item.getMaterialId(), id)) {
                return i;
            }
        }
        return -1;
    }

    private int getPrinterIndex(String type) {
        for (int i = 0; i < CrealityUtils.PRINTER_TYPES.length; i++) {
            if (CrealityUtils.PRINTER_TYPES[i].equalsIgnoreCase(type)) {
                return i;
            }
        }
        return 0;
    }
}