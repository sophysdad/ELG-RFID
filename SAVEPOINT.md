# SpoolTag — Savepoint (2026-07-08)

Resume from here. **v2.1 beta** is on GitHub; this session polished the **brand selection (launch) screen** UI.

## Quick resume

| Item | Value |
|------|-------|
| **GitHub** | https://github.com/sophysdad/SpoolTag |
| **Latest release** | [v2.1 (Beta)](https://github.com/sophysdad/SpoolTag/releases/tag/v2.1) — APK attached |
| **Local repo path** | `C:\Users\sophy\Desktop\Elegoo-RFID\SpoolTag` |
| **Android Studio project** | `Android/ELGRFID/` *(open this folder, not repo root)* |
| **Package ID** | `dngsoftware.spooltag` |
| **App version** | `2.1` / `versionCode 5` |
| **Git branch** | `main` (synced with `origin/main`) |
| **Git remote** | `https://github.com/sophysdad/SpoolTag.git` |

## Build & run

```powershell
cd Android\ELGRFID
.\gradlew.bat assembleDebug    # dev / Android Studio Run
.\gradlew.bat assembleRelease  # release APK
```

Requires `Android/ELGRFID/local.properties` with `sdk.dir=...`.

**Phone:** enable USB debugging, open `Android/ELGRFID` in Android Studio, Run ▶ on device.

## What’s done (v2.1 + launch-screen polish)

### v2.1 (shipped)
- **Formats:** Elegoo, Anycubic, OpenSpool, OpenTag3D, Creality, QIDI, Bambu Lab
- **Navigation:** Main menu on every screen → format picker
- **Tag buying guide**, auto-detect, Creality crash guards
- **Released:** tag `v2.1`, prerelease on GitHub, release issues link fixed

### Launch screen UI (this session)
- **`SpoolTagLogoView`** — custom wordmark: Syne ExtraBold, gradient text, NFC waves integrated in “oo”, halo/glow
- **Frosted brand cards** — 50% transparent fill (`brand_card_fill.xml`), darker description text
- **Clipped scroll panel** — `brand_list_container` between instructions and help link; cards never overlap header/footer (panel is invisible, clipping only)
- **Layout tuning** — margins sized so Elegoo + Anycubic + OpenSpool fit on load on S23

### Housekeeping
- Local folder renamed `ELG-RFID` → `SpoolTag`
- GitHub release v2.1 issues URL → `SpoolTag/issues`
- `.gitignore` excludes `mcps/`, `terminals/`
- Removed unused `outfit_semibold.ttf` font

## Pending / next session

### Testing (S23 checklist)
- [ ] All 7 brands / formats
- [ ] Auto-detect on unknown tags
- [ ] Bambu read → clone flow
- [ ] MIFARE vs NTAG wrong-tag guards
- [ ] Creality printer model switch
- [ ] Main menu navigation
- [ ] Tag buying guide links
- [ ] Launch screen scroll + logo on various screen sizes

### Future work
- **OpenPrintTag** (NfcV) — Phase 4
- Signed APK / Play Store listing
- Beta feedback issue template
- Optional **v2.1.1** polish APK if UI-only changes warrant a tagged build
- Internal rename `dngsoftware.elgrfid` / `Android/ELGRFID` — cosmetic, low priority

## Key files (launch screen)

| File | Role |
|------|------|
| `SpoolTagLogoView.java` | Custom logo draw (text + NFC mark) |
| `activity_brand_selection.xml` | Header, clipped `brand_list_container`, help button |
| `item_brand_card.xml` | Frosted format cards |
| `brand_card_fill.xml` | Semi-transparent card background |
| `contactless_logo_mark.xml` | Full-opacity NFC icon for logo |
| `res/font/syne_extrabold.ttf` | Logo typeface |

## Architecture (unchanged)

- **Brand picker:** `BrandSelectionActivity` → `MainActivity` with `PrinterBrand`
- **Shared nav:** `BrandNavigation.openMainMenu()`
- **NTAG:** `NdefFilamentController` + per-brand codecs
- **MIFARE:** `MifareClassicTransport`, Creality/QIDI/Bambu controllers

## Recent commits

```
d2c35af Update savepoint for launch screen polish session
801e584 Polish brand selection launch screen UI
934ba0a Add savepoint for v2.1 beta session
1c4b496 Update repo links for SpoolTag rename
b9bd950 SpoolTag v2.1 beta: five new tag formats, buying guide, main menu nav
```

## Workspace layout (Desktop)

```
Elegoo-RFID/
  SpoolTag/           ← git repo + Cursor workspace
  ACE-RFID/           ← reference fork, not in SpoolTag repo
  mcps/, terminals/   ← local tooling (gitignored)
```