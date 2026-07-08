package dngsoftware.elgrfid;

import static android.view.View.TEXT_ALIGNMENT_CENTER;
import dngsoftware.elgrfid.databinding.ActivityMainBinding;
import dngsoftware.elgrfid.databinding.PickerDialogBinding;
import dngsoftware.elgrfid.databinding.TagDialogBinding;
import static dngsoftware.elgrfid.Utils.*;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.navigation.NavigationView;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback, NavigationView.OnNavigationItemSelectedListener {
    private NfcAdapter nfcAdapter;
    Tag currentTag = null;
    ArrayAdapter<String> wadapter;
    String MaterialType = "PLA", MaterialWeight = "1 KG", MaterialColor = "0000FF";
    int intType = 0, intSubtype;
    int extMin = 190, extMax = 230, bedMin = 0, bedMax = 0, filamentDiameter = 175;
    Dialog pickerDialog, tagDialog;
    AlertDialog inputDialog;
    private Handler mainHandler;
    int tagType;
    private Toast currentToast;
    int SelectedSize;
    private ActivityMainBinding main;
    Bitmap gradientBitmap;
    tagItem[] tagItems;
    private ExecutorService executorService;
    private ActivityResultLauncher<Void> cameraLauncher;
    NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private static final int PERMISSION_REQUEST_CODE = 2;
    private PickerDialogBinding colorDialog;
    tagAdapter recycleAdapter;
    RecyclerView recyclerView;
    PrinterBrand activeBrand;
    AnycubicController anycubicController;
    ElegooController elegooController;
    NdefFilamentController openSpoolController;
    NdefFilamentController openTag3DController;
    CrealityController crealityController;
    QidiController qidiController;
    BambuController bambuController;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activeBrand = BrandPreferences.getSelectedBrand(this);
        if (activeBrand == null) {
            startActivity(new Intent(this, BrandSelectionActivity.class));
            finish();
            return;
        }
        setThemeMode(GetSetting(this, "enabledm", false));
        Resources res = getApplicationContext().getResources();
        Locale locale = new Locale("en");
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        res.updateConfiguration(config, res.getDisplayMetrics());

        main = ActivityMainBinding.inflate(getLayoutInflater());
        View rv = main.getRoot();
        setContentView(rv);

        SetPermissions(this);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        setupActivityResultLaunchers();

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        MenuItem launchItem = navigationView.getMenu().findItem(R.id.nav_launch);
        SwitchCompat launchSwitch = Objects.requireNonNull(launchItem.getActionView()).findViewById(R.id.drawer_switch);
        launchSwitch.setChecked(GetSetting(this, "autoLaunch", true));
        launchSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setNfcLaunchMode(this, isChecked);
            SaveSetting(this, "autoLaunch", isChecked);
        });

        MenuItem readItem = navigationView.getMenu().findItem(R.id.nav_read);
        SwitchCompat readSwitch = Objects.requireNonNull(readItem.getActionView()).findViewById(R.id.drawer_switch);
        readSwitch.setChecked(GetSetting(this, "autoread", false));
        readSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SaveSetting(this, "autoread", isChecked);
        });

        MenuItem darkItem = navigationView.getMenu().findItem(R.id.nav_dark);
        SwitchCompat darkSwitch = Objects.requireNonNull(darkItem.getActionView()).findViewById(R.id.drawer_switch);
        darkSwitch.setChecked(GetSetting(this, "enabledm", false));
        darkSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SaveSetting(this, "enabledm", isChecked);
            setThemeMode(isChecked);
        });


        main.txtcolor.setText(MaterialColor);
        main.txtcolor.setTextColor(getContrastColor(Color.parseColor("#" + MaterialColor)));

        main.colorview.setOnClickListener(view -> openPicker());
        main.colorview.setBackgroundColor(Color.rgb(0, 0, 255));
        main.readbutton.setOnClickListener(view -> readTag(currentTag));

        main.writebutton.setOnClickListener(view -> getTagOperations().writeTag(currentTag));

        main.menubutton.setOnClickListener(view -> drawerLayout.openDrawer(GravityCompat.START));
        main.mainMenuButton.setOnClickListener(view -> openBrandSelection());

        main.colorspin.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    openPicker();
                    break;
                case MotionEvent.ACTION_UP:
                    v.performClick();
                    break;
                default:
                    break;
            }
            return false;
        });

        wadapter = new ArrayAdapter<>(this, R.layout.spinner_item, materialWeights);
        main.spoolsize.setAdapter(wadapter);
        main.spoolsize.setSelection(SelectedSize);
        main.spoolsize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                SelectedSize = main.spoolsize.getSelectedItemPosition();
                MaterialWeight = wadapter.getItem(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });


        main.diameter.setText("1.75");
        setupOtherSettingsSection();
        setupTempPickers();

        anycubicController = new AnycubicController(this, main, mainHandler, executorService);
        elegooController = new ElegooController(this, main, mainHandler, executorService);
        openSpoolController = new NdefFilamentController(this, main, mainHandler, executorService,
                PrinterBrand.OPEN_SPOOL, new OpenSpoolCodec());
        openTag3DController = new NdefFilamentController(this, main, mainHandler, executorService,
                PrinterBrand.OPEN_TAG3D, new OpenTag3DCodec());
        crealityController = new CrealityController(this, main, mainHandler, executorService);
        qidiController = new QidiController(this, main, mainHandler, executorService);
        bambuController = new BambuController(this, main, mainHandler, executorService);
        configureBrandUi();
    }

    private void configureBrandUi() {
        int labelRes = activeBrand.openStandard ? R.string.active_format : R.string.active_brand;
        main.brandChip.setText(getString(labelRes, getString(activeBrand.nameRes)));
        hideFilamentActionButtons();
        setFilamentMenuVisible(false);
        setColorPickerVisible(!activeBrand.readOnly);
        updateBrandDrawerItems();
        boolean bambuMode = activeBrand == PrinterBrand.BAMBU;
        main.bambuActions.setVisibility(bambuMode ? View.VISIBLE : View.GONE);
        main.buttonContainer.setVisibility(bambuMode ? View.GONE : View.VISIBLE);
        main.writebutton.setVisibility(activeBrand.readOnly ? View.GONE : View.VISIBLE);
        switch (activeBrand) {
            case ANYCUBIC:
                MaterialColor = anycubicController.materialColor;
                anycubicController.initialize();
                anycubicController.configureUi();
                break;
            case OPEN_SPOOL:
                openSpoolController.initialize();
                openSpoolController.configureUi();
                break;
            case OPEN_TAG3D:
                openTag3DController.initialize();
                openTag3DController.configureUi();
                break;
            case CREALITY:
                crealityController.initialize();
                crealityController.configureUi();
                break;
            case QIDI:
                qidiController.initialize();
                qidiController.configureUi();
                break;
            case BAMBU:
                bambuController.initialize();
                bambuController.configureUi();
                break;
            case ELEGOO:
            default:
                elegooController.initialize();
                elegooController.configureUi();
                break;
        }
    }

    private FilamentPresetController getFilamentController() {
        switch (activeBrand) {
            case ANYCUBIC:
                return anycubicController;
            case OPEN_SPOOL:
                return openSpoolController;
            case OPEN_TAG3D:
                return openTag3DController;
            case CREALITY:
                return crealityController;
            case QIDI:
                return qidiController;
            case BAMBU:
                return bambuController;
            case ELEGOO:
            default:
                return elegooController;
        }
    }

    private TagOperations getTagOperations() {
        switch (activeBrand) {
            case ANYCUBIC:
                return anycubicController;
            case OPEN_SPOOL:
                return openSpoolController;
            case OPEN_TAG3D:
                return openTag3DController;
            case CREALITY:
                return crealityController;
            case QIDI:
                return qidiController;
            case BAMBU:
                return bambuController;
            case ELEGOO:
            default:
                return elegooController;
        }
    }

    private void updateBrandDrawerItems() {
        MenuItem cloneItem = navigationView.getMenu().findItem(R.id.nav_clone_tag);
        MenuItem formatItem = navigationView.getMenu().findItem(R.id.nav_format);
        if (cloneItem != null) {
            cloneItem.setVisible(false);
        }
        if (formatItem != null) {
            formatItem.setVisible(!activeBrand.readOnly);
        }
    }

    private void setFilamentMenuVisible(boolean visible) {
        MenuItem add = navigationView.getMenu().findItem(R.id.nav_add_filament);
        MenuItem edit = navigationView.getMenu().findItem(R.id.nav_edit_filament);
        MenuItem delete = navigationView.getMenu().findItem(R.id.nav_delete_filament);
        if (add != null) {
            add.setVisible(visible);
        }
        if (edit != null) {
            edit.setVisible(visible);
        }
        if (delete != null) {
            delete.setVisible(visible);
        }
    }

    void hideFilamentActionButtons() {
        main.filamentActions.setVisibility(View.GONE);
        main.editbutton.setVisibility(View.GONE);
        main.deletebutton.setVisibility(View.GONE);
    }

    void setColorPickerVisible(boolean visible) {
        int state = visible ? View.VISIBLE : View.GONE;
        main.colorspin.setVisibility(state);
        main.colorborder.setVisibility(state);
        main.colorview.setVisibility(state);
        main.txtcolor.setVisibility(state);
        main.lblcolor.setVisibility(state);
    }

    void setTagTemps(int nozzleMin, int nozzleMax, int bedMinTemp, int bedMaxTemp) {
        extMin = nozzleMin;
        extMax = nozzleMax;
        bedMin = bedMinTemp;
        bedMax = bedMaxTemp;
        updateTempFieldsOnUi();
    }

    int getExtMin() {
        return extMin;
    }

    int getExtMax() {
        return extMax;
    }

    int getBedMin() {
        return bedMin;
    }

    int getBedMax() {
        return bedMax;
    }

    int getFilamentDiameterStored() {
        return filamentDiameter;
    }

    void setFilamentDiameterStored(int diameterStored) {
        filamentDiameter = diameterStored;
        main.diameter.setText(String.format(Locale.US, "%.2f", storedToDiameter(diameterStored)));
    }

    void applyElegooMaterial(String type, int subtypeId, int nozzleMin, int nozzleMax,
                               int bedMinTemp, int bedMaxTemp) {
        MaterialType = type;
        intType = ElegooUtils.typeToIndex(type);
        intSubtype = subtypeId;
        setTagTemps(nozzleMin, nozzleMax, bedMinTemp, bedMaxTemp);
    }

    void applyOpenMaterial(String type, String modifier, int nozzleMin, int nozzleMax,
                           int bedMinTemp, int bedMaxTemp) {
        MaterialType = type == null || type.isEmpty() ? "PLA" : type;
        intType = 0;
        intSubtype = 0;
        setTagTemps(nozzleMin, nozzleMax, bedMinTemp, bedMaxTemp);
    }

    public interface TextInputCallback {
        void onText(String value);
    }

    void promptTextInput(String title, String initialValue, TextInputCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setTitle(title);
        final EditText input = new EditText(this);
        input.setText(initialValue == null ? "" : initialValue);
        input.setTextColor(Color.BLACK);
        builder.setView(input);
        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            if (callback != null) {
                callback.onText(input.getText().toString().trim());
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        inputDialog = builder.create();
        inputDialog.show();
    }

    String getMaterialColor() {
        return MaterialColor;
    }

    void setMaterialColor(String color) {
        MaterialColor = color.length() == 8 ? color.substring(2) : color;
    }

    String getMaterialTypeName() {
        return MaterialType;
    }

    int getIntType() {
        return intType;
    }

    int getIntSubtype() {
        return intSubtype;
    }

    private void openBrandSelection() {
        BrandNavigation.openMainMenu(this);
    }

    ArrayAdapter<String> getWeightAdapter() {
        return wadapter;
    }

    String getMaterialWeight() {
        return MaterialWeight;
    }

    Tag getCurrentTag() {
        return currentTag;
    }

    private void setupTempPickers() {
        View.OnClickListener nozzleMinPicker = v -> showTempPicker(
                getString(R.string.nozzle_min),
                resolvePickerDefault(main.extmin.getText().toString()),
                value -> {
                    extMin = value;
                    main.extmin.setText(String.valueOf(value));
                });
        View.OnClickListener nozzleMaxPicker = v -> showTempPicker(
                getString(R.string.nozzle_max),
                resolvePickerDefault(main.extmax.getText().toString()),
                value -> {
                    extMax = value;
                    main.extmax.setText(String.valueOf(value));
                });
        View.OnClickListener bedMinPicker = v -> showTempPicker(
                getString(R.string.bed_min),
                resolvePickerDefault(main.bedmin.getText().toString()),
                value -> {
                    bedMin = value;
                    main.bedmin.setText(String.valueOf(value));
                });
        View.OnClickListener bedMaxPicker = v -> showTempPicker(
                getString(R.string.bed_max),
                resolvePickerDefault(main.bedmax.getText().toString()),
                value -> {
                    bedMax = value;
                    main.bedmax.setText(String.valueOf(value));
                });

        main.extmin.setOnClickListener(nozzleMinPicker);
        main.extmax.setOnClickListener(nozzleMaxPicker);
        main.bedmin.setOnClickListener(bedMinPicker);
        main.bedmax.setOnClickListener(bedMaxPicker);
        main.extminborder.setClickable(true);
        main.extmaxborder.setClickable(true);
        main.bedminborder.setClickable(true);
        main.bedmaxborder.setClickable(true);
        main.extminborder.setOnClickListener(nozzleMinPicker);
        main.extmaxborder.setOnClickListener(nozzleMaxPicker);
        main.bedminborder.setOnClickListener(bedMinPicker);
        main.bedmaxborder.setOnClickListener(bedMaxPicker);
    }

    private int resolvePickerDefault(String currentValue) {
        if (currentValue != null && !currentValue.trim().isEmpty()) {
            return roundTempToStep(parseTemperature(currentValue, getPickerMidpointTemp()));
        }
        return getPickerMidpointTemp();
    }

    private void showTempPicker(String title, int defaultTemp, TempPickerCallback callback) {
        View dialogView = getLayoutInflater().inflate(R.layout.temp_picker_dialog, null);
        TextView titleView = dialogView.findViewById(R.id.temp_picker_title);
        RecyclerView pickerList = dialogView.findViewById(R.id.temp_picker_list);
        View selectionBand = dialogView.findViewById(R.id.temp_picker_selection_band);
        titleView.setText(title);
        selectionBand.setOnTouchListener((v, event) -> false);

        int initialIndex = tempToPickerIndex(defaultTemp);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        pickerList.setLayoutManager(layoutManager);
        tempPickerAdapter adapter = new tempPickerAdapter(this, initialIndex);
        pickerList.setAdapter(adapter);

        LinearSnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(pickerList);

        pickerList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                    return;
                }
                int position = getSnappedPickerIndex(snapHelper, layoutManager);
                if (position < 0 || position == adapter.getSelectedPosition()) {
                    return;
                }
                int previous = adapter.getSelectedPosition();
                adapter.setSelectedPosition(position);
                adapter.notifyItemChanged(previous);
                adapter.notifyItemChanged(position);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setView(dialogView);
        builder.setPositiveButton(R.string.select, (dialog, which) -> {
            int index = getSnappedPickerIndex(snapHelper, layoutManager);
            if (index < 0) {
                index = adapter.getSelectedPosition();
            }
            callback.onTempSelected(pickerIndexToTemp(index));
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.show();
        scrollPickerToIndex(pickerList, layoutManager, initialIndex);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.background_alt);
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            positiveButton.setTextColor(Color.parseColor("#82B1FF"));
            negativeButton.setTextColor(Color.parseColor("#82B1FF"));
        }
    }

    private static int getSnappedPickerIndex(LinearSnapHelper snapHelper, LinearLayoutManager layoutManager) {
        View snapView = snapHelper.findSnapView(layoutManager);
        return snapView != null ? layoutManager.getPosition(snapView) : RecyclerView.NO_POSITION;
    }

    private void scrollPickerToIndex(RecyclerView pickerList, LinearLayoutManager layoutManager, int index) {
        pickerList.post(() -> {
            int itemHeight = getResources().getDimensionPixelSize(R.dimen.temp_picker_item_height);
            int verticalPadding = Math.max(0, (pickerList.getHeight() - itemHeight) / 2);
            pickerList.setPadding(0, verticalPadding, 0, verticalPadding);
            layoutManager.scrollToPositionWithOffset(index, verticalPadding);
            pickerList.post(() -> {
                View itemView = layoutManager.findViewByPosition(index);
                if (itemView != null) {
                    int dy = itemView.getTop() - verticalPadding;
                    if (dy != 0) {
                        pickerList.scrollBy(0, dy);
                    }
                }
            });
        });
    }

    private interface TempPickerCallback {
        void onTempSelected(int value);
    }

    private void setupOtherSettingsSection() {
        main.otherSettingsContent.setVisibility(View.GONE);
        main.otherSettingsToggleIcon.setImageResource(R.drawable.ic_arrow_down);
        main.otherSettingsHeader.setOnClickListener(v -> {
            if (main.otherSettingsContent.getVisibility() == View.VISIBLE) {
                main.otherSettingsContent.setVisibility(View.GONE);
                main.otherSettingsToggleIcon.setImageResource(R.drawable.ic_arrow_down);
            } else {
                main.otherSettingsContent.setVisibility(View.VISIBLE);
                main.otherSettingsToggleIcon.setImageResource(R.drawable.ic_arrow_up);
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        try {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this);
            if (nfcAdapter != null && nfcAdapter.isEnabled()) {
                Bundle options = new Bundle();
                options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);
                nfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A, options);
            }
        }catch (Exception ignored) {}
    }


    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (nfcAdapter != null) {
                nfcAdapter.disableReaderMode(this);
            }
        } catch (Exception ignored) {}
    }


    private void updateTempFieldsOnUi() {
        main.extmin.setText(String.valueOf(extMin));
        main.extmax.setText(String.valueOf(extMax));
        main.bedmin.setText(bedMin > 0 ? String.valueOf(bedMin) : "");
        main.bedmax.setText(bedMax > 0 ? String.valueOf(bedMax) : "");
    }

    boolean syncTagSettingsFromUi() {
        extMin = parseTemperature(main.extmin.getText().toString(), extMin);
        extMax = parseTemperature(main.extmax.getText().toString(), extMax);
        bedMin = parseTemperature(main.bedmin.getText().toString(), 0);
        bedMax = parseTemperature(main.bedmax.getText().toString(), 0);

        if (!isValidTemperature(extMin) || !isValidTemperature(extMax)
                || !isValidTemperature(bedMin) || !isValidTemperature(bedMax)) {
            showToast(R.string.invalid_temp_range, Toast.LENGTH_SHORT);
            return false;
        }
        if (extMax < extMin) {
            showToast(R.string.invalid_temp_range, Toast.LENGTH_SHORT);
            return false;
        }
        if (bedMax > 0 && bedMin > 0 && bedMax < bedMin) {
            showToast(R.string.invalid_temp_range, Toast.LENGTH_SHORT);
            return false;
        }

        try {
            double diameterMm = Double.parseDouble(main.diameter.getText().toString().trim());
            if (!isValidDiameter(diameterMm)) {
                showToast(R.string.invalid_diameter, Toast.LENGTH_SHORT);
                return false;
            }
            filamentDiameter = diameterToStored(diameterMm);
        } catch (NumberFormatException e) {
            showToast(R.string.invalid_diameter, Toast.LENGTH_SHORT);
            return false;
        }

        String productionDate = main.proddate.getText().toString().trim();
        if (!productionDate.isEmpty() && encodeProductionDate(productionDate) == null) {
            showToast(R.string.invalid_production_date, Toast.LENGTH_SHORT);
            return false;
        }

        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            try {
                nfcAdapter.disableReaderMode(this);
            } catch (Exception ignored) {
            }
        }
        if (pickerDialog != null && pickerDialog.isShowing()) {
            pickerDialog.dismiss();
        }
        if (inputDialog != null && inputDialog.isShowing()) {
            inputDialog.dismiss();
        }
    }


    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (pickerDialog != null && pickerDialog.isShowing()) {
            pickerDialog.dismiss();
            openPicker();
        }
        if (inputDialog != null && inputDialog.isShowing()) {
            inputDialog.dismiss();
        }
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_tag_guide) {
            startActivity(new Intent(this, TagGuideActivity.class));
        } else if (id == R.id.nav_add_filament) {
            getFilamentController().openAddDialog(false);
        } else if (id == R.id.nav_edit_filament) {
            getFilamentController().openAddDialog(true);
        } else if (id == R.id.nav_delete_filament) {
            getFilamentController().confirmDeleteFilament();
        } else if (id == R.id.nav_clone_tag) {
            bambuController.promptClone();
        } else if (id == R.id.nav_format) {
            formatTag(currentTag);
        } else if (id == R.id.nav_memory) {
           loadTagMemory();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    public void onTagDiscovered(Tag tag) {
        try {
            runOnUiThread(() -> {
                byte[] uid = tag.getId();
                boolean isClassic = MifareClassicTransport.isClassic(tag);
                if (isClassic) {
                    if (!activeBrand.mifareClassic) {
                        PrinterBrand detected = TagFormatDetector.detect(tag);
                        if (detected != null && detected != activeBrand) {
                            offerDetectedFormat(detected);
                        } else {
                            showToast(R.string.mifare_classic_required, Toast.LENGTH_SHORT);
                        }
                        return;
                    }
                    currentTag = tag;
                    tagType = 0;
                    showToast(getString(R.string.tag_found) + bytesToHex(uid, false), Toast.LENGTH_SHORT);
                    main.tagtype.setText(MifareClassicTransport.describeTag(currentTag));
                    main.lbltagid.setVisibility(View.VISIBLE);
                    main.lbltagtype.setVisibility(View.VISIBLE);
                    main.tagid.setVisibility(View.VISIBLE);
                    main.tagtype.setVisibility(View.VISIBLE);
                    if (activeBrand == PrinterBrand.CREALITY) {
                        crealityController.onTagScanned(tag);
                    } else if (activeBrand == PrinterBrand.BAMBU) {
                        main.tagid.setText(bytesToHex(uid, true));
                        bambuController.onTagScanned(tag);
                        if (bambuController.isClonePending()) {
                            return;
                        }
                    } else {
                        main.tagid.setText(bytesToHex(uid, true));
                    }
                } else if (uid.length >= 6) {
                    if (activeBrand.mifareClassic) {
                        showToast(R.string.ntag_required, Toast.LENGTH_SHORT);
                        return;
                    }
                    currentTag = tag;
                    tagType = getTagType(NfcA.get(currentTag));
                    showToast(getString(R.string.tag_found) + bytesToHex(uid, false), Toast.LENGTH_SHORT);
                    int ntagType = getNtagType(NfcA.get(currentTag));
                    main.tagid.setText(bytesToHex(uid, true));
                    main.tagtype.setText(NdefTransport.describeTag(currentTag));
                    main.lbltagid.setVisibility(View.VISIBLE);
                    main.lbltagtype.setVisibility(View.VISIBLE);
                    main.tagid.setVisibility(View.VISIBLE);
                    main.tagtype.setVisibility(View.VISIBLE);
                } else {
                    currentTag = null;
                    main.tagid.setText("");
                    main.tagtype.setText("");
                    main.lbltagid.setVisibility(View.GONE);
                    main.lbltagtype.setVisibility(View.GONE);
                    main.tagid.setVisibility(View.GONE);
                    main.tagtype.setVisibility(View.GONE);
                    showToast(R.string.invalid_tag_type, Toast.LENGTH_SHORT);
                    return;
                }
                PrinterBrand detected = TagFormatDetector.detect(currentTag);
                if (detected != null && detected != activeBrand) {
                    offerDetectedFormat(detected);
                } else if (GetSetting(this, "autoread", false)) {
                    readTag(currentTag);
                }
            });
        } catch (Exception ignored) {
        }
    }


    public void readTag(Tag tag) {
        getTagOperations().readTag(tag);
    }

    private void offerDetectedFormat(PrinterBrand detected) {
        if (detected == null || detected == activeBrand) {
            return;
        }
        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle(R.string.switch_tag_format)
                .setMessage(getString(R.string.switch_tag_format_message,
                        getString(detected.nameRes), getString(activeBrand.nameRes)))
                .setPositiveButton(R.string.switch_format, (dialog, which) -> {
                    BrandPreferences.saveSelectedBrand(this, detected);
                    activeBrand = detected;
                    configureBrandUi();
                    if (currentTag != null) {
                        readTag(currentTag);
                    }
                })
                .setNegativeButton(R.string.keep_current, (dialog, which) -> {
                    if (GetSetting(this, "autoread", false) && currentTag != null) {
                        readTag(currentTag);
                    }
                })
                .show();
    }

    void writeTag(Tag tag, String typeName, int type, int subType, String colorHex, int weightGrams) {
        try {
            NfcA nfcA = NfcA.get(tag);
            if (tag == null) {
                showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
                return;
            }
            executorService.execute(() -> {
                try {
                    nfcA.connect();
                    byte[] empty = new byte[]{0, 0, 0, 0};
                    for (int i = 4; i < 16; i++) {
                        writeTagPage(nfcA, i, empty);
                    }
                    writeTagPage(nfcA, 16, new byte[]{0x36, (byte) 0xEE, (byte) 0xEE, (byte) 0xEE});
                    writeTagPage(nfcA, 17, new byte[]{(byte) 0xEE, 0, 0, 0});
                    //type
                    writeTagPage(nfcA, 18, encodeMaterial(typeName));
                    // sub type
                    writeTagPage(nfcA, 19, new byte[]{(byte) type, (byte) subType, 0, 0});
                    // color
                    writeTagPage(nfcA, 20, hexToByte(colorHex + "FF"));
                    // nozzle min / max
                    writeTagPage(nfcA, 21, doubleBE(extMin, extMax));
                    // bed min / max
                    writeTagPage(nfcA, 22, doubleBE(bedMin, bedMax));
                    // diameter / weight
                    writeTagPage(nfcA, 23, doubleBE(filamentDiameter, weightGrams));
                    // production date (YYMM)
                    byte[] productionDate = encodeProductionDate(main.proddate.getText().toString());
                    if (productionDate == null) {
                        productionDate = new byte[]{0, 0};
                    }
                    writeTagPage(nfcA, 24, new byte[]{0, productionDate[0], productionDate[1], 0});
                    playBeep();
                    showToast(R.string.data_written_to_tag, Toast.LENGTH_SHORT);
                } catch (Exception e) {
                    showToast(R.string.error_writing_to_tag, Toast.LENGTH_SHORT);
                } finally {
                    try {
                        nfcA.close();
                    } catch (Exception ignored) {
                    }
                }
            });
        } catch (Exception ignored) {
            showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    void openPicker() {
        if (activeBrand != null && activeBrand.readOnly) {
            return;
        }
        try {
            pickerDialog = new Dialog(this, R.style.Theme_ElgRFID);
            pickerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            pickerDialog.setCanceledOnTouchOutside(false);
            pickerDialog.setTitle(R.string.pick_color);
            PickerDialogBinding dl = PickerDialogBinding.inflate(getLayoutInflater());
            View rv = dl.getRoot();
            colorDialog = dl;
            pickerDialog.setContentView(rv);
            gradientBitmap = null;
            boolean argbMode = activeBrand == PrinterBrand.ANYCUBIC
                    || activeBrand == PrinterBrand.OPEN_SPOOL
                    || activeBrand == PrinterBrand.OPEN_TAG3D;
            if (argbMode) {
                dl.alphaSliderTitle.setVisibility(View.VISIBLE);
                dl.alphaSlider.setVisibility(View.VISIBLE);
            }

            dl.btncls.setOnClickListener(v -> {
                String hex = dl.txtcolor.getText().toString().trim();
                if (isValidHexInput(hex)) {
                    try {
                        MaterialColor = argbMode
                                ? expandHexToArgb(hex)
                                : expandHexToRgb(hex);
                        int color = parseHexColor(hex);
                        main.colorview.setBackgroundColor(color);
                        main.txtcolor.setText(MaterialColor);
                        main.txtcolor.setTextColor(getContrastColor(color));
                        if (argbMode) {
                            anycubicController.applySelectedColor(MaterialColor);
                        }
                    } catch (Exception ignored) {
                    }
                }
                pickerDialog.dismiss();
            });

            int pickerColor = argbMode
                    ? Color.parseColor("#" + MaterialColor)
                    : Color.parseColor("#" + (MaterialColor.length() == 6 ? MaterialColor : "0000FF"));
            dl.redSlider.setProgress(Color.red(pickerColor));
            dl.greenSlider.setProgress(Color.green(pickerColor));
            dl.blueSlider.setProgress(Color.blue(pickerColor));
            if (argbMode) {
                dl.alphaSlider.setProgress(Color.alpha(pickerColor));
            }


            setupPresetColors(dl);
            updateColorDisplay(dl, dl.redSlider.getProgress(), dl.greenSlider.getProgress(), dl.blueSlider.getProgress());

            setupGradientPicker(dl);

            dl.gradientPickerView.setOnTouchListener((v, event) -> {
                v.performClick();
                if (gradientBitmap == null) {
                    return false;
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                    float touchX = event.getX();
                    float touchY = event.getY();
                    int pixelX = Math.max(0, Math.min(gradientBitmap.getWidth() - 1, (int) touchX));
                    int pixelY = Math.max(0, Math.min(gradientBitmap.getHeight() - 1, (int) touchY));
                    int pickedColor = gradientBitmap.getPixel(pixelX, pixelY);
                    setSlidersFromColor(dl, Color.rgb(Color.red(pickedColor), Color.green(pickedColor), Color.blue(pickedColor)));
                    return true;
                }
                return false;
            });

            setupCollapsibleSection(dl,
                    dl.rgbSlidersHeader,
                    dl.rgbSlidersContent,
                    dl.rgbSlidersToggleIcon,
                    GetSetting(this,"RGB_VIEW",false)
            );
            setupCollapsibleSection(dl,
                    dl.gradientPickerHeader,
                    dl.gradientPickerContent,
                    dl.gradientPickerToggleIcon,
                    GetSetting(this,"PICKER_VIEW",true)
            );
            setupCollapsibleSection(dl,
                    dl.presetColorsHeader,
                    dl.presetColorsContent,
                    dl.presetColorsToggleIcon,
                    GetSetting(this,"PRESET_VIEW",true)
            );
            setupCollapsibleSection(dl,
                    dl.photoColorHeader,
                    dl.photoColorContent,
                    dl.photoColorToggleIcon,
                    GetSetting(this, "PHOTO_VIEW", false)
            );

            SeekBar.OnSeekBarChangeListener rgbChangeListener = new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    updateColorDisplay(dl, dl.redSlider.getProgress(), dl.greenSlider.getProgress(), dl.blueSlider.getProgress());
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            };

            dl.redSlider.setOnSeekBarChangeListener(rgbChangeListener);
            dl.greenSlider.setOnSeekBarChangeListener(rgbChangeListener);
            dl.blueSlider.setOnSeekBarChangeListener(rgbChangeListener);
            if (argbMode) {
                dl.alphaSlider.setOnSeekBarChangeListener(rgbChangeListener);
            }

            dl.txtcolor.setOnClickListener(v -> showHexInputDialog(dl, argbMode));

            dl.photoImage.setOnClickListener(v -> {
                Drawable drawable = ContextCompat.getDrawable(dl.photoImage.getContext(), R.drawable.camera);
                if (dl.photoImage.getDrawable() != null && drawable != null) {
                    if (Objects.equals(dl.photoImage.getDrawable().getConstantState(), drawable.getConstantState())) {
                        checkPermissionsAndCapture();
                    }
                } else {
                    checkPermissionsAndCapture();
                }
            });

            dl.clearImage.setOnClickListener(v -> {

                dl.photoImage.setImageResource( R.drawable.camera);
                dl.photoImage.setDrawingCacheEnabled(false);
                dl.photoImage.buildDrawingCache(false);
                dl.photoImage.setOnTouchListener(null);
                dl.clearImage.setVisibility(View.GONE);

            });

            pickerDialog.show();
        } catch (Exception ignored) {}
    }


    private void updateColorDisplay(PickerDialogBinding dl,int currentRed,int currentGreen,int currentBlue) {
        int alpha = dl.alphaSlider.getVisibility() == View.VISIBLE
                ? dl.alphaSlider.getProgress() : 255;
        int color = Color.argb(alpha, currentRed, currentGreen, currentBlue);
        dl.colorDisplay.setBackgroundColor(color);
        String hexCode = dl.alphaSlider.getVisibility() == View.VISIBLE
                ? AnycubicUtils.rgbToHexA(currentRed, currentGreen, currentBlue, alpha)
                : rgbToHex(currentRed, currentGreen, currentBlue);
        dl.txtcolor.setText(hexCode);
        double alphaNormalized = 255.0;
        int blendedRed = (int) (currentRed * alphaNormalized + 244 * (1 - alphaNormalized));
        int blendedGreen = (int) (currentGreen * alphaNormalized + 244 * (1 - alphaNormalized));
        int blendedBlue = (int) (currentBlue * alphaNormalized + 244 * (1 - alphaNormalized));
        double brightness = (0.299 * blendedRed + 0.587 * blendedGreen + 0.114 * blendedBlue) / 255;
        if (brightness > 0.5) {
            dl.txtcolor.setTextColor(Color.BLACK);
        } else {
            dl.txtcolor.setTextColor(Color.WHITE);
        }

    }


    private void setupPresetColors(PickerDialogBinding dl) {
        dl.presetColorGrid.removeAllViews();
        for (int color : presetColors()) {
            Button colorButton = new Button(this);
            FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                    (int) getResources().getDimension(R.dimen.preset_circle_size),
                    (int) getResources().getDimension(R.dimen.preset_circle_size)
            );
            params.setMargins(
                    (int) getResources().getDimension(R.dimen.preset_circle_margin),
                    (int) getResources().getDimension(R.dimen.preset_circle_margin),
                    (int) getResources().getDimension(R.dimen.preset_circle_margin),
                    (int) getResources().getDimension(R.dimen.preset_circle_margin)
            );
            colorButton.setLayoutParams(params);
            GradientDrawable circleDrawable = (GradientDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.circle_shape, null);
            assert circleDrawable != null;
            circleDrawable.setColor(color);
            colorButton.setBackground(circleDrawable);
            colorButton.setTag(color);
            colorButton.setOnClickListener(v -> {
                int selectedColor = (int) v.getTag();
                setSlidersFromColor(dl, selectedColor);
            });
            dl.presetColorGrid.addView(colorButton);
        }
    }


    private void setSlidersFromColor(PickerDialogBinding dl, int argbColor) {
        dl.redSlider.setProgress(Color.red(argbColor));
        dl.greenSlider.setProgress(Color.green(argbColor));
        dl.blueSlider.setProgress(Color.blue(argbColor));
        if (dl.alphaSlider.getVisibility() == View.VISIBLE) {
            dl.alphaSlider.setProgress(Color.alpha(argbColor));
        }
        updateColorDisplay(dl, Color.red(argbColor), Color.green(argbColor), Color.blue(argbColor));
    }


    private void showHexInputDialog(PickerDialogBinding dl, boolean argbMode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setTitle(R.string.enter_hex_color_aarrggbb);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setHint(R.string.hex_input_hint);
        input.setTextColor(Color.BLACK);
        input.setHintTextColor(Color.GRAY);
        input.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        input.setText(argbMode
                ? AnycubicUtils.rgbToHexA(dl.redSlider.getProgress(), dl.greenSlider.getProgress(),
                dl.blueSlider.getProgress(), dl.alphaSlider.getProgress())
                : rgbToHex(dl.redSlider.getProgress(), dl.greenSlider.getProgress(), dl.blueSlider.getProgress()));
        InputFilter[] filters = new InputFilter[3];
        filters[0] = new HexInputFilter();
        filters[1] = new InputFilter.LengthFilter(8);
        filters[2] = new InputFilter.AllCaps();
        input.setFilters(filters);
        builder.setView(input);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.submit, (dialog, which) -> {
            String hexInput = input.getText().toString().trim();
            if (isValidHexInput(hexInput)) {
                setSlidersFromColor(dl, parseHexColor(hexInput));
            } else {
                showToast(R.string.invalid_hex_code_please_use_aarrggbb_format, Toast.LENGTH_LONG);
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        inputDialog = builder.create();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidthPx = displayMetrics.widthPixels;
        float density = getResources().getDisplayMetrics().density;
        int maxWidthDp = 100;
        int maxWidthPx = (int) (maxWidthDp * density);
        int dialogWidthPx = (int) (screenWidthPx * 0.80);
        if (dialogWidthPx > maxWidthPx) {
            dialogWidthPx = maxWidthPx;
        }
        Objects.requireNonNull(inputDialog.getWindow()).setLayout(dialogWidthPx, WindowManager.LayoutParams.WRAP_CONTENT);
        inputDialog.getWindow().setGravity(Gravity.CENTER); // Center the dialog on the screen
        inputDialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = inputDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = inputDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            positiveButton.setTextColor(Color.parseColor("#82B1FF"));
            negativeButton.setTextColor(Color.parseColor("#82B1FF"));
        });
        inputDialog.show();
    }


    void setupGradientPicker(PickerDialogBinding dl) {
        dl.gradientPickerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                dl.gradientPickerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int width = dl.gradientPickerView.getWidth();
                int height = dl.gradientPickerView.getHeight();
                if (width > 0 && height > 0) {
                    gradientBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(gradientBitmap);
                    Paint paint = new Paint();
                    float[] hsv = new float[3];
                    hsv[1] = 1.0f;
                    for (int y = 0; y < height; y++) {
                        hsv[2] = 1.0f - (float) y / height;
                        for (int x = 0; x < width; x++) {
                            hsv[0] = (float) x / width * 360f;
                            paint.setColor(Color.HSVToColor(255, hsv));
                            canvas.drawPoint(x, y, paint);
                        }
                    }
                    dl.gradientPickerView.setBackground(new BitmapDrawable(getResources(), gradientBitmap));
                }
            }
        });
    }


    private void setupCollapsibleSection(PickerDialogBinding dl, LinearLayout header, final ViewGroup content, final ImageView toggleIcon, boolean isExpandedInitially) {
        content.setVisibility(isExpandedInitially ? View.VISIBLE : View.GONE);
        toggleIcon.setImageResource(isExpandedInitially ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
        header.setOnClickListener(v -> {
            if (content.getVisibility() == View.VISIBLE) {
                content.setVisibility(View.GONE);
                toggleIcon.setImageResource(R.drawable.ic_arrow_down);
                if (header.getId() == dl.rgbSlidersHeader.getId()) {
                    SaveSetting(this,"RGB_VIEW",false);
                }
                else if (header.getId() == dl.gradientPickerHeader.getId()) {
                    SaveSetting(this,"PICKER_VIEW",false);
                }
                else if (header.getId() == dl.presetColorsHeader.getId()) {
                    SaveSetting(this,"PRESET_VIEW",false);
                }
                else if (header.getId() == dl.photoColorHeader.getId()) {
                    SaveSetting(this,"PHOTO_VIEW",false);
                }
            } else {
                content.setVisibility(View.VISIBLE);
                toggleIcon.setImageResource(R.drawable.ic_arrow_up);
                if (header.getId() == dl.rgbSlidersHeader.getId()) {
                    SaveSetting(this,"RGB_VIEW",true);
                }
                else if (header.getId() == dl.gradientPickerHeader.getId()) {
                    SaveSetting(this,"PICKER_VIEW",true);
                    if (gradientBitmap == null) {
                        setupGradientPicker(dl);
                    }
                }
                else if (header.getId() == dl.presetColorsHeader.getId()) {
                    SaveSetting(this,"PRESET_VIEW",true);
                }
                else if (header.getId() == dl.photoColorHeader.getId()) {
                    SaveSetting(this,"PHOTO_VIEW",true);
                }
            }
        });
    }


    private void setupActivityResultLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap != null) {
                        colorDialog.photoImage.setImageBitmap(bitmap);
                        setupPhotoPicker(colorDialog.photoImage);
                    } else {
                        // Handle failure or cancellation
                        showToast(R.string.photo_capture_cancelled_or_failed, Toast.LENGTH_SHORT);
                    }
                }
        );
    }


    void showToast(final Object content, final int duration) {
        mainHandler.post(() -> {
            if (currentToast != null) currentToast.cancel();
            if (content instanceof Integer) {
                currentToast = Toast.makeText(this, (Integer) content, duration);
            } else if (content instanceof String) {
                currentToast = Toast.makeText(this, (String) content, duration);
            } else {
                currentToast = Toast.makeText(this, String.valueOf(content), duration);
            }
            currentToast.show();
        });
    }


    private void checkPermissionsAndCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
        }
        else {
            takePicture();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePicture();
            } else {
                showToast(R.string.camera_permission_is_required_to_take_photos, Toast.LENGTH_SHORT);
            }
        }
    }


    private void takePicture() {
        if (cameraLauncher != null) {
            cameraLauncher.launch(null);
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private void setupPhotoPicker(ImageView imageView) {
        colorDialog.clearImage.setVisibility(View.VISIBLE);
        imageView.setDrawingCacheEnabled(true);
        imageView.buildDrawingCache(true);
        imageView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                Bitmap bitmap = imageView.getDrawingCache();
                float touchX = event.getX();
                float touchY = event.getY();
                if (touchX >= 0 && touchX < bitmap.getWidth() && touchY >= 0 && touchY < bitmap.getHeight()) {
                    try {
                        int pixel = bitmap.getPixel((int) touchX, (int) touchY);
                        int r = Color.red(pixel);
                        int g = Color.green(pixel);
                        int b = Color.blue(pixel);
                        colorDialog.colorDisplay.setBackgroundColor(Color.rgb(r, g, b));
                        colorDialog.txtcolor.setText(String.format("%06X", (0xFFFFFF & pixel)));
                        setSlidersFromColor(colorDialog, Color.rgb(Color.red(pixel), Color.green(pixel), Color.blue(pixel)));
                    } catch (Exception ignored) {}
                }
            }
            return true;
        });
    }


    void loadTagMemory() {
        if (activeBrand == PrinterBrand.CREALITY) {
            crealityController.readTagMemory();
            return;
        }
        if (activeBrand == PrinterBrand.QIDI) {
            qidiController.readTagMemory();
            return;
        }
        if (activeBrand == PrinterBrand.BAMBU) {
            bambuController.readTagMemory();
            return;
        }
        try {
            tagDialog = new Dialog(this, R.style.Theme_ElgRFID);
            tagDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            tagDialog.setCanceledOnTouchOutside(false);
            tagDialog.setTitle("Tag Memory");
            TagDialogBinding tdl = TagDialogBinding.inflate(getLayoutInflater());
            View rv = tdl.getRoot();
            tagDialog.setContentView(rv);
            tdl.btncls.setOnClickListener(v -> tagDialog.dismiss());
            tdl.btnread.setOnClickListener(v -> readTagMemory(tdl));
            recyclerView = tdl.recyclerView;
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            layoutManager.scrollToPosition(0);
            recyclerView.setLayoutManager(layoutManager);
            tagItems = new tagItem[0];
            recycleAdapter = new tagAdapter(this, tagItems);
            recyclerView.setAdapter(recycleAdapter);
            tagDialog.show();
            readTagMemory(tdl);
        } catch (Exception ignored) {}
    }


    void showBambuMemory(byte[] dump) {
        try {
            tagDialog = new Dialog(this, R.style.Theme_ElgRFID);
            tagDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            tagDialog.setCanceledOnTouchOutside(false);
            tagDialog.setTitle("Bambu Tag Memory");
            TagDialogBinding tdl = TagDialogBinding.inflate(getLayoutInflater());
            tagDialog.setContentView(tdl.getRoot());
            tdl.btncls.setOnClickListener(v -> tagDialog.dismiss());
            tdl.btnread.setVisibility(View.GONE);
            recyclerView = tdl.recyclerView;
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            recyclerView.setLayoutManager(layoutManager);
            int blockCount = dump.length / 16;
            tagItems = new tagItem[blockCount];
            for (int block = 0; block < blockCount; block++) {
                byte[] pageData = new byte[16];
                System.arraycopy(dump, block * 16, pageData, 0, 16);
                tagItems[block] = new tagItem();
                tagItems[block].tKey = String.format(Locale.getDefault(), "Block %d", block);
                tagItems[block].tValue = bytesToHex(pageData, true);
                if (block == 0) {
                    tagItems[block].tImage = AppCompatResources.getDrawable(this, R.drawable.locked);
                } else if (block % 4 == 3) {
                    tagItems[block].tImage = AppCompatResources.getDrawable(this, R.drawable.internal);
                } else {
                    tagItems[block].tImage = AppCompatResources.getDrawable(this, R.drawable.writable);
                }
            }
            tdl.lbldesc.setText("MIFARE Classic 1K");
            recycleAdapter = new tagAdapter(this, tagItems);
            recyclerView.setAdapter(recycleAdapter);
            tagDialog.show();
        } catch (Exception ignored) {
        }
    }

    void readTagMemory(TagDialogBinding tdl) {
        if (currentTag == null) {
            showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
            return;
        }
        executorService.execute(() -> {
            try {

                NfcA nfcA = NfcA.get(currentTag);
                if (nfcA != null) {
                    try {
                        int maxPages = (tagType == 216) ? 231 : (tagType == 215) ? 135 : 45;
                        if (tagType == 100) maxPages = 48;
                        mainHandler.post(() -> tdl.lbldesc.setText(tagType == 100 ? "UL-C" : "NTAG" + tagType));
                        tagItems = new tagItem[maxPages];
                        for (int i = 0; i < maxPages; i += 4) {
                            byte[] data = transceive(nfcA, new byte[]{0x30, (byte) i});
                            for (int offset = 0; offset < 4; offset++) {
                                int currentPage = i + offset;
                                if (currentPage >= maxPages) break;
                                byte[] pageData = new byte[4];
                                System.arraycopy(data, offset * 4, pageData, 0, 4);
                                String hexString = bytesToHex(pageData, true);
                                String definition = getPageDefinition(currentPage, tagType);
                                tagItems[currentPage] = new tagItem();
                                tagItems[currentPage].tKey = String.format(Locale.getDefault(), "Page %d | %s", currentPage, definition);
                                tagItems[currentPage].tValue = hexString;
                                if (currentPage < 2) {
                                    tagItems[currentPage].tImage = AppCompatResources.getDrawable(this, R.drawable.locked);
                                } else if (definition.contains("User Data")) {
                                    tagItems[currentPage].tImage = AppCompatResources.getDrawable(this, R.drawable.writable);
                                } else {
                                    tagItems[currentPage].tImage = AppCompatResources.getDrawable(this, R.drawable.internal);
                                }
                            }
                        }
                        mainHandler.post(() -> {
                            recycleAdapter = new tagAdapter(this, tagItems);
                            recycleAdapter.setHasStableIds(true);
                            recyclerView.setAdapter(recycleAdapter);
                        });
                    } catch (Exception ignored) {
                        showToast(R.string.error_reading_tag, Toast.LENGTH_SHORT);
                    } finally {
                        try {
                            if (nfcA.isConnected()) nfcA.close();
                        } catch (Exception ignored) {
                        }
                    }
                } else {
                    showToast(R.string.invalid_tag_type, Toast.LENGTH_SHORT);
                }

            }catch (Exception ignored) {
                showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
            }
        });
    }


    private void formatTag(Tag tag) {
        if (activeBrand == PrinterBrand.CREALITY) {
            crealityController.formatTag(tag);
            return;
        }
        if (activeBrand == PrinterBrand.QIDI) {
            qidiController.formatTag(tag);
            return;
        }
        if (activeBrand == PrinterBrand.BAMBU) {
            bambuController.formatTag(tag);
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        SpannableString titleText = new SpannableString(getString(R.string.format_tag));
        titleText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.primary_brand)), 0, titleText.length(), 0);
        SpannableString messageText = new SpannableString(getString(R.string.this_will_erase_the_data_on_the_tag_and_format_it_for_writing));
        messageText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.text_main)), 0, messageText.length(), 0);
        builder.setTitle(titleText);
        builder.setMessage(messageText);
        builder.setPositiveButton(R.string.format, (dialog, which) -> {

            if (tag == null) {
                showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
                return;
            }

            executorService.execute(() -> {
                try {
                    NfcA nfcA = NfcA.get(tag);
                    if (nfcA != null) {
                        byte[] empty = new byte[]{0, 0, 0, 0};
                        try {
                            byte[] ccBytes;
                            if (tagType == 216) {
                                ccBytes = new byte[]{(byte) 0xE1, (byte) 0x10, (byte) 0x6D, (byte) 0x00};
                            } else if (tagType == 215) {
                                ccBytes = new byte[]{(byte) 0xE1, (byte) 0x10, (byte) 0x3E, (byte) 0x00};
                            } else if (tagType == 100) {
                                ccBytes = new byte[]{(byte) 0xE1, (byte) 0x10, (byte) 0x06, (byte) 0x00};
                            } else {
                                ccBytes = new byte[]{(byte) 0xE1, (byte) 0x10, (byte) 0x12, (byte) 0x00};
                            }
                            showToast(R.string.formatting_tag, Toast.LENGTH_SHORT);

                            writeTagPage(nfcA, 2, empty);
                            writeTagPage(nfcA, 3, ccBytes);
                            for (int i = 4; i < 32; i++) {
                                writeTagPage(nfcA, i, empty);
                            }
                            showToast(R.string.tag_formatted, Toast.LENGTH_SHORT);
                            if (nfcA.isConnected()) nfcA.close();
                        } catch (Exception e) {
                            showToast(R.string.failed_to_format_tag_for_writing, Toast.LENGTH_SHORT);
                        } finally {
                            try {
                                if (nfcA.isConnected()) nfcA.close();
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
                    }
                } catch (Exception ignored) {
                    showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
                }
            });
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        AlertDialog alert = builder.create();
        alert.show();
        if (alert.getWindow() != null) {
            alert.getWindow().setBackgroundDrawableResource(R.color.background_alt);
            alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.primary_brand));
            alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.primary_brand));
        }
    }


}