pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {

            id("com.android.application") version "8.12.1"
            id("com.android.library") version "8.12.1"
            id("org.jetbrains.kotlin.android") version "1.9.25"

    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "cameraX"
include(":app")
