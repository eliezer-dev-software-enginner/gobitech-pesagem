rootProject.name = "gobitech"
if (providers.gradleProperty("useLocalSources").orNull != "false") {
    val libraries = file("../../framework/libraries")
    listOf("core", "base-desktop", "components", "reactivity", "router", "theme")
        .map { libraries.resolve("megalodonte-$it") }
        .filter { it.isDirectory }
        .forEach { includeBuild(it) }
}
