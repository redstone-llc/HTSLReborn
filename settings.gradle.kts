pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}
rootProject.name = "HTSLReborn"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }

    versionCatalogs {
        create("tegralLibs") {
            from("guru.zoroark.tegral:tegral-catalog:0.0.4")
        }
    }
}