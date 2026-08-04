import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.ruxor.kermitpiano.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Pkg,
                TargetFormat.Deb,
                TargetFormat.Rpm,
            )
            packageName = "KermitPiano"
            packageVersion = "0.1.0"
        }
    }
}
