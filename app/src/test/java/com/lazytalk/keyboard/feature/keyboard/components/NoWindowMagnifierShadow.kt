package com.lazytalk.keyboard.feature.keyboard.components

import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

// Robolectric has no real SurfaceSession for the Android 28 selection magnifier.
// Keep selection/clipboard logic real; omit only the unrelated visual popup in this gesture test.
@Implements(android.widget.Magnifier::class)
class NoWindowMagnifierShadow {
    @Implementation fun show(sourceCenterX: Float, sourceCenterY: Float) {}
    @Implementation fun dismiss() {}
}
