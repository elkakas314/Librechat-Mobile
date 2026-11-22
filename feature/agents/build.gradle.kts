import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("librechat.kmp.feature")
}

android {
    namespace = "com.garfiec.librechat.feature.agents"
}

kotlin {
    targets.withType<KotlinNativeTarget>().configureEach {
        compilations.getByName("main").cinterops.create("nostromoThermalBridge") {
            definitionFile.set(project.file("src/iosMain/cinterop/nostromo_thermal_bridge.def"))
            compilerOpts("-I${project.file("src/iosMain/cinterop").absolutePath}")
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":feature:agents:ui"))
            implementation(project(":core:network"))
            implementation(libs.coil3.compose)
            implementation(libs.coil3.network.ktor)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kermit)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
        getByName("androidUnitTest").dependencies {
            implementation(libs.koin.test)
        }
    }
}
