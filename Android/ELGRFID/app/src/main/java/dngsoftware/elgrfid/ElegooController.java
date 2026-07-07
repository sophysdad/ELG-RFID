package dngsoftware.elgrfid;

import static dngsoftware.elgrfid.Utils.decodeMaterial;
import static dngsoftware.elgrfid.Utils.decodeProductionDate;
import static dngsoftware.elgrfid.Utils.GetMaterialWeight;
import static dngsoftware.elgrfid.Utils.storedToDiameter;
import static dngsoftware.elgrfid.Utils.subArray;
import static dngsoftware.elgrfid.Utils.transceive;

import android.app.Dialog;
import android.graphics.Color;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Handler;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import dngsoftware.elgrfid.databinding.ActivityMainBinding;
import dngsoftware.elgrfid.databinding.AddDialogBinding;

public class ElegooController implements FilamentPresetController {

    private final MainActivity activity;
    private final ActivityMainBinding main;
    private final Handler mainHandler;
    private final ExecutorService executorService;

    private FilamentDao matDb;
    private FilamentPickerHelper picker;
    private Dialog addDialog;
    private boolean userSelect;

    public ElegooController(MainActivity activity, ActivityMainBinding main,
                              Handler mainHandler, ExecutorService executorService) {
        this.activity = activity;
        this.main = main;
        this.mainHandler = mainHandler;
        this.executorService = executorService;
    }

    public void initialize() {
        FilamentDatabase db = FilamentDatabase.getInstance(activity, PrinterBrand.ELEGOO);
        matDb = db.filamentDao();
        if (matDb.getItemCount() == 0) {
            ElegooUtils.populateDatabase(matDb);
        }
        picker = new FilamentPickerHelper(activity, main, PrinterBrand.ELEGOO, matDb, this,
                this::applyFilamentSelection);
        picker.setup();
    }

    public void configureUi() {
        int visible = View.VISIBLE;
        int gone = View.GONE;

        main.vendor.setVisibility(visible);
        main.vendorborder.setVisibility(visible);
        main.lblvendorMain.setVisibility(visible);
        main.type.setVisibility(visible);
        main.typeborder.setVisibility(visible);
        main.lbltype.setVisibility(visible);
        main.subtype.setVisibility(visible);
        main.subtypeborder.setVisibility(visible);
        main.lblsubtype.setVisibility(visible);

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
        main.lblproddate.setVisibility(visible);
        main.proddateborder.setVisibility(visible);
        main.proddate.setVisibility(visible);

        String color = activity.getMaterialColor();
        if (color.length() == 8) {
            color = color.substring(2);
        }
        main.txtcolor.setText(color);
        main.colorview.setBackgroundColor(Color.parseColor("#" + color));
        main.txtcolor.setTextColor(Utils.getContrastColor(Color.parseColor("#" + color)));
        if (picker != null) {
            picker.reloadAll();
        }
        if (activity.getFilamentDiameterStored() <= 0) {
            activity.setFilamentDiameterStored(175);
        }
    }

    private void applyFilamentSelection(DbFilament filament) {
        if (filament == null) {
            return;
        }
        String type = FilamentCatalog.getType(filament);
        int subtypeId;
        try {
            subtypeId = Integer.parseInt(FilamentCatalog.getSubtypeKey(filament).trim());
        } catch (Exception ignored) {
            subtypeId = 0;
        }
        int[] temps = FilamentCatalog.getTemps(filament);
        activity.applyElegooMaterial(type, subtypeId, temps[0], temps[1], temps[2], temps[3]);
    }

    private DbFilament getSelectedFilament() {
        return picker != null ? picker.getSelectedFilament() : null;
    }

