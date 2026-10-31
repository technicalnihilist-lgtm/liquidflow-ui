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

rootProject.name = "liquidflow-ui"

include(
    ":liquidflow-core",
    ":liquidflow-renderer",
    ":liquidflow-motion",
    ":liquidflow-glass",
    ":liquidflow-adaptive",
    ":liquidflow-widgets",
    ":sample-app",
)
