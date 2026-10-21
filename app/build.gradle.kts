import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

plugins { id("com.android.application"); id("org.jetbrains.kotlin.plugin.compose") }
providers.gradleProperty("lazytalk.buildRoot").orNull?.let { layout.buildDirectory.set(file(it)) }
android {
    namespace = "com.lazytalk.keyboard"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.lazytalk.keyboard"
        minSdk = 26
        targetSdk = 36
        versionCode = 40
        versionName = "0.5.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true }
    androidResources { noCompress += "arsc" }
    bundle { language { enableSplit = false } }
    val cloudUrl = providers.gradleProperty("lazytalk.cloudUrl").orElse("").get()
    require(cloudUrl.isEmpty() || cloudUrl.matches(Regex("https://[a-z0-9.-]+")))
    defaultConfig { buildConfigField("String", "CLOUD_BACKEND_URL", "\"$cloudUrl\"") }
    val authKey = providers.gradleProperty("lazytalk.authKey").orElse("").get()
    require(authKey.isEmpty() || authKey.matches(Regex("[A-Za-z0-9_-]{20,100}")))
    defaultConfig { buildConfigField("String", "AUTH_API_KEY", "\"$authKey\"") }
    val googleClientId = providers.gradleProperty("lazytalk.googleClientId").orElse("").get()
    require(googleClientId.isEmpty() || googleClientId.matches(Regex("[A-Za-z0-9_-]+\\.apps\\.googleusercontent\\.com")))
    defaultConfig { buildConfigField("String", "GOOGLE_CLIENT_ID", "\"$googleClientId\"") }
    buildTypes {
        debug { buildConfigField("String", "BACKEND_URL", "\"\"") }
        release {
            isMinifyEnabled = true
            buildConfigField("String", "BACKEND_URL", "\"\"")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    testOptions { unitTests.isReturnDefaultValues = true; unitTests.isIncludeAndroidResources = true }
}
dependencies {
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation(platform("androidx.compose:compose-bom:2025.08.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.3")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("com.google.mlkit:language-id:17.0.6")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
tasks.withType<Test>().configureEach {
    val testTemp = layout.buildDirectory.dir("test-tmp").get().asFile
    val testHome = layout.buildDirectory.dir("test-home").get().asFile
    systemProperty("java.io.tmpdir", testTemp.absolutePath)
    systemProperty("user.home", testHome.absolutePath)
    systemProperty("maven.repo.local", layout.buildDirectory.dir("robolectric-cache").get().asFile.absolutePath)
    systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
    doFirst { testTemp.mkdirs(); testHome.mkdirs() }
}

val verifyDebugApkPackaging = tasks.register("verifyDebugApkPackaging") {
    group = "verification"
    description = "Reject APKs with a compressed or misaligned Android resource table."
    dependsOn("packageDebug")
    doLast {
        val apk = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile
        ZipFile(apk).use { zip ->
            val resource = checkNotNull(zip.getEntry("resources.arsc")) { "Missing resources.arsc" }
            check(resource.method == ZipEntry.STORED) { "resources.arsc must use ZIP method 0 (STORED)" }
        }
        RandomAccessFile(apk, "r").use { file ->
            fun u16(bytes: ByteArray, offset: Int) = ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xffff
            fun u32(bytes: ByteArray, offset: Int) = ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xffffffffL
            val tail = ByteArray(minOf(file.length(), 65557L).toInt())
            file.seek(file.length() - tail.size); file.readFully(tail)
            val end = (tail.size - 22 downTo 0).firstOrNull {
                u32(tail, it) == 0x06054b50L && it + 22 + u16(tail, it + 20) == tail.size
            } ?: error("APK ZIP end record not found")
            file.seek(u32(tail, end + 16))
            var verified = false
            repeat(u16(tail, end + 10)) {
                val header = ByteArray(46); file.readFully(header)
                check(u32(header, 0) == 0x02014b50L) { "Invalid ZIP directory" }
                val name = ByteArray(u16(header, 28)); file.readFully(name)
                file.skipBytes(u16(header, 30) + u16(header, 32))
                val next = file.filePointer
                if (name.toString(Charsets.UTF_8) == "resources.arsc") {
                    val offset = u32(header, 42)
                    file.seek(offset)
                    val local = ByteArray(30); file.readFully(local)
                    check(u32(local, 0) == 0x04034b50L && u16(local, 8) == 0) { "Invalid resource ZIP local header" }
                    val dataOffset = offset + 30 + u16(local, 26) + u16(local, 28)
                    check(dataOffset % 4 == 0L) { "resources.arsc must be aligned on a 4-byte boundary" }
                    logger.lifecycle("APK packaging verified: resources.arsc STORED, offset=$dataOffset, 4-byte aligned")
                    verified = true
                    file.seek(next)
                }
            }
            check(verified) { "resources.arsc not found in ZIP directory" }
        }
    }
}
tasks.matching { it.name == "assembleDebug" }.configureEach { dependsOn(verifyDebugApkPackaging) }
