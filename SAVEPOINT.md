# SpoolTag — Savepoint (2026-07-08)

Resume from here. Last session shipped **v2.1 beta** and renamed the GitHub repo to **SpoolTag**.

## Quick resume

| Item | Value |
|------|-------|
| **GitHub** | https://github.com/sophysdad/SpoolTag |
| **Latest release** | [v2.1 (Beta)](https://github.com/sophysdad/SpoolTag/releases/tag/v2.1) — APK attached |
| **Local repo path** | `C:\Users\sophy\Desktop\Elegoo-RFID\ELG-RFID` *(rename pending — see below)* |
| **Android project** | `Android/ELGRFID/` |
| **Package ID** | `dngsoftware.spooltag` |
| **App version** | `2.1` / `versionCode 5` |
| **Git branch** | `main` @ `1c4b496` |
| **Git remote** | `https://github.com/sophysdad/SpoolTag.git` |

## Build

```powershell
cd Android\ELGRFID
.\gradlew.bat assembleRelease
# APK: app\build\outputs\apk\release\app-release-unsigned.apk
```

Requires `Android/ELGRFID/local.properties` with `sdk.dir=...`.

## What’s done (v2.1)

- **Formats:** Elegoo, Anycubic, OpenSpool, OpenTag3D, Creality, QIDI, Bambu Lab
- **UI fixes:** Main-screen vs drawer actions, Bambu read/clone bar, Creality spinner overlap, Creality crash guards (spinner -1, mutable adapters, Room on executor)
- **Navigation:** Main menu button on every screen → format picker
- **Tag buying guide** in-app
- **Auto-detect** unknown tag format
- **Released:** tag `v2.1`, prerelease on GitHub, `SpoolTag-v2.1.apk` uploaded
- **Repo rename:** GitHub `ELG-RFID` → `SpoolTag`; local remote + README/release-notes links updated

## Pending / next session

### Immediate housekeeping
1. **Rename local folder** (blocked while Cursor has it open):
   ```powershell
   Rename-Item "C:\Users\sophy\Desktop\Elegoo-RFID\ELG-RFID" "SpoolTag"
   ```
   Then reopen workspace from `...\Elegoo-RFID\SpoolTag`.

2. **Optional:** Edit GitHub release v2.1 description — issues link still says `ELG-RFID` (redirects work; repo docs are already fixed).

### Testing (S23 checklist)
- [ ] All 7 brands / formats
- [ ] Auto-detect on unknown tags
- [ ] Bambu read → clone flow
- [ ] MIFARE vs NTAG wrong-tag guards
- [ ] Creality printer model switch
- [ ] Main menu navigation
- [ ] Tag buying guide links

### Future work (not started)
- **OpenPrintTag** (NfcV) — Phase 4
- Signed APK / Play Store listing
- Formal GitHub issue template for beta feedback
- Internal rename `dngsoftware.elgrfid` / `Android/ELGRFID` — cosmetic only, low priority

## Key architecture notes

- **Brand picker:** `BrandSelectionActivity` → `MainActivity` with `PrinterBrand`
- **Shared nav:** `BrandNavigation.openMainMenu()`
- **NTAG formats:** `NdefFilamentController`, codecs per brand
- **MIFARE formats:** `MifareClassicTransport`, Creality/QIDI/Bambu controllers
- **Creality data:** bundled JSON in `res/raw/`
- **Bambu:** read-only official tags; clone to FUID/Gen2 magic blanks

## Recent commits

```
1c4b496 Update repo links for SpoolTag rename
b9bd950 SpoolTag v2.1 beta: five new tag formats, buying guide, main menu nav
fb8e7b7 Polish SpoolTag v2.0 UI and filament picker for public release
```

## Workspace layout (Desktop)

```
Elegoo-RFID/          ← Cursor workspace root (name unchanged)
  ELG-RFID/           ← git repo (rename to SpoolTag when convenient)
  ACE-RFID/           ← reference fork, not part of SpoolTag repo
  mcps/, terminals/   ← tooling (not in git)
```