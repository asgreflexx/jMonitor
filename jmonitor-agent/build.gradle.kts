plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}

dependencies {
    // Bundled (shaded) into the agent jar so the target JVM can load it without
    // ByteBuddy on its own classpath.
    implementation("net.bytebuddy:byte-buddy:1.15.11")
}

tasks.shadowJar {
    // A fixed name the server can locate as a bundled resource.
    archiveBaseName.set("jmonitor-agent")
    archiveClassifier.set("")
    archiveVersion.set("")
    manifest {
        attributes(
            "Agent-Class" to "com.jmonitor.agent.JMonitorAgent",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true",
        )
    }
}

// `assemble`/`build` should produce the shaded agent jar.
tasks.named("assemble") {
    dependsOn(tasks.shadowJar)
}
