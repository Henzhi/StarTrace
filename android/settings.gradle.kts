pluginManagement {
    repositories {
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/google/") }
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/google/") }
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        google()
        mavenCentral()
    }
}

rootProject.name = "StarTrace"
include(":app")
