plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

/**
 * Shared publishing metadata. Modules apply [com.android.library] and the maven-publish
 * plugin, and this block keeps group/version consistent across the framework.
 */
allprojects {
    group = "dev.liquidflow"
    version = "1.0.0"
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
