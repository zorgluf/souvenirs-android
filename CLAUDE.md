# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Native Android app (Java) for creating "Souvenirs" photo-album books: pages with pannable/zoomable
image, text, handwritten-paint, audio and video tiles, plus a photosphere viewer. Albums can be synced
to and shared from a Nextcloud server running the `souvenirs-nextcloud` server app. Published on F-Droid
as `fr.nuage.souvenirs`. Single Gradle module `:app`. minSdk 24, target/compile SDK 35.

## Build & run

    ./gradlew assembleDebug        # build debug APK -> app/build/outputs/apk/
    ./gradlew installDebug         # build + install on a connected device/emulator
    ./gradlew assembleRelease      # release build (unsigned; minify disabled)
    ./gradlew lint                 # lint (abortOnError is false, so it won't fail the build)
    ./gradlew clean

There are no unit or instrumentation tests in this repo (`app/src/` has only `main`). Do not assume a
test command exists; verify by building.

## Architecture

MVVM-style layering under `app/src/main/java/fr/nuage/souvenirs/`:

- `model/` — domain model + local persistence. An album is a folder on disk holding `album.json`
  (`Album.CONFFILE`) and a `data/` assets subfolder (`Album.DATA_DIR`). `Albums` (singleton via
  `Albums.getInstance()`) tracks the album list and watches the directory; `Album` loads/saves its JSON.
  `Page` holds an ordered list of `Element`s. `Element` is abstract with subclasses
  `ImageElement`, `TextElement`, `PaintElement`, `AudioElement`, `VideoElement`, `UnknownElement`;
  each serializes itself to/from JSON. `TilePageBuilder` computes tile layouts for a page.
- `model/nc/` — a parallel mirror of the **Nextcloud server** state, one class per model class
  (`AlbumNC`, `PageNC`, `ImageElementNC`, …). `NCAPI` is the Retrofit REST interface (endpoint
  `/index.php/apps/souvenirs/apiv2/`); `APIProvider` builds it through Nextcloud **SingleSignOn**
  (`SingleAccountHelper`) — so sync requires the Nextcloud Android client installed with an account
  selected. `AlbumsNC`/`AlbumNC` expose state via LiveData and make async HTTP calls.
- `viewmodel/` — `*ViewModel` classes wrap model objects and expose their fields as `LiveData` for
  DataBinding. `AlbumViewModel` ties together a local `Album` and its remote `AlbumNC`.
- `view/` — Fragments, custom `View`s (`PageView`, `ImageElementView`, `PaintElementView`, `PanoView`),
  RecyclerView adapters, and dialogs. `view/helpers/ViewGenerator` inflates element views (DataBinding +
  Glide for image loading); `DataBindingAdapters` holds `@BindingAdapter`s.

When adding a new element type or model field, the change usually spans all three layers plus the `nc`
mirror: `model/XxxElement` ⇄ `model/nc/XxxElementNC` ⇄ `viewmodel/XxxElementViewModel` ⇄
`view/XxxElementView` (+ JSON keys on both `model` and `nc` sides must match the server API contract).

## Sync

`SyncService` is a foreground (`dataSync`) service started by `SyncService.startSync(...)`. It runs
`viewmodel/SyncToNextcloudThread`, which reconciles a local `Album` with its remote `AlbumNC`:
creating either side if missing and pushing/pulling pages based on last-edit vs last-sync dates.
Network/account availability is surfaced through `viewmodel/utils/NCUtils` (initialized in `App`).

The remote `apiv2/` REST API that `model/nc/NCAPI` talks to is implemented by the **Nextcloud server-side
Souvenirs app** (separate `souvenirs-nextcloud` project, PHP). A deployed copy lives at
`/var/snap/nextcloud/53545/nextcloud/extra-apps/souvenirs`; the endpoint handlers are in
`lib/Controller/Api2Controller.php` (routes in `appinfo/routes.php`). Consult it when changing the sync
contract so the Android `NCAPI` interface and the server stay in agreement.

## UI entry points

- `AlbumListActivity` — launcher; hosts the Navigation component graph `res/navigation/nav_main.xml`
  (album list → edit album → edit/show page fragments).
- `SettingsActivity` — preferences, including Nextcloud account selection (`SettingsActivity` constants
  are the canonical SharedPreferences keys).
- `AddImageToAlbumActivity` — `ACTION_SEND`/`SEND_MULTIPLE` share target for images/videos.
- `PanoViewerActivity` / `PanoView` — WebView rendering photosphere images via the photo-sphere-viewer
  JS bundle in `app/src/main/assets/`.

## Conventions

- UI is built with **DataBinding** + **ViewBinding** (both enabled) and **Navigation SafeArgs**; generated
  binding classes come from the layout XML — add/rename layouts rather than hand-writing binding classes.
- Model fields are mirrored as `MutableLiveData` (`ldXxx`) alongside the plain field; update both and call
  the relevant `postValue`/`updateAllLiveDataObject` so the UI observes changes.
- Third-party libs: Glide (images), Retrofit (REST), Nextcloud Android-SingleSignOn (auth), Material
  Components. Java 8 source/target with core library desugaring.
- String literals shown to users live in `res/values*/strings.xml` (French translations in `values-fr/`).
