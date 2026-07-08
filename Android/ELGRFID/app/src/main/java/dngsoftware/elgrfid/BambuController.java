package dngsoftware.elgrfid;

import android.nfc.Tag;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.util.concurrent.ExecutorService;

import dngsoftware.elgrfid.databinding.ActivityMainBinding;

public class BambuController implements FilamentPresetController, TagOperations {

    private final MainActivity activity;
    private final ActivityMainBinding main;
    private final Handler mainHandler;
    private final ExecutorService executorService;

    @Nullable
    private BambuPayload lastPayload;
    private boolean clonePending;

    public BambuController(MainActivity activity, ActivityMainBinding main,
                           Handler mainHandler, ExecutorService executorService) {
        this.activity = activity;
        this.main = main;
        this.mainHandler = mainHandler;
        this.executorService = executorService;
    }

    public void initialize() {
        main.bambuReadButton.setOnClickListener(v -> readTag(activity.getCurrentTag()));
        main.bambuCloneButton.setOnClickListener(v -> promptClone());
    }

    public void configureUi() {
        int visible = View.VISIBLE;
        int gone = View.GONE;

        main.bambuActions.setVisibility(visible);
        main.buttonContainer.setVisibility(gone);

        main.vendor.setVisibility(gone);
        main.vendorborder.setVisibility(gone);
        main.lblvendorMain.setVisibility(gone);
        main.vendorTapTarget.setVisibility(gone);

        main.material.setVisibility(gone);
        main.materialborder.setVisibility(gone);
        main.lblmaterial.setVisibility(gone);

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
        main.spoolsize.setVisibility(gone);
        main.sizeborder.setVisibility(gone);
        main.lblsize.setVisibility(gone);
        main.lbldiameter.setVisibility(gone);
        main.diameterborder.setVisibility(gone);
        main.diameter.setVisibility(gone);
        main.lblproddate.setVisibility(gone);
        main.proddateborder.setVisibility(gone);
        main.proddate.setVisibility(gone);
        main.lbltagtype.setVisibility(gone);
        main.tagtype.setVisibility(gone);
        main.lbltagid.setVisibility(gone);
        main.tagid.setVisibility(gone);

        activity.setColorPickerVisible(false);
        main.infotext.setVisibility(visible);
        main.infotext.setText(R.string.bambu_scan_prompt);
        updateCloneButtonState();
    }

    public void onTagScanned(@NonNull Tag tag) {
        if (clonePending) {
            cloneTag(tag);
        }
    }

    @Override
    public void readTag(Tag tag) {
        if (tag == null) {
            activity.showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
            return;
        }
        executorService.execute(() -> readTagInternal(tag, true));
    }

    @Override
    public void writeTag(Tag tag) {
        activity.showToast(R.string.bambu_read_only, Toast.LENGTH_LONG);
    }

    public void promptClone() {
        if (lastPayload == null || lastPayload.rawDump == null) {
            activity.showToast(R.string.bambu_clone_needs_read, Toast.LENGTH_SHORT);
            return;
        }
        new AlertDialog.Builder(activity, R.style.AlertDialogTheme)
                .setTitle(R.string.bambu_clone_tag)
                .setMessage(R.string.bambu_clone_warning)
                .setPositiveButton(R.string.clone_tag, (dialog, which) -> {
                    clonePending = true;
                    activity.showToast(R.string.bambu_clone_scan_blank, Toast.LENGTH_LONG);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    public void cancelClone() {
        clonePending = false;
    }

    public boolean isClonePending() {
        return clonePending;
    }

    public void readTagMemory() {
        if (lastPayload == null || lastPayload.rawDump == null) {
            activity.showToast(R.string.bambu_clone_needs_read, Toast.LENGTH_SHORT);
            return;
        }
        activity.showBambuMemory(lastPayload.rawDump);
    }

    @Override
    public void openAddDialog(boolean edit) {
        activity.showToast(R.string.bambu_read_only, Toast.LENGTH_SHORT);
    }

    @Override
    public void confirmDeleteFilament() {
        activity.showToast(R.string.bambu_read_only, Toast.LENGTH_SHORT);
    }

    private void readTagInternal(@NonNull Tag tag, boolean showSuccessToast) {
        try {
            byte[] dump = BambuUtils.readDump(tag);
            BambuPayload payload = BambuCodec.decode(dump);
            if (payload == null) {
                activity.showToast(R.string.unknown_or_empty_tag, Toast.LENGTH_SHORT);
                return;
            }
            lastPayload = payload;
            mainHandler.post(() -> {
                applyToUi(payload);
                Utils.playBeep();
                if (showSuccessToast) {
                    activity.showToast(R.string.data_read_from_tag, Toast.LENGTH_SHORT);
                }
            });
        } catch (Exception e) {
            activity.showToast(R.string.error_reading_tag, Toast.LENGTH_SHORT);
        }
    }

    private void cloneTag(@NonNull Tag tag) {
        if (lastPayload == null || lastPayload.rawDump == null) {
            clonePending = false;
            activity.showToast(R.string.bambu_clone_needs_read, Toast.LENGTH_SHORT);
            return;
        }
        byte[] dump = lastPayload.rawDump;
        executorService.execute(() -> {
            try {
                BambuUtils.writeClone(tag, dump);
                clonePending = false;
                Utils.playBeep();
                activity.showToast(R.string.bambu_clone_success, Toast.LENGTH_LONG);
            } catch (Exception e) {
                clonePending = false;
                activity.showToast(R.string.bambu_clone_failed, Toast.LENGTH_LONG);
            }
        });
    }

    private void applyToUi(@NonNull BambuPayload payload) {
        main.infotext.setText(activity.getString(
                R.string.bambu_info,
                payload.getDisplayMaterial(),
                payload.colorName == null ? "Unknown" : payload.colorName,
                payload.trayUid == null ? "" : payload.trayUid,
                payload.nozzleMin,
                payload.nozzleMax,
                payload.bedTemp,
                payload.weightGrams > 0 ? payload.weightGrams : 0,
                payload.diameterMm > 0 ? payload.diameterMm : 1.75f,
                payload.materialId == null ? "" : payload.materialId,
                payload.variantId == null ? "" : payload.variantId));
        updateCloneButtonState();
    }

    private void updateCloneButtonState() {
        boolean canClone = lastPayload != null && lastPayload.rawDump != null;
        main.bambuCloneButton.setEnabled(canClone);
        main.bambuCloneButton.setAlpha(canClone ? 1f : 0.45f);
    }

    public void formatTag(Tag tag) {
        activity.showToast(R.string.bambu_read_only, Toast.LENGTH_SHORT);
    }
}