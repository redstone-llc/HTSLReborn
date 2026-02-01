pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.7.10"
}


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

stonecutter {
    create(rootProject) {
        versions("1.21.11")
        vcsVersion = "1.21.11"
    }
}

rootProject.name = "HTSLReborn"