    public void readTag(Tag tag) {
        if (tag == null) {
            activity.showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
            return;
        }
        executorService.execute(() -> {
            NfcA nfcA = NfcA.get(tag);
            if (nfcA == null) {
                activity.showToast(R.string.invalid_tag_type, Toast.LENGTH_SHORT);
                return;
            }
            try {
                nfcA.connect();
                byte[] data = new byte[96];
                ByteBuffer buff = ByteBuffer.wrap(data);
                for (int page = 4; page < 28; page += 4) {
                    byte[] pageData = transceive(nfcA, new byte[]{(byte) 0x30, (byte) page});
                    if (pageData != null) {
                        buff.put(pageData);
                    }
                }
                if (buff.array()[48] == (byte) 0x36) {
                    mainHandler.post(() -> {
                        String type = decodeMaterial(subArray(buff.array(), 56, 4)).trim();
                        int subtypeId = buff.array()[61] & 0xFF;
                        String color = Utils.bytesToHex(subArray(buff.array(), 64, 3), false);
                        int extMin = ByteBuffer.wrap(subArray(buff.array(), 68, 2)).getShort();
                        int extMax = ByteBuffer.wrap(subArray(buff.array(), 70, 2)).getShort();
                        int bedMin = ByteBuffer.wrap(subArray(buff.array(), 72, 2)).getShort();
                        int bedMax = ByteBuffer.wrap(subArray(buff.array(), 74, 2)).getShort();
                        int diameter = ByteBuffer.wrap(subArray(buff.array(), 76, 2)).getShort();
                        String productionDate = decodeProductionDate(buff.array()[80], buff.array()[81]);
                        String weight = GetMaterialWeight(
                                ByteBuffer.wrap(subArray(buff.array(), 78, 2)).getShort());

                        activity.applyElegooMaterial(type, subtypeId, extMin, extMax, bedMin, bedMax);
                        activity.setMaterialColor(color);
                        main.colorview.setBackgroundColor(Color.parseColor("#" + color));
                        main.txtcolor.setText(color);
                        main.txtcolor.setTextColor(
                                Utils.getContrastColor(Color.parseColor("#" + color)));
                        if (diameter > 0) {
                            activity.setFilamentDiameterStored(diameter);
                        }
                        main.proddate.setText(productionDate);
                        int weightIndex = activity.getWeightAdapter().getPosition(weight);
                        if (weightIndex >= 0) {
                            main.spoolsize.setSelection(weightIndex);
                        }

                        DbFilament match = ElegooUtils.findByTypeAndSubtype(matDb, type, subtypeId);
                        if (match != null) {
                            picker.selectFilament(match);
                        }
                    });
                    activity.showToast(R.string.data_read_from_tag, Toast.LENGTH_SHORT);
                } else {
                    activity.showToast(R.string.unknown_or_empty_tag, Toast.LENGTH_SHORT);
                }
            } catch (Exception e) {
                activity.showToast(R.string.error_reading_tag, Toast.LENGTH_SHORT);
            } finally {
                try {
                    nfcA.close();
                } catch (Exception ignored) {
                }
            }
        });
    }

    public void writeTag(Tag tag) {
        activity.writeTag(tag, activity.getMaterialTypeName(), activity.getIntType(),
                activity.getIntSubtype(), activity.getMaterialColor(),
                Utils.GetMaterialIntWeight(activity.getMaterialWeight()));
    }

