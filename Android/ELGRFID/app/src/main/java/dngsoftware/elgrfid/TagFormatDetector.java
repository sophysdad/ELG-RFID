package dngsoftware.elgrfid;

import android.nfc.Tag;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public final class TagFormatDetector {

    private TagFormatDetector() {
    }

    @Nullable
    public static PrinterBrand detect(Tag tag) {
        if (tag == null) {
            return null;
        }
        PrinterBrand classic = detectClassic(tag);
        if (classic != null) {
            return classic;
        }
        return detectNdef(tag);
    }

    @Nullable
    private static PrinterBrand detectClassic(Tag tag) {
        if (!MifareClassicTransport.isClassic(tag)) {
            return null;
        }
        byte[] qidiBlock = MifareClassicTransport.readQidiPayload(tag);
        if (qidiBlock != null && MifareClassicTransport.looksLikeQidi(qidiBlock)) {
            return PrinterBrand.QIDI;
        }
        byte[] uid = tag.getId();
        if (uid.length > 4) {
            return null;
        }
        boolean encrypted = MifareClassicTransport.isCrealityEncrypted(tag, uid);
        String payload = MifareClassicTransport.readCrealityPayload(tag, uid, encrypted);
        if (CrealityCodec.isCrealityPayload(payload)) {
            return PrinterBrand.CREALITY;
        }
        if (uid.length == 4 && BambuUtils.isBambuTag(tag)) {
            return PrinterBrand.BAMBU;
        }
        return null;
    }

    @Nullable
    private static PrinterBrand detectNdef(Tag tag) {
        try {
            NdefMimeRecord[] records = NdefTransport.readMimeRecords(tag);
            if (records == null || records.length == 0) {
                return null;
            }
            NdefMimeRecord openTag = NdefMimeRecord.find(records, OpenTag3DCodec.MIME_TYPE);
            if (openTag != null) {
                return PrinterBrand.OPEN_TAG3D;
            }
            NdefMimeRecord json = NdefMimeRecord.find(records, OpenSpoolCodec.MIME_TYPE);
            if (json != null) {
                try {
                    JSONObject object = new JSONObject(
                            new String(json.payload, StandardCharsets.UTF_8));
                    if (OpenSpoolCodec.isOpenSpool(object)) {
                        return PrinterBrand.OPEN_SPOOL;
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}