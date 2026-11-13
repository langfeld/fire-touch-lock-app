# Betrieb: Bedienen, Einrichten, Bauen

## 1. Bedienung

- **Sperren und entsperren:** Lauter, Leiser, Lauter, Leiser zügig hintereinander drücken (innerhalb von 3 Sekunden). Eine kurze Meldung zeigt „Touchsperre an“ oder „Touchsperre aus“; beim Sperren erscheinen kurz die vier Eckpunkte.
- **Während der Sperre** reagiert nichts auf Berührung, auch nicht die Navigationsleiste. Die Lautstärketasten sind mitgesperrt, ein Video läuft weiter. Die Ein/Aus-Taste funktioniert normal; nach dem Einschalten ist die Sperre noch da.
- **Entsperren ohne Tasten:** den gesperrten Bildschirm berühren, dann erscheinen dezent vier Punkte in den Ecken und ein Schloss. Die Punkte im Uhrzeigersinn antippen, oben links beginnend, alle innerhalb von 5 Sekunden. Ein Tipp daneben beginnt von vorn.
- **Notausgang:** Ein Neustart hebt die Sperre immer auf, ebenso ein Profilwechsel (die Bedienungshilfe läuft nur im Profil, das gerade vorn ist).

## 2. Einrichten auf dem Fire-Tablet (einmalig, per ADB)

Im Kinderprofil sind die Einstellungen für Bedienungshilfen nicht erreichbar. Installieren und Einschalten geht deshalb einmalig per ADB; danach braucht die Sperre keinen Rechner mehr.

### Voraussetzungen

1. Im **Elternprofil**: *Einstellungen → Geräteoptionen → Über das Fire Tablet*, siebenmal auf die Seriennummer tippen. Dann unter *Geräteoptionen → Entwickleroptionen* „ADB aktivieren“ einschalten.
2. Tablet per USB anschließen und die Frage „USB-Debugging zulassen?“ mit „Von diesem Computer immer zulassen“ bestätigen. Das geht nur im Elternprofil; danach wirkt ADB auch, wenn das Kinderprofil vorn ist.
3. Linux: Meldet `adb devices` „no permissions“, fehlt eine udev-Regel für Amazon (Hersteller-ID `1949`):

   ```bash
   echo 'SUBSYSTEM=="usb", ATTR{idVendor}=="1949", MODE="0660", GROUP="plugdev", TAG+="uaccess"' | sudo tee /etc/udev/rules.d/51-android-amazon-fire.rules
   sudo udevadm control --reload-rules
   sudo udevadm trigger --subsystem-match=usb --attr-match=idVendor=1949
   ```

### Installieren und einschalten

Die Benutzer-ID des Kinderprofils steht in der Liste der Benutzer (das Elternprofil ist `0`, Kinderprofile sind meist zweistellig):

```bash
adb shell pm list users
```

Im Folgenden steht `<ID>` für diese Nummer.

```bash
# App nur ins Kinderprofil installieren (APK aus dem Release oder Artefakt, siehe Abschnitt 3)
adb install --user <ID> fire-touch-lock-<version>.apk

# Vorher nachsehen, ob dort schon andere Bedienungshilfen eingeschaltet sind (meist „null“)
adb shell settings --user <ID> get secure enabled_accessibility_services

# Bedienungshilfe einschalten (stehen schon andere drin: mit „:“ anhängen statt ersetzen)
adb shell settings --user <ID> put secure enabled_accessibility_services org.langfeld.firetouchlock/.LockService
adb shell settings --user <ID> put secure accessibility_enabled 1
```

Prüfen, ob sie läuft (das Kinderprofil muss dafür vorn sein):

```bash
adb shell dumpsys accessibility | grep -i firetouchlock
```

**Falls Amazon Kids die App wieder entfernt** (dort sind nur freigegebene Apps vorgesehen): die APK im Elternprofil installieren (`adb install --user 0 …`), dann in den Amazon-Kids-Einstellungen beim Kind unter *Inhalte hinzufügen → Apps* „Fire Touch Lock“ freigeben und die Bedienungshilfe wie oben einschalten.

### Aktualisieren und entfernen

```bash
# Update: gleiche Signatur, die Bedienungshilfe bleibt eingeschaltet
adb install -r --user <ID> fire-touch-lock-<version>.apk

# Entfernen: Android trägt die Bedienungshilfe dabei selbst aus
adb uninstall --user <ID> org.langfeld.firetouchlock
```

### Fehlersuche

```bash
adb logcat -s FireTouchLock          # Meldungen der App
adb shell dumpsys window windows | grep -A3 FireTouchLock   # liegt die Sperrfläche?
```

## 3. App bauen

Die APK baut die GitHub Action „Android-APK“; lokal sind dafür keine Android-Werkzeuge nötig.

### Einmalig: Signaturschlüssel

```bash
scripts/create-signing-key.sh
```

Das Skript legt den Schlüssel in `~/fire-touch-lock-signaturschluessel` ab. Die beiden **Repository-Secrets** setzt die GitHub-CLI:

```bash
gh secret set ANDROID_KEYSTORE_BASE64 < ~/fire-touch-lock-signaturschluessel/upload.p12.base64
gh secret set ANDROID_KEYSTORE_PASSWORD < ~/fire-touch-lock-signaturschluessel/passwort.txt
```

Schlüssel und Passwort gut aufbewahren (z. B. im Passwortmanager): Updates lassen sich nur installieren, wenn sie mit demselben Schlüssel signiert sind.

### Testbuild und neue Version

- **Testbuild:** *Actions → Android-APK → Run workflow* oder `gh workflow run android.yml`. Die APK liegt dann als Artefakt am Lauf (`gh run download`).
- **Neue Version** (APK im GitHub-Release, auch für Obtainium):

  ```bash
  git tag v0.1.0
  git push origin v0.1.0
  ```

Die Action führt zuerst die Tests aus (`gradle test`), dann baut und signiert sie die APK. `versionCode` ist die Nummer des Laufs, Testbuilds und Releases zählen also dieselbe Nummer hoch: Android installiert keinen älteren Build über einen neueren. `versionName` kommt aus dem Tag; Testbuilds tragen den Standard aus `app/build.gradle.kts`.
