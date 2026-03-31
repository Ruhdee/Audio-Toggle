# AudioToggle

AudioToggle is a no-UI Android utility that toggles mono audio and audio balance so sound can be forced to a working speaker when the other speaker is faulty. When you plug in headphones, you can tap again to revert to normal stereo settings. This app uses Shizuku to apply system settings without root, so Shizuku must be installed and running first.

Shizuku: https://github.com/RikkaApps/Shizuku

## How It Works (Short)

- Mono ON + balance to one side: avoids losing audio from the broken speaker.
- Mono OFF + balance centered: restores normal audio for headphones.
- The app toggles between these two states on each launch.

## Adjusting Left vs. Right Speaker

To switch the app from left-only to right-only output, change the balance value used when mono is ON:

- Left speaker only: `master_balance = -1.0`
- Right speaker only: `master_balance = 1.0`
- Centered (normal): `master_balance = 0.0`

In code, this is the value assigned to `targetBalance` when mono is enabled.

## Shizuku Requirement

1. Install Shizuku from the link above.
2. Start Shizuku and grant the app permission.
3. Launch AudioToggle to switch audio modes.

## Finding System Setting Keys Using ADB

Some Android features (like mono audio or audio balance) are not exposed via public APIs.
However, you can discover the underlying system setting keys using ADB.

This method works by comparing system settings before and after changing a setting manually.

---

### Step 1: Capture current settings (BEFORE)

Run:

```bash
adb shell settings list secure > secure_before.txt
adb shell settings list system > system_before.txt
```

---

### Step 2: Change the setting manually

On your device:

- Go to Settings -> Accessibility -> Audio
- Toggle:
  - Mono audio
  - Audio balance (move slider)

---

### Step 3: Capture settings again (AFTER)

```bash
adb shell settings list secure > secure_after.txt
adb shell settings list system > system_after.txt
```

---

### Step 4: Compare (diff)

#### On Linux / macOS:

```bash
diff secure_before.txt secure_after.txt
diff system_before.txt system_after.txt
```

#### On Windows (PowerShell):

```powershell
Compare-Object (Get-Content secure_before.txt) (Get-Content secure_after.txt)
Compare-Object (Get-Content system_before.txt) (Get-Content system_after.txt)
```

---

### Step 5: Identify changed keys

Look for lines that changed between BEFORE and AFTER.

Example:

```text
< master_balance=-1.0
< master_mono=1
---
> master_balance=0.0
> master_mono=0
```

This tells you:

- `master_mono` controls mono audio (0/1)
- `master_balance` controls left/right balance (-1.0 to 1.0)

---

### Notes

- Settings may differ across devices and OEMs
- Not all settings respond to `adb shell settings put`
- Some values are read-only or handled internally by the system

---

### Tip

Once identified, you can test changes directly:

```bash
adb shell settings put secure master_mono 1
adb shell settings put system master_balance -1.0
```

---

