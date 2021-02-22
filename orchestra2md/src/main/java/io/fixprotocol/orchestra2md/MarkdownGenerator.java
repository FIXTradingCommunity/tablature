package io.fixprotocol.orchestra2md;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.purl.dc.elements._1.SimpleLiteral;
import io.fixprotocol._2020.orchestra.repository.ActorType;
import io.fixprotocol._2020.orchestra.repository.Actors;
import io.fixprotocol._2020.orchestra.repository.Annotation;
import io.fixprotocol._2020.orchestra.repository.Appinfo;
import io.fixprotocol._2020.orchestra.repository.CodeSetType;
import io.fixprotocol._2020.orchestra.repository.CodeType;
import io.fixprotocol._2020.orchestra.repository.ComponentRefType;
import io.fixprotocol._2020.orchestra.repository.ComponentType;
import io.fixprotocol._2020.orchestra.repository.Components;
import io.fixprotocol._2020.orchestra.repository.Datatype;
import io.fixprotocol._2020.orchestra.repository.Datatypes;
import io.fixprotocol._2020.orchestra.repository.Documentation;
import io.fixprotocol._2020.orchestra.repository.FieldRefType;
import io.fixprotocol._2020.orchestra.repository.FieldRuleType;
import io.fixprotocol._2020.orchestra.repository.FieldType;
import io.fixprotocol._2020.orchestra.repository.Fields;
import io.fixprotocol._2020.orchestra.repository.FlowType;
import io.fixprotocol._2020.orchestra.repository.GroupRefType;
import io.fixprotocol._2020.orchestra.repository.GroupType;
import io.fixprotocol._2020.orchestra.repository.Groups;
import io.fixprotocol._2020.orchestra.repository.MappedDatatype;
import io.fixprotocol._2020.orchestra.repository.MessageRefType;
import io.fixprotocol._2020.orchestra.repository.MessageType;
import io.fixprotocol._2020.orchestra.repository.MessageType.Responses;
import io.fixprotocol._2020.orchestra.repository.Messages;
import io.fixprotocol._2020.orchestra.repository.PresenceT;
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
import io.fixprotocol.md.event.StringUtil;
import io.fixprotocol.orchestra.event.EventListener;
import io.fixprotocol.orchestra.event.EventListenerFactory;
import io.fixprotocol.orchestra.event.TeeEventListener;

public class MarkdownGenerator {

  public static final String ASSIGN_KEYWORD = "assign";
  public static final String DOCUMENTATION_KEYWORD = "documentation";
  
  /**
   * Default token to represent a paragraph break in tables (not natively supported by markdown)
   */
  public static final String DEFAULT_PARAGRAPH_DELIMITER = "/P/";

  // todo: integrate into markdown grammar
  public static final String WHEN_KEYWORD = "when";
  private static final String DEFAULT_SCENARIO = "base";
 
  
  private final ContextFactory contextFactory = new ContextFactory();
  private EventListener eventLogger;
  private final Logger logger = LogManager.getLogger(getClass());
  private final String paragraphDelimiterInTables;
  
  public MarkdownGenerator() {
    this.paragraphDelimiterInTables = DEFAULT_PARAGRAPH_DELIMITER;
  }
  
  public MarkdownGenerator(String paragraphDelimiterInTables) {
    this.paragraphDelimiterInTables = paragraphDelimiterInTables;
  }

