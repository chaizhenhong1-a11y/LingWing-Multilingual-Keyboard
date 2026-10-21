# LingWing Keyboard 0.5.4

Added persisted Light (15 ms), Standard (30 ms) and Strong (45 ms) key vibration levels. Standard is the default. Choosing a level or enabling vibration previews the selected pulse. Disabling vibration prevents app key pulses. Existing full touch cells and keycap spacing are preserved.
The connected HONOR does not report amplitude control, so levels vary pulse duration rather than claiming unsupported amplitude changes. System touch feedback must also be enabled on this phone; the user approved that setting change during 0.5.3 verification.
121 Android tests passed; assembleDebug and lintDebug passed. Settings persistence and strength selection tested. APK ZIP integrity, resources.arsc compression/alignment, and embedded-private-key checks passed. Physical perception remains device dependent.
Version: 0.5.4 (39)
SHA256: 2170C1CF7DF96C2EC9269CDD150710A6EEB5538E24558FA6ECD91BD5BFBBA098
