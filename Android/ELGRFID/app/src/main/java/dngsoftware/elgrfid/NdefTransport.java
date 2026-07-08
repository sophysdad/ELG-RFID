package dngsoftware.elgrfid;

import static dngsoftware.elgrfid.Utils.transceive;
import static dngsoftware.elgrfid.Utils.writeTagPage;

import android.nfc.Tag;
import android.nfc.tech.NfcA;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads and writes MIME NDEF records on NTAG Type 2 tags via NfcA.
 */
public final class NdefTransport {

    private NdefTransport() {
    }

    @Nullable
    public static NdefMimeRecord[] readMimeRecords(Tag tag) throws Exception {
        NfcA nfcA = NfcA.get(tag);
        if (nfcA == null) {
            return null;
        }
        try {
            nfcA.connect();
            int maxPage = Math.min(Utils.getNtagType(nfcA) == 100 ? 36 : 230, 230);
            ByteArrayOutputStream memory = new ByteArrayOutputStream();
            for (int page = 4; page <= maxPage; page += 4) {
                byte[] pageData = transceive(nfcA, new byte[]{(byte) 0x30, (byte) page});
                if (pageData == null || pageData.length < 16) {
                    break;
                }
                memory.write(pageData, 0, 16);
            }
            byte[] user = memory.toByteArray();
            byte[] ndefMessage = extractNdefMessage(user);
            if (ndefMessage == null || ndefMessage.length == 0) {
                return new NdefMimeRecord[0];
            }
            return parseNdefMessage(ndefMessage);
        } finally {
            try {
                nfcA.close();
            } catch (Exception ignored) {
            }
        }
    }

    public static void writeMimeRecord(Tag tag, @NonNull String mimeType, @NonNull byte[] payload)
            throws Exception {
        NfcA nfcA = NfcA.get(tag);
        if (nfcA == null) {
            throw new IllegalStateException("Tag is not NfcA");
        }
        byte[] ndefMessage = buildMimeNdefMessage(mimeType, payload);
        byte[] tlv = buildType2Tlv(ndefMessage);
        try {
            nfcA.connect();
            int tagKind = Utils.getNtagType(nfcA);
            int lastPage = tagKind == 216 ? 231 : tagKind == 215 ? 133 : 39;
            for (int page = 4; page <= lastPage; page++) {
                writeTagPage(nfcA, page, new byte[4]);
            }
            for (int offset = 0; offset < tlv.length; offset += 4) {
                byte[] page = new byte[4];
                System.arraycopy(tlv, offset, page, 0, Math.min(4, tlv.length - offset));
                writeTagPage(nfcA, 4 + (offset / 4), page);
            }
        } finally {
            try {
                nfcA.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Nullable
    private static byte[] extractNdefMessage(byte[] userMemory) {
        int index = 0;
        while (index < userMemory.length) {
            int tlvType = userMemory[index] & 0xFF;
            if (tlvType == 0x00) {
                index++;
                continue;
            }
            if (tlvType == 0xFE) {
                break;
            }
            if (index + 1 >= userMemory.length) {
                break;
            }
            int length = userMemory[index + 1] & 0xFF;
            int valueStart = index + 2;
            if (length == 0xFF) {
                if (index + 3 >= userMemory.length) {
                    break;
                }
                length = ((userMemory[index + 2] & 0xFF) << 8) | (userMemory[index + 3] & 0xFF);
                valueStart = index + 4;
            }
            if (tlvType == 0x03) {
                byte[] message = new byte[length];
                System.arraycopy(userMemory, valueStart, message, 0,
                        Math.min(length, userMemory.length - valueStart));
                return message;
            }
            index = valueStart + length;
        }
        return null;
    }

    private static NdefMimeRecord[] parseNdefMessage(byte[] message) {
        List<NdefMimeRecord> records = new ArrayList<>();
        int offset = 0;
        while (offset < message.length) {
            int header = message[offset++] & 0xFF;
            if (offset >= message.length) {
                break;
            }
            int typeLength = message[offset++] & 0xFF;
            int payloadLength;
            if ((header & 0x10) != 0) {
                if (offset >= message.length) {
                    break;
                }
                payloadLength = message[offset++] & 0xFF;
            } else {
                if (offset + 3 >= message.length) {
                    break;
                }
                payloadLength = ((message[offset] & 0xFF) << 24)
                        | ((message[offset + 1] & 0xFF) << 16)
                        | ((message[offset + 2] & 0xFF) << 8)
                        | (message[offset + 3] & 0xFF);
                offset += 4;
            }
            int idLength = (header & 0x08) != 0 ? (message[offset++] & 0xFF) : 0;
            if (offset + typeLength + idLength > message.length) {
                break;
            }
            byte[] typeBytes = new byte[typeLength];
            System.arraycopy(message, offset, typeBytes, 0, typeLength);
            offset += typeLength + idLength;
            if (offset + payloadLength > message.length) {
                break;
            }
            byte[] payload = new byte[payloadLength];
            System.arraycopy(message, offset, payload, 0, payloadLength);
            offset += payloadLength;

            int tnf = header & 0x07;
            if (tnf == 0x02) {
                records.add(new NdefMimeRecord(
                        new String(typeBytes, StandardCharsets.US_ASCII), payload));
            }
            if ((header & 0x40) != 0) {
                break;
            }
        }
        return records.toArray(new NdefMimeRecord[0]);
    }

    private static byte[] buildMimeNdefMessage(String mimeType, byte[] payload) {
        byte[] typeBytes = mimeType.getBytes(StandardCharsets.US_ASCII);
        ByteArrayOutputStream record = new ByteArrayOutputStream();
        int header = 0xD2; // MB, ME, SR, MIME
        record.write(header);
        record.write(typeBytes.length);
        record.write(payload.length);
        record.write(typeBytes, 0, typeBytes.length);
        record.write(payload, 0, payload.length);
        return record.toByteArray();
    }

    private static byte[] buildType2Tlv(byte[] ndefMessage) {
        ByteArrayOutputStream tlv = new ByteArrayOutputStream();
        tlv.write(0x03);
        if (ndefMessage.length < 255) {
            tlv.write(ndefMessage.length);
        } else {
            tlv.write(0xFF);
            tlv.write((ndefMessage.length >> 8) & 0xFF);
            tlv.write(ndefMessage.length & 0xFF);
        }
        tlv.write(ndefMessage, 0, ndefMessage.length);
        tlv.write(0xFE);
        return tlv.toByteArray();
    }

    @NonNull
    public static String describeTag(@NonNull Tag tag) {
        NfcA nfcA = NfcA.get(tag);
        if (nfcA == null) {
            return "Unknown";
        }
        return String.format(Locale.US, "NTAG%d", Utils.getNtagType(nfcA));
    }
}