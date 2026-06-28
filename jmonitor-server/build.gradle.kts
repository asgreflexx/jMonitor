import com.github.gradle.node.npm.task.NpmTask

plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.github.node-gradle.node") version "7.1.0"
}

dependencies {
    implementation(project(":jmonitor-common"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// ---------------------------------------------------------------------------
// Frontend integration
//
// The React/Vite app lives in ../frontend and builds into build/frontend-dist
// (see frontend/vite.config.ts). That output is folded into the boot jar under
// /static, so a single `java -jar` serves both the API and the GUI.
//
// `buildFrontend` downloads its own Node (via the node-gradle plugin), so it
// does not depend on whatever Node happens to be on the developer's PATH.
//
//   ./gradlew build               -> fast; bundles an existing frontend-dist
//   ./gradlew build -Pfrontend    -> also (re)builds the frontend first
// ---------------------------------------------------------------------------
node {
    version.set("22.12.0")
    download.set(true)
    nodeProjectDir.set(file("../frontend"))
}

val buildFrontend by tasks.registering(NpmTask::class) {
    dependsOn(tasks.named("npmInstall"))
    args.set(listOf("run", "build"))
    inputs.dir(file("../frontend/src"))
    inputs.file(file("../frontend/package.json"))
    outputs.dir(layout.projectDirectory.dir("build/frontend-dist"))
}

val frontendDist = layout.projectDirectory.dir("build/frontend-dist")

tasks.processResources {
    if (project.hasProperty("frontend")) {
        dependsOn(buildFrontend)
    }
    if (frontendDist.asFile.exists() || project.hasProperty("frontend")) {
        from(frontendDist) {
            into("static")
        }
    }
}