  public void generate(InputStream inputStream, OutputStreamWriter outputWriter,
      EventListener eventLogger) throws Exception {
    this.eventLogger = eventLogger;
    try (eventLogger; final DocumentWriter documentWriter = new DocumentWriter(outputWriter)) {
      final Repository repository = XmlParser.unmarshal(inputStream, eventLogger);
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

  public void generate(InputStream inputStream, OutputStreamWriter outputWriter,
      OutputStream jsonOutputStream) throws Exception {
    Objects.requireNonNull(inputStream, "Input stream is missing");
    Objects.requireNonNull(outputWriter, "Output writer is missing");
    final EventListenerFactory factory = new EventListenerFactory();
    final TeeEventListener eventLogger = new TeeEventListener();
    final EventListener logEventLogger = factory.getInstance("LOG4J");
    logEventLogger.setResource(logger);
    eventLogger.addEventListener(logEventLogger);
    if (jsonOutputStream != null) {
      final EventListener jsonEventLogger = factory.getInstance("JSON");
      jsonEventLogger.setResource(jsonOutputStream);
      eventLogger.addEventListener(jsonEventLogger);
    }

    generate(inputStream, outputWriter, eventLogger);
  }

  private void addComponentRefRow(Repository repository, ComponentRefType componentRef,
      MutableDetailProperties row) {
    final int tag = componentRef.getId().intValue();
    final String scenario = componentRef.getScenario();
    final ComponentType component = findComponentByTag(repository, tag, scenario);
    if (component != null) {
      row.addProperty("name", component.getName());
    } else {
      eventLogger.warn("Unknown component; id={0} scenario={1}", tag, scenario);
    }
    row.addProperty("tag", "component");
    if (!scenario.equals(DEFAULT_SCENARIO)) {
      row.addProperty("scenario", scenario);
    }
    final PresenceT presence = componentRef.getPresence();
    row.addProperty("presence", presence.toString().toLowerCase());
    addDocumentationColumns(row, componentRef.getAnnotation(), getParagraphDelimiterInTables());
  }

  private void addDocumentationColumns(MutableDetailProperties properties, Annotation annotation,
      String paragraphDelimiter) {
    if (annotation != null) {
      // Synopsis may explicit or implicit (null purpose)
      // handle all 'purpose' values, including appinfo

      SortedMap<String, List<Object>> sorted = groupDocumentationByPurpose(annotation);
      Set<Entry<String, List<Object>>> entries = sorted.entrySet();
      for (Entry<String, List<Object>> e : entries) {
        String text = concatenateDocumentation(e.getValue(), getParagraphDelimiterInTables());
        if (!text.isBlank()) {
          properties.addProperty(e.getKey(), text);
        }
      }
    }
  }

  private void addFieldRefRow(Repository repository, FieldRefType fieldRef,
      MutableDetailProperties row) {
    final int tag = fieldRef.getId().intValue();
    final String scenario = fieldRef.getScenario();
    final FieldType field = findFieldByTag(repository, tag, scenario);
    if (field != null) {
      row.addProperty("name", field.getName());
    } else {
      eventLogger.warn("Unknown field; id={0} scenario={1}", tag, scenario);
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
    addDocumentationColumns(row, fieldRef.getAnnotation(), getParagraphDelimiterInTables());
  }

  private void addGroupRefRow(Repository repository, GroupRefType groupRef,
      MutableDetailProperties row) {
    final int tag = groupRef.getId().intValue();
    final String scenario = groupRef.getScenario();
    final GroupType group = findGroupByTag(repository, tag, scenario);
    if (group != null) {
      row.addProperty("name", group.getName());
    } else {
      eventLogger.warn("Unknown group; id={0} scenario={1}", tag, scenario);
    }
    row.addProperty("tag", "group");
    if (!scenario.equals(DEFAULT_SCENARIO)) {
      row.addProperty("scenario", scenario);
    }
    final PresenceT presence = groupRef.getPresence();
    row.addProperty("presence", presence.toString().toLowerCase());
    addDocumentationColumns(row, groupRef.getAnnotation(), getParagraphDelimiterInTables());
  }

  private void addMemberRows(MutableDetailTable table, Repository repository,
      List<Object> members) {
    for (final Object member : members) {
      final MutableDetailProperties row = table.newRow();
      if (member instanceof FieldRefType) {
        final FieldRefType fieldRef = (FieldRefType) member;
        addFieldRefRow(repository, fieldRef, row);
      } else if (member instanceof GroupRefType) {
        final GroupRefType groupRef = (GroupRefType) member;
        addGroupRefRow(repository, groupRef, row);
      } else if (member instanceof ComponentRefType) {
        final ComponentRefType componentRef = (ComponentRefType) member;
        addComponentRefRow(repository, componentRef, row);
      }
    }
  }

  private String concatenateDocumentation(List<Object> objects, String paragraphDelimiter) {
    return objects.stream().map(o -> {
      if (o instanceof io.fixprotocol._2020.orchestra.repository.Documentation) {
        return documentToString((io.fixprotocol._2020.orchestra.repository.Documentation)o, paragraphDelimiter);
      } else {
        return appinfoToString(o, paragraphDelimiter);
      }
    }).collect(Collectors.joining(paragraphDelimiter));
  }

  public String appinfoToString(Object o, String paragraphDelimiter) {
    io.fixprotocol._2020.orchestra.repository.Appinfo a =
        (io.fixprotocol._2020.orchestra.repository.Appinfo) o;
    return a.getContent().stream()
        .map(c -> c.toString().strip().replace("\n", paragraphDelimiter))
        .map(MarkdownUtil::plainTextToMarkdown)
        .collect(Collectors.joining(paragraphDelimiter));
  }

  private String documentToString(io.fixprotocol._2020.orchestra.repository.Documentation d, String paragraphDelimiter) {
    if (d.getContentType().contentEquals(MarkdownUtil.MARKDOWN_MEDIA_TYPE)) {
      return d.getContent().stream()
          .map(c -> c.toString().strip().replace("\n\n", paragraphDelimiter))        
          .collect(Collectors.joining(paragraphDelimiter));
    } else {
      return d.getContent().stream()
          .map(c -> c.toString().strip().replace("\n", paragraphDelimiter))
          .map(MarkdownUtil::plainTextToMarkdown)
          .collect(Collectors.joining(paragraphDelimiter));
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

    final Annotation annotation = actor.getAnnotation();
    generateDocumentationBlocks(annotation, documentWriter);
    documentWriter.write(context);

    final List<Object> elements = actor.getFieldOrFieldRefOrComponent();
    final List<Object> members = elements.stream().filter(e -> !(e instanceof StateMachineType))
        .collect(Collectors.toList());
    if (!members.isEmpty()) {
      MutableContext variableContext = contextFactory.createContext(3);
      variableContext.addKey("Variables");
      documentWriter.write(variableContext);

      final MutableDetailTable table = contextFactory.createDetailTable();
      addMemberRows(table, repository, members);
      documentWriter.write(table);
    }

    for (final Object state : elements) {
      if (state instanceof StateMachineType) {
        generateStateMachine((StateMachineType) state, documentWriter);
      }
    }
  }

  private void generateActorsAndFlows(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    final Actors actors = repository.getActors();
    if (actors != null) {
      final List<Object> actorsOrFlows = actors.getActorOrFlow();
      for (final Object actorOrFlow : actorsOrFlows) {
        if (actorOrFlow instanceof ActorType) {
          generateActor((ActorType) actorOrFlow, repository, documentWriter);
        } else if (actorOrFlow instanceof FlowType) {
          generateFlow((FlowType) actorOrFlow, repository, documentWriter);
        }
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
    generateDocumentationBlocks(annotation, documentWriter);

    final List<CodeType> codes = codeset.getCode();
    if (!codes.isEmpty()) {
    final MutableDetailTable table = contextFactory.createDetailTable();

    
    for (final CodeType code : codes) {
      final MutableDetailProperties row = table.newRow();
      final String name = code.getName();
      row.addProperty("name", name);
      row.addProperty("value", code.getValue());
      final BigInteger id = code.getId();
      if (id != null) {
        row.addProperty("id", id.toString());
      } else {
        eventLogger.warn("Unknown code id; name={0} scenario={1}", name, scenario);
      }
      addDocumentationColumns(row, code.getAnnotation(), getParagraphDelimiterInTables());
    }
    documentWriter.write(table);
    } else {
      eventLogger.warn("Codeset has no codes; name={0} scenario={1}", codeset.getName(), scenario);
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
    final String name = component.getName();
    context.addPair("Component", name);
    final String scenario = component.getScenario();
    if (!scenario.equals(DEFAULT_SCENARIO)) {
      context.addPair("scenario", scenario);
    }
    
    String abbrName = component.getAbbrName();
    if (abbrName != null) {
      context.addPair("abbrname", abbrName);
    }
    
    String category = component.getCategory();
    if (category != null) {
      context.addPair("category", category);
    }
    
    context.addKey(String.format("(%d)", component.getId().intValue()));
    documentWriter.write(context);

    final Annotation annotation = component.getAnnotation();
    generateDocumentationBlocks(annotation, documentWriter);

    final MutableDetailTable table = contextFactory.createDetailTable();
    final List<Object> members = component.getComponentRefOrGroupRefOrFieldRef();
    if (!members.isEmpty()) {
      addMemberRows(table, repository, members);
    } else {
      eventLogger.warn("Component has no members; name={0} scenario={1}", name, scenario);
    }
    documentWriter.write(table);
  }

  private void generateComponents(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    final Components componentParent = repository.getComponents();
    if (componentParent != null) {
      final List<ComponentType> components = componentParent.getComponent().stream()
          .sorted(Comparator.comparing(ComponentType::getName)).collect(Collectors.toList());
      if (!components.isEmpty()) {
        final MutableContext context = contextFactory.createContext(new String[] {"Components"}, 2);
        documentWriter.write(context);
      }
      for (final ComponentType component : components) {
        generateComponent(repository, documentWriter, component);
      }
    } else {
      logger.warn("No components found");
    }
  }

  private void generateDatatypes(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    final Datatypes datatypeParent = repository.getDatatypes();
    if (datatypeParent != null) {
      final List<Datatype> datatypes = datatypeParent.getDatatype().stream()
          .sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()))
          .collect(Collectors.toList());

      if (!datatypes.isEmpty()) {
        MutableContext context = contextFactory.createContext(2);
        context.addKey("Datatypes");
        documentWriter.write(context);
        final MutableDetailTable table = contextFactory.createDetailTable();

        for (final Datatype datatype : datatypes) {
          final List<MappedDatatype> mappings = datatype.getMappedDatatype();
          Annotation annotation = datatype.getAnnotation();
          MutableDetailProperties row = table.newRow();
          row.addProperty("name", datatype.getName());
          addDocumentationColumns(row, annotation, getParagraphDelimiterInTables());

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
            addDocumentationColumns(row, mapping.getAnnotation(), getParagraphDelimiterInTables());
          }
        }
        documentWriter.write(table);
      } else {
        logger.warn("No datatypes found");
      }
    }
  }

  private void generateDocumentationBlocks(final Annotation annotation,
      DocumentWriter documentWriter) throws IOException {
    if (annotation != null) {
      // Synopsis may explicit or implicit (null purpose)
      // handle all 'purpose' values, including appinfo

      SortedMap<String, List<Object>> sorted = groupDocumentationByPurpose(annotation);
      Set<Entry<String, List<Object>>> entries = sorted.entrySet();
      for (Entry<String, List<Object>> e : entries) {
        String text = concatenateDocumentation(e.getValue(), "\n");
        if (!text.isBlank()) {
          MutableContext documentationContext =
              contextFactory.createContext(new String[] {StringUtil.convertToTitleCase(e.getKey())}, 4);
          documentWriter.write(documentationContext);
          MutableDocumentation documentation = contextFactory.createDocumentation(text);
          documentWriter.write(documentation);
        }
      }
    }
  }

  private void generateFields(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    final Fields fieldParent = repository.getFields();
    if (fieldParent != null) {
      MutableContext context = contextFactory.createContext(2);
      context.addKey("Fields");
      documentWriter.write(context);
      final MutableDetailTable table = contextFactory.createDetailTable();

      final List<FieldType> fields = fieldParent.getField().stream()
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
        addDocumentationColumns(row, field.getAnnotation(), getParagraphDelimiterInTables());

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
        
        String minInclusive = field.getMinInclusive();
        if (minInclusive != null) {
          row.addProperty("minInclusive", minInclusive);
        }
        
        String maxInclusive = field.getMaxInclusive();
        if (maxInclusive != null) {
          row.addProperty("maxInclusive", maxInclusive);
        }
        
        String abbrName = field.getAbbrName();
        if (abbrName != null) {
          row.addProperty("abbrName", abbrName);
        }
        String baseCategoryAbbrName = field.getBaseCategoryAbbrName();
        if (baseCategoryAbbrName != null) {
          row.addProperty("baseCategoryAbbrName", baseCategoryAbbrName);
        }
        
        String baseCategory = field.getBaseCategory();
        if (baseCategory != null) {
          row.addProperty("baseCategory", baseCategory);
        }
        
        BigInteger dicriminatorId = field.getDiscriminatorId();
        if (dicriminatorId != null) {
          row.addIntProperty("dicriminatorId", dicriminatorId.intValue());
        }
        
       /* String added = field.getAdded();
        if (added != null) {
          row.addProperty("added", added);
        }
        
        BigInteger addedEp = field.getAddedEP();
        if (addedEp != null) {
          row.addIntProperty("addedEp", addedEp.intValue());
        }
        
        String deprecated = field.getDeprecated();
        if (deprecated != null) {
          row.addProperty("deprecated", deprecated);
        }
        
        BigInteger deprecatedEp = field.getDeprecatedEP();
        if (deprecatedEp != null) {
          row.addIntProperty("deprecatedEp", deprecatedEp.intValue());
        }
        
        String issue = field.getIssue();
        if (issue != null) {
          row.addProperty("issue", issue);
        }
        
        String lastModified = field.getLastModified();
        if (lastModified != null) {
          row.addProperty("lastModified", lastModified);
        }
        
        String replaced = field.getReplaced();
        if (replaced != null) {
          row.addProperty("replaced", replaced);
        }
        
        BigInteger replacedByField = field.getReplacedByField();
        if (replacedByField != null) {
          row.addIntProperty("replacedByField", replacedByField.intValue());
        }
        
        BigInteger replacedEp = field.getReplacedEP();
        if (replacedEp != null) {
          row.addIntProperty("replacedEp", replacedEp.intValue());
        }
        
        String updated = field.getUpdated();
        if (updated != null) {
          row.addProperty("updated", updated);
        }
        
        BigInteger updatedEp = field.getUpdatedEP();
        if (updatedEp != null) {
          row.addIntProperty("updatedEp", updatedEp.intValue());
        }*/
      }
      documentWriter.write(table);
    } else {
      logger.error("No fields found");
    }
  }

  private void generateFlow(FlowType flow, Repository repository, DocumentWriter documentWriter)
      throws IOException {
    MutableContext context = contextFactory.createContext(2);
    context.addPair("Flow", flow.getName());
    final Annotation annotation = flow.getAnnotation();
    generateDocumentationBlocks(annotation, documentWriter);
    documentWriter.write(context);

    final MutableDetailTable table = contextFactory.createDetailTable();
    final MutableDetailProperties row = table.newRow();
    row.addProperty("source", flow.getSource());
    row.addProperty("destination", flow.getDestination());
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
    
    String abbrName = group.getAbbrName();
    if (abbrName != null) {
      context.addPair("abbrname", abbrName);
    }
    
    String category = group.getCategory();
    if (category != null) {
      context.addPair("category", category);
    }
    
    context.addKey(String.format("(%d)", group.getId().intValue()));
    documentWriter.write(context);

    final Annotation annotation = group.getAnnotation();
    generateDocumentationBlocks(annotation, documentWriter);

    final MutableDetailTable table = contextFactory.createDetailTable();

    final FieldRefType numInGroup = group.getNumInGroup();
    if (numInGroup != null) {
      final MutableDetailProperties row = table.newRow();
      addFieldRefRow(repository, numInGroup, row);
    } else {
      eventLogger.warn("Unknown numInGroup for group; name={0} scenario={1}", name, scenario);
    }
    final List<Object> members = group.getComponentRefOrGroupRefOrFieldRef();
    if (!members.isEmpty()) {
      addMemberRows(table, repository, members);
    } else {
      eventLogger.warn("Group has no members; name={0} scenario={1}", name, scenario);
    }
    documentWriter.write(table);
  }

  private void generateGroups(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    final Groups groupParent = repository.getGroups();
    if (groupParent != null) {
      final List<GroupType> groups = groupParent.getGroup().stream()
          .sorted(Comparator.comparing(GroupType::getName)).collect(Collectors.toList());
      if (!groups.isEmpty()) {

        final MutableContext context = contextFactory.createContext(new String[] {"Groups"}, 2);
        documentWriter.write(context);
      }
      for (final GroupType group : groups) {
        generateGroup(repository, documentWriter, group);
      }
    } else {
      logger.warn("No groups found");
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
            addDocumentationColumns(row, response.getAnnotation(), getParagraphDelimiterInTables());
          }
        }
      }
      documentWriter.write(table);
    }
  }

