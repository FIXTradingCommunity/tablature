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
import java.io.FileNotFoundException;
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
    private List<String> inputFilePatterns = new ArrayList<>();
    private String outputFile;
    private String referenceFile;
  
    public Md2Orchestra build() {
      return new Md2Orchestra(this);
    }

    public Builder inputFilePatterns(List<String> inputFilePatterns) {
      this.inputFilePatterns.clear();
      this.inputFilePatterns.addAll(inputFilePatterns);
      return this;
    }

    public Builder inputFilePattern(String inputFilePatterns) {
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
  -o,--output <arg>      path of output Orchestra file (required)
  -r,--reference <arg>   path of reference Orchestra file
   * </pre>
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    Md2Orchestra mdl2Orchestra = Md2Orchestra.parseArgs(args).build();
    mdl2Orchestra.generate();
  }


  static Builder parseArgs(String[] args) {
    final Options options = new Options();
    options.addOption(Option.builder("o").desc("path of output Orchestra file (required)")
        .longOpt("output").numberOfArgs(1).required().build());
    options.addOption(Option.builder("r").desc("path of reference Orchestra file")
        .longOpt("reference").numberOfArgs(1).build());
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

      return builder;
    } catch (final ParseException e) {
      showHelp(options);
      throw new RuntimeException(e);
    }
  }


  private static void showHelp(Options options) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Md2Orchestra [options] <input-file>...", options);
  }

  private List<String> inputFilePatterns;
  private Logger logger = LogManager.getLogger(getClass());
  private String outputFilename;
  private String referenceFilename;

  private Md2Orchestra(Builder builder) {
    this.inputFilePatterns = builder.inputFilePatterns;
    this.outputFilename = builder.outputFile;
    this.referenceFilename = builder.referenceFile;
  }

  public void generate() {
    try {
      generate(inputFilePatterns, outputFilename, referenceFilename);
    } catch (IOException e) {
      logger.fatal("Md2Orchestra failed", e);
    }
  }

  void generate(List<String> inputFilePatterns, String outputFilename, String referenceFilename)
      throws IOException {
    Objects.requireNonNull(inputFilePatterns, "Input file list is missing");
    Objects.requireNonNull(outputFilename, "Output file is missing");

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

      final RepositoryBuilder outputRepositoryBuilder = RepositoryBuilder.instance(referenceStream);
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

            });
      }

      outputRepositoryBuilder.write(outputStream);
      logger.info("Md2Orchestra output written");
    } catch (final JAXBException e) {
      logger.fatal("Md2Orchestra failed to process XML", e);
      throw new IOException(e);
    }
  }

  void generate(String inputFilePattern, String outputFilename, String referenceFilename)
      throws IOException {
    generate(List.of(inputFilePattern), outputFilename, referenceFilename);
  }

  private void appendInput(Path filePath, RepositoryBuilder outputRepositoryBuilder) 
      throws FileNotFoundException, IOException {
    logger.info("Md2Orchestra opening file {}", filePath.toString());
    final InputStream inputStream = new FileInputStream(filePath.toFile());
    outputRepositoryBuilder.appendInput(inputStream);
  }

}
