# PaperNotes 🌼

Eine minimalistische Notizen-App für Android mit "Premium-Papieroptik": warme Glücks-Farben,
physikbasierte Animationen und haptische Gimmicks. Muji-Stil trifft Material Design 3.

## Tech-Stack
- **Jetpack Compose** (Kotlin) · Material 3
- **minSdk 33** (Android 13) · compileSdk/targetSdk 35 — `RuntimeShader`/AGSL für die Papiertextur
- **Room** (Notizen) · **DataStore** (Teebeutel-Status) · **Hilt** (DI)
- Shared-Element-Transitions, Spring-Physics, AGSL-Shader, native Haptics
- **Performance:** Baseline Profiles (AOT-Vorkompilierung für schnellen Kaltstart), gebackene Shader-Textur, gegateter Animations-Overhead

## Features & Gimmicks
- **Papieroptik:** AGSL-Fasertextur + dezentes Dot-Grid-Raster ([`PaperBackground`](app/src/main/java/com/papernotes/ui/components/PaperBackground.kt), [`Shaders.kt`](app/src/main/java/com/papernotes/util/Shaders.kt)).
- **Checklisten-Notizen:** eigener Typ mit handgezeichneten Checkboxen; Erledigtes wird durchgestrichen und sinkt animiert nach unten ([`ChecklistEditor`](app/src/main/java/com/papernotes/ui/editor/ChecklistEditor.kt)). Beim letzten Haken: Papier-Konfetti ([`Confetti`](app/src/main/java/com/papernotes/ui/components/Confetti.kt)).
- **Knüddeln & Papierkorb:** Löschen knüllt die Karte zusammen und wirft sie in den Papierkorb; dort liegt sie 30 Tage als Papierkugel und lässt sich per Tap wieder **glattstreichen** ([`CrumpleOverlay`](app/src/main/java/com/papernotes/ui/components/CrumpleOverlay.kt), [`ArchiveDrawer`](app/src/main/java/com/papernotes/ui/components/ArchiveDrawer.kt)).
- **Archiv-Schublade:** dezente Lasche am unteren Rand öffnet die Schublade mit Archiv + Papierkorb.
- **Washi-Tape-Pinnen:** Notizen mit einem schiefen Klebestreifen oben festheften ([`WashiTape`](app/src/main/java/com/papernotes/ui/components/WashiTape.kt)).
- **Tinten-Suche:** Pull-down auf dem Grid öffnet die Suche; Nicht-Treffer verblassen wie verdünnte Tinte ([`InkSearch`](app/src/main/java/com/papernotes/ui/components/InkSearch.kt)).
- **Stimmungs-Filter:** Punkteleiste filtert nach Eselsohr-Farbe.
- **Stimmungs-Eselsohr:** Ecke der Karte umknicken; Farbe = Stimmung/Kategorie ([`DogEar`](app/src/main/java/com/papernotes/ui/components/DogEar.kt)).
- **Glücks-Teebeutel:** Schnur am oberen Rand nach unten ziehen → Gimmick des Tages + Tages-Statistik ([`TeabagPull`](app/src/main/java/com/papernotes/ui/components/TeabagPull.kt)).
- **Mitternachtspapier:** warmer Dark Mode (tiefes Espresso-Papier, Creme-Tinte), folgt der Systemeinstellung.
- **Clean Writing Mode:** System-Leisten blenden aus, Karte morpht ins Vollbild ([`EditorScreen`](app/src/main/java/com/papernotes/ui/editor/EditorScreen.kt)).
- **Wiederkehrende Erinnerungen:** Erinnerungen täglich/werktags/wöchentlich; der Papier-Reiter trägt ein „↻" ([`ReminderRule`](app/src/main/java/com/papernotes/domain/model/ReminderRule.kt), [`ReminderReceiver`](app/src/main/java/com/papernotes/reminder/ReminderReceiver.kt)).
- **Zeitkapsel:** Notiz mit Wachs versiegeln + Öffnungsdatum – sie bleibt verschlossen und öffnet sich am Tag X von selbst inkl. Benachrichtigung ([`CapsuleSheet`](app/src/main/java/com/papernotes/ui/components/CapsuleSheet.kt)).
- **Als Papierkarte teilen:** Teilen rendert die Notiz als hübsches Karten-Bild (PNG) und schickt es per Papierflieger los ([`ShareCardRenderer`](app/src/main/java/com/papernotes/util/ShareCardRenderer.kt)).
- **Einkleben & Schnellzettel:** geteilten Text/Link aus anderen Apps als neue Notiz übernehmen; Launcher-Shortcut „Neuer Zettel" für sofortiges Schreiben.
- **Gesten:** Swipe-to-Archive, Pinch-to-Zoom (Spaltenzahl), schwebende Schatten ([`NotesScreen`](app/src/main/java/com/papernotes/ui/notes/NotesScreen.kt)).
- **Haptik:** feine Ticks bei Interaktionen, Knister-Pattern beim Knüllen ([`Haptics.kt`](app/src/main/java/com/papernotes/util/Haptics.kt)).

