package io.fixprotocol.interfaces2md;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.purl.dc.elements._1.SimpleLiteral;
import io.fixprotocol._2020.orchestra.interfaces.Annotation;
import io.fixprotocol._2020.orchestra.interfaces.BaseInterfaceType;
import io.fixprotocol._2020.orchestra.interfaces.EncodingType;
import io.fixprotocol._2020.orchestra.interfaces.IdentifierType;
import io.fixprotocol._2020.orchestra.interfaces.InterfaceType;
import io.fixprotocol._2020.orchestra.interfaces.InterfaceType.Sessions;
import io.fixprotocol._2020.orchestra.interfaces.Interfaces;
import io.fixprotocol._2020.orchestra.interfaces.LayerT;
import io.fixprotocol._2020.orchestra.interfaces.MessageCastT;
import io.fixprotocol._2020.orchestra.interfaces.ProtocolType;
import io.fixprotocol._2020.orchestra.interfaces.ReliabilityT;
import io.fixprotocol._2020.orchestra.interfaces.ServiceType;
import io.fixprotocol._2020.orchestra.interfaces.SessionProtocolType;
import io.fixprotocol._2020.orchestra.interfaces.SessionType;
import io.fixprotocol._2020.orchestra.interfaces.TransportProtocolType;
import io.fixprotocol._2020.orchestra.interfaces.UserIntefaceType;
import io.fixprotocol.md.event.ContextFactory;
import io.fixprotocol.md.event.DetailTable;
import io.fixprotocol.md.event.DocumentWriter;
import io.fixprotocol.md.event.MarkdownUtil;
import io.fixprotocol.md.event.MutableContext;
import io.fixprotocol.md.event.MutableDetailProperties;
import io.fixprotocol.md.event.MutableDetailTable;
import io.fixprotocol.md.event.MutableDocumentation;

public class Interfaces2md {

  public static class Builder {


    private String inputFile;
    private String logFile;
    private String outputFile;
    private boolean verbose = false;

    public Interfaces2md build() {
      return new Interfaces2md(this);
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

  public static void main(String[] args) throws Exception {
    Interfaces2md interfaces2md = new Interfaces2md();
    interfaces2md = interfaces2md.parseArgs(args).build();
    interfaces2md.generate();
  }

  private static void showHelp(Options options) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Interfaces2md", options);
  }

  public File logFile;
  public boolean verbose;
  private final ContextFactory contextFactory = new ContextFactory();
  private File inputFile;
  private Logger logger = null;
  private File outputFile;

  Interfaces2md() {
    this.verbose = true;
  }

  private Interfaces2md(Builder builder) {
    this.inputFile = new File(builder.inputFile);
    this.outputFile = new File(builder.outputFile);
    this.logFile = builder.logFile != null ? new File(builder.logFile) : null;
    this.verbose = builder.verbose;
  }

  public void generate() throws Exception {
    generate(inputFile, outputFile, logFile);
  }

  void generate(File inputFile2, File outputFile2, File logFile2) throws Exception {
    Objects.requireNonNull(inputFile, "Input File is missing");
    Objects.requireNonNull(outputFile, "Output File is missing");

    final Level level = verbose ? Level.DEBUG : Level.ERROR;
    if (logFile != null) {
      // logger = LogUtil.initializeFileLogger(logFile.getCanonicalPath(), level, getClass());
    } else {
      // logger = LogUtil.initializeDefaultLogger(level, getClass());
    }

    final File outputDir = outputFile.getParentFile();
    if (outputDir != null) {
      outputDir.mkdirs();
    }

    try (InputStream inputStream = new FileInputStream(inputFile);
        OutputStreamWriter outputWriter =
            new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
      generate(inputStream, outputWriter);
    }
  }

  void generate(InputStream inputStream, OutputStreamWriter outputWriter) throws Exception {
    Objects.requireNonNull(inputStream, "Input stream is missing");
    Objects.requireNonNull(outputWriter, "Output writer is missing");

    try (final DocumentWriter documentWriter = new DocumentWriter(outputWriter)) {
      Interfaces interfaces = unmarshal(inputStream);
      generateMetadata(interfaces, documentWriter);
      List<InterfaceType> interfaceList = interfaces.getInterface();
      for (InterfaceType interfaceInstance : interfaceList) {
        generateInterface(interfaceInstance, documentWriter);
      }

    } catch (JAXBException e) {
      logger.fatal("Orchestra2md failed to parse XML", e);
      throw new IOException(e);
    } catch (Exception e1) {
      logger.fatal("Orchestra2md error", e1);
      throw e1;
    }
  }

  private void generateInterface(InterfaceType interfaceInstance, DocumentWriter documentWriter)
      throws IOException {
    MutableDocumentation context = contextFactory.createDocumentation(2);
    context.addPair("Interface", interfaceInstance.getName());
    context.documentation(getDocumentation(interfaceInstance.getAnnotation()));
    documentWriter.write(context);

    generateProtocolStack(interfaceInstance, documentWriter);

    Sessions sessions = interfaceInstance.getSessions();
    if (sessions != null) {
      List<SessionType> sessionList = sessions.getSession();
      for (SessionType session : sessionList) {
        generateSession(session, documentWriter);
      }
    }
  }

  private void generateMetadata(Interfaces interfaces, DocumentWriter documentWriter)
      throws IOException {
    final MutableDetailTable table = contextFactory.createDetailTable(1);
    table.addKey("Interfaces");

    final List<JAXBElement<SimpleLiteral>> elements = interfaces.getMetadata().getAny();
    for (final JAXBElement<SimpleLiteral> element : elements) {
      MutableDetailProperties row = table.newRow();
      final String name = element.getName().getLocalPart();
      final String value = String.join(" ", element.getValue().getContent());
      row.addProperty("term", name);
      row.addProperty("value", value);
    }

    documentWriter.write((DetailTable) table);
  }

