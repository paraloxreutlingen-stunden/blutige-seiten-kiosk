# Blutige Seiten Kiosk

Kiosk-App für das Dinner-Event "Escape à la carte" / "Blutige Seiten". Ein Smartphone
pro Gruppe, nur vier Funktionen zugänglich: Telefon, QR-Code scannen, Video, SMS an
die Spielleitung.

## Was die App NICHT selbst macht

- Sie speichert **keine** Rufnummern und schlägt keine vor - der Ziffernblock ist
  immer leer, egal wie oft man die App öffnet.
- Sie zeigt **keinen** Nachrichtenverlauf, keine Kontakte, keine Anrufliste.
- Sie speichert **nichts dauerhaft** (keine Dateien, keine Einstellungen). Dadurch ist
  "Reset zwischen zwei Spielen" kein technischer Vorgang, sondern einfach: App neu
  öffnen bzw. zurück zum Hauptmenü.

## Einmalige Einrichtung pro Gerät

1. **App installieren** (siehe unten, "Build").
2. **Entsperrmuster einrichten**: Einstellungen → Sicherheit → Displaysperre → Muster.
   Das ist eine reine Android-Bordmittel-Einstellung, dafür ist in der App nichts
   programmiert.
3. **App-Fixierung aktivieren** (einmalig, pro Gerät):
   Einstellungen → Sicherheit → Weitere Sicherheitseinstellungen → App-Fixierung → an.
   Dort gibt es meist auch die Option "Bei Lösen der Fixierung nach Entsperrmuster
   fragen" - die ebenfalls aktivieren. Dann verlangt das Gerät beim Verlassen der App
   automatisch das Muster.
   ⚠️ Die genaue Bezeichnung und der Menüpfad unterscheiden sich je nach Android-
   Version und Hersteller (Samsung, Pixel, Xiaomi etc. benennen das teils anders,
   z. B. "Bildschirmfixierung"). Einfach nach "Fixierung" oder "Pin" suchen.
4. **App einmal öffnen und die Einrichtung durchklicken.** Beim ersten Start fragt die
   App nacheinander:
   - alle Berechtigungen auf einmal (Telefon, SMS, Kamera) → alle **zulassen**,
   - „Als Standard-Telefon-App festlegen?" → **Blutige Seiten Kiosk** wählen.
   Wurde etwas abgelehnt, zeigt die App einen Hinweis mit Knopf zu den passenden
   Einstellungen. Erst wenn beides erledigt ist, fixiert sich die App selbst
   (`startLockTask()`). Als Standard-Startbildschirm (Home-App) muss sie NICHT
   festgelegt werden.

   **Warum Standard-Telefon-App?** Während der App-Fixierung blockiert Android
   Samsungs Anrufbildschirm. Ohne eigene Telefon-App könnten die Spielenden während
   eines Anrufs keine Tasten drücken (z. B. 1 = Deutsch, 2 = Englisch im
   easybell-Menü) und nicht auflegen. Als Standard-Telefon-App zeigt die Kiosk-App
   ihren eigenen Anrufbildschirm mit Ziffernblock, Lautsprecher und Auflegen-Knopf.
   Eingehende Anrufe werden automatisch abgelehnt.

   **Nach dem Event:** Einstellungen → Apps → Standard-Apps → Telefon-App wieder auf
   Samsungs „Telefon" stellen (oder die Kiosk-App deinstallieren).

Die Rufnummer der Spielleitung (0176 80297001) ist bereits fest in der App
hinterlegt (`SmsActivity.kt`) - dafür ist nichts mehr einzustellen.

## Vor jedem Spiel (Reset-Routine für die Spielleitung)

1. App öffnen (oder zurück ins Hauptmenü, falls sie schon lief).
2. Fertig. Es gibt nichts zu löschen, weil die App nichts speichert.

Für maximale Sicherheit könnt ihr zusätzlich "Zuletzt verwendete Apps" leeren bzw.
die App einmal per "App beenden" schließen und neu starten - technisch nötig ist das
aber nicht.

## Sprache

**Was die Spielenden sehen, ist textfrei** - nur Symbole, Zahlen und Farben, damit das
Handy nicht auf eine bestimmte Sprache umgestellt werden muss: Scanner mit Sucher-Rahmen
statt Hinweistext, Ladekreis statt "Video wird geladen", beim Anrufen laufende Punkte
bzw. die Gesprächsdauer, bei nicht erlaubter Nummer wird die Nummer rot, wackelt und das
Handy vibriert, bei fehlendem Internet erscheint ein "kein WLAN"-Symbol. Text gibt es nur
noch in den Einrichtungs-Hinweisen für die Spielleitung und im festen Spieldatum auf dem
Startbildschirm.

