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
package org.pkl.gradle.task;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.pkl.cli.CliImportAnalyzer;
import org.pkl.cli.CliImportAnalyzerOptions;
import org.pkl.core.ImportGraph;
import org.pkl.core.OutputFormat;

public abstract class AnalyzeImportsTask extends ModulesTask {
  @OutputFile
  @Optional
  public abstract RegularFileProperty getOutputFile();

  @Input
  public abstract Property<String> getOutputFormat();

  /**
   * Holds the resolved transitive module files parsed from the output of this task. This property
   * is populated during task execution and is used by dependent tasks to track transitive module
   * file inputs for up-to-date checking.
   *
   * <p>This property is {@code @Internal} because it is derived from the task output and is not
   * itself a declared input or output.
   */
  @Internal
  public abstract ListProperty<File> getResolvedTransitiveFiles();

  /**
   * Returns a provider that reads the output file and parses the transitive module files. This
   * provider is suitable for use as an input in dependent tasks because it properly tracks the
   * output file dependency.
   *
   * @return a provider of the list of transitive file-based module imports
   */
  @Internal
  public Provider<List<File>> getTransitiveFilesFromOutput() {
    return getOutputFile()
        .map(
            file -> {
              if (!file.getAsFile().exists()) {
                return Collections.emptyList();
              }
              // Only parse JSON output
              if (!OutputFormat.JSON.toString().equals(getOutputFormat().getOrNull())) {
                return Collections.emptyList();
              }
              try {
                var contents = java.nio.file.Files.readString(file.getAsFile().toPath());
                ImportGraph importGraph = ImportGraph.parseFromJson(contents);
                var imports = importGraph.resolvedImports().values();
                return imports.stream()
                    .filter(it -> it.getScheme().equalsIgnoreCase("file"))
                    .map(File::new)
                    .toList();
              } catch (Exception e) {
                return Collections.emptyList();
              }
            });
  }

  @Override
  protected void doRunTask() {
    //noinspection ResultOfMethodCallIgnored
    getOutputs().getPreviousOutputFiles().forEach(File::delete);
    new CliImportAnalyzer(
            new CliImportAnalyzerOptions(
                getCliBaseOptions(),
                mapAndGetOrNull(getOutputFile(), it -> it.getAsFile().toPath()),
                mapAndGetOrNull(getOutputFormat(), it -> it)))
        .run();

    // Parse the output to populate resolved transitive files for dependent tasks.
    getResolvedTransitiveFiles().set(parseResolvedTransitiveFiles());
  }

  private List<File> parseResolvedTransitiveFiles() {
    var outputFile = getOutputFile().getOrNull();
    if (outputFile == null) {
      return Collections.emptyList();
    }
    // Only parse JSON output — the automatically created analyze tasks
    // (from createModulesTask) always use JSON. User-configured analyze tasks
    // may use other formats and don't need transitive file tracking.
    if (!OutputFormat.JSON.toString().equals(getOutputFormat().getOrNull())) {
      return Collections.emptyList();
    }
    try {
      var contents = java.nio.file.Files.readString(outputFile.getAsFile().toPath());
      ImportGraph importGraph = ImportGraph.parseFromJson(contents);
      var imports = importGraph.resolvedImports().values();
      return imports.stream()
          .filter(it -> it.getScheme().equalsIgnoreCase("file"))
          .map(File::new)
          .toList();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
