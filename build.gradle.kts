/*
 * Copyright 2020 Andrew Geery
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.jfrog.bintray.gradle.BintrayExtension.PackageConfig
import com.jfrog.bintray.gradle.BintrayExtension.VersionConfig
import dev.bombinating.xml.deserializer.gradle.junitVersion
import dev.bombinating.xml.deserializer.gradle.kotlinVersion
import dev.bombinating.xml.deserializer.gradle.logbackVersion
import dev.bombinating.xml.deserializer.gradle.microutilsVersion
import net.researchgate.release.GitAdapter
import net.researchgate.release.ReleaseExtension
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig
import java.util.Base64

val mavenStagingUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
val mavenSnapshotUrl = uri("https://oss.sonatype.org/content/repositories/snapshots")
val vendor = "Bombinating Development"
val libName = rootProject.name
val pubName = "library"
val kdocLoc = "$buildDir/kdoc"
val gitUrl = "https://github.com/bombinating/$libName.git"
val projectUrl = "https://github.com/bombinating/$libName"

repositories {
    mavenCentral()
    jcenter()
}

plugins {
    kotlin("jvm")
    `maven-publish`
    signing
    id("com.gradle.plugin-publish")
    id("com.jfrog.artifactory")
    id("com.jfrog.bintray")
    id("io.codearte.nexus-staging")
    id("io.gitlab.arturbosch.detekt")
    id("net.researchgate.release")
    id("org.jetbrains.dokka")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api(group = "io.github.microutils", name = "kotlin-logging", version = microutilsVersion)
    implementation(group = "ch.qos.logback", name = "logback-classic", version = logbackVersion)
    testImplementation(group = "org.jetbrains.kotlin", name = "kotlin-test-junit5", version = kotlinVersion)
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-params", version = junitVersion)
    testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = junitVersion)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.allWarningsAsErrors = true
    kotlinOptions.verbose = true
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<Jar> {
    from("$projectDir/LICENSE.txt") {
        into("META-INF")
    }
    into("META-INF/maven/${project.group}/${project.name}") {
        from({ tasks.findByName("generatePomFileFor${pubName.capitalize()}Publication") })
        rename(".*", "pom.xml")
    }
}

val sourcesJar = tasks.create<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
    manifest {
        attributes(
            mapOf(
                "Created-By" to "Gradle ${gradle.gradleVersion}",
                "Build-Jdk-Spec" to JavaVersion.current().majorVersion,
                "Specification-Title" to project.description,
                "Specification-Version" to project.version.toString().replace("""(\d+\.\d+)(\..*)""".toRegex()) {
                    it.groups[1]?.value ?: "Unknown"
                },
                "Specification-Vendor" to vendor,
                "Implementation-Title" to project.description,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to vendor
            )
        )
    }
}

val dokka = tasks.withType<DokkaTask> {
    outputFormat = "html"
    outputDirectory = kdocLoc
    configuration {
        jdkVersion = 8
    }
}

val dokkaJar = tasks.create<Jar>("dokkaJar") {
    archiveClassifier.set("javadoc")
    from(dokka)
}

publishing {
    publications {
        create<MavenPublication>(pubName) {
            pom {
                name.set(project.description)
                description.set("Kotlin JVM library for quickly and efficiently converting an XML document into a sequence of objects")
                url.set(projectUrl)
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("bombinating")
                        name.set(vendor)
                        email.set("bombinating.dev@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:$gitUrl")
                    developerConnection.set("scm:git:$gitUrl")
                    url.set(projectUrl)
                }
            }
            from(components.getByName("java"))
            artifact(sourcesJar)
            artifact(dokkaJar)
        }
    }
    repositories {
        val mavenCentralUser: String? by project
        val mavenCentralKey: String? by project
        maven {
            name = "mavenCentral"
            url = mavenStagingUrl
            credentials {
                username = mavenCentralUser
                password = mavenCentralKey
            }
        }
        maven {
            name = "mavenCentralSnapshots"
            url = mavenSnapshotUrl
            credentials {
                username = mavenCentralUser
                password = mavenCentralKey
            }
        }
    }
}

bintray {
    val bintrayUser: String? by project
    val bintrayKey: String? by project
    user = bintrayUser
    key = bintrayKey
    publish = true
    setPublications(pubName)
    pkg(delegateClosureOf<PackageConfig> {
        repo = "maven"
        name = libName
        setLicenses("Apache-2.0")
        vcsUrl = gitUrl
        githubRepo = "bombinating/$libName"
        githubReleaseNotesFile = "README.adoc"
        version(delegateClosureOf<VersionConfig> {
            name = "$version"
            vcsTag = "$version"
        })
    })
}

artifactory {
    val bintrayUser: String? by project
    val bintrayKey: String? by project
    setContextUrl("https://oss.jfrog.org/artifactory")
    publish(delegateClosureOf<PublisherConfig> {
        repository(delegateClosureOf<groovy.lang.GroovyObject> {
            setProperty("repoKey", "oss-snapshot-local")
            setProperty("username", bintrayUser)
            setProperty("password", bintrayKey)
            setProperty("maven", true)
        })
        defaults(delegateClosureOf<groovy.lang.GroovyObject> {
            invokeMethod("publications", pubName)
            setProperty("publishArtifacts", true)
            setProperty("publishPom", true)
        })
    })
}

fun ReleaseExtension.git(config: GitAdapter.GitConfig.() -> Unit) {
    (propertyMissing("git") as GitAdapter.GitConfig).config()
}

release {
    git {
        requireBranch = "master"
        pushReleaseVersionBranch = "release"
    }
}

signing {
    val signingKeyEncoded: String? by project
    val signingPassword: String? by project
    val signingKey = signingKeyEncoded?.let { String(Base64.getDecoder().decode(it)) }
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications[pubName])
}

nexusStaging {
    val mavenCentralUser: String? by project
    val mavenCentralKey: String? by project
    username = mavenCentralUser
    password = mavenCentralKey
}
