# LazyTalk 0.4.1 account verification

- Android: 106 tests, zero failures; assembleDebug/lintDebug passed.
- Backend: 24 offline tests passed.
- Cloud: real registration/login, unverified-email rejection, actual OpenAI translation, durable quota 50 -> 49, exhausted-quota 429, disabled-user 401 and rejection of shared access codes passed. Disposable test user and usage document removed. No verification mail was sent to a third party during tests.
- Redesigned independent login/registration screen, Google sign-in entry, password visibility control, compact Settings account card and unlimited-plan state. Login submission, register switching, landscape scrolling and unlimited UI tests passed; native-rendered previews exported.
- Keystore refresh-token encryption implemented; ID token stays in memory. Automatic renewal and logout invalidation tested using a fake SessionStore. Real Android Keystore and account chooser require physical-phone acceptance.
- Google Credential Manager integration and package/signing registration implemented. Public Google client ID configured: True. Real Google interactive sign-in has not been tested on the phone.
- The confirmed businessstanley8@gmail.com account has server-side UID-bound owner_free entitlement and no personal/service daily count cap or personal ten-per-minute count quota. Real deployed OpenAI translation succeeded after 5000 recorded requests. Final allowlist contains only this real UID; temporary account and entitlement removed. Ordinary users retain 50/user, 500/service; max 500 characters, 10 requests/minute/user, one in-flight request/user, durable transaction reservations. This is a request-count cap, not an exact US-dollar budget. No App Check, subscription or anonymous trial yet.
- resources.arsc STORED and four-byte aligned at 30091864; ZIP CRCs and private-credential scans passed.
- SHA256: 0EF07334DC5E75DA7A8200EAFFAE4CC6AF50D7CEA7617A97CEC82592ACE754F9