  private void generateMessages(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    final Messages messageParent = repository.getMessages();
    if (messageParent != null) {
      final List<MessageType> messages = messageParent.getMessage().stream()
          .sorted(Comparator.comparing(MessageType::getName)).collect(Collectors.toList());
      for (final MessageType message : messages) {
        generateMessageStructure(repository, documentWriter, message);
        generateMessageResponses(repository, documentWriter, message);
      }
    } else {
      logger.error("No message found");
    }
  }

  private void generateMessageStructure(Repository repository, DocumentWriter documentWriter,
      final MessageType message) throws IOException {
    MutableContext context = contextFactory.createContext(2);
    final String name = message.getName();
    context.addPair("Message", name);
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
    
    String abbrName = message.getAbbrName();
    if (abbrName != null) {
      context.addPair("abbrname", abbrName);
    }
    
    String category = message.getCategory();
    if (category != null) {
      context.addPair("category", category);
    }
    
    context.addKey(String.format("(%d)", message.getId().intValue()));

    documentWriter.write(context);

    final Annotation annotation = message.getAnnotation();
    generateDocumentationBlocks(annotation, documentWriter);

    final MutableDetailTable table = contextFactory.createDetailTable();

    final List<Object> members = message.getStructure().getComponentRefOrGroupRefOrFieldRef();
    if (!members.isEmpty()) {
      addMemberRows(table, repository, members);
    } else {
      eventLogger.warn("Message structure has no members; name={0} scenario={1}", name, scenario);
    }
    documentWriter.write(table);
  }

