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
 */
package io.fixprotocol.md2orchestra;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.JAXBException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import io.fixprotocol.md.event.DocumentParser;
import io.fixprotocol.md2orchestra.util.LogUtil;


/**
 * Translates markdown to an Orchestra file
 *
 * @author Don Mendelson
 *
 */
public class Md2Orchestra {

  public static class Builder {
    private List<String> inputFilePatterns = new ArrayList<>();
    private String logFile;
    private String outputFile;
    private String referenceFile;
    private boolean verbose;

    public Md2Orchestra build() {
      return new Md2Orchestra(this);
    }

    public Builder eventLog(String logFile) {
      this.logFile = logFile;
      return this;
    }

    public Builder inputFilePatterns(List<String> inputFilePatterns) {
      this.inputFilePatterns.clear();
      this.inputFilePatterns.addAll(inputFilePatterns);
      return this;
    }

    public Builder inputFilePatterns(String inputFilePatterns) {
      this.inputFilePatterns = List.of(inputFilePatterns);
      return this;
    }

    public Builder outputFile(String outputFile) {
      this.outputFile = outputFile;
      return this;
    }

    public Builder referenceFile(String referenceFile) {
      this.referenceFile = referenceFile;
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

  /**
   * Construct and run Md2Orchestra with command line arguments
   *
   * Md2Orchestra reads one or more input files to produce output.
   *
   * <pre>
  usage: Md2Orchestra [options] <input-file>..."
  -?,--help              display usage
  -e,--eventlog <arg>    path of log file
  -o,--output <arg>      path of output Orchestra file (required)
  -r,--reference <arg>   path of reference Orchestra file
  -v,--verbose           verbose event log
   * </pre>
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    Md2Orchestra mdl2Orchestra = new Md2Orchestra();
    try {
      mdl2Orchestra = mdl2Orchestra.parseArgs(args).build();
      mdl2Orchestra.generate();
    } catch (final Exception e) {
      if (mdl2Orchestra.logger != null) {
        mdl2Orchestra.logger.fatal("Md2Orchestra: exception occurred", e);
      } else {
        e.printStackTrace(System.err);
      }
    }
  }

  private static void showHelp(Options options) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Md2Orchestra [options] <input-file>...", options);
  }

  private List<String> inputFilePatterns;
  private String logFilename;
  private Logger logger = null;
  private String outputFilename;
  private String referenceFilename;
  private boolean verbose = false;

  /**
   * For testing
   */
  Md2Orchestra() {

  }

  private Md2Orchestra(Builder builder) {
    this.inputFilePatterns = builder.inputFilePatterns;
    this.outputFilename = builder.outputFile;
    this.referenceFilename = builder.referenceFile;
    this.logFilename = builder.logFile;
    this.verbose = builder.verbose;
  }

  public void generate() throws IOException {
    generate(inputFilePatterns, outputFilename, referenceFilename, logFilename);
  }

  void generate(List<String> inputFilePatterns, String outputFilename, String referenceFilename,
      String logFilename) throws IOException {
    Objects.requireNonNull(inputFilePatterns, "Input file list is missing");
    Objects.requireNonNull(outputFilename, "Output file is missing");

    final Level level = verbose ? Level.DEBUG : Level.ERROR;
    if (logFilename != null) {
      logger = LogUtil.initializeFileLogger(logFilename, level, getClass());
    } else {
      logger = LogUtil.initializeDefaultLogger(level, getClass());
    }

    final File outputFile = new File(outputFilename);
    final File outputDir = outputFile.getParentFile();
    if (outputDir != null) {
      outputDir.mkdirs();
    }

    try (OutputStream outputStream = new FileOutputStream(outputFile)) {

      InputStream referenceStream = null;
      if (referenceFilename != null) {
        referenceStream = new FileInputStream(referenceFilename);
      }

      final FileSystem fileSystem = FileSystems.getDefault();
      final String separator = fileSystem.getSeparator();

      final RepositoryBuilder outputRepositoryBuilder = builder(referenceStream);
      for (final String inputFilePattern : inputFilePatterns) {
        final int lastSeparatorPos = inputFilePattern.lastIndexOf(separator);
        Path dirPath;
        String glob;
        if (lastSeparatorPos != -1) {
          dirPath = fileSystem.getPath(inputFilePattern.substring(0, lastSeparatorPos));
          glob = "**" + separator + inputFilePattern.substring(lastSeparatorPos + 1);
        } else {
          // current working directory
          dirPath = fileSystem.getPath("");
          glob = inputFilePattern;
        }

        final PathMatcher matcher = fileSystem.getPathMatcher("glob:" + glob);
        Files.walkFileTree(dirPath, EnumSet.noneOf(FileVisitOption.class), 1,
            new FileVisitor<Path>() {

              @Override
              public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                  throws IOException {
                return FileVisitResult.CONTINUE;
              }

              @Override
              public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                  throws IOException {
                return FileVisitResult.CONTINUE;
              }

              @Override
              public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs)
                  throws IOException {

                if (matcher.matches(filePath)) {
                  final InputStream inputStream = new FileInputStream(filePath.toFile());
                  appendInput(inputStream, outputRepositoryBuilder);
                }
                return FileVisitResult.CONTINUE;
              }

              @Override
              public FileVisitResult visitFileFailed(Path file, IOException exc)
                  throws IOException {
                logger.warn("Md2Orchestra failed to access file {}", file.toString());
                return FileVisitResult.SKIP_SUBTREE;
              }

            });
      }

      write(outputStream, outputRepositoryBuilder);
    } catch (final JAXBException e) {
      logger.fatal("Md2Orchestra failed to process XML", e);
      throw new IOException(e);
    }
  }