Die App gibt es auf Deutsch und Englisch - ganz ohne Umschalter in der App selbst.
Android wählt automatisch die passende Version anhand der **Sprache des Geräts**
(Einstellungen → Allgemeine Verwaltung → Sprache und Eingabe → Sprache):
steht das Handy auf Englisch, zeigt die App Englisch, sonst Deutsch (Standard).
Die deutschen Texte liegen in `res/values/strings.xml`, die englischen in
`res/values-en/strings.xml` - beide enthalten exakt dieselben Textbausteine 1:1
übersetzt.

## Optik

Die App sieht bewusst wie ein ganz normales, benutztes Smartphone aus - kein
offensichtliches "Kiosk-Menü":

- **Statusleiste**: eigener Nachbau oben auf allen Bildschirmen (außer Video): Uhrzeit,
  Mobilfunkempfang, WLAN, Akku-Symbol und Prozent - links und rechts der Kamera-Aussparung.
  Die echte Android-Leiste wird ausgeblendet (`StatusBarView`, `KioskBaseActivity`).
- **Startbildschirm**: euer Katzenfoto als Hintergrundbild, App-Icons wie bei einem
  echten Launcher (Dock unten mit genau den vier echten Funktionen: Telefon/
  Nachrichten/Scanner/Video - bewusst keine zusätzlichen Deko-Icons).
- **Telefon**: an einen echten Android-Dialer angelehnt (Ziffernblock mit Buchstaben,
  grüner Anruf-Button), bewusst schlicht gehalten, ohne die dekorativen Zusatzzeilen
  ("Neuer Kontakt", Tab-Leiste) eines echten Dialers.

Die Icons (Telefon, Nachrichten, Scanner, Video, Zurück, Löschen) sind Googles
offizielle "Material Symbols" - 1:1 dieselben Pfaddaten wie im echten Google-Icon-Satz
(Apache-2.0-Lizenz, also frei nutzbar), keine Nachzeichnung. Keine echten Markenlogos
(WhatsApp, Spotify etc.) - die sind geschützt und wurden bewusst nicht verwendet.

## Die vier Funktionen im Detail

| Funktion | Wie es funktioniert |
|---|---|
| **Telefon** | Eigener Ziffernblock (kein System-Telefon, keine Kontakte, keine Anrufliste). Nur die drei Spiel-Nummern sind wählbar. Wählt über `TelecomManager.placeCall`; der Anruf läuft im eigenen Anrufbildschirm (`InCallActivity` + `KioskInCallService`) mit Ziffernblock für Tastentöne, Lautsprecher-Knopf, Auflegen-Knopf und Bildschirm-aus am Ohr. |
| **QR-Code scannen** | Kamera läuft direkt in der App (CameraX + ML Kit, komplett offline). Erkennt nur die hinterlegte Ziel-URL, alles andere wird ignoriert. Ist das Ziel ein Bild, zeigt die App es selbst an: Zoomen mit zwei Fingern, Verschieben, Doppeltippen; die Ansicht dreht sich mit dem Handy. Das Bild wird nur im Arbeitsspeicher gehalten. Ist das Ziel eine Webseite, öffnet sich eine eingeschränkte Webansicht nur dieser Domain. |
| **Video** | Spielt die im Projekt hinterlegte mp4-Datei (`res/raw/blutige_seiten_loop.mp4`) in Dauerschleife, komplett offline. |
| **Nachricht an Spielleitung** | Schickt eine SMS direkt per `SmsManager`, OHNE die System-SMS-App zu öffnen - dadurch bleibt gar kein Nachrichtenverlauf auf dem Gerät zurück. | **Derzeit abgeschaltet** (Icon ausgeblendet, keine SMS-Berechtigung nötig) - zum Wiedereinschalten in `KioskSetup.kt` `SMS_ENABLED = true` setzen. |

## Build: App auf die Handys bekommen

Gewählter Weg: **automatischer Cloud-Build über GitHub Actions**. Der Workflow
(`.github/workflows/build-apk.yml`) ist bereits fertig im Projekt enthalten - bei
jedem Hochladen baut GitHub automatisch eine fertige, installierbare .apk-Datei zum
Herunterladen. Kein Android Studio, keine eigene Entwicklungsumgebung nötig.

### Einmalige Einrichtung

