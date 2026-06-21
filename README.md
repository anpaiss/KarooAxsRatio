![Total Downloads](https://img.shields.io/github/downloads/anpaiss/KarooAxsRatio/total?color=blue)
![Latest Release](https://img.shields.io/github/v/release/anpaiss/KarooAxsRatio)

# Karoo AXS Ratio — Corner Overlay for Hammerhead Karoo

> **Public beta 0.9** — install the APK from the [latest release](../../releases/latest).

A lightweight Hammerhead Karoo extension that draws small, always-visible metric
tiles in the four corners of the ride screen, on top of whatever page you are
viewing. It was born to keep the current **SRAM AXS rear gear** in sight at all
times, and grew to show a handful of other live ride metrics.

## What it shows

Each metric can be placed in one of the four corners (Top-left, Top-right,
Bottom-left, Bottom-right) or turned off. Picking a corner that is already taken
moves the previous metric to *Off*.

| Metric        | Notes |
|---------------|-------|
| **Gear**      | Current AXS rear gear/cog. Tile turns black for cogs ≥ 10. |
| **HR**        | Heart rate, with the tile colored by HR zone (see below). |
| **Power**     | Watts. |
| **Cadence**   | RPM. |
| **Speed**     | km/h. |
| **Grade**     | Elevation grade, signed (e.g. `+4%`). |
| **Temp**      | Temperature in °C. |
| **To next turn** | Distance to the next navigation turn (`m` / `k`). |

### HR zone colors

| Zone | Color  |
|------|--------|
| 1    | Grey   |
| 2    | Blue   |
| 3    | Green  |
| 4    | Orange |
| 5    | Red    |

Tiles use an adaptive text color: white on dark backgrounds, black on light ones
(orange/green), so the value stays readable in every zone.

## Install

1. Download `app-debug.apk` from the [latest release](../../releases/latest).
2. Sideload it onto the Karoo (e.g. `adb install -r app-debug.apk`, or copy and
   open it on the device).
3. Open **AXS Ratio** from the app list.
4. Grant the **overlay** ("Display over other apps") permission when prompted.
5. Tap **Enable Overlay**, then assign each metric to a corner.
6. Use **Preview** to cycle through placed metrics and check positioning.

The overlay runs as a foreground service and re-starts on boot, so it stays up
across rides.

> This is a **beta** distributed as a debug-signed APK. It is not on the
> Hammerhead app store.

## Build from source

Requires JDK 17+ (the Android Studio bundled JBR works) and the Android SDK.

```bash
./gradlew assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
```

Built against the [Hammerhead `karoo-ext`](https://github.com/hammerheadnav/karoo-ext)
SDK. `minSdk 23`, `targetSdk 34`.

## Status

Personal project, shared as-is. Feedback and issues welcome.
