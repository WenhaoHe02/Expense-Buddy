pluginManagement {
    repositories {
        // 阿里云 Gradle 插件仓库
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }

        // 阿里云公共仓库（也支持 Android 插件）
        maven { url = uri("https://maven.aliyun.com/repository/public") }

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
        // 阿里云加速
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }

        google()
        mavenCentral()
    }
}

rootProject.name = "Agent"
include(":app", ":OcrLibrary", "OpenCV")

