# PaperNotes 🌼

Eine minimalistische Notizen-App für Android mit "Premium-Papieroptik": warme Glücks-Farben,
physikbasierte Animationen und haptische Gimmicks. Muji-Stil trifft Material Design 3.

## Tech-Stack
- **Jetpack Compose** (Kotlin) · Material 3
- **minSdk 33** (Android 13) · compileSdk/targetSdk 35 — `RuntimeShader`/AGSL für die Papiertextur
- **Room** (Notizen) · **DataStore** (Teebeutel-Status) · **Hilt** (DI)
- Shared-Element-Transitions, Spring-Physics, AGSL-Shader, native Haptics

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
```

## Bauen & Starten
Voraussetzung: Android Studio (Ladybug+) oder Android SDK 35 + JDK 17.

```bash
# Debug-APK bauen
./gradlew :app:assembleDebug

# Auf angeschlossenem Gerät/Emulator (API 33+) installieren & starten
./gradlew :app:installDebug
```

In Android Studio: Projekt öffnen → Gerät/Emulator (API 33+) wählen → **Run**.
Die `@Preview`-Composables in [`Previews.kt`](app/src/main/java/com/papernotes/ui/preview/Previews.kt) zeigen
Papier-Hintergrund und Karten direkt in der IDE.

> Hinweis: `local.properties` (SDK-Pfad) wird lokal erzeugt und nicht eingecheckt.
