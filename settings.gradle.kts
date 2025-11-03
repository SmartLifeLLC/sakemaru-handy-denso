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
}

rootProject.name = "sakemaru-handy-denso"
include(":app")
include(":core:domain")
include(":core:network")
include(":core:designsystem")
include(":core:ui")
include(":feature:login")
include(":feature:inbound")
include(":feature:outbound")
include(":feature:main")
include(":feature:settings")
