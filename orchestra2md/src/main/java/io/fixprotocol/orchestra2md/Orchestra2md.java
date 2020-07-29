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
import io.fixprotocol._2020.orchestra.repository.ActorType;
import io.fixprotocol._2020.orchestra.repository.Annotation;
import io.fixprotocol._2020.orchestra.repository.Appinfo;
import io.fixprotocol._2020.orchestra.repository.CodeSetType;
import io.fixprotocol._2020.orchestra.repository.CodeType;
import io.fixprotocol._2020.orchestra.repository.ComponentRefType;
import io.fixprotocol._2020.orchestra.repository.ComponentType;
import io.fixprotocol._2020.orchestra.repository.Datatype;
import io.fixprotocol._2020.orchestra.repository.Documentation;
import io.fixprotocol._2020.orchestra.repository.FieldRefType;
import io.fixprotocol._2020.orchestra.repository.FieldRuleType;
import io.fixprotocol._2020.orchestra.repository.FieldType;
import io.fixprotocol._2020.orchestra.repository.FlowType;
import io.fixprotocol._2020.orchestra.repository.GroupRefType;
import io.fixprotocol._2020.orchestra.repository.GroupType;
import io.fixprotocol._2020.orchestra.repository.MappedDatatype;
import io.fixprotocol._2020.orchestra.repository.MessageRefType;
import io.fixprotocol._2020.orchestra.repository.MessageType;
import io.fixprotocol._2020.orchestra.repository.MessageType.Responses;
import io.fixprotocol._2020.orchestra.repository.PresenceT;
import io.fixprotocol._2020.orchestra.repository.PurposeEnum;
import io.fixprotocol._2020.orchestra.repository.Repository;
import io.fixprotocol._2020.orchestra.repository.ResponseType;
import io.fixprotocol._2020.orchestra.repository.StateMachineType;
import io.fixprotocol._2020.orchestra.repository.StateType;
import io.fixprotocol._2020.orchestra.repository.TransitionType;
import io.fixprotocol.md.event.ContextFactory;
import io.fixprotocol.md.event.DocumentWriter;
import io.fixprotocol.md.event.MarkdownUtil;
import io.fixprotocol.md.event.MutableContext;
import io.fixprotocol.md.event.MutableDetailProperties;
import io.fixprotocol.md.event.MutableDetailTable;
import io.fixprotocol.md.event.MutableDocumentation;
import io.fixprotocol.orchestra2md.util.LogUtil;

public class Orchestra2md {

  private static final String NOPURPOSE_KEYWORD = "documentation";

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

  public static final String ASSIGN_KEYWORD = "assign";

  // todo: integrate into markdown grammar
  public static final String WHEN_KEYWORD = "when";

  private static final String DEFAULT_SCENARIO = "base";


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
   * @param args command line arguments
   * @throws Exception if the application fails
   */

  public static void main(String[] args) throws Exception {
    Orchestra2md orchestra2md = new Orchestra2md();
    orchestra2md = orchestra2md.parseArgs(args).build();
    orchestra2md.generate();
  }

