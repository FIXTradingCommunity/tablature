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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Translates markdown to an Orchestra file
 *
 * @author Don Mendelson
 *
 */
public class Md2Orchestra {

  public static class Builder {
    public String eventFilename;
    public String paragraphDelimiter = RepositoryBuilder.DEFAULT_PARAGRAPH_DELIMITER;
    private List<String> inputFilePatterns = new ArrayList<>();
    private String outputFilename;
    private String referenceFile;


    public Md2Orchestra build() {
      return new Md2Orchestra(this);
    }

    public Builder eventFile(String eventFilename) {
      this.eventFilename = eventFilename;
      return this;
    }

    public Builder inputFilePattern(String inputFilePatterns) {
      this.inputFilePatterns = List.of(inputFilePatterns);
      return this;
    }

    public Builder inputFilePatterns(List<String> inputFilePatterns) {
      this.inputFilePatterns.clear();
      this.inputFilePatterns.addAll(inputFilePatterns);
      return this;
    }

    public Builder outputFile(String outputFilename) {
      this.outputFilename = outputFilename;
      return this;
    }

    /**
     * Token to represent a paragraph break in tables (not natively supported by markdown)
     *
     * @param paragraphDelimiter token
     * @return this Builder
     */
    public Builder paragraphDelimiter(String paragraphDelimiter) {
      this.paragraphDelimiter = paragraphDelimiter;
      return this;
    }

    public Builder referenceFile(String referenceFile) {
      this.referenceFile = referenceFile;
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
  usage: Md2Orchestra [options] &lt;input-file&gt;...
  -?,--help              display usage
  -e,--eventlog &lt;arg&gt;    path of JSON event file
  -o,--output &lt;arg&gt;      path of output Orchestra file (required)
     --paragraph &lt;arg&gt;   paragraph delimiter for tables
  -r,--reference &lt;arg&gt;   path of reference Orchestra file
   * </pre>
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    Md2Orchestra md2Orchestra;
    try {
      md2Orchestra = Md2Orchestra.parseArgs(args).build();
      md2Orchestra.generate();
    } catch (final Exception e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }

  static Builder parseArgs(String[] args) throws ParseException {
    final Options options = new Options();
    options.addOption(Option.builder("o").desc("path of output Orchestra file (required)")
        .longOpt("output").numberOfArgs(1).required().build());
    options.addOption(Option.builder("r").desc("path of reference Orchestra file")
        .longOpt("reference").numberOfArgs(1).build());
    options.addOption(Option.builder("e").desc("path of JSON event file").longOpt("eventlog")
        .numberOfArgs(1).build());
    options.addOption(
        Option.builder("?").numberOfArgs(0).desc("display usage").longOpt("help").build());
    options.addOption(Option.builder().desc("paragraph delimiter for tables").longOpt("paragraph")
        .numberOfArgs(1).build());

    final DefaultParser parser = new DefaultParser();
    CommandLine cmd;

    final Builder builder = new Builder();

    try {
      cmd = parser.parse(options, args);

      if (cmd.hasOption("?")) {
        showHelp(options);
        System.exit(1);
      }

      builder.inputFilePatterns = cmd.getArgList();
      builder.outputFilename = cmd.getOptionValue("o");

      if (cmd.hasOption("r")) {
        builder.referenceFile = cmd.getOptionValue("r");
      }

      if (cmd.hasOption("e")) {
        builder.eventFilename = cmd.getOptionValue("e");
      }

      if (cmd.hasOption("paragraph")) {
        builder.paragraphDelimiter(cmd.getOptionValue("paragraph"));
      }

      return builder;
    } catch (final ParseException e) {
      showHelp(options);
      throw e;
    }
  }

  private static void showHelp(Options options) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Md2Orchestra [options] <input-file>...", options);
  }

  private final String eventFilename;
  private final List<String> inputFilePatterns;
  private final Logger logger = LogManager.getLogger(getClass());
  private final String outputFilename;
  private final String paragraphDelimiter;
  private final String referenceFilename;