1. **GitHub-Konto erstellen** (falls noch keins vorhanden): [github.com](https://github.com) →
   "Sign up" - kostenlos.
2. **GitHub Desktop installieren**: [desktop.github.com](https://desktop.github.com) -
   kostenlose App, läuft unter Windows und Mac, keine Kommandozeile nötig.
3. In GitHub Desktop mit dem GitHub-Konto anmelden.
4. Dieses Projekt entpacken (die .zip-Datei, die du von mir bekommen hast) an einen
   Ort auf dem PC, z. B. `Dokumente\BlutigeSeitenKiosk`.
5. In GitHub Desktop: **File → Add Local Repository** → den entpackten
   `BlutigeSeitenKiosk`-Ordner auswählen.
6. GitHub Desktop fragt, ob daraus ein Git-Repository erstellt werden soll - **"create
   a repository"** anklicken, dann **"Publish repository"**. Name kann so bleiben
   (`BlutigeSeitenKiosk`), "Keep this code private" anlassen (privat reicht völlig,
   kostet nichts).

### Bei jeder Änderung (und beim ersten Mal)

1. In GitHub Desktop erscheinen links alle geänderten Dateien.
2. Unten links einen kurzen Commit-Kommentar eintragen (z. B. "Erste Version"),
   **"Commit to main"** klicken.
3. Oben **"Push origin"** klicken (lädt alles zu GitHub hoch).
4. Im Browser zu `github.com/<dein-benutzername>/BlutigeSeitenKiosk` → Tab
   **"Actions"** wechseln. Dort läuft automatisch ein Durchlauf namens "APK bauen"
   (dauert ca. 3-5 Minuten).
5. Ist er fertig (grünes Häkchen) → auf den Durchlauf klicken → ganz unten unter
   **"Artifacts"** liegt **"BlutigeSeitenKiosk-APK"** zum Download (als .zip, darin
   die eigentliche `app-debug.apk`).

### Auf die Handys überspielen (am einfachsten: fester Link)

Jeder Build legt die APK zusätzlich unter diesem festen Link ab - ohne ZIP, ohne
GitHub-Anmeldung. Einfach im Browser des Handys öffnen und die Datei antippen:

**https://github.com/paraloxreutlingen-stunden/blutige-seiten-kiosk/releases/download/apk-aktuell/BlutigeSeitenKiosk.apk**

Die APK ist mit einem festen Schlüssel signiert (`app/debug.keystore`). Neue Versionen
lassen sich deshalb einfach über die alte installieren; Berechtigungen und
Standard-Telefon-App bleiben erhalten.

### Alternativ: per Kabel/Datei überspielen

Die heruntergeladene `app-debug.apk` einfach wie jede andere Datei auf jedes Handy
kopieren (USB-Kabel, Google Drive, E-Mail an sich selbst, WhatsApp an sich selbst -
was auch immer am einfachsten ist) und dort antippen zum Installieren. Vorher einmalig
pro Handy erlauben: Einstellungen → Sicherheit → "Installation aus unbekannten
Quellen" / "Unbekannte Apps installieren" für die jeweilige App (Dateimanager,
Drive, o. ä.) aktivieren.

⚠️ Dieser Build-Weg ist Standardpraxis (GitHub Actions ist Googles eigene Empfehlung
für CI/CD bei Android-Projekten), aber in dieser Sandbox konnte ich ihn nicht selbst
einmal live durchlaufen lassen, weil ich hier keinen Zugriff auf GitHub-Konten habe.
Beim allerersten Durchlauf also bitte kurz prüfen, ob er wirklich grün durchläuft -
falls nicht, schick mir die Fehlermeldung aus dem Actions-Log, dann schau ich sie mir an.

### Alternative: Android Studio lokal

Falls doch lieber lokal gebaut werden soll (z. B. durch Benedikt):

1. [Android Studio](https://developer.android.com/studio) installieren (kostenlos).
2. Projekt-Ordner über "Open" öffnen, Android Studio synchronisiert automatisch.
3. Build → Build Bundle(s) / APK(s) → Build APK(s). Datei liegt danach unter
   `app/build/outputs/apk/debug/app-debug.apk`.
4. Diese Datei wie oben beschrieben auf die Handys überspielen.

## Bekannte Einschränkungen (ehrlich, nicht schöngeredet)

- ⚠️ QR-Scan, Video und SMS wurden mangels Android-Testgerät in dieser Umgebung
  **nicht auf einem echten Smartphone getestet** - nur sorgfältig gegen die
  offizielle Android/CameraX/ML-Kit-Dokumentation geprüft. Vor dem ersten echten
  Spiel unbedingt einmal komplett durchspielen.
- ⚠️ Der eigene Anrufbildschirm (Standard-Telefon-App) wurde in dieser Umgebung
  nicht auf einem echten Gerät getestet. Bitte prüfen: Anruf geht raus, Taste 1/2
  kommt bei easybell an, Lautsprecher und Auflegen funktionieren, Bildschirm geht am
  Ohr aus.
- Bewusst weggelassen (im Spiel nicht nötig): eingehende Anrufe (werden abgelehnt),
  Stummschalten, Halten, zweiter Anruf, Bluetooth-Umschaltung.
- ⚠️ Die Ziel-URL `https://escape-a-la-carte.de/blutige-seiten-qr-code-1` konnte ich
  von hier aus nicht erreichen (Verbindungsfehler) - bitte vor dem Einsatz prüfen,
  ob die Seite schon online ist.
- Je nach Android-Version/Hersteller kann der genaue Pfad zu den
  App-Fixierungs-Einstellungen leicht abweichen (siehe oben).