  private void generateProtocolStack(BaseInterfaceType interfaceInstance, DocumentWriter documentWriter)
      throws IOException {
    final MutableDetailTable table = contextFactory.createDetailTable(4);
    table.addKey("Protocols");

    List<ServiceType> services = interfaceInstance.getService();
    for (ServiceType service : services) {
      MutableDetailProperties row = table.newRow();
      row.addProperty("layer", "Service");
      populateProtocol(row, service);
    }

    List<UserIntefaceType> uis = interfaceInstance.getUserInterface();
    for (UserIntefaceType ui : uis) {
      MutableDetailProperties row = table.newRow();
      row.addProperty("layer", "UI");
      populateProtocol(row, ui);
    }

    List<EncodingType> encodings = interfaceInstance.getEncoding();
    for (EncodingType encoding : encodings) {
      MutableDetailProperties row = table.newRow();
      row.addProperty("layer", "Encoding");
      populateProtocol(row, encoding);
    }

    List<SessionProtocolType> sessionProtocols = interfaceInstance.getSessionProtocol();
    for (SessionProtocolType sessionProtocol : sessionProtocols) {
      MutableDetailProperties row = table.newRow();
      row.addProperty("layer", "Session");
      populateProtocol(row, sessionProtocol);
    }

    List<TransportProtocolType> transports = interfaceInstance.getTransport();
    for (TransportProtocolType transport : transports) {
      MutableDetailProperties row = table.newRow();
      row.addProperty("layer", "Transport");
      String use = transport.getUse();
      if (use != null) {
        row.addProperty("use", use);
      }
      String address = transport.getAddress();
      if (address != null) {
        row.addProperty("address", address);
      }
      MessageCastT messageCast = transport.getMessageCast();
      if (messageCast != null) {
        row.addProperty("messageCast", messageCast.name());
      }
      populateProtocol(row, transport);
    }

    List<ProtocolType> protocols = interfaceInstance.getProtocol();
    for (ProtocolType protocol : protocols) {
      MutableDetailProperties row = table.newRow();
      row.addProperty("layer", protocol.getLayer().name());
      populateProtocol(row, protocol);
    }
    documentWriter.write((DetailTable) table);
  }

  private void generateSession(SessionType session, DocumentWriter documentWriter)
      throws IOException {
    final MutableContext context = contextFactory.createContext(3);
    context.addKey("Session");
    context.addKey(session.getName());
    documentWriter.write(context);

    generateSessionIdentifiers(session, documentWriter);
    generateProtocolStack(session, documentWriter);
  }

  private void generateSessionIdentifiers(SessionType session, DocumentWriter documentWriter)
      throws IOException {
    List<IdentifierType> ids = session.getIdentifier();
    if (!ids.isEmpty()) {
      final MutableDetailTable table = contextFactory.createDetailTable(4);
      table.addKey("Identifiers");

      for (IdentifierType id : ids) {
        MutableDetailProperties row = table.newRow();
        row.addProperty("name", id.getName());
        row.addProperty("value", id.getValue());
      }
      documentWriter.write((DetailTable) table);
    }
  }

  private String getDocumentation(Annotation annotation) {
    if (annotation == null) {
      return "";
    } else {
      final List<Object> objects = annotation.getDocumentationOrAppinfo();
      return objects.stream()
          .filter(o -> o instanceof io.fixprotocol._2020.orchestra.interfaces.Documentation)
          .map(o -> (io.fixprotocol._2020.orchestra.interfaces.Documentation) o).map(d -> {
            if (d.getContentType().contentEquals(MarkdownUtil.MARKDOWN_MEDIA_TYPE)) {
              return d.getContent().stream().map(Object::toString).collect(Collectors.joining(" "));
            } else
              return d.getContent().stream()
                  .map(c -> MarkdownUtil.plainTextToMarkdown(c.toString()))
                  .collect(Collectors.joining(" "));
          }).collect(Collectors.joining(" "));
    }
  }

  private Builder parseArgs(String[] args) throws ParseException {
    final Options options = new Options();
    options.addOption(Option.builder("i").desc("path of interfaces input file").longOpt("input")
        .numberOfArgs(1).required().build());
    options.addOption(Option.builder("o").desc("path of markdown output file").longOpt("output")
        .numberOfArgs(1).required().build());
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
      throw e;
    }
  }

  private void populateProtocol(final MutableDetailProperties row, ProtocolType protocol) {
    final LayerT layer = protocol.getLayer();
    if (layer != null) {
      row.addProperty("layer", layer.name());
    }
    final String name = protocol.getName();
    if (name != null) {
      row.addProperty("name", name);
    }
    final String version = protocol.getVersion();
    if (version != null) {
      row.addProperty("version", version);
    }
    final ReliabilityT reliability = protocol.getReliability();
    if (reliability != null) {
      row.addProperty("reliability", reliability.name());
    }

    String orchestration = protocol.getOrchestration();
    if (orchestration != null) {
      row.addProperty("orchestration", orchestration);
    }
  }

  private Interfaces unmarshal(InputStream is) throws JAXBException {
    final JAXBContext jaxbContext = JAXBContext.newInstance(Interfaces.class);
    final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    return (Interfaces) jaxbUnmarshaller.unmarshal(is);
  }
}
