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
import io.fixprotocol.md.event.DocumentParser;


/**
 * Translates markdown to an Orchestra file
 * 
 * @author Don Mendelson
 *
 */
public class Md2Orchestra {

  public static class Builder {
    private String inputFile;
    private String outputFile;
    private String referenceFile;

    public Md2Orchestra build() {
      return new Md2Orchestra(this);
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
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Construct and run Md2Orchestra with command line arguments
   * 
   * <pre>
   * usage: Md2Orchestra 
   * -?,--help display usage 
   * -i,--input &lt;arg&gt; path of markdown input file 
   * -o,--output &lt;arg&gt; path of output Orchestra file 
   * -r,--reference &lt;arg&gt; path of reference Orchestra file
   * </pre>
   * 
   * @param args command line arguments
   * @throws Exception if a fatal error occurs
   */
  public static void main(String[] args) throws Exception {
    Md2Orchestra mdl2Orchestra = Md2Orchestra.parseArgs(args).build();
    mdl2Orchestra.generate();
  }



  private static Builder parseArgs(String[] args) throws ParseException {
    Options options = new Options();
    options.addOption(Option.builder("i").desc("path of markdown input file").longOpt("input")
        .numberOfArgs(1).required().build());
    options.addOption(Option.builder("o").desc("path of output Orchestra file").longOpt("output")
        .numberOfArgs(1).required().build());
    options.addOption(Option.builder("r").desc("path of reference Orchestra file")
        .longOpt("reference").numberOfArgs(1).build());
    options.addOption(
        Option.builder("?").numberOfArgs(0).desc("display usage").longOpt("help").build());

    DefaultParser parser = new DefaultParser();
    CommandLine cmd;

    Builder builder = new Builder();

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

      return builder;
    } catch (ParseException e) {
      showHelp(options);
      throw e;
    }
  }

  private static void showHelp(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Md2Orchestra", options);
  }

  private File inputFile;
  private final Logger logger = LogManager.getLogger(getClass());
  private File outputFile;
  private File referenceFile;

  private Md2Orchestra(Builder builder) {
    this.inputFile = new File(builder.inputFile);
    this.outputFile = new File(builder.outputFile);
    this.referenceFile = builder.referenceFile != null ? new File(builder.referenceFile) : null;
  }
  
  /**
   * Only for use with {@link #generate(InputStream, OutputStream, InputStream)}
   */
  Md2Orchestra() {
    
  }

  public void generate() throws IOException {
    generate(inputFile, outputFile, referenceFile);
  }

  void generate(File inputFile, File outputFile, File referenceFile) throws IOException {
    Objects.requireNonNull(inputFile, "Input File is missing");
    Objects.requireNonNull(outputFile, "Output File is missing");
    File outputDir = outputFile.getParentFile();
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
    } catch (JAXBException e) {
      logger.fatal("Md2Orchestra failed to process XML", e);
      throw new IOException(e);
    }
  }

  void generate(InputStream inputStream, OutputStream outputStream, InputStream referenceStream)
      throws JAXBException, IOException {
    Objects.requireNonNull(inputStream, "Input stream is missing");
    Objects.requireNonNull(outputStream, "Output stream is missing");
    RepositoryBuilder outputRepositoryBuilder = new RepositoryBuilder();

    if (referenceStream != null) {
      RepositoryBuilder referenceRepositoryBuilder = new RepositoryBuilder(referenceStream);
      outputRepositoryBuilder.setReference(referenceRepositoryBuilder);
    }

    DocumentParser parser = new DocumentParser();
    parser.parse(inputStream, outputRepositoryBuilder);

    outputRepositoryBuilder.marshal(outputStream);
  }

}
