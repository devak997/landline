plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
}

allprojects {
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    spotless {
        kotlin {
            target("src/**/*.kt")
            ktlint(
                rootProject.libs.versions.ktlint
                    .get(),
            )
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint(
                rootProject.libs.versions.ktlint
                    .get(),
            )
        }
    }

    detekt {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        parallel = true
    }

    tasks.withType(io.gitlab.arturbosch.detekt.Detekt::class.java).configureEach { jvmTarget = "21" }
    tasks.withType(io.gitlab.arturbosch.detekt.DetektCreateBaselineTask::class.java).configureEach { jvmTarget = "21" }
}
