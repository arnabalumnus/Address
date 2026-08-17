# Address Finder

A small Jetpack Compose Android app that finds the user's current street
address from their device's GPS coordinates.

## What it does

1. **Requests location permission** (`ACCESS_FINE_LOCATION` /
   `ACCESS_COARSE_LOCATION`) if it hasn't already been granted.
2. **Prompts the user to turn on device location (GPS)** if it's disabled,
   using the Google Play Services "location settings" system dialog
   (no need to manually send the user to Settings).
3. **Fetches the current latitude/longitude** via the Fused Location
   Provider (`FusedLocationProviderClient`).
4. **Reverse-geocodes** the coordinates into a human-readable address using
   Android's `Geocoder` API.
5. **Displays the address** (full address line, street, locality, district,
   state, postal code, country, etc.) in the UI.
6. **Prints the full, highlighted address details to Logcat** (tag
   `AddressFinder`) every time a lookup succeeds.

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3), single-activity app
- **MVVM**: `AddressViewModel` (`AndroidViewModel`) exposes a `StateFlow<AddressUiState>`
  that drives the UI
- **Google Play Services Location**: `FusedLocationProviderClient` for the
  location fix, `SettingsClient` for the "please enable GPS" resolution dialog
- **Geocoder**: reverse geocoding, using the modern async
  `getFromLocation(lat, lon, maxResults, GeocodeListener)` API on Android 13+
  (API 33) and the classic synchronous call on older versions
- **Kotlin Coroutines** (`kotlinx-coroutines-play-services`) to bridge the
  Play Services `Task` callbacks into suspend functions

## Project structure

```
app/src/main/java/com/example/addressfinder/
├── MainActivity.kt              # Hosts the Compose UI
├── AddressViewModel.kt          # Orchestrates permission → GPS check → fetch → geocode
├── AddressUiState.kt            # Sealed UI state (permission, loading, success, error, ...)
├── AddressLogger.kt             # Formats and prints the resolved address to Logcat
├── location/
│   ├── LocationRepository.kt    # FusedLocationProviderClient + SettingsClient + Geocoder wrapper
│   └── AddressDetails.kt        # Flattened data model of an android.location.Address
└── ui/
    ├── AddressScreen.kt         # Compose screen: permission/GPS/loading/result states
    └── theme/                   # Material 3 theme (Color.kt, Type.kt, Theme.kt)
```

## How the flow works

```
Launch
  │
  ▼
Permission granted? ──No──▶ Show rationale + "Grant permission" button
  │Yes                              │ (user grants)
  ▼                                 │
Check device location settings ◀────┘
  │
  ├─ Already satisfied ──▶ Fetch current location
  │
  └─ GPS disabled ──▶ Show system "Turn on location" dialog
                          │
                          ├─ User enables ──▶ Fetch current location
                          └─ User declines ──▶ Show "Location is off" screen with retry

Fetch current location ──▶ Reverse geocode (lat, lon) ──▶ Address
                                                              │
                                          ┌───────────────────┴───────────────────┐
                                          ▼                                       ▼
                                 Display on screen                     Log full details to Logcat
```

## Running the app

1. Open the project root in **Android Studio** (Ladybug/2024.2 or newer
   recommended).
2. Let Gradle sync (the wrapper will download Gradle 8.10.2 automatically).
3. Run the `app` configuration on a device or emulator running **Android 8.0
   (API 26) or newer**.
   - On an emulator, set a location via the Extended Controls → Location
     panel (or send GPX/KML data) so the fused location provider has a fix
     to return.
4. On first launch, grant the location permission when prompted.
5. If device location is off, tap **"Enable"** on the system dialog that
   appears.
6. The resolved address appears on screen; the same details are printed to
   **Logcat** — filter by tag `AddressFinder` to see them:

   ```
   ==================================================================
   ★★★ ADDRESS RESOLVED ★★★
   ==================================================================
   Latitude         : 37.4219999
   Longitude        : -122.0840575
   Accuracy (m)     : 12.5
   Full address     : 1600 Amphitheatre Parkway, Mountain View, CA 94043, USA
   Feature name     : 1600
   Sub-thoroughfare : 1600
   Thoroughfare     : Amphitheatre Parkway
   Sub-locality     : null
   Locality         : Mountain View
   Sub-admin area   : Santa Clara County
   Admin area       : California
   Postal code      : 94043
   Country          : United States (US)
   ==================================================================
   ```

   Tap **Refresh** on the result screen to run the whole flow again.

## Permissions used

| Permission | Why |
|---|---|
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Read the device's GPS/network location |
| `INTERNET`, `ACCESS_NETWORK_STATE` | `Geocoder`'s backend service and the Play Services location settings check may need network access |

## Requirements

- Android Studio Ladybug (2024.2) or newer
- JDK 17+ (bundled with recent Android Studio versions)
- A device/emulator with Google Play services (required for the Fused
  Location Provider and the location-settings resolution dialog)
- minSdk 26, targetSdk / compileSdk 35
