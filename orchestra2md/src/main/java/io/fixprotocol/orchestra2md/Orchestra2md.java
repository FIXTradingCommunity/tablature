package io.fixprotocol.orchestra2md;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.purl.dc.elements._1.SimpleLiteral;
import io.fixprotocol._2020.orchestra.repository.Annotation;
import io.fixprotocol._2020.orchestra.repository.CodeSetType;
import io.fixprotocol._2020.orchestra.repository.CodeType;
import io.fixprotocol._2020.orchestra.repository.ComponentRefType;
import io.fixprotocol._2020.orchestra.repository.ComponentType;
import io.fixprotocol._2020.orchestra.repository.Datatype;
import io.fixprotocol._2020.orchestra.repository.FieldRefType;
import io.fixprotocol._2020.orchestra.repository.FieldRuleType;
import io.fixprotocol._2020.orchestra.repository.FieldType;
import io.fixprotocol._2020.orchestra.repository.GroupRefType;
import io.fixprotocol._2020.orchestra.repository.GroupType;
import io.fixprotocol._2020.orchestra.repository.MessageType;
import io.fixprotocol._2020.orchestra.repository.PresenceT;
import io.fixprotocol._2020.orchestra.repository.Repository;
import io.fixprotocol.md.event.ContextFactory;
import io.fixprotocol.md.event.DetailTable;
import io.fixprotocol.md.event.DocumentWriter;
import io.fixprotocol.md.event.MutableDetailProperties;
import io.fixprotocol.md.event.MutableDetailTable;
import io.fixprotocol.md.event.MutableDocumentation;
import io.fixprotocol.md.util.StringUtil;

public class Orchestra2md {
  
  // todo: integrate into markdown grammar
  public static final String WHEN_KEYWORD = "when";
  public static final String ASSIGN_KEYWORD = "assign";

  public static class Builder {
    public String logFile;
    public boolean verbose;
    private String inputFile;
    private String outputFile;

    public Orchestra2md build() {
      return new Orchestra2md(this);
    }

    public Builder inputFile(String inputFile) {
      this.inputFile = inputFile;
      return this;
    }

    public Builder outputFile(String outputFile) {
      this.outputFile = outputFile;
      return this;
    }
  }

  private static final String DEFAULT_SCENARIO = "base";
  private static final String MARKDOWN_MEDIA_TYPE = "text/markdown";

