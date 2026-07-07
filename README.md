# SpoolTag

A universal NFC spool tag programmer based on [DnG-Crafts/ELG-RFID](https://github.com/DnG-Crafts/ELG-RFID) and [DnG-Crafts/ACE-RFID](https://github.com/DnG-Crafts/ACE-RFID).

Choose your printer brand on startup, then read and write NTAG filament tags using the correct format for that printer.

## Supported Brands

- **Elegoo** — Canvas / Centauri tags (ELG format)
- **Anycubic** — ACE / Kobra tags (ACE format)

More brands can be added as their tag formats are integrated.

## Features

### Elegoo
- Manual nozzle and bed temperature ranges
- Custom filament diameter and production date
- Scroll-wheel temperature pickers (5°C steps, 0–400°C)

### Anycubic
- Material presets with extruder/bed temps from DnG's ACE database
- Add, edit, and delete custom filament profiles
- ARGB color format and spool length encoding

### App
- **SpoolTag** — package ID `dngsoftware.spooltag`
- Brand selection screen on launch; change brand anytime from the drawer

## Requirements

The tags required are [NTAG213](https://www.nxp.com/products/NTAG213_215_216) or [NTAG215](https://www.nxp.com/products/NTAG213_215_216).

The Canvas is programmed to read 45 pages (0 to 44) of the tag to verify filament data. Because this data spans a 144-byte range, you must use a tag that supports at least that many pages. Although Elegoo uses the NTAG213 as standard, the NTAG215 and NTAG216 work perfectly as well since they offer even more storage while maintaining the same page structure.

## Download

Get the latest APK from the [Releases](https://github.com/sophysdad/ELG-RFID/releases/latest) page.

## Building the App

1. Install [Android Studio](https://developer.android.com/studio) with Android SDK 36
2. Clone this repository
3. Create `Android/ELGRFID/local.properties` with your SDK path:

   ```
   sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
   ```

4. Build from the `Android/ELGRFID` directory:

   ```bash
   ./gradlew assembleDebug
   ```

   The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Original App

The upstream Android app is also available on Google Play:

<a href="https://play.google.com/store/apps/details?id=dngsoftware.elgrfid&hl=en"><img src=https://github.com/DnG-Crafts/ELG-RFID/blob/main/docs/gp.webp width="30%" height="30%"></a>

[![https://www.youtube.com/watch?v=bgkRoVAXhig](https://img.youtube.com/vi/bgkRoVAXhig/0.jpg)](https://www.youtube.com/watch?v=bgkRoVAXhig)

https://www.youtube.com/watch?v=bgkRoVAXhig

## Tag Format

You can view a full tag dump [here](https://github.com/DnG-Crafts/ELG-RFID/blob/main/docs/README.md).

### URI Section

This data is only for smartphone compatibility. It allows a phone to open the Elegoo website when tapped against the spool, but the printer itself does not use this information to identify the filament. The tag does not need this data present on the tag for the printer to read the filament section and identify the filament.

| Page (Dec) | Byte 0 | Byte 1 | Byte 2 | Byte 3 | Field Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **4** | 01 | 03 | A0 | 0C | NDEF Structure |
| **5** | 34 | 03 | 0F | D1 | 03 NDEF Message; 0F length (15 bytes) |
| **6** | 01 | 0B | 55 | 04 | 55 Record Type "U" (URI); 04 URI Prefix (https://) |
| **7** | 65 | 6C | 65 | 67 | ASCII for "**eleg**" |
| **8** | 6F | 6F | 2E | 63 | ASCII for "**oo.c**" |
| **9** | 6F | 6D | FE | 00 | ASCII for "**om**"; FE Terminator (TLV) |
| **10** | 00 | 00 | 00 | 00 | Reserved For URI |
| **11** | 00 | 00 | 00 | 00 | Reserved For URI |
| **12** | 00 | 00 | 00 | 00 | Reserved For URI |
| **13** | 00 | 00 | 00 | 00 | Reserved For URI |
| **14** | 00 | 00 | 00 | 00 | Reserved For URI |
| **15** | 00 | 00 | 00 | 00 | Reserved For URI |

### Filament Section

This data is what the printer uses to identify the filament.

| Page (Dec) | Byte 0 | Byte 1 | Byte 2 | Byte 3 | Field Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **16** | 36 | EE | EE | EE | Header |
| **17** | EE | 00 | 00 | 00 | Manufacturer ID |
| **18** | 00 | 80 | 76 | 65 | Material Type (PLA) |
| **19** | 00 | 04 | 00 | 00 | Material Position (PLA-CF) |
| **20** | FF | 37 | 00 | FF | Color Code (#FF3700) |
| **21** | 00 | D2 | 00 | F0 | Nozzle Temp Min / Max (210 / 240) |
| **22** | 00 | 00 | 00 | 00 | Bed Temp Min / Max |
| **23** | 00 | AF | 03 | E8 | Diameter / Weight (175 / 1000) |
| **24** | 00 | 36 | C8 | 00 | Production Date (YYMM hex) |

## Credits

Based on [ELG RFID](https://github.com/DnG-Crafts/ELG-RFID) by DnG-Crafts.

## License

This fork inherits the license of the upstream project. See the original repository for details.