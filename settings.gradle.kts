pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // LiveKit's audio-routing dependency (audioswitch) is published on JitPack.
        // Build-time artifact repository only; nothing is contacted at runtime.
        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "WePlay"
include(":app")
