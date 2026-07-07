package dngsoftware.elgrfid;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.List;

import dngsoftware.elgrfid.databinding.ActivityMainBinding;

public class FilamentPickerHelper {

    public interface SelectionListener {
        void onFilamentSelected(DbFilament filament);
    }

    private final MainActivity activity;
    private final ActivityMainBinding main;
    private final PrinterBrand brand;
    private final FilamentDao matDb;
    private final FilamentPresetController actions;
    private final SelectionListener selectionListener;

    private ArrayAdapter<String> vendorAdapter;
    private ArrayAdapter<String> typeAdapter;
    private ArrayAdapter<String> subtypeAdapter;
    private boolean suppressEvents;
    private DbFilament selectedFilament;
    private PopupWindow vendorPopup;

    public FilamentPickerHelper(MainActivity activity, ActivityMainBinding main, PrinterBrand brand,
                                FilamentDao matDb, FilamentPresetController actions,
                                SelectionListener selectionListener) {
        this.activity = activity;
        this.main = main;
        this.brand = brand;
        this.matDb = matDb;
        this.actions = actions;
        this.selectionListener = selectionListener;
    }

    public void setup() {
        main.filamentActions.setVisibility(View.GONE);
        main.editbutton.setVisibility(View.GONE);
        main.deletebutton.setVisibility(View.GONE);

        main.vendor.setClickable(false);
        main.vendor.setFocusable(false);
        main.vendorTapTarget.setOnClickListener(v -> showVendorPickerPopup());

        main.vendor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressEvents) {
                    return;
                }
                reloadTypes();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        main.type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressEvents) {
                    return;
                }
                reloadSubtypes();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        main.subtype.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressEvents) {
                    return;
                }
                applySubtypeSelection();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        reloadVendors();
        reloadTypes();
    }

    public DbFilament getSelectedFilament() {
        return selectedFilament;
    }

    public String getSelectedVendor() {
        return getSpinnerValue(main.vendor, vendorAdapter);
    }

    public void selectFilament(DbFilament filament) {
        if (filament == null) {
            return;
        }
        suppressEvents = true;
        String vendor = FilamentCatalog.displayVendor(filament.filamentVendor, brand);
        String type = FilamentCatalog.getType(filament);
        String subtypeLabel = FilamentCatalog.getSubtypeLabel(filament, brand);

        reloadVendors();
        setSpinnerSelection(main.vendor, vendorAdapter, vendor);
        reloadTypes();
        setSpinnerSelection(main.type, typeAdapter, type);
        reloadSubtypes();
        setSpinnerSelection(main.subtype, subtypeAdapter, subtypeLabel);
        selectedFilament = filament;
        suppressEvents = false;
        selectionListener.onFilamentSelected(filament);
    }

    public void reloadAll() {
        suppressEvents = true;
        reloadVendors();
        reloadTypes();
        reloadSubtypes();
        applySubtypeSelection();
        suppressEvents = false;
    }

    public void dismissVendorPopup() {
        if (vendorPopup != null && vendorPopup.isShowing()) {
            vendorPopup.dismiss();
        }
    }

    private void showVendorPickerPopup() {
        dismissVendorPopup();

        View popupView = LayoutInflater.from(activity).inflate(R.layout.vendor_picker_popup, null);
        ListView vendorList = popupView.findViewById(R.id.vendor_list);
        View addButton = popupView.findViewById(R.id.vendor_add_button);
        Button selectButton = popupView.findViewById(R.id.vendor_select_button);
        Button editButton = popupView.findViewById(R.id.vendor_edit_button);
        Button removeButton = popupView.findViewById(R.id.vendor_remove_button);

        List<String> vendors = FilamentCatalog.getVendors(matDb, brand);
        VendorRadioAdapter listAdapter = new VendorRadioAdapter(vendors);
        vendorList.setAdapter(listAdapter);

        int currentIndex = vendorAdapter != null
                ? vendorAdapter.getPosition(getSelectedVendor()) : -1;
        if (currentIndex < 0 && !vendors.isEmpty()) {
            currentIndex = 0;
        }
        listAdapter.setSelectedIndex(currentIndex);
        if (currentIndex >= 0) {
            vendorList.setSelection(currentIndex);
        }

        float density = activity.getResources().getDisplayMetrics().density;
        int itemHeight = Math.round(48 * density);
        int maxListHeight = itemHeight * 6;
        int listHeight = Math.min(itemHeight * vendors.size() + Math.round(8 * density),
                maxListHeight);
        ViewGroup.LayoutParams listParams = vendorList.getLayoutParams();
        listParams.height = listHeight;
        vendorList.setLayoutParams(listParams);

        vendorList.setOnItemClickListener((parent, view, position, id) ->
                listAdapter.setSelectedIndex(position));

        boolean canModifyFilament = selectedFilament != null && !isPreset(selectedFilament);
        editButton.setEnabled(canModifyFilament);
        removeButton.setEnabled(canModifyFilament);
        float disabledAlpha = 0.35f;
        editButton.setAlpha(canModifyFilament ? 1f : disabledAlpha);
        removeButton.setAlpha(canModifyFilament ? 1f : disabledAlpha);

        selectButton.setOnClickListener(v -> {
            int selectedIndex = listAdapter.getSelectedIndex();
            if (selectedIndex >= 0) {
                applyVendorSelection(selectedIndex);
            }
            dismissVendorPopup();
        });

        addButton.setOnClickListener(v -> {
            int selectedIndex = listAdapter.getSelectedIndex();
            if (selectedIndex >= 0) {
                applyVendorSelection(selectedIndex);
            }
            dismissVendorPopup();
            actions.openAddDialog(false);
        });

        editButton.setOnClickListener(v -> {
            if (!canModifyFilament) {
                activity.showToast(R.string.cannot_edit_preset, android.widget.Toast.LENGTH_SHORT);
                return;
            }
            dismissVendorPopup();
            actions.openAddDialog(true);
        });

        removeButton.setOnClickListener(v -> {
            if (!canModifyFilament) {
                activity.showToast(R.string.cannot_delete_preset, android.widget.Toast.LENGTH_SHORT);
                return;
            }
            dismissVendorPopup();
            actions.confirmDeleteFilament();
        });

        int popupWidth = main.vendor.getWidth() > 0
                ? main.vendor.getWidth()
                : Math.round(280 * density);
        vendorPopup = new PopupWindow(popupView, popupWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        vendorPopup.setOutsideTouchable(true);
        vendorPopup.setBackgroundDrawable(
                ContextCompat.getDrawable(activity, R.drawable.vendor_picker_popup_bg));
        vendorPopup.setElevation(12f);
        vendorPopup.showAsDropDown(main.vendor, 0, 0);
    }

    private void applyVendorSelection(int index) {
        if (vendorAdapter == null || index < 0 || index >= vendorAdapter.getCount()) {
            return;
        }
        suppressEvents = true;
        main.vendor.setSelection(index, false);
        suppressEvents = false;
        reloadTypes();
    }

    private void reloadVendors() {
        String previousVendor = getSelectedVendor();
        vendorAdapter = new ArrayAdapter<>(activity, R.layout.spinner_item,
                FilamentCatalog.getVendors(matDb, brand));
        main.vendor.setAdapter(vendorAdapter);

        if (previousVendor != null && vendorAdapter.getPosition(previousVendor) >= 0) {
            setSpinnerSelection(main.vendor, vendorAdapter, previousVendor);
        } else {
            setSpinnerSelection(main.vendor, vendorAdapter,
                    FilamentCatalog.getDefaultVendor(brand));
        }
    }

    private void reloadTypes() {
        String vendor = getSelectedVendor();
        typeAdapter = new ArrayAdapter<>(activity, R.layout.spinner_item,
                FilamentCatalog.getTypes(matDb, brand, vendor));
        main.type.setAdapter(typeAdapter);
        if (typeAdapter.getCount() > 0) {
            main.type.setSelection(0, false);
            reloadSubtypes(typeAdapter.getItem(0));
        } else {
            reloadSubtypes(null);
        }
    }

    private void reloadSubtypes() {
        reloadSubtypes(getSelectedType());
    }

    private void reloadSubtypes(String type) {
        String vendor = getSelectedVendor();
        if (type == null || type.isEmpty()) {
            subtypeAdapter = new ArrayAdapter<>(activity, R.layout.spinner_item, new String[0]);
            main.subtype.setAdapter(subtypeAdapter);
            selectedFilament = null;
            return;
        }
        java.util.List<DbFilament> entries =
                FilamentCatalog.getSubtypes(matDb, brand, vendor, type);
        String[] labels = new String[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            labels[i] = FilamentCatalog.getSubtypeLabel(entries.get(i), brand);
        }
        subtypeAdapter = new ArrayAdapter<>(activity, R.layout.spinner_item, labels);
        main.subtype.setAdapter(subtypeAdapter);
        if (subtypeAdapter.getCount() > 0) {
            main.subtype.setSelection(0, false);
        }
        applySubtypeSelection();
    }

    private void applySubtypeSelection() {
        String vendor = getSelectedVendor();
        String type = getSelectedType();
        String subtype = getSelectedSubtype();
        if (type == null || subtype == null || subtypeAdapter == null
                || subtypeAdapter.getCount() == 0) {
            selectedFilament = null;
            return;
        }
        selectedFilament = FilamentCatalog.findSubtype(matDb, brand, vendor, type, subtype);
        if (!suppressEvents && selectedFilament != null) {
            selectionListener.onFilamentSelected(selectedFilament);
        }
    }

    private boolean isPreset(DbFilament item) {
        if (item == null) {
            return true;
        }
        if (brand == PrinterBrand.ELEGOO) {
            return ElegooUtils.isPreset(item);
        }
        return AnycubicUtils.isPreset(item);
    }

    private String getSelectedType() {
        return getSpinnerValue(main.type, typeAdapter);
    }

    private String getSelectedSubtype() {
        return getSpinnerValue(main.subtype, subtypeAdapter);
    }

    private static String getSpinnerValue(android.widget.Spinner spinner,
                                          ArrayAdapter<String> adapter) {
        if (adapter == null || adapter.getCount() == 0) {
            return null;
        }
        int position = spinner.getSelectedItemPosition();
        if (position >= 0 && position < adapter.getCount()) {
            return adapter.getItem(position);
        }
        return adapter.getItem(0);
    }

    private static void setSpinnerSelection(android.widget.Spinner spinner,
                                             ArrayAdapter<String> adapter, String value) {
        if (adapter == null || value == null) {
            return;
        }
        int index = adapter.getPosition(value);
        if (index >= 0) {
            spinner.setSelection(index, false);
        }
    }

    private class VendorRadioAdapter extends ArrayAdapter<String> {

        private int selectedIndex = -1;

        VendorRadioAdapter(List<String> vendors) {
            super(activity, R.layout.vendor_picker_item, vendors);
        }

        void setSelectedIndex(int index) {
            selectedIndex = index;
            notifyDataSetChanged();
        }

        int getSelectedIndex() {
            return selectedIndex;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = LayoutInflater.from(getContext())
                        .inflate(R.layout.vendor_picker_item, parent, false);
            }
            RadioButton radio = row.findViewById(R.id.vendor_radio);
            TextView name = row.findViewById(R.id.vendor_name);
            String vendor = getItem(position);
            name.setText(vendor);
            radio.setChecked(position == selectedIndex);
            return row;
        }
    }
}