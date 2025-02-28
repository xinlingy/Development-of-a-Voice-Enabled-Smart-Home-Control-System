pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            library("androidx-compose-ui", "androidx.compose.ui:ui:1.6.0")
            library("androidx-compose-foundation", "androidx.compose.foundation:foundation:1.6.0")
            library("androidx-compose-material3", "androidx.compose.material3:material3:1.2.0")
            // 添加其他库...
        }
    }
}

rootProject.name = "My Application"
include(":app")
