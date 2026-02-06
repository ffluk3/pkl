/*
 * Copyright © 2024-2026 Apple Inc. and the Pkl project authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pkl.gradle

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Tests that all pkl-gradle tasks are compatible with the Gradle configuration cache.
 *
 * Each test runs a task twice: once to store the configuration cache entry, and once to verify it
 * is reused. The second run must report "Reusing configuration cache."
 */
class ConfigurationCacheTest : AbstractTest() {

  @Test
  fun `eval task is configuration cache compatible`() {
    writeFile(
      "build.gradle",
      """
      plugins {
        id "org.pkl-lang"
      }

      pkl {
        evaluators {
          evalTest {
            sourceModules = ["test.pkl"]
            outputFormat = "yaml"
            settingsModule = "pkl:settings"
          }
        }
      }
    """,
    )
    writeFile(
      "test.pkl",
      """
      person {
        name = "Pigeon"
        age = 30
      }
    """,
    )

    val (firstRun, secondRun) = runTaskWithConfigurationCache("evalTest")

    assertThat(firstRun.output).contains("Configuration cache entry stored")
    assertThat(secondRun.output).contains("Reusing configuration cache")
  }

  @Test
  fun `pkldoc task is configuration cache compatible`() {
    writeFile(
      "build.gradle",
      """
      plugins {
        id "org.pkl-lang"
      }

      pkl {
        pkldocGenerators {
          pkldoc {
            sourceModules = ["doc-package-info.pkl", "mod.pkl"]
            outputDir = file("build/pkldoc")
            settingsModule = "pkl:settings"
          }
        }
      }
    """,
    )
    writeFile(
      "doc-package-info.pkl",
      """
      /// Overview documentation for the test package.
      amends "pkl:DocPackageInfo"
      name = "testpkg"
      version = "1.0.0"
      importUri = "https://example.com/"
      authors { "test@example.com" }
      sourceCode = "https://example.com/source"
      issueTracker = "https://example.com/issues"
    """,
    )
    writeFile(
      "mod.pkl",
      """
      module testpkg.mod

      greeting: String = "hello"
    """,
    )

    val (firstRun, secondRun) = runTaskWithConfigurationCache("pkldoc")

    assertThat(firstRun.output).contains("Configuration cache entry stored")
    assertThat(secondRun.output).contains("Reusing configuration cache")
  }

  @Test
  fun `test task is configuration cache compatible`() {
    writeFile(
      "build.gradle",
      """
      plugins {
        id "org.pkl-lang"
      }

      pkl {
        tests {
          evalTest {
            sourceModules = ["test.pkl"]
            settingsModule = "pkl:settings"
          }
        }
      }
    """,
    )
    writeFile(
      "test.pkl",
      """
      amends "pkl:test"

      facts {
        ["basic"] {
          1 + 1 == 2
        }
      }
    """,
    )

    val (firstRun, secondRun) = runTaskWithConfigurationCache("evalTest")

    assertThat(firstRun.output).contains("Configuration cache entry stored")
    assertThat(secondRun.output).contains("Reusing configuration cache")
  }

  @Test
  fun `project resolve task is configuration cache compatible`() {
    writeFile(
      "build.gradle",
      """
      plugins {
        id "org.pkl-lang"
      }

      pkl {
        project {
          resolvers {
            resolveMyProj {
              projectDirectories.from(file("proj1"))
              settingsModule = "pkl:settings"
            }
          }
        }
      }
    """,
    )
    writeFile(
      "proj1/PklProject",
      """
      amends "pkl:Project"
    """,
    )

    val (firstRun, secondRun) = runTaskWithConfigurationCache("resolveMyProj")

    assertThat(firstRun.output).contains("Configuration cache entry stored")
    assertThat(secondRun.output).contains("Reusing configuration cache")
  }

  @Test
  fun `project package task is configuration cache compatible`() {
    writeFile(
      "build.gradle",
      """
      plugins {
        id "org.pkl-lang"
      }

      pkl {
        project {
          packagers {
            createMyPackages {
              projectDirectories.from(file("proj1"))
              skipPublishCheck.set(true)
              settingsModule = "pkl:settings"
            }
          }
        }
      }
    """,
    )
    writeFile(
      "proj1/PklProject",
      """
      amends "pkl:Project"

      package {
        name = "proj1"
        baseUri = "package://example.com/proj1"
        version = "1.0.0"
        packageZipUrl = "https://example.com/proj1@\(version).zip"
      }
    """,
    )
    writeFile(
      "proj1/main.pkl",
      """
      x = 1
    """,
    )

    val (firstRun, secondRun) = runTaskWithConfigurationCache("createMyPackages")

    assertThat(firstRun.output).contains("Configuration cache entry stored")
    assertThat(secondRun.output).contains("Reusing configuration cache")
  }
}