  private Md2Orchestra(Builder builder) {
    this.inputFilePatterns = builder.inputFilePatterns;
    this.outputFilename = builder.outputFilename;
    this.referenceFilename = builder.referenceFile;
    this.eventFilename = builder.eventFilename;
    this.paragraphDelimiter = builder.paragraphDelimiter;
  }

  /**
   * Generate an Orchestra repository file from markdown files
   *
   * @throws Exception IllegalArgumentException if inputFilePatterns is empty NullPointerException
   *         if inputFilePatterns or outputFilename is {@code null}
   */
  public void generate() throws Exception {
    try {
      generate(inputFilePatterns, outputFilename, referenceFilename, eventFilename);
    } catch (final Exception e) {
      logger.fatal("Md2Orchestra generate failed", e);
      throw e;
    }
  }

  /**
   * Generate an Orchestra repository file from markdown files
   *
   * @param inputFilePatterns file names or glob patterns for markdown files
   * @param outputFilename name of Orchestra file to create
   * @param referenceFilename optional Orchestra reference file
   * @param eventFilename optional JSON event file suitable for rendering
   * @throws Exception IllegalArgumentException if inputFilePatterns is empty NullPointerException
   *         if inputFilePatterns or outputFilename is {@code null}
   */
  void generate(List<String> inputFilePatterns, String outputFilename, String referenceFilename,
      String eventFilename) throws Exception {
    Objects.requireNonNull(inputFilePatterns, "Input file list is missing");
    Objects.requireNonNull(outputFilename, "Output file is missing");
    if (inputFilePatterns.isEmpty()) {
      throw new IllegalArgumentException("No input file specified");
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

      OutputStream jsonOutputStream = null;
      if (eventFilename != null) {
        jsonOutputStream = new FileOutputStream(eventFilename);
      }

      final FileSystem fileSystem = FileSystems.getDefault();
      final String separator = fileSystem.getSeparator();

      final RepositoryBuilder outputRepositoryBuilder =
          RepositoryBuilder.instance(referenceStream, jsonOutputStream, paragraphDelimiter);
      for (final String inputFilePattern : inputFilePatterns) {
        int lastSeparatorPos = inputFilePattern.lastIndexOf(separator);
        // Handle Windows case for portability of '/' separator 
        if (lastSeparatorPos == -1 && !separator.equals("/")) {
          lastSeparatorPos = inputFilePattern.lastIndexOf("/");
        }
        Path dirPath;
        String glob;
        if (lastSeparatorPos != -1) {
          dirPath = fileSystem.getPath(inputFilePattern.substring(0, lastSeparatorPos)).toAbsolutePath();
          glob = "**" + separator + inputFilePattern.substring(lastSeparatorPos + 1);
        } else {
          // current working directory
          dirPath = fileSystem.getPath("").toAbsolutePath();
          glob = inputFilePattern;
        }

        logger.info("Md2Orchestra searching for input at path {} file name pattern {}", dirPath, glob);
        
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
                  appendInput(filePath, outputRepositoryBuilder);
                }
                return FileVisitResult.CONTINUE;
              }


              @Override
              public FileVisitResult visitFileFailed(Path file, IOException exc)
                  throws IOException {
                logger.warn("Md2Orchestra failed to access file {}", file.toString());
                return FileVisitResult.SKIP_SUBTREE;
              }

              private void appendInput(Path filePath, RepositoryBuilder outputRepositoryBuilder)
                  throws IOException {
                logger.info("Md2Orchestra opening file {}", filePath.toString());
                final InputStream inputStream = new FileInputStream(filePath.toFile());
                outputRepositoryBuilder.appendInput(inputStream);
              }
            });
      }

      outputRepositoryBuilder.write(outputStream);
      logger.info("Md2Orchestra output written");
    } catch (final JAXBException e) {
      logger.fatal("Md2Orchestra failed to process XML", e);
      throw new IOException(e);
    }
  }

  void generate(String inputFilePattern, String outputFilename, String referenceFilename,
      String eventFilename) throws Exception {
    generate(List.of(inputFilePattern), outputFilename, referenceFilename, eventFilename);
  }

}
