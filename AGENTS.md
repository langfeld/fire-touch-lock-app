# Hinweise für KI-Agenten

Android-App für Fire-Tablets (Fire OS 7 und neuer), die den Bildschirm im Amazon-Kids-Profil gegen Berührungen sperrt, etwa während ein Kind ein Video schaut. Gesperrt und entsperrt wird mit Lauter, Leiser, Lauter, Leiser; entsperren geht auch über die vier Eckpunkte im Uhrzeigersinn.

## Grundsätze

- Texte in der Oberfläche, Kommentare und Fehlermeldungen auf Deutsch, Bezeichner im Code auf Englisch.
- Einfach halten: nur bauen, was gebraucht wird. Java ohne Bibliotheken, nur JUnit für die Tests.
- Nichts an Amazons Komponenten ändern: nichts deaktivieren und nichts einschalten, was Amazon im Kinderprofil gesperrt hat (etwa Launcher3).
- Das Repo ist öffentlich: keine Namen, Konten, Seriennummern oder Profil-IDs eines bestimmten Geräts.

## Aufbau

- `app/src/main/java/org/langfeld/firetouchlock/`:
  - `LockService.java`: die Bedienungshilfe. Sie filtert nur die Lautstärketasten (`onKeyEvent`) und schaltet die Sperre. Gesperrt liegt ein Fenster `TYPE_ACCESSIBILITY_OVERLAY` über allem (nicht fokussierbar, damit das Video den Fokus behält), und die Lautstärketasten werden geschluckt, jeweils Drücken und Loslassen derselben Taste.
  - `LockOverlay.java`: die durchsichtige Fläche. Sie fängt jede Berührung ab und zeigt dabei für drei Sekunden dezent die vier Eckpunkte und ein Schloss. Die Punkte zeigen absichtlich keinen Fortschritt, damit ein Kind die Reihenfolge nicht durch Ausprobieren findet.
  - `KeySequence.java` und `CornerSequence.java`: reine Logik ohne Android, mit JUnit-Tests in `app/src/test`.
  - `MainActivity.java`: Anleitung und Status. Das Startsymbol braucht Amazon Kids, um die App unter „Inhalte hinzufügen“ anzubieten.
- `res/xml/lock_service.xml`: Konfiguration der Bedienungshilfe (Tastenfilter, kein Lesen von Fensterinhalten).

## Hintergrund: Fire OS und das Kinderprofil

Gemessen auf einem Fire HD 8 (10. Generation) mit Fire OS 7.3:

- Fire OS hat eine eigene Touchsperre für fixierte Apps („Kleinkind-Modus“: Häkchen im Dialog „Bildschirm fixieren?“, Dienst `com.amazon.toddlermode.ToddlerModeService` in `fosservices`, Einstellung `toddler_mode_user_selection` in Secure, je Benutzer). Im Kinderprofil gibt es aber weder Einstieg noch Ausstieg:
  - Die Übersichtstaste ist dort per Statusleisten-Flag `DISABLE_RECENT` ausgeblendet. Das Flag hält Amazons Systemcode; die ADB-Shell darf es ohne `android.permission.STATUS_BAR` nicht ändern.
  - Launcher3, der die Übersicht liefert, ist im Kinderprofil deaktiviert. `input keyevent 187` öffnet die Übersicht deshalb unsichtbar im Elternprofil.
  - Im Kleinkind-Modus löst nur „Übersicht gedrückt halten“ die Fixierung; das Kinderprofil hat keinen Sperrbildschirm.
  - Per ADB geht es trotzdem: `settings --user <id> put secure toddler_mode_user_selection 1`, dann `am task lock <task-id>`. Lösen dann nur mit `am task lock stop` oder einem Neustart. Deshalb diese App.
- Das Kinderprofil ist ein eigener Android-Benutzer, Profilbesitzer ist Amazon Kids (`com.amazon.tahoe`). Dort sind nur freigegebene Apps installiert. Bedienungshilfen schränkt Amazon Kids nicht ein.
- Die Systemumgebung des Fire HD 8 (2020) ist 32-Bit: keine nativen Bibliotheken nur für arm64.
- USB-Debugging lässt sich nur im Elternprofil erlauben; danach wirkt ADB auch, wenn das Kinderprofil vorn ist. Unter Android 9 kennt `settings` den Schalter `--user` nur bei `get` und `put`, und `content query --user` liefert bei Einstellungen stillschweigend die Werte von Benutzer 0.

## Befehle

Lokal gibt es kein Android-SDK: Tests und Build laufen in der GitHub Action „Android-APK“ (`gradle test`, dann `gradle assembleRelease`). Bevor eine Aufgabe als fertig gilt, muss die Action grün sein. Einrichten auf dem Tablet, Build und Release: `docs/BETRIEB.md`.
