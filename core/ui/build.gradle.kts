import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream
import java.io.InputStreamReader

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val deployConfig = Properties()
val deployConfigFile = rootProject.file("deploy-config.properties")
if (deployConfigFile.exists()) {
    deployConfig.load(InputStreamReader(FileInputStream(deployConfigFile), Charsets.UTF_8))
}

android {
    namespace = "biz.smt_life.android.core.ui"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        val defaultApiHost = deployConfig.getProperty("default.api.host") ?: ""
        buildConfigField("String", "DEFAULT_API_HOST", "\"$defaultApiHost\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:designsystem"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
}
