package dngsoftware.elgrfid;

import static dngsoftware.elgrfid.AnycubicUtils.combineArrays;
import static dngsoftware.elgrfid.Utils.hexToByte;
import static dngsoftware.elgrfid.Utils.playBeep;
import static dngsoftware.elgrfid.Utils.subArray;
import static dngsoftware.elgrfid.Utils.transceive;
import static dngsoftware.elgrfid.Utils.writeTagPage;

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
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import dngsoftware.elgrfid.databinding.ActivityMainBinding;
import dngsoftware.elgrfid.databinding.AddDialogBinding;

public class AnycubicController {

    private final MainActivity activity;
    private final ActivityMainBinding main;
    private final Handler mainHandler;
    private final ExecutorService executorService;

    private FilamentDao matDb;
    private ArrayAdapter<String> materialAdapter;
    private Dialog addDialog;
    private boolean userSelect;

    String materialName = "PLA";
    String materialColor = "FF0000FF";

    public AnycubicController(MainActivity activity, ActivityMainBinding main,
                                Handler mainHandler, ExecutorService executorService) {
        this.activity = activity;
        this.main = main;
        this.mainHandler = mainHandler;
        this.executorService = executorService;
    }

    public void initialize() {
        FilamentDatabase db = FilamentDatabase.getInstance(activity);
        matDb = db.filamentDao();
        if (matDb.getItemCount() == 0) {
            AnycubicUtils.populateDatabase(matDb);
        }
        setupMaterialSpinner();
        main.addbutton.setOnClickListener(v -> openAddDialog(false));
        main.editbutton.setOnClickListener(v -> openAddDialog(true));
        main.deletebutton.setOnClickListener(v -> confirmDeleteFilament());
        loadMaterials(false);
    }

    public void configureUi() {
        int visible = View.VISIBLE;
        int gone = View.GONE;

        main.material.setVisibility(visible);
        main.materialborder.setVisibility(visible);
        main.lblmaterial.setVisibility(visible);
        main.infotext.setVisibility(visible);
        main.addbutton.setVisibility(visible);

        main.type.setVisibility(gone);
        main.typeborder.setVisibility(gone);
        main.lbltype.setVisibility(gone);
        main.subtype.setVisibility(gone);
        main.subtypeborder.setVisibility(gone);
        main.lblsubtype.setVisibility(gone);
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
        main.otherSettingsContent.setVisibility(gone);

        main.txtcolor.setText(materialColor);
        main.colorview.setBackgroundColor(Color.parseColor("#" + materialColor));
        main.txtcolor.setTextColor(Utils.getContrastColor(Color.parseColor("#" + materialColor)));
    }

