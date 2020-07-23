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

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.bind.JAXBException;
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
import io.fixprotocol._2020.orchestra.repository.ResponseType;
import io.fixprotocol._2020.orchestra.repository.StateMachineType;
import io.fixprotocol._2020.orchestra.repository.StateType;
import io.fixprotocol._2020.orchestra.repository.TransitionType;
import io.fixprotocol.md.event.Context;
import io.fixprotocol.md.event.Detail;
import io.fixprotocol.md.event.DetailProperties;
import io.fixprotocol.md.event.DetailTable;
import io.fixprotocol.md.event.Documentation;

class RepositoryBuilder implements Consumer<Context> {

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
          logger.error("RepositoryBuilder unknown component; name={} scenario={}", name, scenario);
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
      ComponentType componentType =
          repositoryAdapter.findComponentByName(name, componentRef.getScenario());
      if (componentType != null) {
        componentRef.setId(componentType.getId());
      } else {
        logger.error("RepositoryBuilder unknown componentRef ID; name={} scenario={}", name,
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

    public FieldBuilder(int tag, String name, String scenario) {
      this.tag = tag;
      this.name = name;
      this.scenario = scenario;
    }

    @Override
    public void build() {
      FieldType fieldType = null;
      if (tag != -1) {
        fieldType = repositoryAdapter.findFieldByTag(tag, scenario);
      } else if (name != null) {
        fieldType = repositoryAdapter.findFieldByName(name, scenario);
      }

      if ((fieldType == null || fieldType.getType() == null) && referenceRepositoryAdapter != null) {
        if (tag != -1) {
          fieldType = referenceRepositoryAdapter.findFieldByTag(tag, scenario);
        } else if (name != null) {
          fieldType = referenceRepositoryAdapter.findFieldByName(name, scenario);
        }
        if (fieldType != null) {
          repositoryAdapter.copyField(fieldType);
        }
      }
      if (fieldType == null) {
        fieldType = new FieldType();
        fieldType.setName(Objects.requireNonNullElseGet(name, () -> "Field" + tag));
        if (tag > 0) {
          fieldType.setId(BigInteger.valueOf(tag));
        } else {
          fieldType.setId(BigInteger.ZERO);
          logger.error("RepositoryBuilder unknown field ID; name={} scenario={}", name, scenario);
        }
        addFieldAndType(fieldType);
      }
    }
  }

  /**
   * 
   * Populates the ID of a GroupRef when the group name is known
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
      FieldType componentType = repositoryAdapter.findFieldByName(name, fieldRef.getScenario());
      if (componentType != null) {
        fieldRef.setId(componentType.getId());
      } else {
        logger.error("RepositoryBuilder unknown fieldRef ID; name={} scenario={}", name,
            fieldRef.getScenario());
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
          logger.error("RepositoryBuilder unknown group; name={} scenario={}", name, scenario);
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
      GroupType groupType = repositoryAdapter.findGroupByName(name, groupRef.getScenario());
      if (groupType != null) {
        groupRef.setId(groupType.getId());
      } else {
        logger.error("RepositoryBuilder unknown groupRef ID; name={} scenario={}", name,
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
        if (codeset != null ) {
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
        // if not found as a datatype or codeset in either current or referenceRepositoryAdapter repository, then
        // assume its a datatype name
        datatype = new io.fixprotocol._2020.orchestra.repository.Datatype();
        datatype.setName(type);
        repositoryAdapter.addDatatype(datatype);
        logger.info("RepositoryBuilder datatype added; name={}", datatype.getName());
      }
    }
  }

  public static final String ASSIGN_KEYWORD = "assign";
  public static final String WHEN_KEYWORD = "when";

  private static final String DEFAULT_CODE_TYPE = "char";
  private static final String DEFAULT_SCENARIO = "base";
  private static final int NAME_POSITION = 1;

  private final Queue<ElementBuilder> buildSteps = new LinkedList<>();
  private int lastId = 10000;
  private final Logger logger = LogManager.getLogger(getClass());
  private RepositoryAdapter referenceRepositoryAdapter = null;
  private final RepositoryAdapter repositoryAdapter = new RepositoryAdapter();

  public RepositoryBuilder() {
    this.repositoryAdapter.createRepository();
  }

  public RepositoryBuilder(InputStream inputStream) throws JAXBException {
    this.repositoryAdapter.unmarshal(inputStream);
  }

  @Override
  public void accept(Context context) {
    final String type = context.getKey(0);
    if (type == null) {
      // log unknown type
    } else
      switch (type.toLowerCase()) {
        case "actor":
          addActor(context);
          break;
        case "codeset":
          addCodeset(context);
          break;
        case "component":
          addComponent(context);
          break;
        case "datatypes":
          addDatatype(context);
          break;
        case "fields":
          addField(context);
          break;
        case "flow":
          addFlow(context);
          break;
        case "group":
          addGroup(context);
          break;
        case "message":
          addMessageMembers(context);
          break;
        case "responses":
          addMessageResponses(context);
          break;
        case "statemachine":
          addActorStates(context);
          break;
        case "variables":
          addActorVariables(context);
          break;
        default:
          if (context.getLevel() == 1) {
            addMetadata(context);
          } else {
            logger.warn("RepositoryBuilder received unknown context type {}", type);
          }
      }
  }



  public void setReference(RepositoryAdapter reference) {
    this.referenceRepositoryAdapter = reference;
  }

  public void write(OutputStream outputStream) throws JAXBException {
    executeDefferedBuildSteps();
    repositoryAdapter.marshal(outputStream);
  }

  void copyMembers(List<Object> members) {
    for (final Object member : members) {
      if (member instanceof FieldRefType) {
        final FieldRefType fieldRef = (FieldRefType) member;
        FieldType field =
            repositoryAdapter.findFieldByTag(fieldRef.getId().intValue(), fieldRef.getScenario());
        if (field == null) {
          field = referenceRepositoryAdapter.findFieldByTag(fieldRef.getId().intValue(), fieldRef.getScenario());
          if (field != null) {
            addFieldAndType(field);
          } else {
            logger.error("RepositoryBuilder unknown field; lastId={} scenario={}",
                fieldRef.getId().intValue(), fieldRef.getScenario());
          }
        }
      } else if (member instanceof GroupRefType) {
        final GroupRefType groupRef = (GroupRefType) member;
        GroupType group =
            repositoryAdapter.findGroupByTag(groupRef.getId().intValue(), groupRef.getScenario());
        if (group == null) {
          group = referenceRepositoryAdapter.findGroupByTag(groupRef.getId().intValue(), groupRef.getScenario());
          if (group != null) {
            group = repositoryAdapter.copyGroup(group);
            final List<Object> groupMembers = group.getComponentRefOrGroupRefOrFieldRef();
            copyMembers(groupMembers);
          } else {
            logger.error("RepositoryBuilder unknown group; lastId={} scenario={}",
                groupRef.getId().intValue(), groupRef.getScenario());
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
            logger.error("RepositoryBuilder unknown component; lastId={} scenario={}",
                componentRef.getId().intValue(), componentRef.getScenario());
          }
        }
      }
    }
  }

  private void addActor(Context context) {
    if (context instanceof Documentation) {
      final Documentation detail = (Documentation) context;
      final String name = detail.getKey(NAME_POSITION);

      final ActorType actor = repositoryAdapter.findActorByName(name);
      if (actor != null) {
        Annotation annotation = actor.getAnnotation();
        if (annotation == null) {
          annotation = new Annotation();
          actor.setAnnotation(annotation);
        }
        repositoryAdapter.addDocumentation(detail.getDocumentation(), null, annotation);
      }
    } else {
      final String name = context.getKey(NAME_POSITION);
      final ActorType actor = new ActorType();
      actor.setName(name);

      repositoryAdapter.addActor(actor);
    }
  }

  private void addActorStates(Context context) {
    if (context instanceof DetailTable) {
      final Context actorContext = context.getParent();
      if (actorContext != null) {
        final String actorName = actorContext.getKeyValue("actor");
        final ActorType actor = repositoryAdapter.findActorByName(actorName);
        if (actor != null) {
          final String name = actorContext.getKeyValue("statemachine");
          final StateMachineType statemachine = new StateMachineType();
          statemachine.setName(name);
          final List<Object> actorMembers = actor.getFieldOrFieldRefOrComponent();
          actorMembers.add(statemachine);

          final DetailTable detailTable = (DetailTable) context;

          final List<String> sources = detailTable.rows().get().map(r -> r.getProperty("state"))
              .distinct().collect(Collectors.toList());
          final List<String> targets = detailTable.rows().get().map(r -> r.getProperty("target"))
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
            detailTable.rows().get().forEach(r -> {
              final TransitionType transition = new TransitionType();
              final Annotation annotation = new Annotation();

              String sourceStateName = null;
              for (Entry<String, String> p : r.getProperties()) {

                switch (p.getKey().toLowerCase()) {
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
                      repositoryAdapter.addDocumentation(p.getValue(), getPurpose(p.getKey()), annotation);
                      transition.setAnnotation(annotation);
                    } else {
                      repositoryAdapter.addAppinfo(p.getValue(), p.getKey(), annotation);
                      transition.setAnnotation(annotation);
                    }
                }
              }

              StateType sourceState = null;
              if (sourceStateName.equals(initialStateName)) {
                sourceState = statemachine.getInitial();
              } else {
                for (StateType state : statemachine.getState()) {
                  if (state.getName().equals(sourceStateName)) {
                    sourceState = state;
                    break;
                  }
                }
              }
              sourceState.getTransition().add(transition);
            });
          } catch (final NoSuchElementException e) {
            logger.warn("RepositoryBuilder no states defined for state machine; name={}", name);
          }
        } else {
          logger.error("RepositoryBuilder unknown actor for state machine; name={}", actorName);
        }
      }
    }
  }

  private void addActorVariables(Context context) {
    if (context instanceof DetailTable) {
      final Context actorContext = context.getParent();
      if (actorContext != null) {
        final String actorName = actorContext.getKeyValue("actor");
        final ActorType actor = repositoryAdapter.findActorByName(actorName);
        if (actor != null) {
          final DetailTable detailTable = (DetailTable) context;
          final List<Object> members = actor.getFieldOrFieldRefOrComponent();
          addMembers(detailTable.rows().get(), members);
        } else {
          logger.error("RepositoryBuilder unknown actor for variables; name={}", actorName);
        }
      }
    }
  }


  private void addCodeset(Context context) {
    if (context instanceof DetailTable) {
      final DetailTable detailTable = (DetailTable) context;
      final String name = detailTable.getKey(NAME_POSITION);
      int tag = tagToInt(detailTable.getKeyValue("tag"));
      final String scenario = scenarioOrDefault(detailTable.getKeyValue("scenario"));

      final CodeSetType codeset = new CodeSetType();
      CodeSetType refCodeset = null;
      if (referenceRepositoryAdapter != null) {
        refCodeset = referenceRepositoryAdapter.findCodesetByName(name, scenario);
      }
      if (tag == -1 && refCodeset != null) {
        tag = refCodeset.getId().intValue();
      }
      if (tag == -1) {
        tag = assignId();
      }
      codeset.setId(BigInteger.valueOf(tag));
      codeset.setName(name);
      if (!DEFAULT_SCENARIO.equals(scenario)) {
        codeset.setScenario(scenario);
      }

      String type = detailTable.getKeyValue("type");
      if (type == null && refCodeset != null) {
        type = refCodeset.getType();
      }
      if (type != null) {
        codeset.setType(type);
      } else {
        logger.error("RepositoryBuilder unknown CodeSet underlying type; name={}", name);
      }
      repositoryAdapter.addCodeset(codeset);

      final List<CodeType> codes = codeset.getCode();
      detailTable.rows().get().forEach(detail ->addCode(detail, codes));
    } else if (context instanceof Documentation) {
      final Documentation detail = (Documentation) context;
      final String name = detail.getKey(NAME_POSITION);
      final String scenario = scenarioOrDefault(detail.getKeyValue("scenario"));

      final CodeSetType codeset = repositoryAdapter.findCodesetByName(name, scenario);
      if (codeset != null) {
        Annotation annotation = codeset.getAnnotation();
        if (annotation == null) {
          annotation = new Annotation();
          codeset.setAnnotation(annotation);
        }
        repositoryAdapter.addDocumentation(detail.getDocumentation(), null, annotation);
      }
    }
  }

  private void addCode(final DetailProperties detail, List<CodeType> codes) {
    final CodeType codeType = new CodeType();
    final Annotation annotation = new Annotation();
    
    for (Entry<String, String> p : detail.getProperties()) {

      switch (p.getKey().toLowerCase()) {
        case "name":
          codeType.setName(p.getValue());
          break;
        case "value":
          break;
        case "tag":
        case "id":
          codeType.setId(BigInteger.valueOf(tagToInt(p.getValue())));
          break;
        default:
          if (isDocumentationKey(p.getKey())) {
            repositoryAdapter.addDocumentation(p.getValue(), getPurpose(p.getKey()), annotation);
            codeType.setAnnotation(annotation);
          } else {
            repositoryAdapter.addAppinfo(p.getValue(), p.getKey(), annotation);
            codeType.setAnnotation(annotation);
          }
      }
    }

    if (codeType.getId() == null) {
      codeType.setId(BigInteger.valueOf(assignId()));
    }
    codes.add(codeType);
  }

  private void addComponent(Context context) {
    if (context instanceof DetailTable) {
      final DetailTable detailTable = (DetailTable) context;
      final String name = detailTable.getKey(NAME_POSITION);
      int tag = tagToInt(detailTable.getKeyValue("tag"));
      final String scenario = scenarioOrDefault(detailTable.getKeyValue("scenario"));

      final ComponentType component = new ComponentType();
      ComponentType refComponent = null;
      if (referenceRepositoryAdapter != null) {
        refComponent = referenceRepositoryAdapter.findComponentByName(name, scenario);
      }
      if (refComponent != null) {
        tag = refComponent.getId().intValue();
      }
      if (tag == -1) {
        tag = assignId();
      }
      component.setId(BigInteger.valueOf(tag));
      component.setName(name);
      if (!DEFAULT_SCENARIO.equals(scenario)) {
        component.setScenario(scenario);
      }
      final List<Object> members = component.getComponentRefOrGroupRefOrFieldRef();
      addMembers(detailTable.rows().get(), members);
      repositoryAdapter.addComponent(component);

    } else if (context instanceof Documentation) {
      final Documentation detail = (Documentation) context;
      final String name = detail.getKey(NAME_POSITION);
      final String scenario = scenarioOrDefault(detail.getKeyValue("scenario"));

      final ComponentType component = repositoryAdapter.findComponentByName(name, scenario);
      if (component != null) {
        Annotation annotation = component.getAnnotation();
        if (annotation == null) {
          annotation = new Annotation();
          component.setAnnotation(annotation);
        }
        repositoryAdapter.addDocumentation(detail.getDocumentation(), null, annotation);
      }
    }
  }


  private void addDatatype(Context context) {
    if (context instanceof Detail) {
      final Detail detail = (Detail) context;
      final String name = detail.getProperty("name");
      io.fixprotocol._2020.orchestra.repository.Datatype datatype =
          repositoryAdapter.findDatatypeByName(name);
      if (datatype == null) {
        datatype = new io.fixprotocol._2020.orchestra.repository.Datatype();
        datatype.setName(name);
        repositoryAdapter.addDatatype(datatype);
        final String markdown = detail.getProperty("documentation");
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
    }
  }

  private void addDatatypeMapping(final Detail detail, final String standard,
      final List<MappedDatatype> mappings) {
    final MappedDatatype mapping = new MappedDatatype();
    final Annotation annotation = new Annotation();
    mapping.setStandard(standard);

    for (Entry<String, String> p : detail.getProperties()) {
      switch (p.getKey().toLowerCase()) {
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
            repositoryAdapter.addDocumentation(p.getValue(), getPurpose(p.getKey()), annotation);
            mapping.setAnnotation(annotation);
          } else {
            repositoryAdapter.addAppinfo(p.getValue(), p.getKey(), annotation);
            mapping.setAnnotation(annotation);
          }
      }
    }
    mappings.add(mapping);
  }

  private void addField(Context context) {
    if (context instanceof Detail) {
      final Detail detail = (Detail) context;
      final FieldType field = new FieldType();
      final Annotation annotation = new Annotation();
      for (Entry<String, String> p : detail.getProperties()) {

        switch (p.getKey().toLowerCase()) {
          case "tag":
          case "id":
            field.setId(BigInteger.valueOf(tagToInt(p.getValue())));
            break;
          case "name":
            field.setName(p.getValue());
            break;
          case "scenario":
            field.setScenario(scenarioOrDefault(p.getValue()));
            break;
          case "type":
            field.setType(p.getValue());
            break;
          case "values":
            String values = p.getValue();
            if (values != null && !values.isEmpty()) {
              final String[] valueTokens = values.split("[ =\t]");
              final String codesetName = detail.getProperty("name") + "Codeset";
              createCodeset(codesetName, detail.getProperty("scenario"), detail.getProperty("type"), valueTokens);
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
          default:
            if (isDocumentationKey(p.getKey())) {
              repositoryAdapter.addDocumentation(p.getValue(), getPurpose(p.getKey()), annotation);
              field.setAnnotation(annotation);
            } else {
              repositoryAdapter.addAppinfo(p.getValue(), p.getKey(), annotation);
              field.setAnnotation(annotation);
            }
        }
      }
      addFieldAndType(field);
    }
  }

  private void addFieldAndType(FieldType field) {
    repositoryAdapter.addField(field);

    final String type = field.getType();
    final String scenario = field.getScenario();

    if (type != null) {
      buildSteps.add(new TypeBuilder(type, scenario));
    } else {
      logger.error("RepositoryBuilder unknown type for field; id= {} name={}", field.getId(), field.getName());
    }
  }

  private void addFlow(Context context) {
    if (context instanceof DetailTable) {
      final DetailTable table = (DetailTable) context;
      final String name = table.getKey(NAME_POSITION);
      final FlowType flow = new FlowType();
      flow.setName(name);
      table.rows().get().findFirst().ifPresent(r -> {
        flow.setSource(r.getProperty("source"));
        flow.setDestination(r.getProperty("destination"));
      });

      repositoryAdapter.addFlow(flow);
    } else if (context instanceof Documentation) {
      final Documentation detail = (Documentation) context;
      final String name = detail.getKey(NAME_POSITION);

      final FlowType flow = repositoryAdapter.findFlowByName(name);
      if (flow != null) {
        Annotation annotation = flow.getAnnotation();
        if (annotation == null) {
          annotation = new Annotation();
          flow.setAnnotation(annotation);
        }
        repositoryAdapter.addDocumentation(detail.getDocumentation(), null, annotation);
      }
    }
  }

  private void addGroup(Context context) {
    if (context instanceof DetailTable) {
      final DetailTable detailTable = (DetailTable) context;
      final String name = detailTable.getKey(NAME_POSITION);
      int tag = tagToInt(detailTable.getKeyValue("tag"));
      final String scenario = scenarioOrDefault(detailTable.getKeyValue("scenario"));

      final GroupType group = new GroupType();
      GroupType refComponent = null;
      if (referenceRepositoryAdapter != null) {
        refComponent = referenceRepositoryAdapter.findGroupByName(name, scenario);
      }
      if (refComponent != null) {
        tag = refComponent.getId().intValue();
      }
      if (tag == -1) {
        tag = assignId();
      }
      group.setId(BigInteger.valueOf(tag));
      group.setName(name);
      group.setScenario(scenario);
      detailTable.rows().get().findFirst().ifPresentOrElse(dt -> populateNumInGroup(dt, group),
          () -> logger.error("RepositoryBuilder unknown NumInGroup for group; name={}",
              group.getName()));

      final List<Object> members = group.getComponentRefOrGroupRefOrFieldRef();
      final Stream<? extends DetailProperties> rows = detailTable.rows().get();
      // skip the row for NumInGroup, already processed
      final Stream<? extends DetailProperties> remainingRows = rows.skip(1);
      addMembers(remainingRows, members);
      repositoryAdapter.addGroup(group);
    } else if (context instanceof Documentation) {
      final Documentation detail = (Documentation) context;
      final String name = detail.getKey(NAME_POSITION);
      final String scenario = scenarioOrDefault(detail.getKeyValue("scenario"));

      final GroupType group = repositoryAdapter.findGroupByName(name, scenario);
      if (group != null) {
        Annotation annotation = group.getAnnotation();
        if (annotation == null) {
          annotation = new Annotation();
          group.setAnnotation(annotation);
        }
        repositoryAdapter.addDocumentation(detail.getDocumentation(), null, annotation);
      }
    }
  }

  private void addMembers(Stream<? extends DetailProperties> stream, List<Object> members)
      throws IllegalArgumentException {
    stream.forEach(detail -> {
      final String tagStr = detail.getProperty("tag");
      if (tagStr != null && !tagStr.isEmpty()) {
        if ("group".startsWith(tagStr.toLowerCase())) {
          final GroupRefType groupRefType = populateGroupRef(detail);
          members.add(groupRefType);
        } else if ("component".startsWith(tagStr.toLowerCase())) {
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

  private void addMessageMembers(Context context) {
    if (context instanceof DetailTable) {
      final DetailTable detailTable = (DetailTable) context;
      final String name = detailTable.getKey(NAME_POSITION);
      final int tag = tagToInt(detailTable.getKeyValue("tag"));
      final String scenario = scenarioOrDefault(detailTable.getKeyValue("scenario"));
      final String msgType = detailTable.getKeyValue("type");
      final MessageType message = getOrAddMessage(name, scenario, tag, msgType);
      final String flow = detailTable.getKeyValue("flow");
      if (flow != null) {
        message.setFlow(flow);
      }

      final MessageType.Structure structure = new MessageType.Structure();
      message.setStructure(structure);
      final List<Object> members = structure.getComponentRefOrGroupRefOrFieldRef();
      addMembers(detailTable.rows().get(), members);
    } else if (context instanceof Documentation) {
      final Documentation detail = (Documentation) context;
      final String name = detail.getKey(NAME_POSITION);
      final String scenario = scenarioOrDefault(detail.getKeyValue("scenario"));
      final String scenarioOrDefault = scenarioOrDefault(scenario);
      final MessageType message = repositoryAdapter.findMessageByName(name, scenarioOrDefault);
      if (message != null) {
        Annotation annotation = message.getAnnotation();
        if (annotation == null) {
          annotation = new Annotation();
          message.setAnnotation(annotation);
        }
        repositoryAdapter.addDocumentation(detail.getDocumentation(), null, annotation);
      }
    }
  }

  private void addMessageResponses(Context context) {
    if (context instanceof DetailTable) {
      final Context messageContext = context.getParent();
      if (messageContext != null) {
        final String messageName = messageContext.getKeyValue("message");
        final int tag = tagToInt(messageContext.getKeyValue("tag"));
        final String scenario = scenarioOrDefault(messageContext.getKeyValue("scenario"));
        final String msgType = messageContext.getKeyValue("type");

        final MessageType message = getOrAddMessage(messageName, scenario, tag, msgType);
        final MessageType.Responses responses = new MessageType.Responses();
        message.setResponses(responses);
        final List<ResponseType> responseList = responses.getResponse();
        final DetailTable detailTable = (DetailTable) context;
        detailTable.rows().get().forEach(detail -> {
          final ResponseType response = new ResponseType();
          final List<Object> responseRefs = response.getMessageRefOrAssignOrTrigger();
          final MessageRefType messageRef = new MessageRefType();
          messageRef.setName(detail.getProperty("name"));

          final String refScenario = detail.getProperty("scenario");
          if (refScenario != null && !DEFAULT_SCENARIO.equals(refScenario)) {
            messageRef.setScenario(refScenario);
          }

          messageRef.setMsgType(detail.getProperty("msgType"));
          response.setWhen(detail.getProperty("when"));
          final String markdown = detail.getProperty("documentation");
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
        logger.error("RepositoryBuilder unknown message for responses; keys={}",
            String.join(" ", context.getKeys()));
      }
    }
  }


  private void addMetadata(Context context) {
    final String name = String.join(" ", context.getKeys());
    repositoryAdapter.setName(name);
    final String version = context.getKeyValue("version");
    if (version != null) {
      repositoryAdapter.setVersion(version);
    } else {
      repositoryAdapter.setVersion("1.0");
    }
    if (context instanceof DetailTable) {
      final DetailTable detailTable = (DetailTable) context;

      detailTable.rows().get().forEach(detail -> {
        final String term = detail.getProperty("term");
        final String value = detail.getProperty("value");
        repositoryAdapter.setMetadata(term, value);
      });

    }
  }

  private int assignId() {
    lastId++;
    return lastId;
  }

  private void createCodeset(String codesetName, String scenario, String type,
      String[] valueTokens) {
    final CodeSetType codeset = new CodeSetType();
    codeset.setId(BigInteger.valueOf(assignId()));
    codeset.setName(codesetName);
    if (!DEFAULT_SCENARIO.equals(scenario)) {
      codeset.setScenario(scenario);
    }
    if (type != null) {
      codeset.setType(type);
    }
    final List<CodeType> codes = codeset.getCode();
    for (int i = 0; i < valueTokens.length; i += 2) {
      final CodeType code = new CodeType();
      code.setValue(valueTokens[i]);
      code.setName(valueTokens[i + 1]);
      codes.add(code);
    }
    repositoryAdapter.addCodeset(codeset);
  }

  private void executeDefferedBuildSteps() {
    ElementBuilder builder;
    while ((builder = buildSteps.poll()) != null) {
      builder.build();
    }
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
        tag = assignId();
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
    final String name = detail.getProperty("name");
    final String scenario = scenarioOrDefault(detail.getProperty("scenario"));
    final ComponentRefType componentRefType = new ComponentRefType();

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
    final String presenceString = detail.getProperty("presence");
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
    } else {
      componentRefType.setPresence(presence);
    }

    if (!DEFAULT_SCENARIO.equals(scenario)) {
      componentRefType.setScenario(scenario);
    }

    return componentRefType;
  }

  private FieldRefType populateFieldRef(DetailProperties detail) {
    final FieldRefType fieldRefType = new FieldRefType();
    final Annotation annotation = new Annotation();
    String name = null;
    String presenceString = null;
    String valueString = null;

    for (Entry<String, String> p : detail.getProperties()) {
       switch (p.getKey().toLowerCase()) {
        case "name":
          name = p.getValue();
          break;
        case "presence":
          presenceString = p.getValue();
          break;
        case "values":
          valueString = p.getValue();
        case "tag":
        case "id":
          fieldRefType.setId(BigInteger.valueOf(tagToInt(p.getValue())));
          break;
        case "scenario":
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
        default:
          if (isDocumentationKey(p.getKey())) {
            repositoryAdapter.addDocumentation(p.getValue(), getPurpose(p.getKey()), annotation);
            fieldRefType.setAnnotation(annotation);
          } else {
            repositoryAdapter.addAppinfo(p.getValue(), p.getKey(), annotation);
            fieldRefType.setAnnotation(annotation);
          }
      }
    }

    final String scenario = scenarioOrDefault(detail.getProperty("scenario"));
    if (fieldRefType.getId() != null) {
      FieldType fieldType =
          repositoryAdapter.findFieldByTag(fieldRefType.getId().intValue(), scenario);
      if (fieldType == null) {
        buildSteps.add(new FieldBuilder(fieldRefType.getId().intValue(), name, scenario));
      }
    } else {
      FieldType fieldType = repositoryAdapter.findFieldByName(name, scenario);
      if (fieldType != null) {
        fieldRefType.setId(fieldType.getId());
      } else {
        fieldRefType.setId(BigInteger.ZERO);
        buildSteps.add(new FieldBuilder(0, name, scenario));
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
        if (presence == PresenceT.CONSTANT) {
          fieldRefType.setValue(valueString);
          if (!DEFAULT_SCENARIO.equals(scenario)) {
            fieldRefType.setScenario(scenario);
          }
        } else {
           final String[] valueTokens = valueString.split("[ =\t]");
          final String codesetName = name + "Codeset";
          // use the scenario of the parent element
          createCodeset(codesetName, scenario, DEFAULT_CODE_TYPE, valueTokens);
          logger.warn("RepositoryBuilder unknown codeset datatype; name={} scenario={}",
              codesetName, scenario);
        }
      }
    }
    return fieldRefType;
  }

  private GroupRefType populateGroupRef(DetailProperties detail) {
    final String name = detail.getProperty("name");
    final String scenario = scenarioOrDefault(detail.getProperty("scenario"));
    final GroupRefType groupRefType = new GroupRefType();

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
    final String presenceString = detail.getProperty("presence");
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
    } else {
      groupRefType.setPresence(presence);
    }

    if (!DEFAULT_SCENARIO.equals(scenario)) {
      groupRefType.setScenario(scenario);
    }

    return groupRefType;
  }

  private void populateNumInGroup(DetailProperties detail, GroupType group) {
    final FieldRefType numInGroup = populateFieldRef(detail);
    group.setNumInGroup(numInGroup);
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
  
  private boolean isDocumentationKey(String word) {
    for (PurposeEnum purpose : PurposeEnum.values()) {
      if (purpose.value().compareToIgnoreCase(word) == 0) {
        return true;
      }
    }
    if ("documentation".compareToIgnoreCase(word) == 0) {
      return true;
    } else {
      return false;
    }
  }
  
  private String getPurpose(String word) {
    for (PurposeEnum purpose : PurposeEnum.values()) {
      if (purpose.value().compareToIgnoreCase(word) == 0) {
        return word.toUpperCase();
      }
    }
    return null;
  }

  private int tagToInt(String str) {
    if (str == null) {
      return -1;
    }
    final int strLen = str.length();
    int end = strLen - 1;
    int begin = 0;

    while ((begin < strLen) && (str.charAt(begin) == ' ' || str.charAt(begin) == '\t'
        || str.charAt(begin) == '|' || str.charAt(begin) == '(')) {
      begin++;
    }
    while ((begin < end) && (str.charAt(end) == ' ' || str.charAt(end) == ')')) {
      end--;
    }
    final String str2 = ((begin > 0) || (end < strLen)) ? str.substring(begin, end + 1) : str;

    if (str2.isEmpty()) {
      return -1;
    } else {
      try {
        return Integer.parseInt(str2);
      } catch (final NumberFormatException e) {
        logger.warn("RepositoryBuilder numeric tag value expected, was {}", str);
        return -1;
      }
    }
  }
}