  public static Builder builder() {
    return new Builder();
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
   * @param args
   * @throws Exception
   */

  public static void main(String[] args) throws Exception {
    Orchestra2md orchestra2md = new Orchestra2md();
    orchestra2md = orchestra2md.parseArgs(args).build();
    orchestra2md.generate();
  }

  private static void showHelp(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Orchestra2md", options);
  }

  private final ContextFactory contextFactory = new ContextFactory();
  private File inputFile;
  private File logFile;
  private Logger logger = null;
  private File outputFile;
  private boolean verbose;

  private Orchestra2md(Builder builder) {
    this.inputFile = new File(builder.inputFile);
    this.outputFile = new File(builder.outputFile);
    this.logFile = builder.logFile != null ? new File(builder.logFile) : null;
    this.verbose = builder.verbose;
  }

  /**
   * For use with {@link #generate(InputStream, OutputStreamWriter)} or {@link #main(String[])}
   */
  Orchestra2md() {
    this.verbose = true;
  }

  public void generate() throws Exception {
    generate(inputFile, outputFile, logFile);
  }

  private void addComponentRef(Repository repository, ComponentRefType componentRef,
      MutableDetailProperties row) {
    int tag = componentRef.getId().intValue();
    String scenario = componentRef.getScenario();
    ComponentType component = findComponentByTag(repository, tag, scenario);
    if (component != null) {
      row.addProperty("name", component.getName());
    } else {
      logger.warn("Orchestra2md unknown component; id={} scenario={}", tag, scenario);
    }
    row.addProperty("tag", "component");
    if (!scenario.equals(DEFAULT_SCENARIO)) {
      row.addProperty("scenario", scenario);
    }
    PresenceT presence = componentRef.getPresence();
    row.addProperty("presence", presence.toString().toLowerCase());
  }

  private void addFieldRef(Repository repository, FieldRefType fieldRef,
      MutableDetailProperties row) {
    int tag = fieldRef.getId().intValue();
    String scenario = fieldRef.getScenario();
    FieldType field = findFieldByTag(repository, tag, scenario);
    if (field != null) {
      row.addProperty("name", field.getName());
    } else {
      logger.warn("Orchestra2md unknown field; id={} scenario={}", tag, scenario);
    }
    row.addProperty("tag", Integer.toString(tag));
    if (!scenario.equals(DEFAULT_SCENARIO)) {
      row.addProperty("scenario", scenario);
    }
    PresenceT presence = fieldRef.getPresence();
    StringBuilder presenceString = new StringBuilder();
    List<FieldRuleType> rules = fieldRef.getRule();
    if (rules.isEmpty()) {
      presenceString.append(presence.toString().toLowerCase());
    } else {    
      for (FieldRuleType rule : rules) {
        PresenceT rulePresence = rule.getPresence();
        if (rulePresence != null) {
          presenceString.append(rulePresence.toString().toLowerCase());
        }
        String when = rule.getWhen();
        if (when != null) {
          presenceString.append(" " + WHEN_KEYWORD + " " + when + " ");
        }
      }
    }
    row.addProperty("presence", presenceString.toString());
    String assign = fieldRef.getAssign();
    if (presence == PresenceT.CONSTANT) {
      String value = fieldRef.getValue();
      if (value != null) {
        row.addProperty("values", value);
      }
    } else if (assign != null) {
      row.addProperty("values", ASSIGN_KEYWORD + " " + assign);
    }
  }

  private void addGroupRef(Repository repository, GroupRefType groupRef,
      MutableDetailProperties row) {
    int tag = groupRef.getId().intValue();
    String scenario = groupRef.getScenario();
    GroupType group = findGroupByTag(repository, tag, scenario);
    if (group != null) {
      row.addProperty("name", group.getName());
    } else {
      logger.warn("Orchestra2md unknown group; id={} scenario={}", tag, scenario);
    }
    row.addProperty("tag", "group");
    if (!scenario.equals(DEFAULT_SCENARIO)) {
      row.addProperty("scenario", scenario);
    }
    PresenceT presence = groupRef.getPresence();
    row.addProperty("presence", presence.toString().toLowerCase());
  }

  private void addMembers(MutableDetailTable table, Repository repository, List<Object> members) {
    for (Object member : members) {
      MutableDetailProperties row = table.newRow();
      if (member instanceof FieldRefType) {
        FieldRefType fieldRef = (FieldRefType) member;
        addFieldRef(repository, fieldRef, row);
      } else if (member instanceof GroupRefType) {
        GroupRefType groupRef = (GroupRefType) member;
        addGroupRef(repository, groupRef, row);
      } else if (member instanceof ComponentRefType) {
        ComponentRefType componentRef = (ComponentRefType) member;
        addComponentRef(repository, componentRef, row);
      }
    }
  }

  private ComponentType findComponentByTag(Repository repository, int tag, String scenario) {
    List<ComponentType> components = repository.getComponents().getComponent();
    for (ComponentType component : components) {
      if (component.getId().intValue() == tag && component.getScenario().equals(scenario)) {
        return component;
      }
    }
    return null;
  }

  private FieldType findFieldByTag(Repository repository, int tag, String scenario) {
    List<FieldType> fields = repository.getFields().getField();
    for (FieldType field : fields) {
      if (field.getId().intValue() == tag && field.getScenario().equals(scenario)) {
        return field;
      }
    }
    return null;
  }

  private GroupType findGroupByTag(Repository repository, int tag, String scenario) {
    List<GroupType> groups = repository.getGroups().getGroup();
    for (GroupType group : groups) {
      if (group.getId().intValue() == tag && group.getScenario().equals(scenario)) {
        return group;
      }
    }
    return null;
  }

  private void generateCodesets(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    List<CodeSetType> codesets = repository.getCodeSets().getCodeSet().stream()
        .sorted(Comparator.comparing(CodeSetType::getName)).collect(Collectors.toList());
    if (!codesets.isEmpty()) {
      MutableDocumentation documentation =
          contextFactory.createDocumentation(new String[] {"Codesets"}, 2);
      documentWriter.write(documentation);
    }
    for (CodeSetType codeset : codesets) {
      MutableDetailTable table = contextFactory.createDetailTable(3);
      table.documentation(getDocumentation(codeset.getAnnotation()));
      table.addPair("Codeset", codeset.getName());
      String scenario = codeset.getScenario();
      if (!scenario.equals(DEFAULT_SCENARIO)) {
        table.addPair("scenario", scenario);
      }
      table.addPair("type", codeset.getType());
      table.addKey(String.format("(%d)", codeset.getId().intValue()));
      for (CodeType code : codeset.getCode()) {
        MutableDetailProperties row = table.newRow();
        final String name = code.getName();
        row.addProperty("name", name);
        row.addProperty("value", code.getValue());
        final BigInteger id = code.getId();
        if (id != null) {
          row.addProperty("id", id.toString());
        } else {
          logger.warn("Orchestra2md unknown code id; name={} scenario={}", name, scenario);
        }
        row.addProperty("documentation", getDocumentation(code.getAnnotation()));
      }
      documentWriter.write((DetailTable) table);
    }
  }

  private void generateComponents(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    List<ComponentType> components = repository.getComponents().getComponent().stream()
        .sorted(Comparator.comparing(ComponentType::getName)).collect(Collectors.toList());
    if (!components.isEmpty()) {
      MutableDocumentation documentation =
          contextFactory.createDocumentation(new String[] {"Components"}, 2);
      documentWriter.write(documentation);
    }
    for (ComponentType component : components) {
      MutableDetailTable table = contextFactory.createDetailTable(3);
      table.addPair("Component", component.getName());
      String scenario = component.getScenario();
      if (!scenario.equals(DEFAULT_SCENARIO)) {
        table.addPair("scenario", scenario);
      }
      table.addKey(String.format("(%d)", component.getId().intValue()));
      table.documentation(getDocumentation(component.getAnnotation()));
      List<Object> members = component.getComponentRefOrGroupRefOrFieldRef();
      addMembers(table, repository, members);
      documentWriter.write((DetailTable) table);
    }
  }

  private void generateDatatypes(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    MutableDetailTable table = contextFactory.createDetailTable(2);
    table.addKey("Datatypes");
    List<Datatype> datatypes = repository.getDatatypes().getDatatype().stream()
        .sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()))
        .collect(Collectors.toList());

