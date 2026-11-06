# Overlay theme and touch area — 0.1.50 (51)

## Controls

- Read/overlay page: restored pickup touch-area slider, 100–300%, with a live preview. `pickupTouchPercent` is reused; existing values are retained and clamped by the existing geometry helper. The percentage scales label hit width and height, not the drawn label. At 300%, width and height are each 3 times the label size.
- Left navigation from both map and native pages: **ธีมการวาดทับ**. The theme page has a drawing preview, seven color controls, swatches, HEX `#RRGGBB` / `#AARRGGBB`, and confirmed color reset. Reset removes only color overrides and preserves touch-area settings.

## Color mapping

| Preference | Drawing |
|---|---|
| rootOverlayBackground | All data-label and detail-panel backgrounds |
| rootOverlayText | Data values/text |
| rootOverlayLabel | Data headings/captions |
| rootOverlayBorder | Data-label and detail-panel outlines |
| pickupOverlayBackground | Coordinate-label background |
| pickupOverlayText | Coordinate text |
| pickupOverlayBorder | Coordinate-label outline |

`OverlayPalette` shares defaults and names. `ScreenActions.controls` reads the persisted settings and publishes an `overlayStyleKey`. Both list/detail drawing caches include that key. Coordinate tags update colors in place and invalidate themselves. No style values remain fixed in these active drawing paths.

The existing direct-GPS pickup action, touch validation, scroll cancellation, coordinate matching and read-only job behavior are retained. No filter/open/accept feature is restored. On first installing new injected drawing code, close/reopen Driver so it loads the new APK; subsequent color/area changes are delivered through existing configuration polling without reinstalling.

## Validation

Build, instrumentation APK, lint and 47 GPS/policy unit tests pass. `OverlaySettingsChecks` is built and can be run with instrumentation `-e mode overlay`; it checks 7 color transports and their drawing-cache revisions, touch limits/geometry, and preserved direct-GPS/read-only flags, restoring existing preferences afterwards. Device execution and live Driver drawing are pending until ADB reconnects. Successful compilation alone does not prove visual/touch behavior on Driver.