  void generate(String inputFilePattern, String outputFilename, String referenceFilename,
      String logFilename) throws IOException {
    generate(List.of(inputFilePattern), outputFilename, referenceFilename, logFilename);
  }

  private void appendInput(InputStream inputStream, final RepositoryBuilder outputRepositoryBuilder)
      throws IOException {
    final DocumentParser parser = new DocumentParser();
    parser.parse(inputStream, outputRepositoryBuilder);
  }

  private RepositoryBuilder builder(InputStream referenceStream) throws JAXBException, IOException {
    final RepositoryBuilder outputRepositoryBuilder = new RepositoryBuilder();

    if (referenceStream != null) {
      final RepositoryAdapter referenceRepository = new RepositoryAdapter();
      referenceRepository.unmarshal(referenceStream);
      outputRepositoryBuilder.setReference(referenceRepository);
    }
    return outputRepositoryBuilder;
  }

  private Builder parseArgs(String[] args) throws ParseException {
    final Options options = new Options();
    options.addOption(Option.builder("o").desc("path of output Orchestra file (required)")
        .longOpt("output").numberOfArgs(1).required().build());
    options.addOption(Option.builder("r").desc("path of reference Orchestra file")
        .longOpt("reference").numberOfArgs(1).build());
    options.addOption(
        Option.builder("e").desc("path of log file").longOpt("eventlog").numberOfArgs(1).build());
    options.addOption(Option.builder("v").desc("verbose event log").longOpt("verbose").build());
    options.addOption(
        Option.builder("?").numberOfArgs(0).desc("display usage").longOpt("help").build());

    final DefaultParser parser = new DefaultParser();
    CommandLine cmd;

    final Builder builder = new Builder();

    try {
      cmd = parser.parse(options, args);

      if (cmd.hasOption("?")) {
        showHelp(options);
        System.exit(0);
      }

      builder.inputFilePatterns = cmd.getArgList();
      builder.outputFile = cmd.getOptionValue("o");

      if (cmd.hasOption("r")) {
        builder.referenceFile = cmd.getOptionValue("r");
      }

      if (cmd.hasOption("e")) {
        builder.logFile = cmd.getOptionValue("e");
      }

      if (cmd.hasOption("v")) {
        builder.verbose = true;
      }

      return builder;
    } catch (final ParseException e) {
      showHelp(options);
      throw e;
    }
  }

  private void write(OutputStream outputStream, final RepositoryBuilder outputRepositoryBuilder)
      throws JAXBException {
    outputRepositoryBuilder.write(outputStream);
    logger.info("Md2Orchestra completed");
  }

}
