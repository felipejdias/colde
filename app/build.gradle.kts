import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val keystoreProps = Properties().apply {
    val propsFile = rootProject.file("keystore.properties")
    if (propsFile.exists()) propsFile.inputStream().use(::load)
}

fun signingSecret(name: String): String? =
    System.getenv(name) ?: keystoreProps.getProperty(name)

android {
    namespace = "app.colde"
    compileSdk = 36

    signingConfigs {
        create("release") {
            storeFile = signingSecret("RELEASE_STORE_FILE")?.let { rootProject.file(it) }
            storePassword = signingSecret("RELEASE_STORE_PASSWORD")
            keyAlias = signingSecret("RELEASE_KEY_ALIAS")
            keyPassword = signingSecret("RELEASE_KEY_PASSWORD")
        }
    }

    defaultConfig {
        applicationId = "app.colde"
        minSdk = 26
        targetSdk = 36
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "BUILD_TIMESTAMP",
            "\"${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}\""
        )
    }

    buildTypes {
        release {
            signingConfig = if (signingSecret("RELEASE_STORE_FILE") != null)
                signingConfigs.getByName("release")
            else
                signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "lib/x86_64/darwin/libscrypt.dylib"
            excludes += "lib/x86_64/freebsd/libscrypt.so"
            excludes += "lib/x86_64/linux/libscrypt.so"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    // BitcoinJ
    implementation(libs.bitcoinj.core)
    implementation("org.slf4j:slf4j-nop:2.0.16")

    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}