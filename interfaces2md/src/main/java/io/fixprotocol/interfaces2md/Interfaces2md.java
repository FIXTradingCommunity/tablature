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
package io.fixprotocol.interfaces2md;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Interfaces2md {

  public static class Builder {


    private String eventFile;
    private String inputFile;
    private String outputFile;

    public Interfaces2md build() {
      return new Interfaces2md(this);
    }

    public Builder eventFile(final String eventFile) {
      this.eventFile = eventFile;
      return this;
    }

    public Builder inputFile(final String inputFile) {
      this.inputFile = inputFile;
      return this;
    }

    public Builder outputFile(final String outputFile) {
      this.outputFile = outputFile;
      return this;
    }
  }

  /**
   * Construct and run Interfaces2md with command line arguments
   *
   * <pre>
   * usage: Interfaces2md [options] &lt;input-file&gt;
 -?,--help             display usage
 -e,--eventlog lt;arg&gt;   path of JSON event file
 -o,--output lt;arg&gt;     path of markdown output file (required)
   * </pre>
   * @param args command line arguments
   */
  public static void main(final String[] args) {
    final Interfaces2md interfaces2md = parseArgs(args).build();
    interfaces2md.generate();
  }

  private static Builder parseArgs(final String[] args) {
    final Options options = new Options();
    options.addOption(Option.builder("o").desc("path of markdown output file (required)")
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

      builder.inputFile = !cmd.getArgList().isEmpty() ? cmd.getArgList().get(0) : null;
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
    formatter.printHelp("Interfaces2md [options] <input-file>", options);
  }

  private final String eventFilename;
  private final String inputFilename;
  private final Logger logger = LogManager.getLogger(getClass());
  private final String outputFilename;

  private Interfaces2md(final Builder builder) {
    this.inputFilename = builder.inputFile;
    this.outputFilename = builder.outputFile;
    this.eventFilename = builder.eventFile;
  }

  public void generate() {
    try {
      String version = getClass().getPackage().getImplementationVersion();
      if (version != null) {
        logger.info("{} version {}", getClass().getCanonicalName(), version);
      }
      generate(inputFilename, outputFilename, eventFilename);
      logger.info("Interfaces2md complete");
    } catch (final Exception e) {
      logger.fatal("Interfaces2md failed", e);
    }
  }


  void generate(final String inputFilename, final String outputFilename, final String eventFilename)
      throws Exception {
    Objects.requireNonNull(inputFilename, "Input file is missing");
    Objects.requireNonNull(outputFilename, "Output file is missing");

    final File outputFile = new File(outputFilename);
    final File outputDir = outputFile.getParentFile();
    if (outputDir != null) {
      outputDir.mkdirs();
    }

    try (final InputStream inputStream = new FileInputStream(inputFilename);
         final OutputStreamWriter outputWriter =
            new OutputStreamWriter(new FileOutputStream(outputFilename), StandardCharsets.UTF_8)) {

      OutputStream eventStream = null;
      if (eventFilename != null) {
        final File eventFile = new File(eventFilename);
        final File eventDir = eventFile.getParentFile();
        if (eventDir != null) {
          eventDir.mkdirs();
        }
        eventStream = new FileOutputStream(eventFile);
      }
      final MarkdownGenerator generator = new MarkdownGenerator();
      generator.generate(inputStream, outputWriter, eventStream);
    }
  }



}