    for (Datatype datatype : datatypes) {
      MutableDetailProperties row = table.newRow();
      row.addProperty("name", datatype.getName());
      row.addProperty("documentation", getDocumentation(datatype.getAnnotation()));
    }
    documentWriter.write((DetailTable) table);
  }

  private void generateFields(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    MutableDetailTable table = contextFactory.createDetailTable(2);
    table.addKey("Fields");
    List<FieldType> fields = repository.getFields().getField().stream()
        .sorted(Comparator.comparing(FieldType::getId)).collect(Collectors.toList());

    for (FieldType field : fields) {
      MutableDetailProperties row = table.newRow();
      row.addProperty("tag", field.getId().toString());
      row.addProperty("name", field.getName());
      String scenario = field.getScenario();
      if (!scenario.equals(DEFAULT_SCENARIO)) {
        row.addProperty("scenario", scenario);
      }
      row.addProperty("type", field.getType());
      row.addProperty("documentation", getDocumentation(field.getAnnotation()));
    }
    documentWriter.write((DetailTable) table);
  }

  private void generateGroups(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    List<GroupType> groups = repository.getGroups().getGroup().stream()
        .sorted(Comparator.comparing(GroupType::getName)).collect(Collectors.toList());
    if (!groups.isEmpty()) {
      MutableDocumentation documentation =
          contextFactory.createDocumentation(new String[] {"Groups"}, 2);
      documentWriter.write(documentation);
    }
    for (GroupType group : groups) {
      MutableDetailTable table = contextFactory.createDetailTable(3);
      final String name = group.getName();
      table.addPair("Group", name);
      String scenario = group.getScenario();
      if (!scenario.equals(DEFAULT_SCENARIO)) {
        table.addPair("scenario", scenario);
      }
      table.addKey(String.format("(%d)", group.getId().intValue()));
      table.documentation(getDocumentation(group.getAnnotation()));
      FieldRefType numInGroup = group.getNumInGroup();
      if (numInGroup != null) {
        MutableDetailProperties row = table.newRow();
        addFieldRef(repository, numInGroup, row);
      } else {
        logger.warn("Orchestra2md unknown numInGroup for group; name={} scenario={}", name, scenario);
      }
      List<Object> members = group.getComponentRefOrGroupRefOrFieldRef();
      addMembers(table, repository, members);
      documentWriter.write((DetailTable) table);
    }
  }

  private void generateMessages(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    List<MessageType> messages = repository.getMessages().getMessage().stream()
        .sorted(Comparator.comparing(MessageType::getName)).collect(Collectors.toList());
    for (MessageType message : messages) {
      MutableDetailTable table = contextFactory.createDetailTable(2);
      table.addPair("Message", message.getName());
      String scenario = message.getScenario();
      if (!scenario.equals(DEFAULT_SCENARIO)) {
        table.addPair("scenario", scenario);
      }
      final String msgType = message.getMsgType();
      if (msgType != null) {
        table.addPair("type", msgType);
      }
      table.addKey(String.format("(%d)", message.getId().intValue()));
      table.documentation(getDocumentation(message.getAnnotation()));
      List<Object> members = message.getStructure().getComponentRefOrGroupRefOrFieldRef();
      addMembers(table, repository, members);
      documentWriter.write((DetailTable) table);
    }
  }

  private void generateRepositoryMetadata(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    MutableDocumentation documentation = contextFactory.createDocumentation(1);
    documentation.addKey(repository.getName());
    if (!repository.getName().toLowerCase().contains("version")) {
      documentation.addKey(repository.getVersion());
    }

    StringBuilder sb = new StringBuilder();
    List<JAXBElement<SimpleLiteral>> elements = repository.getMetadata().getAny();
    for (JAXBElement<SimpleLiteral> element : elements) {
      String name = element.getName().getLocalPart();
      String value = String.join(" ", element.getValue().getContent());
      sb.append(String.format("*%s*: %s%n%n", name, value));
    }
    String text = sb.toString();
    if (!text.isEmpty()) {
      documentation.documentation(text);
    }
    documentWriter.write(documentation);
  }

  private String getDocumentation(Annotation annotation) {
    if (annotation == null) {
      return "";
    } else {
      List<Object> objects = annotation.getDocumentationOrAppinfo();
      return objects.stream()
          .filter(o -> o instanceof io.fixprotocol._2020.orchestra.repository.Documentation)
          .map(o -> (io.fixprotocol._2020.orchestra.repository.Documentation) o).map(d -> {
            if (d.getContentType().contentEquals(MARKDOWN_MEDIA_TYPE)) {
              return d.getContent().stream().map(Object::toString).collect(Collectors.joining(" "));
            } else
              return d.getContent().stream().map(c -> StringUtil.stripWhitespace(c.toString()))
                  .collect(Collectors.joining(" "));
          }).collect(Collectors.joining(" "));
    }
  }

  private Logger initializeDefaultLogger(Level level) {
    final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    final Configuration config = ctx.getConfiguration();
    ConsoleAppender appender = ConsoleAppender.newBuilder().setName("Console").build();
    config.addAppender(appender);
    AppenderRef ref = AppenderRef.createAppenderRef("Console", level, null);
    AppenderRef[] refs = new AppenderRef[] {ref};
    LoggerConfig loggerConfig = LoggerConfig.createLogger(true, level, getClass().getName(), null,
        refs, null, config, null);
    config.addLogger(getClass().getName(), loggerConfig);
    ctx.updateLoggers();
    return LogManager.getLogger(getClass());
  }

  // todo: move to a common utility package
  private Logger initializeFileLogger(String fileName, Level level) {
    ConfigurationBuilder<BuiltConfiguration> builder =
        ConfigurationBuilderFactory.newConfigurationBuilder();
    AppenderComponentBuilder appenderBuilder =
        builder.newAppender("file", "File").addAttribute("fileName", fileName);
    builder.add(appenderBuilder);
    builder.add(builder.newLogger(getClass().getCanonicalName(), level)
        .add(builder.newAppenderRef("file")));
    builder.add(builder.newRootLogger(level).add(builder.newAppenderRef("file")));
    Configurator.initialize(builder.build());
    return LogManager.getLogger(getClass());
  }

  private Builder parseArgs(String[] args) throws ParseException {
    Options options = new Options();
    options.addOption(Option.builder("i").desc("path of Orchestra input file").longOpt("input")
        .numberOfArgs(1).required().build());
    options.addOption(Option.builder("o").desc("path of markdown output file").longOpt("output")
        .numberOfArgs(1).required().build());
    options.addOption(
        Option.builder("?").numberOfArgs(0).desc("display usage").longOpt("help").build());
    options.addOption(
        Option.builder("e").desc("path of log file").longOpt("eventlog").numberOfArgs(1).build());
    options.addOption(Option.builder("v").desc("verbose event log").longOpt("verbose").build());

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

      if (cmd.hasOption("e")) {
        builder.logFile = cmd.getOptionValue("e");
      }

      if (cmd.hasOption("v")) {
        builder.verbose = true;
      }

      return builder;
    } catch (ParseException e) {
      showHelp(options);
      throw e;
    }
  }

  private Repository unmarshal(InputStream is) throws JAXBException {
    final JAXBContext jaxbContext = JAXBContext.newInstance(Repository.class);
    final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    return (Repository) jaxbUnmarshaller.unmarshal(is);
  }

  void generate(File inputFile, File outputFile, File logFile) throws Exception {
    Objects.requireNonNull(inputFile, "Input File is missing");
    Objects.requireNonNull(outputFile, "Output File is missing");

    final Level level = verbose ? Level.DEBUG : Level.ERROR;
    if (logFile != null) {
      logger = initializeFileLogger(logFile.getCanonicalPath(), level);
    } else {
      initializeDefaultLogger(level);
    }

    File outputDir = outputFile.getParentFile();
    if (outputDir != null) {
      outputDir.mkdirs();
    }

    try (InputStream inputStream = new FileInputStream(inputFile);
        OutputStreamWriter outputWriter =
            new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
      generate(inputStream, outputWriter);
    } catch (JAXBException e) {
      logger.fatal("Orchestra2md failed to process XML", e);
      throw new IOException(e);
    }
  }

  void generate(InputStream inputStream, OutputStreamWriter outputWriter)
      throws JAXBException, IOException {
    Objects.requireNonNull(inputStream, "Input stream is missing");
    Objects.requireNonNull(outputWriter, "Output writer is missing");
    DocumentWriter documentWriter = new DocumentWriter(outputWriter);
    Repository repository = unmarshal(inputStream);
    generateRepositoryMetadata(repository, documentWriter);
    generateDatatypes(repository, documentWriter);
    generateCodesets(repository, documentWriter);
    generateFields(repository, documentWriter);
    generateComponents(repository, documentWriter);
    generateGroups(repository, documentWriter);
    generateMessages(repository, documentWriter);
  }
}
