/*
 * Copyright 2020 FIX Protocol Ltd.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.fixprotocol._2020.orchestra.repository.ActorType;
import io.fixprotocol._2020.orchestra.repository.Annotation;
import io.fixprotocol._2020.orchestra.repository.CodeSetType;
import io.fixprotocol._2020.orchestra.repository.CodeType;
import io.fixprotocol._2020.orchestra.repository.ComponentRefType;
import io.fixprotocol._2020.orchestra.repository.ComponentRuleType;
import io.fixprotocol._2020.orchestra.repository.ComponentType;
import io.fixprotocol._2020.orchestra.repository.FieldRefType;
import io.fixprotocol._2020.orchestra.repository.FieldRuleType;
import io.fixprotocol._2020.orchestra.repository.FieldType;
import io.fixprotocol._2020.orchestra.repository.FlowType;
import io.fixprotocol._2020.orchestra.repository.GroupRefType;
import io.fixprotocol._2020.orchestra.repository.GroupType;
import io.fixprotocol._2020.orchestra.repository.MappedDatatype;
import io.fixprotocol._2020.orchestra.repository.MessageRefType;
import io.fixprotocol._2020.orchestra.repository.MessageType;
import io.fixprotocol._2020.orchestra.repository.PresenceT;
import io.fixprotocol._2020.orchestra.repository.PurposeEnum;
import io.fixprotocol._2020.orchestra.repository.Repository;
import io.fixprotocol._2020.orchestra.repository.ResponseType;
import io.fixprotocol._2020.orchestra.repository.StateMachineType;
import io.fixprotocol._2020.orchestra.repository.StateType;
import io.fixprotocol._2020.orchestra.repository.TransitionType;
import io.fixprotocol._2020.orchestra.repository.UnionDataTypeT;
import io.fixprotocol.md.event.Context;
import io.fixprotocol.md.event.Contextual;
import io.fixprotocol.md.event.Detail;
import io.fixprotocol.md.event.DetailProperties;
import io.fixprotocol.md.event.DetailTable;
import io.fixprotocol.md.event.DocumentParser;
import io.fixprotocol.md.event.Documentation;
import io.fixprotocol.md.util.AssociativeSet;
import io.fixprotocol.orchestra.event.EventListener;
import io.fixprotocol.orchestra.event.EventListenerFactory;
import io.fixprotocol.orchestra.event.TeeEventListener;
import io.fixprotocol.md2orchestra.util.IdGenerator;

public class RepositoryBuilder {
  private class ComponentBuilder implements ElementBuilder {

    private final String name;
    private final String scenario;

    public ComponentBuilder(String name, String scenario) {
      this.name = name;
      this.scenario = scenario;
    }

    @Override
    public void build() {
      ComponentType componentType = repositoryAdapter.findComponentByName(name, scenario);
      if (componentType == null && referenceRepositoryAdapter != null) {
        componentType = referenceRepositoryAdapter.findComponentByName(name, scenario);
        if (componentType != null) {
          repositoryAdapter.copyComponent(componentType);
        } else {
          eventLogger.error("Unknown component; name={0} scenario={1}", name, scenario);
        }
      }
    }
  }

  /**
   *
   * Populates the ID of a ComponentRef when the component name is known
   */
  private class ComponentRefBuilder implements ElementBuilder {

    private final ComponentRefType componentRef;
    private final String name;

    public ComponentRefBuilder(String name, ComponentRefType componentRef) {
      this.name = name;
      this.componentRef = componentRef;
    }

    @Override
    public void build() {
      final ComponentType componentType =
          repositoryAdapter.findComponentByName(name, componentRef.getScenario());
      if (componentType != null) {
        componentRef.setId(componentType.getId());
      } else {
        eventLogger.error("Unknown componentRef ID; name={0} scenario={1}", name,
            componentRef.getScenario());
      }
    }
  }

  private interface ElementBuilder {
    void build();
  }

  private class FieldBuilder implements ElementBuilder {
    private final String name;
    private final String scenario;
    private final int tag;
    private final String type;

    public FieldBuilder(int tag, String name, String scenario, String type) {
      this.tag = tag;
      this.name = name;
      this.scenario = scenario;
      this.type = type;
    }

    @Override
    public void build() {
      FieldType fieldType = null;
      if (tag > 0) {
        fieldType = repositoryAdapter.findFieldByTag(tag, scenario);
      } else if (name != null) {
        fieldType = repositoryAdapter.findFieldByName(name, scenario);
      }

      if ((fieldType == null || fieldType.getType() == null)
          && referenceRepositoryAdapter != null) {
        if (tag > 0) {
          fieldType = referenceRepositoryAdapter.findFieldByTag(tag, scenario);
        } else if (name != null) {
          fieldType = referenceRepositoryAdapter.findFieldByName(name, scenario);
        }
        if (fieldType != null) {
          repositoryAdapter.copyField(fieldType);
        }
      }

      // if not found retry with base scenario
      if (fieldType == null && !DEFAULT_SCENARIO.equals(scenario)
          && referenceRepositoryAdapter != null) {
        FieldType baseFieldType = null;
        if (tag > 0) {
          baseFieldType = referenceRepositoryAdapter.findFieldByTag(tag, DEFAULT_SCENARIO);
        } else {
          baseFieldType = referenceRepositoryAdapter.findFieldByName(name, DEFAULT_SCENARIO);
        }
        if (baseFieldType != null) {
          fieldType = new FieldType();
          fieldType.setId(baseFieldType.getId());
          fieldType.setName(baseFieldType.getName());
          fieldType.setType(baseFieldType.getType());
          if (!DEFAULT_SCENARIO.equals(scenario)) {
            fieldType.setScenario(scenario);
          }
          repositoryAdapter.addField(fieldType);
        }
      }
      


      if (fieldType != null) {
        if (fieldType.getType() != null) {
          buildSteps.add(new TypeBuilder(fieldType.getType(), scenario));
        }
      } else {
        fieldType = new FieldType();
        fieldType.setName(Objects.requireNonNullElseGet(name, () -> "Field" + tag));
        if (!DEFAULT_SCENARIO.equals(scenario)) {
          fieldType.setScenario(scenario);
        }
        if (tag > 0) {
          fieldType.setId(BigInteger.valueOf(tag));
        } else {
          fieldType.setId(BigInteger.ZERO);
          eventLogger.error("Unknown field ID; name={0} scenario={1}", name, scenario);
        }
        if (type == null) {
          eventLogger.error("Unknown type for field; id={0, number, #0} name={1} scenario={2}",
              fieldType.getId(), fieldType.getName(), scenario);
        }
        repositoryAdapter.addField(fieldType);
      }
    }
  }


  /**
   *
   * Populates the ID of a FieldRef when the field name is known
   */
  private class FieldRefBuilder implements ElementBuilder {

    private final FieldRefType fieldRef;
    private final String name;

    public FieldRefBuilder(String name, FieldRefType fieldRef) {
      this.name = name;
      this.fieldRef = fieldRef;
    }

    @Override
    public void build() {
      final String scenario = fieldRef.getScenario();
      FieldType fieldType = repositoryAdapter.findFieldByName(name, scenario);
      // if not found retry with base scenario
      if (fieldType == null && !DEFAULT_SCENARIO.equals(scenario)) {
        fieldType = repositoryAdapter.findFieldByName(name, DEFAULT_SCENARIO);
      }
      if (fieldType != null) {
        fieldRef.setId(fieldType.getId());
      } else {
        eventLogger.error("Unknown fieldRef ID; name={0} scenario={1}", name, scenario);
      }
    }
  }

  private class GroupBuilder implements ElementBuilder {

    private final String name;
    private final String scenario;

    public GroupBuilder(String name, String scenario) {
      this.name = name;
      this.scenario = scenario;
    }

    @Override
    public void build() {
      GroupType groupType = repositoryAdapter.findGroupByName(name, scenario);
      if (groupType == null && referenceRepositoryAdapter != null) {
        groupType = referenceRepositoryAdapter.findGroupByName(name, scenario);
        if (groupType != null) {
          repositoryAdapter.copyGroup(groupType);
        } else {
          eventLogger.error("Unknown group; name={0} scenario={1}", name, scenario);
        }
      }
    }
  }
  /**
   *
   * Populates the ID of a GroupRef when the group name is known
   */
  private class GroupRefBuilder implements ElementBuilder {

    private final GroupRefType groupRef;
    private final String name;

    public GroupRefBuilder(String name, GroupRefType groupRef) {
      this.name = name;
      this.groupRef = groupRef;
    }

    @Override
    public void build() {
      final GroupType groupType = repositoryAdapter.findGroupByName(name, groupRef.getScenario());
      if (groupType != null) {
        groupRef.setId(groupType.getId());
      } else {
        eventLogger.error("Unknown groupRef ID; name={0} scenario={1}", name,
            groupRef.getScenario());
      }
    }
  }
  private class TypeBuilder implements ElementBuilder {
    final String scenario;
    final String type;

    public TypeBuilder(String type, String scenario) {
      this.type = type;
      this.scenario = scenario;
    }

    @Override
    public void build() {
      boolean found = false;
      io.fixprotocol._2020.orchestra.repository.Datatype datatype =
          repositoryAdapter.findDatatypeByName(type);
      if (datatype != null) {
        found = true;
      } else if (referenceRepositoryAdapter != null) {
        datatype = referenceRepositoryAdapter.findDatatypeByName(type);
        if (datatype != null) {
          repositoryAdapter.copyDatatype(datatype);
          found = true;
        }
      }
      if (!found) {
        CodeSetType codeset = repositoryAdapter.findCodesetByName(type, scenario);
        if (codeset != null) {
          found = true;
        } else if (referenceRepositoryAdapter != null) {
          codeset = referenceRepositoryAdapter.findCodesetByName(type, scenario);
          if (codeset != null) {
            repositoryAdapter.copyCodeset(codeset);
            found = true;
          }
        }
      }
      if (!found) {
        // if not found as a datatype or codeset in either current or referenceRepositoryAdapter
        // repository, then assume its a datatype name
        datatype = new io.fixprotocol._2020.orchestra.repository.Datatype();
        datatype.setName(type);
        repositoryAdapter.addDatatype(datatype);
        eventLogger.info("Datatype added; name={0}", datatype.getName());
      }
    }
  }

  public static final String ABBRNAME_KEYWORD = "abbrname";
  public static final String ACTOR_KEYWORD = "actor";
  public static final String ASSIGN_KEYWORD = "assign";
  public static final String CATEGORY_KEYWORD = "category";
  public static final String CODESET_KEYWORD = "codeset";
  public static final String COMPONENT_KEYWORD = "component";

  public static final String DATATYPES_KEYWORD = "datatypes";
  /**
   * Default token to represent a paragraph break in tables (not natively supported by markdown)
   */
  public static final String DEFAULT_PARAGRAPH_DELIMITER = "/P/";
  public static final String DESCRIPTION_KEYWORD = "description";
  public static final String DOCUMENTATION_KEYWORD = "documentation";
  public static final String FIELDS_KEYWORD = "fields";
  public static final String FLOW_KEYWORD = "flow";
  public static final String GROUP_KEYWORD = "group";
  public static final String MESSAGE_KEYWORD = "message";
  public static final String RESPONSES_KEYWORD = "responses";
  public static final String SCENARIO_KEYWORD = "scenario";
  public static final String STATEMACHINE_KEYWORD = "statemachine";
  public static final String VARIABLES_KEYWORD = "variables";
  public static final String WHEN_KEYWORD = "when";

  // the form code=name with optional space before and after =
  private static final Pattern codePattern = Pattern.compile("(\\S+) *= *([^ \"]+|\".+\")");

  private static final String DEFAULT_CODE_TYPE = "char";
  private static final String DEFAULT_SCENARIO = "base";

  private static final int KEY_POSITION = 0;
  private static final int NAME_POSITION = 1;

  /**
   * Create an instance of RepositoryBuilder
   *
   * @param referenceStream an InputStream from an Orchestra file used as a reference. May be
   *        {@code null}.
   * @param jsonOutputStream output stream with JSON errors or warnings
   * @return an instance of RepositoryBuilder
   * @throws Exception if streams cannot be read or written, or a reference cannot be parsed
   */
  public static RepositoryBuilder instance(InputStream referenceStream,
      OutputStream jsonOutputStream) throws Exception {
    return instance(referenceStream, jsonOutputStream, DEFAULT_PARAGRAPH_DELIMITER);
  }

  /**
   * Create an instance of RepositoryBuilder
   *
   * @param referenceStream an InputStream from an Orchestra file used as a reference. May be
   *        {@code null}.
   * @param jsonOutputStream output stream with JSON errors or warnings
   * @param paragraphDelimiterInTables token to represent a paragraph break in markdown tables
   * @return an instance of RepositoryBuilder
   * @throws Exception if streams cannot be read or written, or a reference cannot be parsed
   */
  public static RepositoryBuilder instance(InputStream referenceStream,
      OutputStream jsonOutputStream, String paragraphDelimiterInTables) throws Exception {
    final RepositoryBuilder outputRepositoryBuilder =
        new RepositoryBuilder(jsonOutputStream, paragraphDelimiterInTables);

    if (referenceStream != null) {
      final RepositoryAdapter referenceRepository =
          new RepositoryAdapter(outputRepositoryBuilder.eventLogger);
      referenceRepository.unmarshal(referenceStream);
      outputRepositoryBuilder.setReference(referenceRepository);
    }
    return outputRepositoryBuilder;
  }

  static TeeEventListener createEventListener(Logger logger, OutputStream jsonOutputStream)
      throws Exception {
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
    return eventLogger;
  }

  private final Queue<ElementBuilder> buildSteps = new LinkedList<>();

  private final String[] contextKeys =
      new String[] {ACTOR_KEYWORD, CODESET_KEYWORD, COMPONENT_KEYWORD, DATATYPES_KEYWORD,
          FIELDS_KEYWORD, FLOW_KEYWORD, GROUP_KEYWORD, MESSAGE_KEYWORD, STATEMACHINE_KEYWORD};
  private TeeEventListener eventLogger;
  private final AssociativeSet headings = new AssociativeSet();
  private final IdGenerator idGenerator = new IdGenerator(5000, 39999);
  private final Logger logger = LogManager.getLogger(getClass());

  private final Consumer<Contextual> markdownConsumer = contextual -> {
    final Context keyContext = getKeyContext(contextual);
    if (keyContext == null) {
      eventLogger.warn("Element with unknown context; perhaps missing heading");
      return;
    }
    final String type = keyContext.getKey(KEY_POSITION);
    if (type == null) {
      eventLogger.warn("RepositoryBuilder received element with unknown context of class {0}",
          contextual.getClass());
    } else
      switch (type.toLowerCase()) {
        case ACTOR_KEYWORD:
          addActor(contextual, keyContext);
          break;
        case CODESET_KEYWORD:
          addCodeset(contextual, keyContext);
          break;
        case COMPONENT_KEYWORD:
          addComponent(contextual, keyContext);
          break;
        case DATATYPES_KEYWORD:
          addDatatype(contextual, keyContext);
          break;
        case FIELDS_KEYWORD:
          addField(contextual, keyContext);
          break;
        case FLOW_KEYWORD:
          addFlow(contextual, keyContext);
          break;
        case GROUP_KEYWORD:
          addGroup(contextual, keyContext);
          break;
        case MESSAGE_KEYWORD:
          addMessage(contextual, keyContext);
          break;
        case RESPONSES_KEYWORD:
          addMessageResponses(contextual, keyContext);
          break;
        case STATEMACHINE_KEYWORD:
          addActorStates(contextual, keyContext);
          break;
        case VARIABLES_KEYWORD:
          addActorVariables(contextual, keyContext);
          break;
        default:
          if (keyContext.getLevel() == 1) {
            addMetadata(contextual, keyContext);
          } else {
            eventLogger.warn("RepositoryBuilder received unknown context type {}", type);
          }
      }
  };
  private final String paragraphDelimiterInTables;
  private RepositoryAdapter referenceRepositoryAdapter = null;
  private RepositoryAdapter repositoryAdapter = null;

  private final RepositoryTextUtil textUtil = new RepositoryTextUtil();

  RepositoryBuilder(OutputStream jsonOutputStream) throws Exception {
    this(jsonOutputStream, DEFAULT_PARAGRAPH_DELIMITER);
  }

  RepositoryBuilder(OutputStream jsonOutputStream, String paragraphDelimiterInTables)
      throws Exception {
    this.paragraphDelimiterInTables = paragraphDelimiterInTables;
    this.eventLogger = createEventListener(this.logger, jsonOutputStream);
    this.repositoryAdapter = new RepositoryAdapter(this.eventLogger);
    this.repositoryAdapter.createRepository();
    // Populate column heading translations. First element is lower case key, second is display
    // format.
    this.headings.addAll(new String[][] {{"abbrname", "XMLName"},
        {"basecategoryabbrname", "Category XMLName"}, {"basecategory", "Category"},
        {"discriminatorid", "Discriminator"}, {"addedep", "Added EP"}, {"updatedep", "Updated EP"},
        {"deprecatedep", "Deprecated EP"}, {"uniondatatype", "Union Type"}});
  }

  /**
   * Append input to a repository
   *
   * @param inputStream an markdown file input
   * @throws IOException if an IO error occurs
   */
  public void appendInput(InputStream inputStream) throws IOException {
    final DocumentParser parser = new DocumentParser();
    parser.parse(inputStream, markdownConsumer);
  }

  /**
   * Finalize the repository and write it
   *
   * @param outputStream a stream to write repository to
   * @throws Exception if output fails to be written
   */
  public void write(OutputStream outputStream) throws Exception {
    executeDefferedBuildSteps();
    repositoryAdapter.marshal(outputStream);
    closeEventLogger();
  }

  void closeEventLogger() throws Exception {
    eventLogger.close();
  }

  void copyMembers(List<Object> members) {
    for (final Object member : members) {
      if (member instanceof FieldRefType) {
        final FieldRefType fieldRef = (FieldRefType) member;
        FieldType field =
            repositoryAdapter.findFieldByTag(fieldRef.getId().intValue(), fieldRef.getScenario());
        if (field == null) {
          field = referenceRepositoryAdapter.findFieldByTag(fieldRef.getId().intValue(),
              fieldRef.getScenario());
          if (field != null) {
            addFieldAndType(field);
          } else {
            eventLogger.error("Unknown field; lastId={0, number, ##0} scenario={1}",
                fieldRef.getId().intValue(), fieldRef.getScenario());
          }
        }
      } else if (member instanceof GroupRefType) {
        final GroupRefType groupRef = (GroupRefType) member;
        GroupType group =
            repositoryAdapter.findGroupByTag(groupRef.getId().intValue(), groupRef.getScenario());
        if (group == null) {
          group = referenceRepositoryAdapter.findGroupByTag(groupRef.getId().intValue(),
              groupRef.getScenario());
          if (group != null) {
            group = repositoryAdapter.copyGroup(group);
            final List<Object> groupMembers = group.getComponentRefOrGroupRefOrFieldRef();
            copyMembers(groupMembers);
          } else {
            eventLogger.error("Unknown group; lastId={0} scenario={1}", groupRef.getId().intValue(),
                groupRef.getScenario());
          }
        }
      } else if (member instanceof ComponentRefType) {
        final ComponentRefType componentRef = (ComponentRefType) member;
        ComponentType component = repositoryAdapter
            .findComponentByTag(componentRef.getId().intValue(), componentRef.getScenario());
        if (component == null) {
          component = referenceRepositoryAdapter.findComponentByTag(componentRef.getId().intValue(),
              componentRef.getScenario());
          if (component != null) {
            component = repositoryAdapter.copyComponent(component);
            final List<Object> componentMembers = component.getComponentRefOrGroupRefOrFieldRef();
            copyMembers(componentMembers);
          } else {
            eventLogger.error("Unknown component; lastId={0} scenario={1}",
                componentRef.getId().intValue(), componentRef.getScenario());
          }
        }
      }
    }
  }

  void setReference(RepositoryAdapter reference) {
    this.referenceRepositoryAdapter = reference;
  }

  private void addActor(Contextual contextual, Context context) {
    final String name = context.getKey(NAME_POSITION);
    if (contextual instanceof Documentation) {
      final Documentation documentation = (Documentation) contextual;
      final ActorType actor = repositoryAdapter.findActorByName(name);
      if (actor != null) {
        Annotation annotation = actor.getAnnotation();
        if (annotation == null) {
          annotation = new Annotation();
          actor.setAnnotation(annotation);
        }
        final String parentKey = contextual.getParent().getKey(KEY_POSITION);
        repositoryAdapter.addDocumentation(documentation.getDocumentation(), 
            ACTOR_KEYWORD.equalsIgnoreCase(parentKey) ? null : getPurpose(parentKey),
            annotation);
      }
    } // make sure it's not a lower level heading
    else if (contextual instanceof Context
        && ACTOR_KEYWORD.equalsIgnoreCase(((Context) contextual).getKey(KEY_POSITION))) {
      final ActorType actor = new ActorType();
      actor.setName(name);

      repositoryAdapter.addActor(actor);
    }
  }

  private void addActorStates(Contextual contextual, Context context) {
    if (contextual instanceof DetailTable) {
      final Context actorContext = context.getParent();
      if (actorContext != null) {
        final String actorName = actorContext.getKeyValue(ACTOR_KEYWORD);
        final ActorType actor = repositoryAdapter.findActorByName(actorName);
        if (actor != null) {
          final String name = actorContext.getKeyValue(STATEMACHINE_KEYWORD);
          final StateMachineType statemachine = new StateMachineType();
          statemachine.setName(name);
          final List<Object> actorMembers = actor.getFieldOrFieldRefOrComponent();
          actorMembers.add(statemachine);

          final DetailTable detailTable = (DetailTable) contextual;

          final List<String> sources = detailTable.rows().stream().map(r -> r.getProperty("state"))
              .distinct().collect(Collectors.toList());
          final List<String> targets = detailTable.rows().stream().map(r -> r.getProperty("target"))
              .distinct().collect(Collectors.toList());
          // preserves insertion order
          final Set<String> allStates = new LinkedHashSet<>(sources);
          allStates.addAll(targets);
          // candidates for initial state are not the target of any transition
          final List<String> possibleInitial =
              sources.stream().filter(s -> !targets.contains(s)).collect(Collectors.toList());
          // assume initial state is listed first in case more than one not a target of transitions
          final String initialStateName =
              (!possibleInitial.isEmpty()) ? possibleInitial.get(0) : null;

          final List<StateType> states = statemachine.getState();

          for (final String stateName : allStates) {
            final StateType stateType = new StateType();
            stateType.setName(stateName);
            if (stateName.equals(initialStateName)) {
              statemachine.setInitial(stateType);
            } else {
              states.add(stateType);
            }
          }

          try {
            detailTable.rows().forEach(r -> {
              final TransitionType transition = new TransitionType();
              final Annotation annotation = new Annotation();

              String sourceStateName = null;
              for (final Entry<String, String> p : r.getProperties()) {
                String key = headings.getSecondOrDefault(p.getKey(), p.getKey().toLowerCase());
                switch (key) {
                  case "state":
                    sourceStateName = p.getValue();
                    break;
                  case "transition":
                    transition.setName(p.getValue());
                    break;
                  case "when":
                    transition.setWhen(p.getValue());
                    break;
                  case "target":
                    transition.setTarget(p.getValue());
                    break;
                  default:
                    if (isDocumentationKey(p.getKey())) {
                      repositoryAdapter.addDocumentation(p.getValue(), paragraphDelimiterInTables,
                          getPurpose(p.getKey()), annotation);
                      transition.setAnnotation(annotation);
                    } else {
                      repositoryAdapter.addAppinfo(p.getValue(), paragraphDelimiterInTables,
                          p.getKey(), annotation);
                      transition.setAnnotation(annotation);
                    }
                }
              }

              StateType sourceState = null;
              if (Objects.requireNonNull(sourceStateName).equals(initialStateName)) {
                sourceState = statemachine.getInitial();
              } else {
                for (final StateType state : statemachine.getState()) {
                  if (state.getName().equals(sourceStateName)) {
                    sourceState = state;
                    break;
                  }
                }
              }
              Objects.requireNonNull(sourceState).getTransition().add(transition);
            });
          } catch (final NoSuchElementException e) {
            eventLogger.warn("No states defined for state machine; name={0}", name);
          }
        } else {
          eventLogger.error("Unknown actor for state machine; name={0}", actorName);
        }
      }
    }
  }

  private void addActorVariables(Contextual contextual, Context context) {
    if (contextual instanceof DetailTable) {
      final String name = context.getKey(NAME_POSITION);
      final ActorType actor = repositoryAdapter.findActorByName(name);
      if (actor != null) {
        final DetailTable detailTable = (DetailTable) contextual;
        final List<Object> members = actor.getFieldOrFieldRefOrComponent();
        addMembers(detailTable.rows(), members);
      } else {
        eventLogger.error("Unknown actor for variables; name={0}", name);
      }
    }
  }

  private void addCode(final DetailProperties detail, List<? super CodeType> codes,
      CodeSetType codeset) {
    final CodeType codeType = new CodeType();

    String name = "Unknown";
    for (final Entry<String, String> p : detail.getProperties()) {
      String key = headings.getSecondOrDefault(p.getKey(), p.getKey().toLowerCase());
      
      switch (key) {
        case "name":
          name = textUtil.stripName(p.getValue());
          codeType.setName(name);
          break;
        case "value":
          codeType.setValue(p.getValue());
          break;
        case "tag":
        case "id":
          codeType.setId(BigInteger.valueOf(textUtil.tagToInt(p.getValue())));
          break;
        case "sort":
          codeType.setSort(p.getValue());
          break;
        case "group":
          codeType.setGroup(p.getValue());
          break;
        case "abbrname":
          codeType.setAbbrName(p.getValue());
          break;
        case "added":
          codeType.setAdded(p.getValue());
          break;
        case "addedep":
          codeType.setAddedEP(new BigInteger(p.getValue()));
          break;
        case "deprecated":
          codeType.setDeprecated(p.getValue());
          break;
        case "deprecatedep":
          codeType.setDeprecatedEP(new BigInteger(p.getValue()));
          break;
        case "issue":
          codeType.setIssue(p.getValue());
          break;
        case "lastmodified":
          codeType.setLastModified(p.getValue());
          break;
        case "replaced":
          codeType.setReplaced(p.getValue());
          break;
        case "replacedbyfield":
          codeType.setReplacedByField(new BigInteger(p.getValue()));
          break;
        case "replacedep":
          codeType.setReplacedEP(new BigInteger(p.getValue()));
          break;
        case "updated":
          codeType.setUpdated(p.getValue());
          break;
        case "updatedep":
          codeType.setUpdatedEP(new BigInteger(p.getValue()));
          break;
        default:
          Annotation annotation = codeType.getAnnotation();
          if (annotation == null) {
            annotation = new Annotation();
          }

          if (isDocumentationKey(p.getKey())) {
            repositoryAdapter.addDocumentation(p.getValue(), paragraphDelimiterInTables,
                getPurpose(p.getKey()), annotation);
            codeType.setAnnotation(annotation);
          } else {
            repositoryAdapter.addAppinfo(p.getValue(), paragraphDelimiterInTables, p.getKey(),
                annotation);
            codeType.setAnnotation(annotation);
          }
      }
    }

    if (codeType.getId() == null) {
      codeType.setId(BigInteger.valueOf(assignId(codeset.getName(), name)));
    }

    if (codeType.getName() == null) {
      eventLogger.error("Missing name for code in codeset {0}; value={1}", codeset.getName(),
          Objects.requireNonNullElse(codeType.getValue(), "Unknown"));
    }

    if (codeType.getValue() == null) {
      eventLogger.error("Missing value for code in codeset {0}; name={1}", codeset.getName(),
          Objects.requireNonNullElse(codeType.getName(), "Unknown"));
    }

    codes.add(codeType);
  }

  private void addCodeset(Contextual contextual, Context context) {
    final String name = context.getKey(NAME_POSITION);
    final String scenario = scenarioOrDefault(context.getKeyValue(SCENARIO_KEYWORD));

    if (contextual instanceof DetailTable) {
      final DetailTable detailTable = (DetailTable) contextual;
      CodeSetType codeset = repositoryAdapter.findCodesetByName(name, scenario);

      if (codeset != null && !codeset.getCode().isEmpty()) {
        eventLogger.error("Duplicate definitions of codeset {0} scenario {1}", name, scenario);
        String codesetScenario = scenario + "Dup";
        CodeSetType dupCodeset = new CodeSetType();
        dupCodeset.setName(name);
        dupCodeset.setScenario(codesetScenario);
        dupCodeset.setType(codeset.getType());
        dupCodeset.setId(BigInteger.valueOf(assignId(name, codesetScenario)));
        repositoryAdapter.addCodeset(dupCodeset);
        codeset = dupCodeset;
      }
      final List<CodeType> codes = codeset.getCode();
      for (DetailProperties detail : detailTable.rows()) {
        addCode(detail, codes, codeset);
      }
    } else if (contextual instanceof Documentation) {
      final Documentation documentation = (Documentation) contextual;
      final CodeSetType codeset = repositoryAdapter.findCodesetByName(name, scenario);
      if (codeset != null) {
        Annotation annotation = codeset.getAnnotation();
        if (annotation == null) {
          annotation = new Annotation();
          codeset.setAnnotation(annotation);
        }
        final String parentKey = contextual.getParent().getKey(KEY_POSITION);
        repositoryAdapter.addDocumentation(documentation.getDocumentation(), 
            CODESET_KEYWORD.equalsIgnoreCase(parentKey) ? null : getPurpose(parentKey),
            annotation);
      }
    } // make sure it's not a lower level heading
    else if (contextual instanceof Context
        && CODESET_KEYWORD.equalsIgnoreCase(((Context) contextual).getKey(KEY_POSITION))) {
      CodeSetType codeset = repositoryAdapter.findCodesetByName(name, scenario);
      if (codeset != null) {
        eventLogger.error("Duplicate definitions of codeset {0} scenario {1}", name, scenario);
      } else if (referenceRepositoryAdapter != null) {
        CodeSetType refCodeset = referenceRepositoryAdapter.findCodesetByName(name, scenario);
        if (refCodeset != null) {
          // Copy all codeset attributes but without codes
          codeset = repositoryAdapter.copyCodeset(refCodeset);
          codeset.getCode().clear();
        }
      }
      if (codeset == null) {
        codeset = new CodeSetType();
        int tag = textUtil.getTag(context.getKeys());
        
        if (tag == -1) {
          tag = assignId(name, scenario);
        }
        codeset.setId(BigInteger.valueOf(tag));

        codeset.setName(name);
        if (!DEFAULT_SCENARIO.equals(scenario)) {
          codeset.setScenario(scenario);
        }

        String type = context.getKeyValue("type");
        if (type != null) {
          codeset.setType(type);
        } else {
          eventLogger.error("Unknown CodeSet underlying datatype; name={0}", name);
        }
        repositoryAdapter.addCodeset(codeset);
      }
    }
  }

  private void addComponent(Contextual contextual, Context context) {
    final String name = context.getKey(NAME_POSITION);
    final String scenario = scenarioOrDefault(context.getKeyValue(SCENARIO_KEYWORD));

    if (contextual instanceof DetailTable) {
      final DetailTable detailTable = (DetailTable) contextual;
      final ComponentType component = repositoryAdapter.findComponentByName(name, scenario);
      final List<Object> members = component.getComponentRefOrGroupRefOrFieldRef();
      addMembers(detailTable.rows(), members);
    } else if (contextual instanceof Documentation) {
      final Documentation detail = (Documentation) contextual;
      final ComponentType component = repositoryAdapter.findComponentByName(name, scenario);
      if (component != null) {
        Annotation annotation = component.getAnnotation();
        if (annotation == null) {
          annotation = new Annotation();
          component.setAnnotation(annotation);
        }
        final String parentKey = contextual.getParent().getKey(KEY_POSITION);
        repositoryAdapter.addDocumentation(detail.getDocumentation(), 
            COMPONENT_KEYWORD.equalsIgnoreCase(parentKey) ? null : getPurpose(parentKey),
            annotation);
      }
    } // make sure it's not a lower level heading
    else if (contextual instanceof Context
        && COMPONENT_KEYWORD.equalsIgnoreCase(((Context) contextual).getKey(KEY_POSITION))) {
      int tag = textUtil.getTag(context.getKeys());
      final ComponentType component = new ComponentType();

      ComponentType refComponent = null;
      if (referenceRepositoryAdapter != null) {
        refComponent = referenceRepositoryAdapter.findComponentByName(name, scenario);
      }
      if (refComponent != null) {
        tag = refComponent.getId().intValue();
      }
      if (tag == -1) {
        tag = assignId(name, scenario);
      }
      component.setId(BigInteger.valueOf(tag));
      component.setName(name);
      if (!DEFAULT_SCENARIO.equals(scenario)) {
        component.setScenario(scenario);
      }

      final String abbrName = context.getKeyValue(ABBRNAME_KEYWORD);
      if (abbrName != null) {
        component.setAbbrName(abbrName);
      }

      final String category = context.getKeyValue(CATEGORY_KEYWORD);
      if (category != null) {
        component.setCategory(category);
      }

      repositoryAdapter.addComponent(component);
    }
  }

  private void addDatatype(Contextual contextual, Context context) {
    if (contextual instanceof Detail) {
      final Detail detail = (Detail) contextual;
      final String name = detail.getProperty("name");
      if (name != null) {
        io.fixprotocol._2020.orchestra.repository.Datatype datatype =
            repositoryAdapter.findDatatypeByName(name);
        if (datatype == null) {
          datatype = new io.fixprotocol._2020.orchestra.repository.Datatype();
          datatype.setName(name);
          repositoryAdapter.addDatatype(datatype);
          final String markdown = detail.getProperty(DOCUMENTATION_KEYWORD);
          if (markdown != null && !markdown.isEmpty()) {
            Annotation annotation = datatype.getAnnotation();
            if (annotation == null) {
              annotation = new Annotation();
              datatype.setAnnotation(annotation);
            }
            repositoryAdapter.addDocumentation(markdown, null, annotation);
          }
        }
        final List<MappedDatatype> mappings = datatype.getMappedDatatype();
        final String standard = detail.getProperty("standard");
        if (standard != null && !standard.isEmpty()) {
          addDatatypeMapping(detail, standard, mappings);
        }
      } else {
        eventLogger.error("Unknown name for datatype");
      }
    }
  }

  private void addDatatypeMapping(final Detail detail, final String standard,
      final List<? super MappedDatatype> mappings) {
    final MappedDatatype mapping = new MappedDatatype();
    final Annotation annotation = new Annotation();
    mapping.setStandard(standard);

    for (final Entry<String, String> p : detail.getProperties()) {
      String key = headings.getSecondOrDefault(p.getKey(), p.getKey().toLowerCase());
      switch (key) {
        case "standard":
          mapping.setStandard(p.getValue());
          break;
        case "base":
          mapping.setBase(p.getValue());
          break;
        case "builtin":
          mapping.setBuiltin(Boolean.valueOf(p.getValue()));
          break;
        case "element":
          mapping.setElement(p.getValue());
          break;
        case "parameter":
          mapping.setParameter(p.getValue());
          break;
        case "pattern":
          mapping.setPattern(p.getValue());
          break;
        case "mininclusive":
          mapping.setMinInclusive(p.getValue());
          break;
        case "maxinclusive":
          mapping.setMaxInclusive(p.getValue());
          break;
        case "name":
          // an attribute of parent datatype
          break;
        default:
          if (isDocumentationKey(p.getKey())) {
            repositoryAdapter.addDocumentation(p.getValue(), paragraphDelimiterInTables,
                getPurpose(p.getKey()), annotation);
            mapping.setAnnotation(annotation);
          } else {
            repositoryAdapter.addAppinfo(p.getValue(), paragraphDelimiterInTables, p.getKey(),
                annotation);
            mapping.setAnnotation(annotation);
          }
      }
    }
    mappings.add(mapping);
  }

  private void addField(Contextual contextual, Context context) {
    if (contextual instanceof Detail) {
      final Detail detail = (Detail) contextual;
      final FieldType field = new FieldType();
      final Annotation annotation = new Annotation();
      for (final Entry<String, String> p : detail.getProperties()) {
        String key = headings.getSecondOrDefault(p.getKey(), p.getKey().toLowerCase());
        switch (key) {
          case "tag":
          case "id":
            field.setId(BigInteger.valueOf(textUtil.tagToInt(p.getValue())));
            break;
          case "name":
            field.setName(p.getValue());
            break;
          case SCENARIO_KEYWORD:
            field.setScenario(scenarioOrDefault(p.getValue()));
            break;
          case "type":
            field.setType(p.getValue());
            break;
          case "values":
            final String values = p.getValue();
            if (values != null && !values.isEmpty()) {
              final String codesetName = detail.getProperty("name") + "Codeset";           
              String codesetScenario = field.getScenario();
              CodeSetType existingCodeset = repositoryAdapter.findCodesetByName(codesetName, codesetScenario);
              if (existingCodeset != null) {
                eventLogger.error("Duplicate definitions of codeset {0} scenario {1}", codesetName, codesetScenario);
                codesetScenario = codesetScenario + "Dup";
              }
              createCodesetFromString(codesetName, codesetScenario,
                  detail.getProperty("type"), values);
              field.setType(codesetName);
            }
            break;
          case "encoding":
            field.setEncoding(p.getValue());
            break;
          case "implminlength":
            field.setImplMinLength(Short.parseShort(p.getValue()));
            break;
          case "implmaxlength":
            field.setImplMaxLength(Short.parseShort(p.getValue()));
            break;
          case "impllength":
            field.setImplLength(Short.parseShort(p.getValue()));
            break;
          case "uniondatatype":
            field.setUnionDataType(UnionDataTypeT.fromValue(p.getValue()));
            break;
          case ABBRNAME_KEYWORD:
            field.setAbbrName(p.getValue());
            break;
          case "basecategory":
            field.setBaseCategory(p.getValue());
            break;
          case "basecategoryabbrname":
            field.setBaseCategoryAbbrName(p.getValue());
            break;
          case "discriminatorid":
            field.setDiscriminatorId(new BigInteger(p.getValue()));
            break;
          case "maxinclusive":
            field.setMaxInclusive(p.getValue());
            break;
          case "mininclusive":
            field.setMinInclusive(p.getValue());
            break;
          case "added":
            field.setAdded(p.getValue());
            break;
          case "addedep":
            field.setAddedEP(new BigInteger(p.getValue()));
            break;
          case "deprecated":
            field.setDeprecated(p.getValue());
            break;
          case "deprecatedep":
            field.setDeprecatedEP(new BigInteger(p.getValue()));
            break;
          case "issue":
            field.setIssue(p.getValue());
            break;
          case "lastmodified":
            field.setLastModified(p.getValue());
            break;
          case "replaced":
            field.setReplaced(p.getValue());
            break;
          case "replacedbyfield":
            field.setReplacedByField(new BigInteger(p.getValue()));
            break;
          case "replacedep":
            field.setReplacedEP(new BigInteger(p.getValue()));
            break;
          case "updated":
            field.setUpdated(p.getValue());
            break;
          case "updatedep":
            field.setUpdatedEP(new BigInteger(p.getValue()));
            break;
          default:
            if (isDocumentationKey(p.getKey())) {
              repositoryAdapter.addDocumentation(p.getValue(), paragraphDelimiterInTables,
                  getPurpose(p.getKey()), annotation);
              field.setAnnotation(annotation);
            } else {
              repositoryAdapter.addAppinfo(p.getValue(), paragraphDelimiterInTables, p.getKey(),
                  annotation);
              field.setAnnotation(annotation);
            }
        }
      }
      addFieldAndType(field);
    }
  }

  private void addFieldAndType(FieldType field) {
    final BigInteger id = field.getId();
    final String name = field.getName();
    final String type = field.getType();
    final String scenario = field.getScenario();

    if (id == null) {
      buildSteps.add(new FieldBuilder(0, name, scenario, type));
    } else if (name == null || type == null) {
      buildSteps.add(new FieldBuilder(id.intValue(), name, scenario, type));
    } else {
      buildSteps.add(new TypeBuilder(type, scenario));
      repositoryAdapter.addField(field);
    }
  }

  private void addFlow(Contextual contextual, Context context) {
    final String name = context.getKey(NAME_POSITION);
    if (contextual instanceof Documentation) {
      final Documentation documentation = (Documentation) contextual;
      final FlowType flow = repositoryAdapter.findFlowByName(name);
      if (flow != null) {
        Annotation annotation = flow.getAnnotation();
        if (annotation == null) {
          annotation = new Annotation();
          flow.setAnnotation(annotation);
        }
        final String parentKey = contextual.getParent().getKey(KEY_POSITION);       
        repositoryAdapter.addDocumentation(documentation.getDocumentation(), 
            FLOW_KEYWORD.equalsIgnoreCase(parentKey) ? null : getPurpose(parentKey),
            annotation);
      }
    } else if (contextual instanceof DetailTable) {
      final DetailTable table = (DetailTable) contextual;
      final FlowType flow = repositoryAdapter.findFlowByName(name);
      if (flow != null) {
        table.rows().forEach(r -> {
          flow.setSource(r.getProperty("source"));
          flow.setDestination(r.getProperty("destination"));
        });
      }
    } // make sure it's not a lower level heading
    else if (contextual instanceof Context
        && FLOW_KEYWORD.equalsIgnoreCase(((Context) contextual).getKey(KEY_POSITION))) {
      final FlowType flow = new FlowType();
      flow.setName(name);
      repositoryAdapter.addFlow(flow);
    }
  }

  private void addGroup(Contextual contextual, Context context) {
    final String name = context.getKey(NAME_POSITION);
    final String scenario = scenarioOrDefault(context.getKeyValue(SCENARIO_KEYWORD));

    if (contextual instanceof DetailTable) {
      final DetailTable detailTable = (DetailTable) contextual;
      final GroupType group = repositoryAdapter.findGroupByName(name, scenario);
      final List<Object> members = group.getComponentRefOrGroupRefOrFieldRef();
      final Iterator<? extends DetailProperties> rowIter = detailTable.rows().iterator();
      int skipRows = 1;
      if (rowIter.hasNext()) {
        if (!populateNumInGroup(rowIter.next(), group)) {
          skipRows = 0;
        }
      } else {
        eventLogger.error("Unknown NumInGroup for group; name={0}", group.getName());
      }
      final Stream<? extends DetailProperties> stream = detailTable.rows().stream();
      final List<? extends DetailProperties> remainingRows =
          stream.skip(skipRows).collect(Collectors.toList());
      addMembers(remainingRows, members);
    } else if (contextual instanceof Documentation) {
      final Documentation detail = (Documentation) contextual;
      final GroupType group = repositoryAdapter.findGroupByName(name, scenario);
      if (group != null) {
        Annotation annotation = group.getAnnotation();
        if (annotation == null) {
          annotation = new Annotation();
          group.setAnnotation(annotation);
        }
        final String parentKey = contextual.getParent().getKey(KEY_POSITION);
        repositoryAdapter.addDocumentation(detail.getDocumentation(), 
            GROUP_KEYWORD.equalsIgnoreCase(parentKey) ? null : getPurpose(parentKey),
            annotation);
      }
    } // make sure it's not a lower level heading
    else if (contextual instanceof Context
        && GROUP_KEYWORD.equalsIgnoreCase(((Context) contextual).getKey(KEY_POSITION))) {
      int tag = textUtil.getTag(context.getKeys());
      final GroupType group = new GroupType();

      GroupType refComponent = null;
      if (referenceRepositoryAdapter != null) {
        refComponent = referenceRepositoryAdapter.findGroupByName(name, scenario);
      }
      if (refComponent != null) {
        tag = refComponent.getId().intValue();
      }
      if (tag == -1) {
        tag = assignId(name, scenario);
      }
      group.setId(BigInteger.valueOf(tag));
      group.setName(name);
      if (!DEFAULT_SCENARIO.equals(scenario)) {
        group.setScenario(scenario);
      }

      final String abbrName = context.getKeyValue(ABBRNAME_KEYWORD);
      if (abbrName != null) {
        group.setAbbrName(abbrName);
      }

      final String category = context.getKeyValue(CATEGORY_KEYWORD);
      if (category != null) {
        group.setCategory(category);
      }

      repositoryAdapter.addGroup(group);
    }
  }

  private void addMembers(Iterable<? extends DetailProperties> properties, List<Object> members)
      throws IllegalArgumentException {
    properties.forEach(detail -> {
      final String tagStr = detail.getProperty("tag");
      if (tagStr != null && !tagStr.isEmpty()) {
        if (GROUP_KEYWORD.startsWith(tagStr.toLowerCase())) {
          final GroupRefType groupRefType = populateGroupRef(detail);
          members.add(groupRefType);
        } else if (COMPONENT_KEYWORD.startsWith(tagStr.toLowerCase())) {
          final ComponentRefType componentRefType = populateComponentRef(detail);
          members.add(componentRefType);
        } else {
          final FieldRefType fieldRefType = populateFieldRef(detail);
          members.add(fieldRefType);
        }
      } else {
        final FieldRefType fieldRefType = populateFieldRef(detail);
        members.add(fieldRefType);
      }
    });
  }

  private void addMessage(Contextual contextual, Context context) {
    final String name = context.getKey(NAME_POSITION);
    final String scenario = scenarioOrDefault(context.getKeyValue(SCENARIO_KEYWORD));

    if (contextual instanceof DetailTable) {
      final DetailTable detailTable = (DetailTable) contextual;
      final MessageType message = repositoryAdapter.findMessageByName(name, scenario);
      if (message != null) {

        final MessageType.Structure structure = new MessageType.Structure();
        message.setStructure(structure);
        final List<Object> members = structure.getComponentRefOrGroupRefOrFieldRef();
        addMembers(detailTable.rows(), members);
      } else {
        eventLogger.error("Unknown message; name={0} scenario={1}", name, scenario);
      }
    } else if (contextual instanceof Documentation) {
      final Documentation detail = (Documentation) contextual;
      final MessageType message = repositoryAdapter.findMessageByName(name, scenario);
      if (message != null) {
        Annotation annotation = message.getAnnotation();
        if (annotation == null) {
          annotation = new Annotation();
          message.setAnnotation(annotation);
        }
        final String parentKey = contextual.getParent().getKey(KEY_POSITION);
        repositoryAdapter.addDocumentation(detail.getDocumentation(), 
            MESSAGE_KEYWORD.equalsIgnoreCase(parentKey) ? null : getPurpose(parentKey),
            annotation);
      } else {
        eventLogger.error("Unknown message; name={0} scenario={1}", name, scenario);
      }
    } // make sure it's not a lower level heading
    else if (contextual instanceof Context
        && MESSAGE_KEYWORD.equalsIgnoreCase(((Context) contextual).getKey(KEY_POSITION))) {
      int tag = textUtil.getTag(context.getKeys());
      final String msgType = context.getKeyValue("type");
      final MessageType message = getOrAddMessage(name, scenario, tag, msgType);
      final String flow = context.getKeyValue(FLOW_KEYWORD);
      if (flow != null) {
        message.setFlow(flow);
      }

      final String abbrName = context.getKeyValue(ABBRNAME_KEYWORD);
      if (abbrName != null) {
        message.setAbbrName(abbrName);
      }

      final String category = context.getKeyValue(CATEGORY_KEYWORD);
      if (category != null) {
        message.setCategory(category);
      }
    }
  }

  private void addMessageResponses(Contextual contextual, Context context) {
    if (contextual instanceof DetailTable) {
      final Context messageContext = context.getParent();
      if (messageContext != null) {
        final String messageName = messageContext.getKeyValue(MESSAGE_KEYWORD);
        int tag = textUtil.getTag(context.getKeys());
        final String scenario = scenarioOrDefault(messageContext.getKeyValue(SCENARIO_KEYWORD));
        final String msgType = messageContext.getKeyValue("type");

        final MessageType message = getOrAddMessage(messageName, scenario, tag, msgType);
        final MessageType.Responses responses = new MessageType.Responses();
        message.setResponses(responses);
        final List<ResponseType> responseList = responses.getResponse();
        final DetailTable detailTable = (DetailTable) context;
        detailTable.rows().forEach(detail -> {
          final ResponseType response = new ResponseType();
          final List<Object> responseRefs = response.getMessageRefOrAssignOrTrigger();
          final MessageRefType messageRef = new MessageRefType();
          messageRef.setName(detail.getProperty("name"));

          final String refScenario = detail.getProperty(SCENARIO_KEYWORD);
          if (refScenario != null && !DEFAULT_SCENARIO.equals(refScenario)) {
            messageRef.setScenario(refScenario);
          }

          messageRef.setMsgType(detail.getProperty("msgType"));
          response.setWhen(detail.getProperty("when"));
          final String markdown = detail.getProperty(DOCUMENTATION_KEYWORD);
          if (markdown != null) {
            Annotation annotation = response.getAnnotation();
            if (annotation == null) {
              annotation = new Annotation();
              response.setAnnotation(annotation);
            }
            repositoryAdapter.addDocumentation(markdown, null, annotation);
          }
          responseRefs.add(messageRef);
          responseList.add(response);
        });
      } else {
        eventLogger.error("Unknown message for responses; keys={0}",
            String.join(" ", context.getKeys()));
      }
    }
  }

  private void addMetadata(Contextual contextual, Context context) {
    if (contextual instanceof DetailTable) {
      final DetailTable detailTable = (DetailTable) contextual;
      detailTable.rows().forEach(detail -> {
        final String term = detail.getProperty("term");
        final String value = detail.getProperty("value");
        if (term != null && value != null) {
          repositoryAdapter.setMetadata(term, value);
        }
      });
    } else if (contextual instanceof Documentation) {
      final Documentation detail = (Documentation) contextual;
      final Repository repository = repositoryAdapter.getRepository();
      if (repository != null) {
        Annotation annotation = repository.getAnnotation();
        if (annotation == null) {
          annotation = new Annotation();
          repository.setAnnotation(annotation);
        }
        final String parentKey = contextual.getParent().getKey(KEY_POSITION);
        repositoryAdapter.addDocumentation(detail.getDocumentation(), getPurpose(parentKey),
            annotation);
      }
    } else {
      final String name = String.join(" ", context.getKeys());
      repositoryAdapter.setName(name);
      final String version = context.getKeyValue("version");
      repositoryAdapter.setVersion(Objects.requireNonNullElse(version, "1.0"));
    }
  }

  private int assignId(String... seeds) {
    return idGenerator.generate(seeds);
  }

  private void createCodesetFromString(String codesetName, String scenario, String type,
      String valueString) {
    final CodeSetType codeset = new CodeSetType();
    codeset.setId(BigInteger.valueOf(assignId(codesetName, scenario)));
    codeset.setName(codesetName);
    if (!DEFAULT_SCENARIO.equals(scenario)) {
      codeset.setScenario(scenario);
    }
    if (type != null) {
      codeset.setType(type);
    } else {
      eventLogger.warn("Unknown codeset datatype; name={0} scenario={1}", codesetName, scenario);
    }
    final List<CodeType> codes = codeset.getCode();

    final Matcher codeMatcher = codePattern.matcher(valueString);
    int matchOffset = 0;
    while (codeMatcher.find()) {
      matchOffset = codeMatcher.end();
      final CodeType code = new CodeType();
      code.setValue(codeMatcher.group(1));
      final String name = codeMatcher.group(2).replaceAll("\"", "");
      code.setName(name);
      code.setId(BigInteger.valueOf(assignId(codesetName, name, scenario)));
      codes.add(code);
    }

    String unmatched = valueString.substring(matchOffset);
    if (!unmatched.isBlank()) {
      eventLogger.error("Malformed inline code in codeset {0}; {1}", codesetName, unmatched);
    }

    repositoryAdapter.addCodeset(codeset);
  }

  private void executeDefferedBuildSteps() {
    ElementBuilder builder;
    while ((builder = buildSteps.poll()) != null) {
      builder.build();
    }
  }

  private Context getKeyContext(Contextual contextual) {
    Context context = null;
    if (contextual instanceof Context) {
      context = (Context) contextual;
    } else {
      context = contextual.getParent();
    }
    while (context != null) {
      final String key = context.getKey(KEY_POSITION);
      if ((Arrays.binarySearch(contextKeys, key.toLowerCase()) >= 0) || (context.getLevel() == 1)) {
        break;
      } else {
        context = context.getParent();
      }
    }
    return context;
  }

  private MessageType getOrAddMessage(String name, String scenario, int tag, String msgType) {
    final String scenarioOrDefault = scenarioOrDefault(scenario);
    MessageType message = repositoryAdapter.findMessageByName(name, scenarioOrDefault);
    if (message == null) {
      message = new MessageType();
      MessageType refMessage = null;
      if (referenceRepositoryAdapter != null) {
        refMessage = referenceRepositoryAdapter.findMessageByName(name, scenarioOrDefault);
      }

      if (tag == -1 && refMessage != null) {
        tag = refMessage.getId().intValue();
      }
      if (tag == -1) {
        tag = assignId(name, scenario);
      }
      message.setId(BigInteger.valueOf(tag));
      message.setName(name);
      if (!DEFAULT_SCENARIO.equals(scenarioOrDefault)) {
        message.setScenario(scenarioOrDefault);
      }
      if (msgType == null && refMessage != null) {
        msgType = refMessage.getMsgType();
      }
      if (msgType != null) {
        message.setMsgType(msgType);
      }
      repositoryAdapter.addMessage(message);

    }
    return message;
  }

  private String getPurpose(String word) {
    // May or may not match an enumerated value, but force case for all
    return word.toUpperCase();
  }

  private boolean isDocumentationKey(String word) {
    for (final PurposeEnum purpose : PurposeEnum.values()) {
      if (purpose.value().compareToIgnoreCase(word) == 0) {
        return true;
      }
    }
    return (DOCUMENTATION_KEYWORD.compareToIgnoreCase(word) == 0)
        || (DESCRIPTION_KEYWORD.compareToIgnoreCase(word) == 0);
  }

  private boolean isPresence(String word) {
    if (word == null || word.isEmpty()) {
      return false;
    } else {
      final String lcWord = word.toLowerCase();
      return "required".startsWith(lcWord) || "optional".startsWith(lcWord)
          || "forbidden".startsWith(lcWord) || "ignored".startsWith(lcWord)
          || "constant".startsWith(lcWord);
    }
  }

  private ComponentRefType populateComponentRef(DetailProperties detail) {
    final ComponentRefType componentRefType = new ComponentRefType();

    String name = null;
    String scenario = DEFAULT_SCENARIO;
    String presenceString = null;
    for (final Entry<String, String> p : detail.getProperties()) {
      String key = headings.getSecondOrDefault(p.getKey(), p.getKey().toLowerCase());
      switch (key) {
        case "name":
          name = p.getValue();
          break;
        case SCENARIO_KEYWORD:
          scenario = scenarioOrDefault(detail.getProperty(SCENARIO_KEYWORD));
          break;
        case "presence":
          presenceString = p.getValue();
          break;
        case "tag":
        case "id":
          final int id = textUtil.tagToInt(p.getValue());
          if (id >= 0) {
            componentRefType.setId(BigInteger.valueOf(id));
          }
          break;
        case "added":
          componentRefType.setAdded(p.getValue());
          break;
        case "addedep":
          componentRefType.setAddedEP(new BigInteger(p.getValue()));
          break;
        case "deprecated":
          componentRefType.setDeprecated(p.getValue());
          break;
        case "deprecatedep":
          componentRefType.setDeprecatedEP(new BigInteger(p.getValue()));
          break;
        case "issue":
          componentRefType.setIssue(p.getValue());
          break;
        case "lastmodified":
          componentRefType.setLastModified(p.getValue());
          break;
        case "replaced":
          componentRefType.setReplaced(p.getValue());
          break;
        case "replacedep":
          componentRefType.setReplacedEP(new BigInteger(p.getValue()));
          break;
        case "updated":
          componentRefType.setUpdated(p.getValue());
          break;
        case "updatedep":
          componentRefType.setUpdatedEP(new BigInteger(p.getValue()));
          break;
        default:
          Annotation annotation = componentRefType.getAnnotation();
          if (annotation == null) {
            annotation = new Annotation();
          }
          if (isDocumentationKey(p.getKey())) {
            repositoryAdapter.addDocumentation(p.getValue(), paragraphDelimiterInTables,
                getPurpose(p.getKey()), annotation);
            componentRefType.setAnnotation(annotation);
          } else {
            repositoryAdapter.addAppinfo(p.getValue(), paragraphDelimiterInTables, p.getKey(),
                annotation);
            componentRefType.setAnnotation(annotation);
          }
      }
    }

    ComponentType componentType = repositoryAdapter.findComponentByName(name, scenario);
    if (componentType != null) {
      componentRefType.setId(componentType.getId());
    } else if (referenceRepositoryAdapter != null) {
      componentType = referenceRepositoryAdapter.findComponentByName(name, scenario);
      if (componentType != null) {
        componentRefType.setId(componentType.getId());
        buildSteps.add(new ComponentBuilder(name, scenario));
      }
    }
    if (componentType == null) {
      // Component not found, but write referenceRepositoryAdapter to be corrected later
      componentRefType.setId(BigInteger.ZERO);
      buildSteps.add(new ComponentBuilder(name, scenario));
      buildSteps.add(new ComponentRefBuilder(name, componentRefType));
    }

    final List<ComponentRuleType> rules = componentRefType.getRule();

    PresenceT presence = PresenceT.OPTIONAL;
    if (presenceString != null) {
      final String[] presenceWords = presenceString.split("[ \t]");

      boolean inWhen = false;
      final List<String> whenWords = new ArrayList<>();
      for (final String word : presenceWords) {
        if (isPresence(word)) {
          if (!whenWords.isEmpty()) {
            final ComponentRuleType rule = new ComponentRuleType();
            rule.setPresence(presence);
            rule.setWhen(String.join(" ", whenWords));
            rules.add(rule);
          }
          presence = stringToPresence(word);
          inWhen = false;
          whenWords.clear();
        } else if (word.equalsIgnoreCase(WHEN_KEYWORD)) {
          inWhen = true;
        } else if (inWhen) {
          whenWords.add(word);
        }
      }

      if (presence != PresenceT.OPTIONAL && whenWords.isEmpty()) {
        componentRefType.setPresence(presence);
      } else if (!whenWords.isEmpty()) {
        final ComponentRuleType rule = new ComponentRuleType();
        rule.setPresence(presence);
        rule.setWhen(String.join(" ", whenWords));
        rules.add(rule);
      }
    } else if (presence != PresenceT.OPTIONAL) {
      componentRefType.setPresence(presence);
    }

    if (!DEFAULT_SCENARIO.equals(scenario)) {
      componentRefType.setScenario(scenario);
    }

    return componentRefType;
  }

  private FieldRefType populateFieldRef(DetailProperties detail) {
    final FieldRefType fieldRefType = new FieldRefType();
    String name = null;
    String presenceString = null;
    String valueString = null;

    for (final Entry<String, String> p : detail.getProperties()) {
      String key = headings.getSecondOrDefault(p.getKey(), p.getKey().toLowerCase());
      switch (key) {
        case "name":
          name = p.getValue();
          break;
        case "presence":
          presenceString = p.getValue();
          break;
        case "values":
          valueString = p.getValue();
          break;
        case "tag":
        case "id":
          fieldRefType.setId(BigInteger.valueOf(textUtil.tagToInt(p.getValue())));
          break;
        case SCENARIO_KEYWORD:
          fieldRefType.setScenario(scenarioOrDefault(p.getValue()));
          break;
        case "encoding":
          fieldRefType.setEncoding(p.getValue());
          break;
        case "implminlength":
          fieldRefType.setImplMinLength(Short.parseShort(p.getValue()));
          break;
        case "implmaxlength":
          fieldRefType.setImplMaxLength(Short.parseShort(p.getValue()));
          break;
        case "impllength":
          fieldRefType.setImplLength(Short.parseShort(p.getValue()));
          break;
        case "maxinclusive":
          fieldRefType.setMaxInclusive(p.getValue());
          break;
        case "mininclusive":
          fieldRefType.setMinInclusive(p.getValue());
          break;
        case "added":
          fieldRefType.setAdded(p.getValue());
          break;
        case "addedep":
          fieldRefType.setAddedEP(new BigInteger(p.getValue()));
          break;
        case "deprecated":
          fieldRefType.setDeprecated(p.getValue());
          break;
        case "deprecatedep":
          fieldRefType.setDeprecatedEP(new BigInteger(p.getValue()));
          break;
        case "issue":
          fieldRefType.setIssue(p.getValue());
          break;
        case "lastmodified":
          fieldRefType.setLastModified(p.getValue());
          break;
        case "replaced":
          fieldRefType.setReplaced(p.getValue());
          break;
        case "replacedbyfield":
          fieldRefType.setReplacedByField(new BigInteger(p.getValue()));
          break;
        case "replacedep":
          fieldRefType.setReplacedEP(new BigInteger(p.getValue()));
          break;
        case "updated":
          fieldRefType.setUpdated(p.getValue());
          break;
        case "updatedep":
          fieldRefType.setUpdatedEP(new BigInteger(p.getValue()));
          break;
        default:
          Annotation annotation = fieldRefType.getAnnotation();
          if (annotation == null) {
            annotation = new Annotation();
          }
          if (isDocumentationKey(p.getKey())) {
            repositoryAdapter.addDocumentation(p.getValue(), paragraphDelimiterInTables,
                getPurpose(p.getKey()), annotation);
            fieldRefType.setAnnotation(annotation);
          } else {
            repositoryAdapter.addAppinfo(p.getValue(), paragraphDelimiterInTables, p.getKey(),
                annotation);
            fieldRefType.setAnnotation(annotation);
          }
      }
    }

    final String scenario = scenarioOrDefault(detail.getProperty(SCENARIO_KEYWORD));
    if (fieldRefType.getId() != null) {
      final FieldType fieldType =
          repositoryAdapter.findFieldByTag(fieldRefType.getId().intValue(), scenario);
      if (fieldType == null) {
        buildSteps.add(new FieldBuilder(fieldRefType.getId().intValue(), name, scenario, null));
      }
    } else {
      final FieldType fieldType = repositoryAdapter.findFieldByName(name, scenario);
      if (fieldType != null) {
        fieldRefType.setId(fieldType.getId());
      } else {
        fieldRefType.setId(BigInteger.ZERO);
        buildSteps.add(new FieldBuilder(0, name, scenario, null));
        buildSteps.add(new FieldRefBuilder(name, fieldRefType));
      }
    }

    final List<FieldRuleType> rules = fieldRefType.getRule();
    PresenceT presence = PresenceT.OPTIONAL;

    final List<String> whenWords = new ArrayList<>();

    if (presenceString != null) {
      final String[] presenceWords = presenceString.split("[ \t]");
      boolean inWhen = false;
      for (final String word : presenceWords) {
        if (isPresence(word)) {
          if (!whenWords.isEmpty()) {
            final FieldRuleType rule = new FieldRuleType();
            rule.setPresence(presence);
            rule.setWhen(String.join(" ", whenWords));
            rules.add(rule);
          }
          presence = stringToPresence(word);
          inWhen = false;
          whenWords.clear();
        } else if (word.equalsIgnoreCase(WHEN_KEYWORD)) {
          inWhen = true;
        } else if (inWhen) {
          whenWords.add(word);
        }
      }
    }

    if (presence != PresenceT.OPTIONAL && whenWords.isEmpty()) {
      fieldRefType.setPresence(presence);
    } else if (!whenWords.isEmpty()) {
      final FieldRuleType rule = new FieldRuleType();
      rule.setPresence(presence);
      rule.setWhen(String.join(" ", whenWords));
      rules.add(rule);
    }

    if (valueString != null && !valueString.isEmpty()) {
      final int keywordPos = valueString.indexOf(ASSIGN_KEYWORD);
      if (keywordPos != -1) {
        fieldRefType.setAssign(valueString.substring(keywordPos + ASSIGN_KEYWORD.length() + 1));
      } else {
        final Matcher codeMatcher = codePattern.matcher(valueString);
        if (codeMatcher.find()) {
          final String codesetName = name + "Codeset";
          String codesetScenario = scenario;
          CodeSetType existingCodeset = repositoryAdapter.findCodesetByName(codesetName, codesetScenario);
          if (existingCodeset != null) {
            eventLogger.error("Duplicate definitions of codeset {0} scenario {1}", codesetName, codesetScenario);
            codesetScenario = codesetScenario + "Dup";
          }
          createCodesetFromString(codesetName, codesetScenario, DEFAULT_CODE_TYPE, valueString);
        } else if (presence == PresenceT.CONSTANT) {
          fieldRefType.setValue(valueString);
          if (!DEFAULT_SCENARIO.equals(scenario)) {
            fieldRefType.setScenario(scenario);
          }
        } else {
          eventLogger.error("Field has value but not constant; id={0, number, ###}, presence={1}",
              fieldRefType.getId(), fieldRefType.getPresence().toString());
        }
      }

    } else if (presence == PresenceT.CONSTANT) {
      eventLogger.error("Missing value for constant presence field; id={0, number, ###}",
          fieldRefType.getId());
    }
    return fieldRefType;
  }

  private GroupRefType populateGroupRef(DetailProperties detail) {
    final GroupRefType groupRefType = new GroupRefType();

    String name = null;
    String scenario = DEFAULT_SCENARIO;
    String presenceString = null;
    for (final Entry<String, String> p : detail.getProperties()) {
      String key = headings.getSecondOrDefault(p.getKey(), p.getKey().toLowerCase());
      switch (key) {
        case "name":
          name = p.getValue();
          break;
        case SCENARIO_KEYWORD:
          scenario = scenarioOrDefault(detail.getProperty(SCENARIO_KEYWORD));
          break;
        case "presence":
          presenceString = p.getValue();
          break;
        case "tag":
        case "id":
          final int id = textUtil.tagToInt(p.getValue());
          if (id >= 0) {
            groupRefType.setId(BigInteger.valueOf(id));
          }
          break;
        case "added":
          groupRefType.setAdded(p.getValue());
          break;
        case "addedep":
          groupRefType.setAddedEP(new BigInteger(p.getValue()));
          break;
        case "deprecated":
          groupRefType.setDeprecated(p.getValue());
          break;
        case "deprecatedep":
          groupRefType.setDeprecatedEP(new BigInteger(p.getValue()));
          break;
        case "issue":
          groupRefType.setIssue(p.getValue());
          break;
        case "lastmodified":
          groupRefType.setLastModified(p.getValue());
          break;
        case "replaced":
          groupRefType.setReplaced(p.getValue());
          break;
        case "replacedep":
          groupRefType.setReplacedEP(new BigInteger(p.getValue()));
          break;
        case "updated":
          groupRefType.setUpdated(p.getValue());
          break;
        case "updatedep":
          groupRefType.setUpdatedEP(new BigInteger(p.getValue()));
          break;
        default:
          Annotation annotation = groupRefType.getAnnotation();
          if (annotation == null) {
            annotation = new Annotation();
          }
          if (isDocumentationKey(p.getKey())) {
            repositoryAdapter.addDocumentation(p.getValue(), paragraphDelimiterInTables,
                getPurpose(p.getKey()), annotation);
            groupRefType.setAnnotation(annotation);
          } else {
            repositoryAdapter.addAppinfo(p.getValue(), paragraphDelimiterInTables, p.getKey(),
                annotation);
            groupRefType.setAnnotation(annotation);
          }
      }
    }
    GroupType groupType = repositoryAdapter.findGroupByName(name, scenario);
    if (groupType != null) {
      groupRefType.setId(groupType.getId());
    } else if (referenceRepositoryAdapter != null) {
      groupType = referenceRepositoryAdapter.findGroupByName(name, scenario);
      if (groupType != null) {
        groupRefType.setId(groupType.getId());
        buildSteps.add(new GroupBuilder(name, scenario));
      }
    }
    if (groupType == null) {
      // Group not found, but write referenceRepositoryAdapter to be corrected later
      groupRefType.setId(BigInteger.ZERO);
      buildSteps.add(new GroupBuilder(name, scenario));
      buildSteps.add(new GroupRefBuilder(name, groupRefType));
    }

    final List<ComponentRuleType> rules = groupRefType.getRule();

    PresenceT presence = PresenceT.OPTIONAL;
    if (presenceString != null) {
      final String[] presenceWords = presenceString.split("[ \t]");

      boolean inWhen = false;
      final List<String> whenWords = new ArrayList<>();
      for (final String word : presenceWords) {
        if (isPresence(word)) {
          if (!whenWords.isEmpty()) {
            final ComponentRuleType rule = new ComponentRuleType();
            rule.setPresence(presence);
            rule.setWhen(String.join(" ", whenWords));
            rules.add(rule);
          }
          presence = stringToPresence(word);
          inWhen = false;
          whenWords.clear();
        } else if (word.equalsIgnoreCase(WHEN_KEYWORD)) {
          inWhen = true;
        } else if (inWhen) {
          whenWords.add(word);
        }
      }

      if (presence != PresenceT.OPTIONAL && whenWords.isEmpty()) {
        groupRefType.setPresence(presence);
      } else if (!whenWords.isEmpty()) {
        final ComponentRuleType rule = new ComponentRuleType();
        rule.setPresence(presence);
        rule.setWhen(String.join(" ", whenWords));
        rules.add(rule);
      }
    } else if(presence != PresenceT.OPTIONAL) {
      groupRefType.setPresence(presence);
    }

    if (!DEFAULT_SCENARIO.equals(scenario)) {
      groupRefType.setScenario(scenario);
    }

    return groupRefType;
  }

  private boolean populateNumInGroup(DetailProperties detail, GroupType group) {
    FieldType fieldType = null;
    Integer id = detail.getIntProperty("id");
    if (id == null) {
      id = detail.getIntProperty("tag");
    }
    if (id != null) {
      fieldType = repositoryAdapter.findFieldByTag(id, DEFAULT_SCENARIO); 
      if (fieldType == null && referenceRepositoryAdapter != null) {
        fieldType = referenceRepositoryAdapter.findFieldByTag(id, DEFAULT_SCENARIO);
      }
    }
    if (fieldType == null) {
      String name = detail.getProperty("name");
      fieldType = repositoryAdapter.findFieldByName(name, DEFAULT_SCENARIO); 
      if (fieldType == null && referenceRepositoryAdapter != null) {
        fieldType = referenceRepositoryAdapter.findFieldByName(name, DEFAULT_SCENARIO);
      }
    }
    if ((fieldType != null) && !"NumInGroup".equals(fieldType.getType())) {
      eventLogger.error("First group field not NumInGroup datatype; id={0, number, ###} in group={1}",
          fieldType.getId(), group.getName());
      return false;
    }
    
    final FieldRefType numInGroup = populateFieldRef(detail);
    group.setNumInGroup(numInGroup);
    return true;
  }

  private String scenarioOrDefault(String scenario) {
    return (scenario != null && !scenario.isEmpty()) ? scenario : DEFAULT_SCENARIO;
  }

  private PresenceT stringToPresence(String word) {
    if (word == null || word.isEmpty()) {
      return PresenceT.OPTIONAL;
    } else {
      final String lcWord = word.toLowerCase();
      if ("required".startsWith(lcWord)) {
        return PresenceT.REQUIRED;
      } else if ("forbidden".startsWith(lcWord)) {
        return PresenceT.FORBIDDEN;
      } else if ("ignored".startsWith(lcWord)) {
        return PresenceT.IGNORED;
      } else if ("constant".startsWith(lcWord)) {
        return PresenceT.CONSTANT;
      } else {
        return PresenceT.OPTIONAL;
      }
    }
  }
}
