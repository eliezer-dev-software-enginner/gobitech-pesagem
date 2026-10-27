rootProject.name = "gobitech"
val core = file("../../framework/libraries/megalodonte-core")
if (providers.gradleProperty("useLocalSources").orNull != "false" && core.isDirectory) includeBuild(core)
