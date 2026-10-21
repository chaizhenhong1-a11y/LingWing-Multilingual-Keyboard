# 0.2.1 — chat accessibility feature removed

Update for 0.2.2: a bundled language-ID model is used again solely for user-confirmed copied-sentence identification. The accessibility service, WhatsApp reader, monitoring and automatic chat detection remain removed. See COPIED-SENTENCE-IDENTIFICATION.md.

At the user's request, the experimental chat language assist has been removed, rather than left behind a disabled switch.

- No AccessibilityService component or accessibility metadata is declared.
- WhatsApp tree reading, incoming-message selection and automatic reply-language switching were deleted.
- The enabling/disclosure UI, feature preference and in-memory hint store were deleted.
- The ML Kit dependency, bundled native detector and language-identification model were removed.
- Manual translation-target selection, offline language choices, the normal keyboard and explicit Convert-to-OpenAI flow remain.

Install this update over 0.2.0 using `adb install -r`. The package no longer offers the chat recognition service in Android accessibility settings. Disable the old service manually before updating if it was enabled. Updating/removing the service does not prove anything about historical data exposure; no chat-history storage/upload mechanism was implemented in the removed feature.

Verification checks the merged Android package for no accessibility service, confirms that only the IME service remains, reruns existing layout/translation/language-list tests, and scans the final APK for the removed model/native libraries and feature package.
