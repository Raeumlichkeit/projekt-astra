import java.util.Properties
import java.util.zip.ZipFile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}

abstract class PreparePrivacyAssets : DefaultTask() {
    @get:InputFile abstract val policy: RegularFileProperty
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty
    @TaskAction fun generate() {
        val dir = outputDirectory.get().asFile.apply { mkdirs() }
        policy.get().asFile.copyTo(dir.resolve("privacy-policy.html"), overwrite = true)
    }
}

val privacyAssets = tasks.register<PreparePrivacyAssets>("preparePrivacyAssets") {
    policy.set(rootProject.layout.projectDirectory.file("play-store/privacy-policy.html"))
    outputDirectory.set(layout.buildDirectory.dir("generated/privacyAssets"))
}

android {
    namespace = "de.projektastra.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "de.projektastra.app"
        minSdk = 28
        targetSdk = 37
        versionCode = 16
        versionName = "1.1.7-pre.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

// Ordinary bundleRelease remains useful for technical tests without publishing credentials.
tasks.register("verifyPlayRelease") {
    dependsOn("bundleRelease")
    doLast {
        check(keystorePropertiesFile.exists()) { "Upload-Key fehlt: Bundle ist nicht für Play signiert." }
        check(!rootProject.file("play-store/privacy-policy.html").readText().let {
            it.contains("RELEASE_BLOCKER") || it.contains("LEGAL_REVIEW_PENDING")
        }) {
            "Datenschutzfreigabe steht noch aus. Nicht veröffentlichen."
        }
        ZipFile(layout.buildDirectory.file("outputs/bundle/release/app-release.aab").get().asFile).use { zip ->
            check(zip.entries().asSequence().any { it.name.matches(Regex("META-INF/.*\\.(RSA|EC|DSA)")) }) {
                "Bundle enthält keine Signatur. Nicht hochladen."
            }
        }
    }
}

dependencyLocking { lockAllConfigurations() }

androidComponents.onVariants { variant ->
    variant.sources.assets?.addGeneratedSourceDirectory(privacyAssets, PreparePrivacyAssets::outputDirectory)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.camera:camera-camera2:1.6.2")
    implementation("androidx.camera:camera-lifecycle:1.6.2")
    implementation("androidx.camera:camera-view:1.6.2")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.core.locationbutton:locationbutton-compose:1.0.0-alpha01")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0") {
        exclude(group = "org.bouncycastle")
    }
    implementation("io.github.cosinekitty:astronomy:2.1.19")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20260814")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
}