## Projektstruktur
```
app/src/main/java/com/papernotes/
  domain/model/   Note, MoodCategory, DailyDelight
  data/           Room (local), repository, delight, prefs (DataStore)
  di/             Hilt-Module
  ui/theme/       Glücks-Farbpalette, Typo, Shapes
  ui/components/  PaperBackground, NoteCard, DogEar, CrumpleOverlay, TeabagPull, AddFab, MoodPickerSheet
  ui/notes/       Grid-Screen + ViewModel
  ui/editor/      Editor-Screen + ViewModel
  ui/navigation/  SharedTransition-Container
  util/           Haptics, Shaders, Context-Helper
baselineprofile/  Macrobenchmark, der das Baseline-/Startup-Profil erzeugt
```

## Bauen & Starten
Voraussetzung: Android Studio (Ladybug+) oder Android SDK 35 + **JDK 17**.
Es liegt **kein `gradlew`-Wrapper** im Repo — gebaut wird mit einem installierten `gradle`
(z. B. via Homebrew). Vorher Umgebung setzen:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export ANDROID_HOME="$HOME/Library/Android/sdk"
```

### Debug (schnelles Iterieren)
```bash
gradle :app:installDebug   # baut & installiert auf verbundenem Gerät/Emulator (API 33+)
```
In Android Studio: Projekt öffnen → Gerät (API 33+) wählen → **Run**.

> ⚠️ Der Debug-Build ist absichtlich **unoptimiert und deutlich langsamer** (kein R8,
> `debuggable=true`, kein AOT-Profil). Er eignet sich zum Entwickeln, **nicht** zur
> Beurteilung der gefühlten Performance.

### Release (echte Performance testen)
Der `release`-Build (R8 + Resource-Shrinking) ist mit dem Debug-Schlüssel signiert und
daher ohne eigenes Keystore direkt installierbar:
```bash
gradle :app:installRelease
```
In Android Studio alternativ **Build → Select Build Variant → `:app` auf `release`**,
dann normal **Run**. (WLAN-Debugging funktioniert genauso — `adb` sieht das Gerät.)

### Baseline Profile (Kaltstart-Optimierung)
Ein eingecheckter Baseline-/Startup-Profil-Datensatz
(`app/src/release/generated/baselineProfiles/`) wird bei jedem Release-Build automatisch
mit eingebacken — **kein Schritt pro Build nötig**. Neu erzeugen lohnt nur nach größeren
UI-/Startsequenz-Umbauten (Gerät muss verbunden sein):
```bash
gradle :app:generateBaselineProfile
```
ProfileInstaller wendet das Profil kurz nach dem ersten Start im Hintergrund an
(der *zweite* Kaltstart ist also am schnellsten). Sofort erzwingen:
```bash
adb shell cmd package compile -m speed-profile -f com.papernotes
```

Die `@Preview`-Composables in [`Previews.kt`](app/src/main/java/com/papernotes/ui/preview/Previews.kt)
zeigen Papier-Hintergrund und Karten direkt in der IDE.

> Hinweis: `local.properties` (SDK-Pfad) wird lokal erzeugt und nicht eingecheckt.
