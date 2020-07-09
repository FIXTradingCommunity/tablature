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
    private boolean verbose;
    private String inputFile;
    private String logFile;
    private String outputFile;
    private String referenceFile;

    public Md2Orchestra build() {
      return new Md2Orchestra(this);
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
   * <pre>
  usage: Md2Orchestra
  -?,--help              display usage
  -e,--eventlog <arg>    path of log file
  -i,--input <arg>       path of markdown input file
  -o,--output <arg>      path of output Orchestra file
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
    formatter.printHelp("Md2Orchestra", options);
  }

  private File inputFile;
  private File logFile;
  private Logger logger = null;
  private File outputFile;
  private File referenceFile;
  private boolean verbose = false;

  /**
   * Only for use with {@link #generate(InputStream, OutputStream, InputStream)} or
   * {@link #main(String[])}
   */
  Md2Orchestra() {

  }

  private Md2Orchestra(Builder builder) {
    this.inputFile = new File(builder.inputFile);
    this.outputFile = new File(builder.outputFile);
    this.referenceFile = builder.referenceFile != null ? new File(builder.referenceFile) : null;
    this.logFile = builder.logFile != null ? new File(builder.logFile) : null;
    this.verbose = builder.verbose;
  }

  public void generate() throws IOException {
    generate(inputFile, outputFile, referenceFile, logFile);
  }

  void generate(File inputFile, File outputFile, File referenceFile, File logFile)
      throws IOException {
    Objects.requireNonNull(inputFile, "Input File is missing");
    Objects.requireNonNull(outputFile, "Output File is missing");

    final Level level = verbose ? Level.DEBUG : Level.ERROR;
    if (logFile != null) {
      logger = LogUtil.initializeFileLogger(logFile.getCanonicalPath(), level, getClass());
    } else {
      logger = LogUtil.initializeDefaultLogger(level, getClass());
    }

    final File outputDir = outputFile.getParentFile();
    if (outputDir != null) {
      outputDir.mkdirs();
    }

    try (InputStream inputStream = new FileInputStream(inputFile);
        OutputStream outputStream = new FileOutputStream(outputFile)) {

      InputStream referenceStream = null;
      if (referenceFile != null) {
        referenceStream = new FileInputStream(referenceFile);
      }

      generate(inputStream, outputStream, referenceStream);
    } catch (final JAXBException e) {
      logger.fatal("Md2Orchestra failed to process XML", e);
      throw new IOException(e);
    }
  }

  void generate(InputStream inputStream, OutputStream outputStream, InputStream referenceStream)
      throws JAXBException, IOException {
    Objects.requireNonNull(inputStream, "Input stream is missing");
    Objects.requireNonNull(outputStream, "Output stream is missing");

    if (logger == null) {
      logger = LogUtil.initializeDefaultLogger(Level.ERROR, getClass());
    }

    final RepositoryBuilder outputRepositoryBuilder = new RepositoryBuilder();

    if (referenceStream != null) {
      final RepositoryAdapter referenceRepository = new RepositoryAdapter();
      referenceRepository.unmarshal(referenceStream);
      outputRepositoryBuilder.setReference(referenceRepository);
    }

    final DocumentParser parser = new DocumentParser();
    parser.parse(inputStream, outputRepositoryBuilder);

    outputRepositoryBuilder.marshal(outputStream);
    logger.info("Md2Orchestra completed");
  }

  private Builder parseArgs(String[] args) throws ParseException {
    final Options options = new Options();
    options.addOption(Option.builder("i").desc("path of markdown input file").longOpt("input")
        .numberOfArgs(1).required().build());
    options.addOption(Option.builder("o").desc("path of output Orchestra file").longOpt("output")
        .numberOfArgs(1).required().build());
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

      builder.inputFile = cmd.getOptionValue("i");
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

}
