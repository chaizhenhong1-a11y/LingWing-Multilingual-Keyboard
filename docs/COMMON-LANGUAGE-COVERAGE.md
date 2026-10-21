# Common-language coverage — 0.3.0

59 language categories / 77 layouts, counting region/script/layout variants separately in the layout total. This is the common-language milestone requested by the user, not a claim of worldwide coverage or Gboard feature parity.

## Phonetic / composing engines

- Simplified Chinese: full pinyin and candidate selection, AOSP-derived 55,320-entry offline dictionary with basic segmentation. No neural prediction, handwriting or Cantonese engine.
- Japanese: romaji to hiragana/katakana and 767,686 filtered Mozc dictionary entries, capped at 48 candidates per reading and loaded by initial-character shard. Word-by-word conversion, not full Mozc sentence conversion.
- Korean: two-set jamo to syllable composition including compound vowels, finals and resyllabification. No predictive word dictionary.
- Vietnamese: basic Telex vowel shapes and five tones; common combinations tested. No prediction dictionary.

## Direct layouts

Language-specific lower/upper token rows and popup alternatives are imported from FlorisBoard at pinned commit fe1241f4921b3eae923571ff7a3e113a7e10d677. Multi-code-point tokens are preserved. First ten top-row keys retain numeric long presses; where accents coexist, long press opens character choices. Key geometry remains LTR independently of the system locale; entered text keeps its natural Unicode direction.

Language choice is persisted independently of translation target. Switching needs no download; the APK contains dictionaries and layouts. Translation still uses the separately configured OpenAI backend. Keyboard language availability does not imply availability as a translation target.

## Attribution

License and original authors in third_party/florisboard, third_party/pinyin, third_party/mozc and APK assets/licenses. FlorisBoard Apache-2.0; AOSP dictionary Apache-2.0; Mozc license plus IPAdic/Okinawa notices in README.txt. Source references and transformation notes accompany each dataset.

## Acceptance

Automated sample dictionary, composition, cursor, selection, debounce and input-connection tests are run. Every language has not been reviewed by a native speaker. Physical-phone acceptance is still required; tool-side ADB cannot access the user's phone.