    private void setupMaterialSpinner() {
        materialAdapter = new ArrayAdapter<>(activity, R.layout.spinner_item,
                AnycubicUtils.getAllMaterials(matDb));
        main.material.setAdapter(materialAdapter);
        main.material.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                materialName = materialAdapter.getItem(position);
                updateInfoText();
                String vendor = new String(AnycubicUtils.getBrand(matDb, materialName),
                        StandardCharsets.UTF_8).trim();
                if ("AC".equalsIgnoreCase(vendor)) {
                    main.editbutton.setVisibility(View.INVISIBLE);
                    main.deletebutton.setVisibility(View.INVISIBLE);
                } else {
                    main.editbutton.setVisibility(View.VISIBLE);
                    main.deletebutton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void updateInfoText() {
        int[] temps = AnycubicUtils.getTemps(matDb, materialName);
        main.infotext.setText(String.format(Locale.getDefault(),
                activity.getString(R.string.info_temps),
                temps[0], temps[1], temps[2], temps[3]));
    }

    public void loadMaterials(boolean select) {
        materialAdapter = new ArrayAdapter<>(activity, R.layout.spinner_item,
                AnycubicUtils.getAllMaterials(matDb));
        main.material.setAdapter(materialAdapter);
        if (select) {
            int index = materialAdapter.getPosition(materialName);
            if (index >= 0) {
                main.material.setSelection(index);
            }
        } else {
            int plaIndex = materialAdapter.getPosition("PLA");
            main.material.setSelection(plaIndex >= 0 ? plaIndex : 0);
        }
        updateInfoText();
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
                byte[] data = new byte[144];
                ByteBuffer buff = ByteBuffer.wrap(data);
                for (int page = 4; page <= 36; page += 4) {
                    byte[] pageData = transceive(nfcA, new byte[]{(byte) 0x30, (byte) page});
                    if (pageData != null) {
                        buff.put(pageData);
                    }
                }
                if (buff.array()[0] != 0x00) {
                    mainHandler.post(() -> {
                        materialName = new String(subArray(buff.array(), 44, 16),
                                StandardCharsets.UTF_8).trim();
                        int index = materialAdapter.getPosition(materialName);
                        if (index >= 0) {
                            main.material.setSelection(index);
                        }
                        String color = AnycubicUtils.parseColorBytes(subArray(buff.array(), 65, 3));
                        String alpha = Utils.bytesToHex(subArray(buff.array(), 64, 1), false);
                        if ("010101".equals(color)) {
                            color = "000000";
                        }
                        materialColor = alpha + color;
                        main.colorview.setBackgroundColor(Color.parseColor("#" + materialColor));
                        main.txtcolor.setText(materialColor);
                        main.txtcolor.setTextColor(
                                Utils.getContrastColor(Color.parseColor("#" + materialColor)));
                        int extMin = AnycubicUtils.parseNumber(subArray(buff.array(), 80, 2));
                        int extMax = AnycubicUtils.parseNumber(subArray(buff.array(), 82, 2));
                        int bedMin = AnycubicUtils.parseNumber(subArray(buff.array(), 100, 2));
                        int bedMax = AnycubicUtils.parseNumber(subArray(buff.array(), 102, 2));
                        main.infotext.setText(String.format(Locale.getDefault(),
                                activity.getString(R.string.info_temps),
                                extMin, extMax, bedMin, bedMax));
                        String weight = AnycubicUtils.getMaterialWeightLabel(
                                AnycubicUtils.parseNumber(subArray(buff.array(), 106, 2)));
                        int weightIndex = activity.getWeightAdapter().getPosition(weight);
                        if (weightIndex >= 0) {
                            main.spoolsize.setSelection(weightIndex);
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
        if (tag == null) {
            activity.showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
            return;
        }
        String weight = activity.getMaterialWeight();
        executorService.execute(() -> {
            NfcA nfcA = NfcA.get(tag);
            if (nfcA == null) {
                activity.showToast(R.string.invalid_tag_type, Toast.LENGTH_SHORT);
                return;
            }
            try {
                nfcA.connect();
                writeTagPage(nfcA, 4, new byte[]{123, 0, 101, 0});
                for (int i = 0; i < 5; i++) {
                    writeTagPage(nfcA, 5 + i,
                            subArray(AnycubicUtils.getSku(matDb, materialName), i * 4, 4));
                }
                for (int i = 0; i < 5; i++) {
                    writeTagPage(nfcA, 10 + i,
                            subArray(AnycubicUtils.getBrand(matDb, materialName), i * 4, 4));
                }
                byte[] matData = new byte[20];
                java.util.Arrays.fill(matData, (byte) 0);
                System.arraycopy(materialName.getBytes(StandardCharsets.UTF_8), 0, matData, 0,
                        Math.min(20, materialName.getBytes(StandardCharsets.UTF_8).length));
                writeTagPage(nfcA, 15, subArray(matData, 0, 4));
                writeTagPage(nfcA, 16, subArray(matData, 4, 4));
                writeTagPage(nfcA, 17, subArray(matData, 8, 4));
                writeTagPage(nfcA, 18, subArray(matData, 12, 4));

                String color = materialColor.substring(2);
                String alpha = materialColor.substring(0, 2);
                if ("000000".equals(color)) {
                    color = "010101";
                }
                writeTagPage(nfcA, 20, combineArrays(hexToByte(alpha),
                        AnycubicUtils.parseColorHex(color)));

                int[] temps = AnycubicUtils.getTemps(matDb, materialName);
                byte[] extTemp = new byte[4];
                System.arraycopy(AnycubicUtils.numToBytes(temps[0]), 0, extTemp, 0, 2);
                System.arraycopy(AnycubicUtils.numToBytes(temps[1]), 0, extTemp, 2, 2);
                writeTagPage(nfcA, 24, extTemp);

                byte[] bedTemp = new byte[4];
                System.arraycopy(AnycubicUtils.numToBytes(temps[2]), 0, bedTemp, 0, 2);
                System.arraycopy(AnycubicUtils.numToBytes(temps[3]), 0, bedTemp, 2, 2);
                writeTagPage(nfcA, 29, bedTemp);

                byte[] filData = new byte[4];
                System.arraycopy(AnycubicUtils.numToBytes(175), 0, filData, 0, 2);
                System.arraycopy(AnycubicUtils.numToBytes(
                        AnycubicUtils.getMaterialLength(weight)), 0, filData, 2, 2);
                writeTagPage(nfcA, 30, filData);
                writeTagPage(nfcA, 31, new byte[]{(byte) 232, 3, 0, 0});

                playBeep();
                activity.showToast(R.string.data_written_to_tag, Toast.LENGTH_SHORT);
            } catch (Exception e) {
                activity.showToast(R.string.error_writing_to_tag, Toast.LENGTH_SHORT);
            } finally {
                try {
                    nfcA.close();
                } catch (Exception ignored) {
                }
            }
        });
    }

    public void applySelectedColor(String hexColor) {
        materialColor = hexColor;
    }

    public String getMaterialColor() {
        return materialColor;
    }

    public boolean usesArgbColor() {
        return true;
    }

    private void confirmDeleteFilament() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        SpannableString titleText = new SpannableString(activity.getString(R.string.delete_filament));
        titleText.setSpan(new ForegroundColorSpan(
                        ContextCompat.getColor(activity, R.color.primary_brand)),
                0, titleText.length(), 0);
        builder.setTitle(titleText);
        builder.setMessage(materialName);
        builder.setPositiveButton(R.string.delete, (dialog, which) -> {
            DbFilament item = matDb.getFilamentByName(materialName);
            if (item != null) {
                matDb.deleteItem(item);
                loadMaterials(false);
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void openAddDialog(boolean edit) {
        addDialog = new Dialog(activity, R.style.Theme_ElgRFID);
        addDialog.setCanceledOnTouchOutside(false);
        AddDialogBinding dl = AddDialogBinding.inflate(activity.getLayoutInflater());
        addDialog.setContentView(dl.getRoot());

        dl.btncls.setOnClickListener(v -> addDialog.dismiss());
        dl.btnsave.setText(edit ? R.string.save : R.string.add);
        dl.lbltitle.setText(edit ? R.string.edit_filament : R.string.add_filament);

        ArrayAdapter<String> vendorAdapter = new ArrayAdapter<>(activity,
                R.layout.spinner_item, AnycubicUtils.FILAMENT_VENDORS);
        dl.vendor.setAdapter(vendorAdapter);
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(activity,
                R.layout.spinner_item, AnycubicUtils.FILAMENT_TYPES);
        dl.type.setAdapter(typeAdapter);

        dl.chkvendor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                dl.layoutVendor.setVisibility(View.VISIBLE);
                dl.vendorborder.setVisibility(View.INVISIBLE);
                dl.lblvendor.setVisibility(View.INVISIBLE);
                dl.vendor.setVisibility(View.INVISIBLE);
            } else {
                dl.layoutVendor.setVisibility(View.INVISIBLE);
                dl.vendorborder.setVisibility(View.VISIBLE);
                dl.lblvendor.setVisibility(View.VISIBLE);
                dl.vendor.setVisibility(View.VISIBLE);
            }
        });

        dl.type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (userSelect) {
                    int[] temps = AnycubicUtils.getDefaultTemps(typeAdapter.getItem(position));
                    dl.txtextmin.setText(String.valueOf(temps[0]));
                    dl.txtextmax.setText(String.valueOf(temps[1]));
                    dl.txtbedmin.setText(String.valueOf(temps[2]));
                    dl.txtbedmax.setText(String.valueOf(temps[3]));
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

        if (edit) {
            int[] temps = AnycubicUtils.getTemps(matDb, materialName);
            dl.txtextmin.setText(String.valueOf(temps[0]));
            dl.txtextmax.setText(String.valueOf(temps[1]));
            dl.txtbedmin.setText(String.valueOf(temps[2]));
            dl.txtbedmax.setText(String.valueOf(temps[3]));
        } else {
            dl.vendor.setSelection(0);
            dl.type.setSelection(6);
            int[] temps = AnycubicUtils.getDefaultTemps("PLA");
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
            String vendor = dl.vendor.getSelectedItem().toString();
            if (dl.chkvendor.isChecked()) {
                vendor = Objects.requireNonNull(dl.txtvendor.getText()).toString().trim();
            }
            String type = dl.type.getSelectedItem().toString();
            String serial = dl.txtserial.getText().toString().trim();
            String params = String.format("%s|%s|%s|%s",
                    dl.txtextmin.getText(), dl.txtextmax.getText(),
                    dl.txtbedmin.getText(), dl.txtbedmax.getText());
            String newName = String.format("%s %s %s", vendor.trim(), type, serial);

            if (edit) {
                DbFilament current = matDb.getFilamentByName(materialName);
                if (current != null) {
                    int position = current.position;
                    matDb.deleteItem(current);
                    DbFilament updated = new DbFilament();
                    updated.position = position;
                    updated.filamentID = "";
                    updated.filamentName = newName;
                    updated.filamentVendor = "";
                    updated.filamentParam = params;
                    matDb.addItem(updated);
                    materialName = newName;
                    loadMaterials(true);
                }
            } else {
                DbFilament filament = new DbFilament();
                filament.position = matDb.getItemCount();
                filament.filamentID = "";
                filament.filamentName = newName;
                filament.filamentVendor = "";
                filament.filamentParam = params;
                matDb.addItem(filament);
                materialName = newName;
                loadMaterials(true);
            }
            addDialog.dismiss();
        });

        addDialog.show();
    }
}