    @Override
    public void confirmDeleteFilament() {
        DbFilament item = getSelectedFilament();
        if (ElegooUtils.isPreset(item)) {
            activity.showToast(R.string.cannot_delete_preset, Toast.LENGTH_SHORT);
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        SpannableString titleText = new SpannableString(activity.getString(R.string.delete_filament));
        titleText.setSpan(new ForegroundColorSpan(
                        ContextCompat.getColor(activity, R.color.primary_brand)),
                0, titleText.length(), 0);
        builder.setTitle(titleText);
        builder.setMessage(item != null ? item.filamentName : "");
        builder.setPositiveButton(R.string.delete, (dialog, which) -> {
            if (item != null) {
                matDb.deleteItem(item);
                picker.reloadAll();
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    @Override
    public void openAddDialog(boolean edit) {
        DbFilament current = getSelectedFilament();
        if (edit && ElegooUtils.isPreset(current)) {
            activity.showToast(R.string.cannot_edit_preset, Toast.LENGTH_SHORT);
            return;
        }

        addDialog = new Dialog(activity, R.style.Theme_ElgRFID);
        addDialog.setCanceledOnTouchOutside(false);
        AddDialogBinding dl = AddDialogBinding.inflate(activity.getLayoutInflater());
        addDialog.setContentView(dl.getRoot());

        dl.btncls.setOnClickListener(v -> addDialog.dismiss());
        dl.btnsave.setText(edit ? R.string.save : R.string.add);
        dl.lbltitle.setText(edit ? R.string.edit_filament : R.string.add_filament);

        dl.chkvendor.setVisibility(View.GONE);
        dl.layoutVendor.setVisibility(View.GONE);
        dl.vendorborder.setVisibility(View.GONE);
        dl.lblvendor.setVisibility(View.GONE);
        dl.vendor.setVisibility(View.GONE);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(activity,
                R.layout.spinner_item, Utils.filamentTypes);
        dl.type.setAdapter(typeAdapter);

        dl.type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (userSelect) {
                    String type = typeAdapter.getItem(position);
                    int bedMin = Utils.getDefaultBedMin(type);
                    int bedMax = Utils.getDefaultBedMax(type);
                    int[] subtypeTemps = getDefaultSubtypeTemps(type);
                    dl.txtextmin.setText(String.valueOf(subtypeTemps[0]));
                    dl.txtextmax.setText(String.valueOf(subtypeTemps[1]));
                    dl.txtbedmin.setText(String.valueOf(bedMin));
                    dl.txtbedmax.setText(String.valueOf(bedMax));
                    userSelect = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                userSelect = false;
            }
        });
        dl.type.setOnTouchListener((v, event) -> {
            userSelect = true;
            v.performClick();
            return false;
        });

        if (edit && current != null) {
            String[] parts = ElegooUtils.parseParams(current.filamentParam);
            int typeIndex = ElegooUtils.typeToIndex(parts[0]);
            dl.type.setSelection(typeIndex);
            dl.txtserial.setText(current.filamentName);
            dl.txtextmin.setText(parts[2]);
            dl.txtextmax.setText(parts[3]);
            dl.txtbedmin.setText(parts[4]);
            dl.txtbedmax.setText(parts[5]);
        } else {
            dl.type.setSelection(0);
            int[] temps = ElegooUtils.getTemps(matDb, "PLA");
            dl.txtextmin.setText(String.valueOf(temps[0]));
            dl.txtextmax.setText(String.valueOf(temps[1]));
            dl.txtbedmin.setText(String.valueOf(temps[2]));
            dl.txtbedmax.setText(String.valueOf(temps[3]));
        }

        dl.btnsave.setOnClickListener(v -> {
            if (Objects.requireNonNull(dl.txtserial.getText()).toString().isEmpty()
                    || Objects.requireNonNull(dl.txtextmin.getText()).toString().isEmpty()
                    || Objects.requireNonNull(dl.txtextmax.getText()).toString().isEmpty()
                    || Objects.requireNonNull(dl.txtbedmin.getText()).toString().isEmpty()
                    || Objects.requireNonNull(dl.txtbedmax.getText()).toString().isEmpty()) {
                activity.showToast(R.string.fill_all_fields, Toast.LENGTH_SHORT);
                return;
            }
            String type = dl.type.getSelectedItem().toString();
            String name = dl.txtserial.getText().toString().trim();
            int subtypeId = edit && current != null
                    ? Integer.parseInt(ElegooUtils.parseParams(current.filamentParam)[1])
                    : ElegooUtils.nextCustomSubtypeId(matDb);
            String params = ElegooUtils.buildParams(type, subtypeId,
                    Integer.parseInt(dl.txtextmin.getText().toString()),
                    Integer.parseInt(dl.txtextmax.getText().toString()),
                    Integer.parseInt(dl.txtbedmin.getText().toString()),
                    Integer.parseInt(dl.txtbedmax.getText().toString()));

            String vendorCode = edit && current != null
                    ? FilamentCatalog.vendorCodeForSave(
                    FilamentCatalog.displayVendor(current.filamentVendor, PrinterBrand.ELEGOO),
                    PrinterBrand.ELEGOO)
                    : FilamentCatalog.vendorCodeForSave(picker.getSelectedVendor(),
                    PrinterBrand.ELEGOO);

            if (edit && current != null) {
                int position = current.position;
                matDb.deleteItem(current);
                DbFilament updated = new DbFilament();
                updated.position = position;
                updated.filamentID = String.valueOf(subtypeId);
                updated.filamentName = name;
                updated.filamentVendor = vendorCode;
                updated.filamentParam = params;
                matDb.addItem(updated);
                picker.selectFilament(updated);
            } else {
                DbFilament filament = new DbFilament();
                filament.position = matDb.getItemCount();
                filament.filamentID = String.valueOf(subtypeId);
                filament.filamentName = name;
                filament.filamentVendor = vendorCode;
                filament.filamentParam = params;
                matDb.addItem(filament);
                picker.selectFilament(filament);
            }
            addDialog.dismiss();
        });

        addDialog.show();
    }

    private static int[] getDefaultSubtypeTemps(String type) {
        if (!Utils.getFilamentSubTypes(type).isEmpty()) {
            Filament first = Utils.getFilamentSubTypes(type).get(0);
            return new int[]{first.minTemp, first.maxTemp};
        }
        return new int[]{190, 230};
    }
}