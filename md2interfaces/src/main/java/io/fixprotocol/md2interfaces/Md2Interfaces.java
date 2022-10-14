/*
 * Copyright 2020 FIX Protocol Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package io.fixprotocol.md2interfaces;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.JAXBException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Md2Interfaces {

  public static class Builder {
    private List<String> inputFiles = new ArrayList<>();
    private String eventFile;
    private String outputFile;

    public Md2Interfaces build() {
      return new Md2Interfaces(this);
    }

    public Builder eventFile(final String eventFile) {
      this.eventFile = eventFile;
      return this;
    }

    public Builder inputFile(final String inputFile) {
      inputFiles(List.of(inputFile));
      return this;
    }

    public Builder inputFiles(final List<String> inputFiles) {
      this.inputFiles.clear();
      this.inputFiles.addAll(inputFiles);
      return this;
    }

    public Builder outputFile(final String outputFile) {
      this.outputFile = outputFile;
      return this;
    }

  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Convert markdown to Orchestra interfaces schema
   *
   * <pre>
  usage: Md2Interfaces  [options] &lt;input-file&gt;...
  -?,--help             display usage
  -e,--eventlog &lt;arg&gt;   path of JSON event file
  -o,--output &lt;arg&gt;     path of output interfaces file (required)
   * </pre>
   *
   * @param args command line arguments
   *
   */
  public static void main(final String[] args) {
    final Md2Interfaces md2interfaces = parseArgs(args).build();
    md2interfaces.generate();
  }

  private static Builder parseArgs(final String[] args) {
    final Options options = new Options();
    options.addOption(Option.builder("o").desc("path of output interfaces file (required)")
        .longOpt("output").numberOfArgs(1).required().build());
    options.addOption(Option.builder("e").desc("path of JSON event file").longOpt("eventlog")
        .numberOfArgs(1).build());
    options.addOption(
        Option.builder("?").numberOfArgs(0).desc("display usage").longOpt("help").build());

    final DefaultParser parser = new DefaultParser();
    final CommandLine cmd;

    final Builder builder = new Builder();

    try {
      cmd = parser.parse(options, args);

      if (cmd.hasOption("?")) {
        showHelp(options);
        System.exit(0);
      }

      builder.inputFiles = cmd.getArgList();
      builder.outputFile = cmd.getOptionValue("o");

      if (cmd.hasOption("e")) {
        builder.eventFile = cmd.getOptionValue("e");
      }

      return builder;
    } catch (final ParseException e) {
      showHelp(options);
      throw new RuntimeException(e);
    }
  }

  private static void showHelp(final Options options) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Md2Interfaces  [options] <input-file>...", options);
  }

  private final List<String> inputFiles;
  private final String eventFile;
  private final Logger logger = LogManager.getLogger(getClass());

  private final String outputFile;

  private Md2Interfaces(final Builder builder) {
    this.inputFiles = builder.inputFiles;
    this.outputFile = builder.outputFile;
    this.eventFile = builder.eventFile;
  }

  public void generate() {
    try {
      generate(inputFiles, outputFile, eventFile);
    } catch (final Exception e) {
      logger.fatal("Md2Interfaces failed", e);
    }
  }

  void generate(final List<String> inputFiles, final String outputFilename, final String eventFilename)
      throws Exception {
    Objects.requireNonNull(inputFiles, "Input File is missing");
    Objects.requireNonNull(outputFile, "Output File is missing");
    String version = getClass().getPackage().getImplementationVersion();
    if (version != null) {
      logger.info("{} version {}", getClass().getCanonicalName(), version);
    }

    final File outputFile = new File(outputFilename);
    final File outputDir = outputFile.getParentFile();
    if (outputDir != null) {
      outputDir.mkdirs();
    }

    OutputStream jsonOutputStream = null;
    if (eventFilename != null) {
      jsonOutputStream = new FileOutputStream(eventFilename);
    }

    final InterfacesBuilder interfacesBuilder = new InterfacesBuilder(jsonOutputStream);

    try (final OutputStream outputStream = new FileOutputStream(outputFile)) {

      for (final String inputFile : inputFiles) {
        appendInput(inputFile, interfacesBuilder);
      }
      interfacesBuilder.write(outputStream);
      logger.info("Md2Interfaces completed");
    } catch (final JAXBException e) {
      logger.fatal("Md2Interfaces failed to process XML", e);
      throw new IOException(e);
    }
  }

  private void appendInput(final String filePath, final InterfacesBuilder interfacesBuilder)
      throws IOException {
    logger.info("Md2Interfaces opening file {}", filePath);
    final InputStream inputStream = new FileInputStream(filePath);
    interfacesBuilder.appendInput(inputStream);
  }

}
