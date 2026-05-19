plugins {
    alias(libs.plugins.agp.app)
}

val appVersion = System.getenv("APP_VERSION") ?: "1.0.0"

android {
    namespace = "io.github.zypho.xpdebug"
    compileSdk = 33

    defaultConfig {
        minSdk = 26
        targetSdk = 33
        versionCode = appVersion
            .replace("v", "")
            .split(".")
            .map { it.toInt() }
            .let { it[0] * 10000 + it.getOrElse(1) { 0 } * 100 + it.getOrElse(2) { 0 } }
        versionName = appVersion.replace("v", "")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles("proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }
}

dependencies {
    compileOnly(libs.libxposed.api)
}
