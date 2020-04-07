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
import org.apache.logging.log4j.Logger;
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
import io.fixprotocol.orchestra2md.util.LogUtil;
import io.fixprotocol.orchestra2md.util.StringUtil;

public class Orchestra2md {

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

  // todo: integrate into markdown grammar
  public static final String WHEN_KEYWORD = "when";

  public static final String ASSIGN_KEYWORD = "assign";

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
   * 
   * @param args
   * @throws Exception
   */

  public static void main(String[] args) throws Exception {
    Orchestra2md orchestra2md = new Orchestra2md();
    orchestra2md = orchestra2md.parseArgs(args).build();
    orchestra2md.generate();
  }

  private static void showHelp(Options options) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Orchestra2md", options);
  }

  private final ContextFactory contextFactory = new ContextFactory();
  private File inputFile;
  private File logFile;
  private Logger logger = null;
  private File outputFile;
  private final boolean verbose;

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
    final int tag = componentRef.getId().intValue();
    final String scenario = componentRef.getScenario();
    final ComponentType component = findComponentByTag(repository, tag, scenario);
    if (component != null) {
      row.addProperty("name", component.getName());
    } else {
      logger.warn("Orchestra2md unknown component; id={} scenario={}", tag, scenario);
    }
    row.addProperty("tag", "component");
    if (!scenario.equals(DEFAULT_SCENARIO)) {
      row.addProperty("scenario", scenario);
    }
    final PresenceT presence = componentRef.getPresence();
    row.addProperty("presence", presence.toString().toLowerCase());
  }

  private void addFieldRef(Repository repository, FieldRefType fieldRef,
      MutableDetailProperties row) {
    final int tag = fieldRef.getId().intValue();
    final String scenario = fieldRef.getScenario();
    final FieldType field = findFieldByTag(repository, tag, scenario);
    if (field != null) {
      row.addProperty("name", field.getName());
    } else {
      logger.warn("Orchestra2md unknown field; id={} scenario={}", tag, scenario);
    }
    row.addProperty("tag", Integer.toString(tag));
    if (!scenario.equals(DEFAULT_SCENARIO)) {
      row.addProperty("scenario", scenario);
    }
    final PresenceT presence = fieldRef.getPresence();
    final StringBuilder presenceString = new StringBuilder();
    final List<FieldRuleType> rules = fieldRef.getRule();
    if (rules.isEmpty()) {
      presenceString.append(presence.toString().toLowerCase());
    } else {
      for (final FieldRuleType rule : rules) {
        final PresenceT rulePresence = rule.getPresence();
        if (rulePresence != null) {
          presenceString.append(rulePresence.toString().toLowerCase());
        }
        final String when = rule.getWhen();
        if (when != null) {
          presenceString.append(" " + WHEN_KEYWORD + " " + when + " ");
        }
      }
    }
    row.addProperty("presence", presenceString.toString());
    final String assign = fieldRef.getAssign();
    if (presence == PresenceT.CONSTANT) {
      final String value = fieldRef.getValue();
      if (value != null) {
        row.addProperty("values", value);
      }
    } else if (assign != null) {
      row.addProperty("values", ASSIGN_KEYWORD + " " + assign);
    }

    final Short implMinLength = fieldRef.getImplMinLength();
    if (implMinLength != null) {
      row.addIntProperty("implMinLength", implMinLength);
    }

    final Short implMaxLength = fieldRef.getImplMaxLength();
    if (implMaxLength != null) {
      row.addIntProperty("implMaxLength", implMaxLength);
    }

    final Short implLength = fieldRef.getImplLength();
    if (implLength != null) {
      row.addIntProperty("implLength", implLength);
    }
  }

  private void addGroupRef(Repository repository, GroupRefType groupRef,
      MutableDetailProperties row) {
    final int tag = groupRef.getId().intValue();
    final String scenario = groupRef.getScenario();
    final GroupType group = findGroupByTag(repository, tag, scenario);
    if (group != null) {
      row.addProperty("name", group.getName());
    } else {
      logger.warn("Orchestra2md unknown group; id={} scenario={}", tag, scenario);
    }
    row.addProperty("tag", "group");
    if (!scenario.equals(DEFAULT_SCENARIO)) {
      row.addProperty("scenario", scenario);
    }
    final PresenceT presence = groupRef.getPresence();
    row.addProperty("presence", presence.toString().toLowerCase());
  }

  private void addMembers(MutableDetailTable table, Repository repository, List<Object> members) {
    for (final Object member : members) {
      final MutableDetailProperties row = table.newRow();
      if (member instanceof FieldRefType) {
        final FieldRefType fieldRef = (FieldRefType) member;
        addFieldRef(repository, fieldRef, row);
      } else if (member instanceof GroupRefType) {
        final GroupRefType groupRef = (GroupRefType) member;
        addGroupRef(repository, groupRef, row);
      } else if (member instanceof ComponentRefType) {
        final ComponentRefType componentRef = (ComponentRefType) member;
        addComponentRef(repository, componentRef, row);
      }
    }
  }

  private ComponentType findComponentByTag(Repository repository, int tag, String scenario) {
    final List<ComponentType> components = repository.getComponents().getComponent();
    for (final ComponentType component : components) {
      if (component.getId().intValue() == tag && component.getScenario().equals(scenario)) {
        return component;
      }
    }
    return null;
  }

  private FieldType findFieldByTag(Repository repository, int tag, String scenario) {
    final List<FieldType> fields = repository.getFields().getField();
    for (final FieldType field : fields) {
      if (field.getId().intValue() == tag && field.getScenario().equals(scenario)) {
        return field;
      }
    }
    return null;
  }

  private GroupType findGroupByTag(Repository repository, int tag, String scenario) {
    final List<GroupType> groups = repository.getGroups().getGroup();
    for (final GroupType group : groups) {
      if (group.getId().intValue() == tag && group.getScenario().equals(scenario)) {
        return group;
      }
    }
    return null;
  }

  private void generateCodesets(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    final List<CodeSetType> codesets = repository.getCodeSets().getCodeSet().stream()
        .sorted(Comparator.comparing(CodeSetType::getName)).collect(Collectors.toList());
    if (!codesets.isEmpty()) {
      final MutableDocumentation documentation =
          contextFactory.createDocumentation(new String[] {"Codesets"}, 2);
      documentWriter.write(documentation);
    }
    for (final CodeSetType codeset : codesets) {
      final MutableDetailTable table = contextFactory.createDetailTable(3);
      table.documentation(getDocumentation(codeset.getAnnotation()));
      table.addPair("Codeset", codeset.getName());
      final String scenario = codeset.getScenario();
      if (!scenario.equals(DEFAULT_SCENARIO)) {
        table.addPair("scenario", scenario);
      }
      table.addPair("type", codeset.getType());
      table.addKey(String.format("(%d)", codeset.getId().intValue()));
      for (final CodeType code : codeset.getCode()) {
        final MutableDetailProperties row = table.newRow();
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
    final List<ComponentType> components = repository.getComponents().getComponent().stream()
        .sorted(Comparator.comparing(ComponentType::getName)).collect(Collectors.toList());
    if (!components.isEmpty()) {
      final MutableDocumentation documentation =
          contextFactory.createDocumentation(new String[] {"Components"}, 2);
      documentWriter.write(documentation);
    }
    for (final ComponentType component : components) {
      final MutableDetailTable table = contextFactory.createDetailTable(3);
      table.addPair("Component", component.getName());
      final String scenario = component.getScenario();
      if (!scenario.equals(DEFAULT_SCENARIO)) {
        table.addPair("scenario", scenario);
      }
      table.addKey(String.format("(%d)", component.getId().intValue()));
      table.documentation(getDocumentation(component.getAnnotation()));
      final List<Object> members = component.getComponentRefOrGroupRefOrFieldRef();
      addMembers(table, repository, members);
      documentWriter.write((DetailTable) table);
    }
  }

  private void generateDatatypes(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    final MutableDetailTable table = contextFactory.createDetailTable(2);
    table.addKey("Datatypes");
    final List<Datatype> datatypes = repository.getDatatypes().getDatatype().stream()
        .sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()))
        .collect(Collectors.toList());

    for (final Datatype datatype : datatypes) {
      final MutableDetailProperties row = table.newRow();
      row.addProperty("name", datatype.getName());
      row.addProperty("documentation", getDocumentation(datatype.getAnnotation()));
    }
    documentWriter.write((DetailTable) table);
  }

  private void generateFields(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    final MutableDetailTable table = contextFactory.createDetailTable(2);
    table.addKey("Fields");
    final List<FieldType> fields = repository.getFields().getField().stream()
        .sorted(Comparator.comparing(FieldType::getId)).collect(Collectors.toList());

    for (final FieldType field : fields) {
      final MutableDetailProperties row = table.newRow();
      row.addProperty("tag", field.getId().toString());
      row.addProperty("name", field.getName());
      final String scenario = field.getScenario();
      if (!scenario.equals(DEFAULT_SCENARIO)) {
        row.addProperty("scenario", scenario);
      }
      row.addProperty("type", field.getType());
      row.addProperty("documentation", getDocumentation(field.getAnnotation()));

      final Short implMinLength = field.getImplMinLength();
      if (implMinLength != null) {
        row.addIntProperty("implMinLength", implMinLength);
      }

      final Short implMaxLength = field.getImplMaxLength();
      if (implMaxLength != null) {
        row.addIntProperty("implMaxLength", implMaxLength);
      }

      final Short implLength = field.getImplLength();
      if (implLength != null) {
        row.addIntProperty("implLength", implLength);
      }
    }
    documentWriter.write((DetailTable) table);
  }

  private void generateGroups(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    final List<GroupType> groups = repository.getGroups().getGroup().stream()
        .sorted(Comparator.comparing(GroupType::getName)).collect(Collectors.toList());
    if (!groups.isEmpty()) {
      final MutableDocumentation documentation =
          contextFactory.createDocumentation(new String[] {"Groups"}, 2);
      documentWriter.write(documentation);
    }
    for (final GroupType group : groups) {
      final MutableDetailTable table = contextFactory.createDetailTable(3);
      final String name = group.getName();
      table.addPair("Group", name);
      final String scenario = group.getScenario();
      if (!scenario.equals(DEFAULT_SCENARIO)) {
        table.addPair("scenario", scenario);
      }
      table.addKey(String.format("(%d)", group.getId().intValue()));
      table.documentation(getDocumentation(group.getAnnotation()));
      final FieldRefType numInGroup = group.getNumInGroup();
      if (numInGroup != null) {
        final MutableDetailProperties row = table.newRow();
        addFieldRef(repository, numInGroup, row);
      } else {
        logger.warn("Orchestra2md unknown numInGroup for group; name={} scenario={}", name,
            scenario);
      }
      final List<Object> members = group.getComponentRefOrGroupRefOrFieldRef();
      addMembers(table, repository, members);
      documentWriter.write((DetailTable) table);
    }
  }

  private void generateMessages(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    final List<MessageType> messages = repository.getMessages().getMessage().stream()
        .sorted(Comparator.comparing(MessageType::getName)).collect(Collectors.toList());
    for (final MessageType message : messages) {
      final MutableDetailTable table = contextFactory.createDetailTable(2);
      table.addPair("Message", message.getName());
      final String scenario = message.getScenario();
      if (!scenario.equals(DEFAULT_SCENARIO)) {
        table.addPair("scenario", scenario);
      }
      final String msgType = message.getMsgType();
      if (msgType != null) {
        table.addPair("type", msgType);
      }
      table.addKey(String.format("(%d)", message.getId().intValue()));
      table.documentation(getDocumentation(message.getAnnotation()));
      final List<Object> members = message.getStructure().getComponentRefOrGroupRefOrFieldRef();
      addMembers(table, repository, members);
      documentWriter.write((DetailTable) table);
    }
  }

  private void generateRepositoryMetadata(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    final MutableDetailTable table = contextFactory.createDetailTable(1);
    table.addKey(repository.getName());
    if (!repository.getName().toLowerCase().contains("version")) {
      table.addKey(repository.getVersion());
    }
 
    final StringBuilder sb = new StringBuilder();
    final List<JAXBElement<SimpleLiteral>> elements = repository.getMetadata().getAny();
    for (final JAXBElement<SimpleLiteral> element : elements) {
      MutableDetailProperties row = table.newRow();
      final String name = element.getName().getLocalPart();
      final String value = String.join(" ", element.getValue().getContent());
      row.addProperty("term", name);
      row.addProperty("value", value);
    }

    documentWriter.write((DetailTable)table);
  }

  private String getDocumentation(Annotation annotation) {
    if (annotation == null) {
      return "";
    } else {
      final List<Object> objects = annotation.getDocumentationOrAppinfo();
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

  private Builder parseArgs(String[] args) throws ParseException {
    final Options options = new Options();
    options.addOption(Option.builder("i").desc("path of Orchestra input file").longOpt("input")
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
      logger = LogUtil.initializeFileLogger(logFile.getCanonicalPath(), level, getClass());
    } else {
      LogUtil.initializeDefaultLogger(level, getClass());
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
    
    try (final DocumentWriter documentWriter = new DocumentWriter(outputWriter)){
      Repository repository = unmarshal(inputStream);
      generateRepositoryMetadata(repository, documentWriter);
      generateDatatypes(repository, documentWriter);
      generateCodesets(repository, documentWriter);
      generateFields(repository, documentWriter);
      generateComponents(repository, documentWriter);
      generateGroups(repository, documentWriter);
      generateMessages(repository, documentWriter);
    } catch (JAXBException e) {
      logger.fatal("Orchestra2md failed to parse XML", e);
      throw new IOException(e);
    } catch (Exception e1) {
      logger.fatal("Orchestra2md IO error", e1);
      throw e1;
    }
  }
}
