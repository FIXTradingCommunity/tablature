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
package io.fixprotocol.orchestra2md;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import io.fixprotocol.orchestra2md.util.LogUtil;

public class Orchestra2md {


  public static class Builder {
    public String logFile;
    public boolean verbose;
    private String inputFile;
    private String outputFile;

    public Orchestra2md build() {
      return new Orchestra2md(this);
    }

    public Builder eventLog(String logFile) {
      this.logFile = logFile;
      return this;
    }

    public Builder inputFile(String inputFile) {
      this.inputFile = inputFile;
      return this;
    }

    public Builder outputFile(String outputFile) {
      this.outputFile = outputFile;
      return this;
    }

    public Builder verbose(boolean verbose) {
      this.verbose = verbose;
      return this;
    }
  }


  public static Builder builder() {
    return new Builder();
  }

  public static void generate(InputStream inputStream, OutputStreamWriter outputWriter)
      throws Exception {
    MarkdownGenerator generator = new MarkdownGenerator();
    generator.generate(inputStream, outputWriter);
  }

  /**
   * Construct and run Md2Orchestra with command line arguments
   *
   * <pre>
  usage: Md2Orchestra
  -?,--help             display usage
  -e,--eventlog <arg>   path of log file
  -i,--input <arg>      path of Orchestra input file
  -o,--output <arg>     path of markdown output file
  -v,--verbose          verbose event log
   * </pre>
   *
   * @param args command line arguments
   */

  public static void main(String[] args) {
    Orchestra2md orchestra2md = parseArgs(args).build();
    orchestra2md.generate();
  }

  private static Builder parseArgs(String[] args) {
    final Options options = new Options();
    options.addOption(Option.builder("i").desc("path of Orchestra input file (required)")
        .longOpt("input").numberOfArgs(1).required().build());
    options.addOption(Option.builder("o").desc("path of markdown output file (required)")
        .longOpt("output").numberOfArgs(1).required().build());
    options.addOption(
        Option.builder("?").numberOfArgs(0).desc("display usage").longOpt("help").build());
    options.addOption(
        Option.builder("e").desc("path of log file").longOpt("eventlog").numberOfArgs(1).build());
    options.addOption(Option.builder("v").desc("verbose event log").longOpt("verbose").build());

    final DefaultParser parser = new DefaultParser();
    CommandLine cmd;

    final Builder builder = new Builder();

    try {
      cmd = parser.parse(options, args);

      if (cmd.hasOption("?")) {
        showHelp(options);
        System.exit(0);
      }

      builder.inputFile = cmd.getOptionValue("i");
      builder.outputFile = cmd.getOptionValue("o");

      if (cmd.hasOption("e")) {
        builder.logFile = cmd.getOptionValue("e");
      }

      if (cmd.hasOption("v")) {
        builder.verbose = true;
      }

      return builder;
    } catch (final ParseException e) {
      showHelp(options);
      throw new RuntimeException(e);
    }
  }

  private static void showHelp(Options options) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Orchestra2md [options]", options);
  }

  private final String inputFilename;
  private final String logFilename;
  private Logger logger = null;
  private final String outputFilename;
  private final boolean verbose;

  private Orchestra2md(Builder builder) {
    this.inputFilename = builder.inputFile;
    this.outputFilename = builder.outputFile;
    this.logFilename = builder.logFile;
    this.verbose = builder.verbose;
  }

  public void generate() {
    this.logger = initializeLogger(this.verbose, logFilename);
    try {
      generate(inputFilename, outputFilename);
    } catch (Exception e) {
      logger.fatal("Orchestra2md failed", e);
    }
  }

  void generate(String inputFilename, String outputFilename) throws Exception {
    Objects.requireNonNull(inputFilename, "Input file is missing");
    Objects.requireNonNull(outputFilename, "Output file is missing");

    final File outputFile = new File(outputFilename);
    final File outputDir = outputFile.getParentFile();
    if (outputDir != null) {
      outputDir.mkdirs();
    }

    try (InputStream inputStream = new FileInputStream(inputFilename);
        OutputStreamWriter outputWriter =
            new OutputStreamWriter(new FileOutputStream(outputFilename), StandardCharsets.UTF_8)) {
      generate(inputStream, outputWriter);
    }
  }

  private Logger initializeLogger(boolean verbose) {
    final Level level = verbose ? Level.DEBUG : Level.ERROR;
    return LogUtil.initializeDefaultLogger(level, getClass());
  }

  private Logger initializeLogger(boolean verbose, String logFilename) {
    final Level level = verbose ? Level.DEBUG : Level.ERROR;
    if (logFilename != null) {
      return LogUtil.initializeFileLogger(logFilename, level, getClass());
    } else {
      return initializeLogger(verbose);
    }
  }
}