  private void generateRepositoryMetadata(Repository repository, DocumentWriter documentWriter)
      throws IOException {
    MutableContext context = contextFactory.createContext(1);

    final String repositoryName = repository.getName();
    if (repositoryName != null) {
      context.addKey(repositoryName);
      if (!repositoryName.toLowerCase().contains("version")) {
        context.addKey(repository.getVersion());
      }
    } else {
      context.addKey("Repository");
    }
    documentWriter.write(context);


    final List<JAXBElement<SimpleLiteral>> elements = repository.getMetadata().getAny();
    if (!elements.isEmpty()) {
      final MutableDetailTable table = contextFactory.createDetailTable();
      for (final JAXBElement<SimpleLiteral> element : elements) {
        final MutableDetailProperties row = table.newRow();
        final String name = element.getName().getLocalPart();
        final String value = String.join(" ", element.getValue().getContent());
        row.addProperty("term", name);
        row.addProperty("value", value);
      }
      documentWriter.write(table);
    }
  }

  private void generateStateMachine(StateMachineType stateMachine, DocumentWriter documentWriter)
      throws IOException {
    MutableContext context = contextFactory.createContext(3);
    context.addPair("StateMachine", stateMachine.getName());
    documentWriter.write(context);

    final Annotation annotation = stateMachine.getAnnotation();
    generateDocumentationBlocks(annotation, documentWriter);

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
      addDocumentationColumns(row, transition.getAnnotation(), getParagraphDelimiterInTables());
      row.addProperty("when", transition.getWhen());
    }
  }

  private String getParagraphDelimiterInTables() {
    return paragraphDelimiterInTables;
  }

  private SortedMap<String, List<Object>> groupDocumentationByPurpose(Annotation annotation) {
    if (annotation == null) {
      return Collections.emptySortedMap();
    } else {
      final List<Object> annotations = annotation.getDocumentationOrAppinfo();
      Function<Object, String> classifier = o -> {
        if (o instanceof Documentation) {
          final String purpose = ((Documentation) o).getPurpose();
          if (purpose != null) {
            return purpose;
          } else {
            return DOCUMENTATION_KEYWORD;
          }
        } else {
          final String purpose = ((Appinfo) o).getPurpose();
          if (purpose != null) {
            return purpose;
          } else {
            return DOCUMENTATION_KEYWORD;
          }
        }
      };
      return sortDocumentationByPurpose(
          annotations.stream().collect(Collectors.groupingBy(classifier)));
    }
  }

  private SortedMap<String, List<Object>> sortDocumentationByPurpose(
      Map<String, List<Object>> groups) {
    SortedMap<String, List<Object>> sorted =
        new TreeMap<String, List<Object>>(new Comparator<>() {

          private int purposeRank(String purpose) {
            switch (purpose.toLowerCase()) {
              case "synopsis":
              case DOCUMENTATION_KEYWORD:
                return 0;
              case "elaboration":
                return 1;
              case "example":
                return 2;
              case "display":
                return 3;
              default:
                return 4;
            }
          }

          @Override
          public int compare(String o1, String o2) {
            return purposeRank(o1) - purposeRank(o2);
          }

        });
    sorted.putAll(groups);
    return sorted;
  }


}
