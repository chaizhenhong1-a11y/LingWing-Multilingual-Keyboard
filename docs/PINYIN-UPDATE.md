# LazyTalk Keyboard 0.4.5

Added offline dictionary-derived syllable-initial indexing and bounded phrase search for abbreviated and mixed pinyin. Full-pinyin exact words retain priority. Initial matching is frequency-based and does not provide Gboard-equivalent contextual prediction. Launcher and Settings share one page. Enable and Select keyboard show actual system status. The old demo home and Mock examples are removed. Settings reorganized into compact rows and separate Translation, Language history, Privacy, About and debug-only connection pages.
App language defaults to the system; Simplified Chinese and English overrides are persisted independently from typing layout and translation target. Other system languages fall back to English for localized strings.
A fixed globe/Language recovery entry is available in Settings, the keyboard-language chooser, clipboard panel and translation options. Keyboard-language names use the interface locale plus native names.
Localized the primary controls in the three keyboard panels and account pages. Provider-generated messages and some legacy/developer descriptions retain their original language.
Validation: 113 Android tests passed, assembleDebug/lintDebug passed; resources.arsc stored and four-byte aligned; ZIP integrity and embedded-secret scans passed. No physical-device installation performed for this release.
Version: 0.4.5 (34)
SHA256: E2C4C9647E1732589CE01308DFF2362B98A4B45D7BAC1FACC5ACAAE11A09B461
