import org.gradle.api.DefaultTask
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

plugins {
    id("com.android.application") version "9.2.0" apply false
    id("com.android.library") version "9.2.0" apply false
    id("org.jetbrains.kotlin.jvm") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("com.google.devtools.ksp") version "2.3.10" apply false
    id("androidx.room3") version "3.0.2" apply false
}

abstract class VerifyModuleBoundariesTask : DefaultTask() {
    @get:Input
    abstract val actualDependencies: MapProperty<String, Set<String>>

    @get:Input
    abstract val allowedDependencies: MapProperty<String, Set<String>>

    @TaskAction
    fun verify() {
        allowedDependencies.get().forEach { (modulePath, allowed) ->
            val actual = actualDependencies.get().getValue(modulePath)
            check(actual == allowed) {
                "$modulePath project dependencies must be $allowed but were $actual"
            }
        }
    }
}

val declaredProjectDependencies = mapOf(
    ":domain" to file("domain/build.gradle.kts"),
    ":data" to file("data/build.gradle.kts"),
    ":ai" to file("ai/build.gradle.kts"),
    ":app" to file("app/build.gradle.kts"),
).mapValues { (_, buildFile) ->
    Regex("""project\(\"(:[^\"]+)\"\)""")
        .findAll(buildFile.readText())
        .map { match -> match.groupValues[1] }
        .toSet()
}

tasks.register<VerifyModuleBoundariesTask>("verifyModuleBoundaries") {
    group = "verification"
    description = "Checks the four-module dependency direction defined by ADR-001."
    actualDependencies.set(declaredProjectDependencies)
    allowedDependencies.set(
        mapOf(
            ":domain" to emptySet(),
            ":data" to setOf(":domain"),
            ":ai" to setOf(":domain"),
            ":app" to setOf(":domain", ":data", ":ai"),
        ),
    )
}

tasks.register("gate00") {
    group = "verification"
    description = "Runs every automatable M00 foundation gate."
    dependsOn(
        "verifyModuleBoundaries",
        ":domain:test",
        ":data:testDebugUnitTest",
        ":ai:testDebugUnitTest",
        ":app:testDebugUnitTest",
        ":app:testReleaseUnitTest",
        ":data:assembleDebugAndroidTest",
        ":app:assembleDebugAndroidTest",
        ":app:lint",
        ":app:assembleRelease",
    )
}

tasks.register("gate01") {
    group = "verification"
    description = "Runs the complete M01 intake gate after Gate-00."
    dependsOn(
        "gate00",
        ":app:assembleDebug",
    )
}
