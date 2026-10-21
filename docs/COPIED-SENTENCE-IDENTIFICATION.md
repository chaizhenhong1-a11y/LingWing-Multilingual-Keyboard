# 0.2.10 — Import new copied content when opening recognition

1. Copy one incoming sentence in any app.
2. The normal toolbar 识别, Translate's left clipboard icon, or Translate → More → 识别列表 reads the current eligible plain-text clipboard once on opening. New content is automatically appended. The last imported clip's SHA-256 fingerprint stays in process memory even after Clear: reopening with that same content does not import it again. Copy different content to resume automatic imports. Explicit Paste can deliberately re-add the same clipboard. No background listener or continuous clipboard monitoring is used.
3. One sentence is identified immediately on device. The panel stays open after identification. Tap 粘贴 to add more copied sentences (up to 50 unique entries), or tap an existing sentence to identify it and switch the target. Multiple incoming sentences require choosing one. Uncertain/unsupported/short text keeps the target unchanged.
4. Success sets the detected language as TARGET and SOURCE to Auto Detect. The panel shows the current target. Tap 完成 when ready to return to translation draft mode. Done, hiding the keyboard and ordinary input-session changes retain the temporary list. 清空 erases it; entering secure/password fields or process termination also erases it. Copied references never become your reply. Existing own draft is preserved during identification. This is not background clipboard monitoring or permanent history.

Copy one sentence, then explicitly paste it into this panel before copying another: Android's ordinary clipboard API exposes the latest clip, not earlier overwritten clips. Reopening and pasting appends/deduplicates up to 50 entries in memory.
5. Enter/import your own reply, tap Convert, then select a translation to insert. More → Paste my reply · 粘贴我的原文 imports your own text without changing the target. Latin keys are currently implemented; Chinese Pinyin input is not yet implemented, so Chinese text must currently be imported.

No AccessibilityService, screen reading, screenshots, clipboard listeners or automatic sending. Clipboard reads occur only when you tap the action. Sensitive clips/URI clips and secure fields are excluded. Copied references are not stored in history/logs or passed to the translation repository. No network identification fallback. System clipboard contents are unchanged.

The bundled ML Kit detector uses at most the first 200 characters of the selected sentence. Text is capped at 4000 characters and previews at 50 items. Mixed, short and unsupported languages may require manual choice. Local identification needs neither USB nor OpenAI. Translating your reply still needs the configured backend.

Automated tests use injected model results; actual native identification and layout on HONOR remain pending device acceptance.