  private static void showHelp(Options options) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Orchestra2md [options]", options);
  }

  private final ContextFactory contextFactory = new ContextFactory();
  private File inputFile;
  private File logFile;
  private Logger logger = null;
  private File outputFile;
  private final boolean verbose;

  /**
   * For use with {@link #generate(InputStream, OutputStreamWriter)} or {@link #main(String[])}
   */
  Orchestra2md() {
    this.verbose = true;
  }

  private Orchestra2md(Builder builder) {
    this.inputFile = new File(builder.inputFile);
    this.outputFile = new File(builder.outputFile);
    this.logFile = builder.logFile != null ? new File(builder.logFile) : null;
    this.verbose = builder.verbose;
  }

  public void generate() throws Exception {
    generate(inputFile, outputFile, logFile);
  }

  void generate(File inputFile, File outputFile, File logFile) throws Exception {
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
        OutputStreamWriter outputWriter =
            new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
      generate(inputStream, outputWriter);
    }
  }

  void generate(InputStream inputStream, OutputStreamWriter outputWriter) throws Exception {
    Objects.requireNonNull(inputStream, "Input stream is missing");
    Objects.requireNonNull(outputWriter, "Output writer is missing");

    if (logger == null) {
      logger = LogUtil.initializeDefaultLogger(Level.DEBUG, getClass());
    }

    try (final DocumentWriter documentWriter = new DocumentWriter(outputWriter)) {
      final Repository repository = unmarshal(inputStream);
      generateRepositoryMetadata(repository, documentWriter);
      generateActorsAndFlows(repository, documentWriter);
      generateDatatypes(repository, documentWriter);
      generateCodesets(repository, documentWriter);
      generateFields(repository, documentWriter);
      generateComponents(repository, documentWriter);
      generateGroups(repository, documentWriter);
      generateMessages(repository, documentWriter);
    } catch (final JAXBException e) {
      logger.fatal("Orchestra2md failed to parse XML", e);
      throw new IOException(e);
    } catch (final Exception e1) {
      logger.fatal("Orchestra2md error", e1);
      throw e1;
    }
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
          presenceString.append(" " + WHEN_KEYWORD + " ")
              .append(MarkdownUtil.plainTextToMarkdown(when)).append(" ");
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

  private void generateActor(ActorType actor, Repository repository, DocumentWriter documentWriter)
      throws IOException {
    final MutableContext context = contextFactory.createContext(2);
    context.addPair("Actor", actor.getName());
    documentWriter.write(context);

    final List<Object> elements = actor.getFieldOrFieldRefOrComponent();
    final List<Object> members = elements.stream().filter(e -> !(e instanceof StateMachineType))
        .collect(Collectors.toList());
    if (!members.isEmpty()) {
      MutableContext variableContext = contextFactory.createContext(3);
      variableContext.addKey("Variables");
      documentWriter.write(variableContext);

      final MutableDetailTable table = contextFactory.createDetailTable();
      addMembers(table, repository, members);
      documentWriter.write(table);
    }

    for (final Object state : elements) {
      if (state instanceof StateMachineType) {
        generateStateMachine((StateMachineType) state, documentWriter);
      }
    }
  }


  private void generateFlow(FlowType flow, Repository repository, DocumentWriter documentWriter)
      throws IOException {
    MutableContext context = contextFactory.createContext(2);
    context.addPair("Flow", flow.getName());
    documentWriter.write(context);

    final MutableDetailTable table = contextFactory.createDetailTable();
    final MutableDetailProperties row = table.newRow();
    row.addProperty("source", flow.getSource());
    row.addProperty("destination", flow.getDestination());
    documentWriter.write(table);
  }

  private void generateActorsAndFlows(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    final List<Object> actorsOrFlows = repository.getActors().getActorOrFlow();
    for (final Object actorOrFlow : actorsOrFlows) {
      if (actorOrFlow instanceof ActorType) {
        generateActor((ActorType) actorOrFlow, repository, documentWriter);
      } else if (actorOrFlow instanceof FlowType) {
        generateFlow((FlowType) actorOrFlow, repository, documentWriter);
      }
    }
  }

  private void generateCodeset(DocumentWriter documentWriter, final CodeSetType codeset)
      throws IOException {
    MutableContext context = contextFactory.createContext(3);
    context.addPair("Codeset", codeset.getName());
    final String scenario = codeset.getScenario();
    if (!scenario.equals(DEFAULT_SCENARIO)) {
      context.addPair("scenario", scenario);
    }
    context.addPair("type", codeset.getType());
    context.addKey(String.format("(%d)", codeset.getId().intValue()));
    documentWriter.write(context);

    final Annotation annotation = codeset.getAnnotation();
    generateDocumentation(annotation, documentWriter);

    final MutableDetailTable table = contextFactory.createDetailTable();

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
      addDocumentationProperties(row, code.getAnnotation());
    }
    documentWriter.write(table);
  }

  private void generateDocumentation(final Annotation annotation, DocumentWriter documentWriter)
      throws IOException {
    String synopsis = concatenateDocumentation(annotation, List.of(NOPURPOSE_KEYWORD, PurposeEnum.SYNOPSIS.value()));
    if (!synopsis.isBlank()) {
      MutableContext documentationContext = contextFactory.createContext(new String[] {"Synopsis"}, 4);
      documentWriter.write(documentationContext);
      MutableDocumentation documentation =
          contextFactory.createDocumentation(synopsis);
      documentWriter.write(documentation);
    }
    String elaboration = concatenateDocumentation(annotation, List.of(PurposeEnum.ELABORATION.value()));
    if (!elaboration.isBlank()) {
      MutableContext documentationContext = contextFactory.createContext(new String[] {"Elaboration"}, 4);
      documentWriter.write(documentationContext);
      MutableDocumentation documentation =
          contextFactory.createDocumentation(elaboration);
      documentWriter.write(documentation);
    }
  }

  private void generateCodesets(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    final List<CodeSetType> codesets = repository.getCodeSets().getCodeSet().stream()
        .sorted(Comparator.comparing(CodeSetType::getName)).collect(Collectors.toList());
    if (!codesets.isEmpty()) {
      final MutableContext context = contextFactory.createContext(new String[] {"Codesets"}, 2);
      documentWriter.write(context);
    }
    for (final CodeSetType codeset : codesets) {
      generateCodeset(documentWriter, codeset);
    }
  }

  private void generateComponent(Repository repository, DocumentWriter documentWriter,
      final ComponentType component) throws IOException {
    MutableContext context = contextFactory.createContext(3);
    context.addPair("Component", component.getName());
    final String scenario = component.getScenario();
    if (!scenario.equals(DEFAULT_SCENARIO)) {
      context.addPair("scenario", scenario);
    }
    context.addKey(String.format("(%d)", component.getId().intValue()));
    documentWriter.write(context);

    final Annotation annotation = component.getAnnotation();
    generateDocumentation(annotation, documentWriter);
    
    final MutableDetailTable table = contextFactory.createDetailTable();
    final List<Object> members = component.getComponentRefOrGroupRefOrFieldRef();
    addMembers(table, repository, members);
    documentWriter.write(table);
  }

  private void generateComponents(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    final List<ComponentType> components = repository.getComponents().getComponent().stream()
        .sorted(Comparator.comparing(ComponentType::getName)).collect(Collectors.toList());
    if (!components.isEmpty()) {
      final MutableContext context = contextFactory.createContext(new String[] {"Components"}, 2);
      documentWriter.write(context);
    }
    for (final ComponentType component : components) {
      generateComponent(repository, documentWriter, component);
    }
  }

  private void generateDatatypes(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    MutableContext context = contextFactory.createContext(2);
    context.addKey("Datatypes");
    documentWriter.write(context);
    final MutableDetailTable table = contextFactory.createDetailTable();

    final List<Datatype> datatypes = repository.getDatatypes().getDatatype().stream()
        .sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()))
        .collect(Collectors.toList());

    for (final Datatype datatype : datatypes) {
      final List<MappedDatatype> mappings = datatype.getMappedDatatype();
      MutableDetailProperties row = table.newRow();
      row.addProperty("name", datatype.getName());
      row.addProperty(NOPURPOSE_KEYWORD, concatenateDocumentation(datatype.getAnnotation(), 
          List.of(NOPURPOSE_KEYWORD, PurposeEnum.SYNOPSIS.value(), PurposeEnum.ELABORATION.value())));
      for (final MappedDatatype mapping : mappings) {
        row = table.newRow();
        row.addProperty("name", datatype.getName());
        final String standard = mapping.getStandard();
        row.addProperty("standard", standard);
        final String base = mapping.getBase();
        if (base != null) {
          row.addProperty("base", base);
        }
        final String element = mapping.getElement();
        if (element != null) {
          row.addProperty("element", element);
        }
        final String parameter = mapping.getParameter();
        if (parameter != null) {
          row.addProperty("parameter", parameter);
        }
        final String pattern = mapping.getPattern();
        if (pattern != null) {
          row.addProperty("pattern", pattern);
        }
        final String min = mapping.getMinInclusive();
        if (min != null) {
          row.addProperty("minInclusive", min);
        }
        final String max = mapping.getMaxInclusive();
        if (max != null) {
          row.addProperty("maxInclusive", max);
        }
        addDocumentationProperties(row, mapping.getAnnotation());
      }
    }
    documentWriter.write(table);
  }

  private void generateFields(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    MutableContext context = contextFactory.createContext(2);
    context.addKey("Fields");
    documentWriter.write(context);
    final MutableDetailTable table = contextFactory.createDetailTable();

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
      addDocumentationProperties(row, field.getAnnotation());

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
    documentWriter.write(table);
  }

  private void generateGroup(Repository repository, DocumentWriter documentWriter,
      final GroupType group) throws IOException {
    MutableContext context = contextFactory.createContext(3);

    final String name = group.getName();
    context.addPair("Group", name);
    final String scenario = group.getScenario();
    if (!scenario.equals(DEFAULT_SCENARIO)) {
      context.addPair("scenario", scenario);
    }
    context.addKey(String.format("(%d)", group.getId().intValue()));
    documentWriter.write(context);

    final Annotation annotation = group.getAnnotation();
    generateDocumentation(annotation, documentWriter);

    final MutableDetailTable table = contextFactory.createDetailTable();

    final FieldRefType numInGroup = group.getNumInGroup();
    if (numInGroup != null) {
      final MutableDetailProperties row = table.newRow();
      addFieldRef(repository, numInGroup, row);
    } else {
      logger.warn("Orchestra2md unknown numInGroup for group; name={} scenario={}", name, scenario);
    }
    final List<Object> members = group.getComponentRefOrGroupRefOrFieldRef();
    addMembers(table, repository, members);
    documentWriter.write(table);
  }

  private void generateGroups(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    final List<GroupType> groups = repository.getGroups().getGroup().stream()
        .sorted(Comparator.comparing(GroupType::getName)).collect(Collectors.toList());
    if (!groups.isEmpty()) {

      final MutableContext context = contextFactory.createContext(new String[] {"Groups"}, 2);
      documentWriter.write(context);
    }
    for (final GroupType group : groups) {
      generateGroup(repository, documentWriter, group);
    }
  }

  private void generateMessageResponses(Repository repository, DocumentWriter documentWriter,
      MessageType message) throws IOException {
    final Responses responses = message.getResponses();
    if (responses != null) {
      MutableContext context = contextFactory.createContext(3);
      context.addKey("Responses");
      documentWriter.write(context);
      final MutableDetailTable table = contextFactory.createDetailTable();
      // md2orchestra should be able to get message identifiers from higher level message header


      final List<ResponseType> responseList = responses.getResponse();
      for (final ResponseType response : responseList) {
        final List<Object> responseRefs = response.getMessageRefOrAssignOrTrigger();
        for (final Object responseRef : responseRefs) {
          if (responseRef instanceof MessageRefType) {
            final MessageRefType messageRef = (MessageRefType) responseRef;
            final MutableDetailProperties row = table.newRow();
            row.addProperty("name", messageRef.getName());
            final String refScenario = messageRef.getScenario();
            if (!refScenario.equals(DEFAULT_SCENARIO)) {
              row.addProperty("scenario", refScenario);
            }
            final String msgType = messageRef.getMsgType();
            if (msgType != null) {
              row.addProperty("msgType", msgType);
            }
            row.addProperty("when", response.getWhen());
            addDocumentationProperties(row, response.getAnnotation());
          }
        }
      }
      documentWriter.write(table);
    }
  }

  private void generateMessages(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    final List<MessageType> messages = repository.getMessages().getMessage().stream()
        .sorted(Comparator.comparing(MessageType::getName)).collect(Collectors.toList());
    for (final MessageType message : messages) {
      generateMessageStructure(repository, documentWriter, message);
      generateMessageResponses(repository, documentWriter, message);
    }
  }

  private void generateMessageStructure(Repository repository, DocumentWriter documentWriter,
      final MessageType message) throws IOException {
    MutableContext context = contextFactory.createContext(2);
    context.addPair("Message", message.getName());
    final String scenario = message.getScenario();
    if (!scenario.equals(DEFAULT_SCENARIO)) {
      context.addPair("scenario", scenario);
    }
    final String msgType = message.getMsgType();
    if (msgType != null) {
      context.addPair("type", msgType);
    }
    String flow = message.getFlow();
    if (flow != null) {
      context.addPair("flow", flow);
    }
    context.addKey(String.format("(%d)", message.getId().intValue()));

    documentWriter.write(context);

    final Annotation annotation = message.getAnnotation();
    generateDocumentation(annotation, documentWriter);

    final MutableDetailTable table = contextFactory.createDetailTable();

    final List<Object> members = message.getStructure().getComponentRefOrGroupRefOrFieldRef();
    addMembers(table, repository, members);
    documentWriter.write(table);
  }

  private void generateRepositoryMetadata(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    MutableContext context = contextFactory.createContext(1);

    context.addKey(repository.getName());
    if (!repository.getName().toLowerCase().contains("version")) {
      context.addKey(repository.getVersion());
    }
    documentWriter.write(context);

    final MutableDetailTable table = contextFactory.createDetailTable();
    final List<JAXBElement<SimpleLiteral>> elements = repository.getMetadata().getAny();
    for (final JAXBElement<SimpleLiteral> element : elements) {
      final MutableDetailProperties row = table.newRow();
      final String name = element.getName().getLocalPart();
      final String value = String.join(" ", element.getValue().getContent());
      row.addProperty("term", name);
      row.addProperty("value", value);
    }

    documentWriter.write(table);
  }

  private void generateStateMachine(StateMachineType stateMachine, DocumentWriter documentWriter)
      throws IOException {
    MutableContext context = contextFactory.createContext(3);
    context.addPair("StateMachine", stateMachine.getName());
    documentWriter.write(context);
    
    final Annotation annotation = stateMachine.getAnnotation();
    generateDocumentation(annotation, documentWriter);

    final MutableDetailTable table = contextFactory.createDetailTable();
    final StateType initial = stateMachine.getInitial();
    final List<StateType> states = stateMachine.getState();
    generationTransitions(table, initial);
    for (final StateType state : states) {
      generationTransitions(table, state);
    }
    documentWriter.write(table);
  }

  private void generationTransitions(final MutableDetailTable table, StateType state) {
    final List<TransitionType> transitions = state.getTransition();
    for (final TransitionType transition : transitions) {
      final MutableDetailProperties row = table.newRow();
      row.addProperty("state", state.getName());
      row.addProperty("transition", transition.getName());
      row.addProperty("target", transition.getTarget());
      addDocumentationProperties(row, transition.getAnnotation());
      row.addProperty("when", transition.getWhen());
    }
  }

  private String concatenateDocumentation(Annotation annotation, List<String> purposes) {
    if (annotation == null) {
      return "";
    } else {
      final List<Object> objects = annotation.getDocumentationOrAppinfo();
      return objects.stream()
          .filter(o -> o instanceof io.fixprotocol._2020.orchestra.repository.Documentation)
          .map(o -> (io.fixprotocol._2020.orchestra.repository.Documentation) o)
          .filter(d -> purposes.contains(Objects.requireNonNullElse(d.getPurpose(), NOPURPOSE_KEYWORD)))
          .map(d -> {
            if (d.getContentType().contentEquals(MarkdownUtil.MARKDOWN_MEDIA_TYPE)) {
              return d.getContent().stream().map(Object::toString).collect(Collectors.joining(" "));
            } else
              return d.getContent().stream()
                  .map(c -> MarkdownUtil.plainTextToMarkdown(c.toString()))
                  .collect(Collectors.joining(" "));
          }).collect(Collectors.joining(" "));
    }
  }

  private void addDocumentationProperties(MutableDetailProperties properties,
      Annotation annotation) {
    if (annotation == null) {
      return;
    }
    List<Object> objects = annotation.getDocumentationOrAppinfo();
    for (Object obj : objects) {
      if (obj instanceof io.fixprotocol._2020.orchestra.repository.Documentation) {
        io.fixprotocol._2020.orchestra.repository.Documentation documentation = (Documentation) obj;
        String purpose = documentation.getPurpose();
        String markdown = null;
        if (MarkdownUtil.MARKDOWN_MEDIA_TYPE.equals(documentation.getContentType())) {
          markdown = documentation.getContent().stream().map(Object::toString)
              .collect(Collectors.joining(" "));
        } else {
          markdown = documentation.getContent().stream()
              .map(c -> MarkdownUtil.plainTextToMarkdown(c.toString()))
              .collect(Collectors.joining(" "));
        }

        properties.addProperty(Objects.requireNonNullElse(purpose, NOPURPOSE_KEYWORD), markdown);
      } else if (obj instanceof Appinfo) {
        Appinfo appinfo = (Appinfo) obj;
        String contents =
            appinfo.getContent().stream().map(Object::toString).collect(Collectors.joining(" "));
        properties.addProperty(appinfo.getPurpose(), contents);
      }
    }
  }

  private Builder parseArgs(String[] args) throws ParseException {
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
      throw e;
    }
  }

  private Repository unmarshal(InputStream is) throws JAXBException {
    final JAXBContext jaxbContext = JAXBContext.newInstance(Repository.class);
    final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    return (Repository) jaxbUnmarshaller.unmarshal(is);
  }
}
