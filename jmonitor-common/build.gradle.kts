plugins {
    java
    `java-library`
}

dependencies {
    // Pure DTO/model module — kept dependency-free on purpose so it can be
    // shared by the server and (later) the java-agent without dragging in
    // Spring or other heavy transitive dependencies.
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
