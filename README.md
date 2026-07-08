# SpoolTag

A universal NFC spool tag programmer based on [DnG-Crafts/ELG-RFID](https://github.com/DnG-Crafts/ELG-RFID) and [DnG-Crafts/ACE-RFID](https://github.com/DnG-Crafts/ACE-RFID).

Choose your printer brand or open tag format on startup, then read and write NFC filament tags using the correct chip and encoding for that printer.

## Supported Formats

| Format | Chip | Read / Write |
|--------|------|--------------|
| **Elegoo** | NTAG213/215 | Read & write Canvas / Centauri tags |
| **Anycubic** | NTAG213 / Ultralight | Read & write ACE / Kobra tags |
| **OpenSpool** | NTAG215/216 | Read & write open JSON NDEF tags |
| **OpenTag3D** | NTAG213/215/216 | Read & write open consortium tags |
| **Creality** | MIFARE Classic 1K | Read & write K2 / K1 / HI CFS tags |
| **QIDI** | MIFARE Classic 1K | Read & write QIDI Box tags |
| **Bambu Lab** | MIFARE Classic 1K | Read official AMS tags; optional clone to FUID magic blanks |

Auto-detect suggests the correct format when you scan an unknown tag. Use the in-app **Tag buying guide** for exact sticker search terms and suppliers.

## Features

### Shared
- Brand / format picker on launch; **Main menu** button on every screen to switch formats
- Vendor → Type → Subtype filament picker with 80+ vendor catalog (Elegoo / Anycubic)
- Color picker with RGB sliders, gradient picker, presets, and hex entry
- Scroll-wheel temperature pickers (5°C steps, 0–400°C)
- NFC auto-read and tag memory viewer (NTAG formats)

### App
- **SpoolTag** — package ID `dngsoftware.spooltag`
- Drawer menu for dark mode, format tag, NFC launch settings, and more

## Tag Requirements

Chip requirements differ by format — see the in-app **Tag buying guide** or the [Releases](https://github.com/sophysdad/ELG-RFID/releases/latest) notes.

NTAG stickers (Elegoo, Anycubic, OpenSpool, OpenTag3D) are not interchangeable with MIFARE Classic stickers (Creality, QIDI, Bambu). Samsung Galaxy S21 and newer phones support MIFARE Classic; some budget phones are NTAG-only.

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
   ./gradlew assembleRelease
   ```

   The release APK will be at `app/build/outputs/apk/release/app-release-unsigned.apk`.

## Tag Format (Elegoo)

You can view a full Elegoo tag dump [here](docs/README.md).

## Credits

Based on [ELG RFID](https://github.com/DnG-Crafts/ELG-RFID) by DnG-Crafts.

## License

This fork inherits the license of the upstream project. See the original repository for details.