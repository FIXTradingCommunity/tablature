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

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.purl.dc.elements._1.ObjectFactory;
import org.purl.dc.elements._1.SimpleLiteral;
import org.purl.dc.terms.ElementOrRefinementContainer;
import io.fixprotocol._2020.orchestra.repository.Annotation;
import io.fixprotocol._2020.orchestra.repository.CodeSetType;
import io.fixprotocol._2020.orchestra.repository.CodeSets;
import io.fixprotocol._2020.orchestra.repository.CodeType;
import io.fixprotocol._2020.orchestra.repository.ComponentRefType;
import io.fixprotocol._2020.orchestra.repository.ComponentRuleType;
import io.fixprotocol._2020.orchestra.repository.ComponentType;
import io.fixprotocol._2020.orchestra.repository.Components;
import io.fixprotocol._2020.orchestra.repository.Datatype;
import io.fixprotocol._2020.orchestra.repository.Datatypes;
import io.fixprotocol._2020.orchestra.repository.FieldRefType;
import io.fixprotocol._2020.orchestra.repository.FieldRuleType;
import io.fixprotocol._2020.orchestra.repository.FieldType;
import io.fixprotocol._2020.orchestra.repository.Fields;
import io.fixprotocol._2020.orchestra.repository.GroupRefType;
import io.fixprotocol._2020.orchestra.repository.GroupType;
import io.fixprotocol._2020.orchestra.repository.Groups;
import io.fixprotocol._2020.orchestra.repository.MessageType;
import io.fixprotocol._2020.orchestra.repository.Messages;
import io.fixprotocol._2020.orchestra.repository.PresenceT;
import io.fixprotocol._2020.orchestra.repository.Repository;
import io.fixprotocol.md.event.Context;
import io.fixprotocol.md.event.Detail;
import io.fixprotocol.md.event.DetailProperties;
import io.fixprotocol.md.event.DetailTable;
import io.fixprotocol.md.event.Documentation;
import io.fixprotocol.md.util.StringUtil;

class RepositoryBuilder implements Consumer<Context> {

  private static final String DEFAULT_CODE_TYPE = "char";
  private static final String DEFAULT_SCENARIO = "base";
  private static final String MARKDOWN_MEDIA_TYPE = "text/markdown";
  private static final int NAME_POSITION = 1;
  
  // todo: integrate into markdown grammar
  public static final String WHEN_KEYWORD = "when";
  public static final String ASSIGN_KEYWORD = "assign";

  private int id = 10000;
  private final Logger logger = LogManager.getLogger(getClass());
  private RepositoryBuilder reference = null;
  private final Repository repository;

  public RepositoryBuilder() {
    this.repository = createRepository();
  }

  public RepositoryBuilder(InputStream inputStream) throws JAXBException {
    this.repository = unmarshal(inputStream);
  }

  public RepositoryBuilder(Repository repository) {
    this.repository = repository;
  }

  @Override
  public void accept(Context context) {
    String type = context.getKey(0);
    if (type == null) {
      // log unknown type
    } else
      switch (type.toLowerCase()) {
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
        case "group":
          addGroup(context);
          break;
        case "message":
          addMessage(context);
          break;
        default:
          if (context.getLevel() == 1) {
            addMetadata(context);
          } else {
            logger.warn("RepositoryBuilder received unknown context type {}", type);
          }
      }
  }

  public void marshal(OutputStream os) throws JAXBException {
    final JAXBContext jaxbContext = JAXBContext.newInstance(Repository.class);
    final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
    jaxbMarshaller.setProperty("jaxb.formatted.output", true);
    jaxbMarshaller.marshal(repository, os);
  }

  public void setReference(RepositoryBuilder reference) {
    this.reference = reference;
  }

  private void addCodeset(Context context) {
    if (context instanceof DetailTable) {
      DetailTable detailTable = (DetailTable) context;
      String name = detailTable.getKey(NAME_POSITION);
      int tag = tagToInt(detailTable.getKeyValue("tag"));
      String scenario = scenarioOrDefault(detailTable.getKeyValue("scenario"));

      CodeSetType codeset = new CodeSetType();
      CodeSetType refCodeset = null;
      if (reference != null) {
        refCodeset = reference.findCodesetByName(name, scenario);
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
        logger.error("RepositoryBuilder unknown CodeSet type; name={}", name);
      }
      repository.getCodeSets().getCodeSet().add(codeset);

      List<CodeType> codes = codeset.getCode();
      detailTable.rows().get().forEach(detail -> {
        String codeName = StringUtil.stripCell(detail.getProperty("name"));
        String codeValue = StringUtil.stripCell(detail.getProperty("value"));
        int codeTag = tagToInt(detail.getProperty("tag"));
        if (codeTag == -1) {
          codeTag = assignId();
        }
        CodeType codeType = new CodeType();
        codeType.setName(codeName);
        codeType.setValue(codeValue);
        codeType.setId(BigInteger.valueOf(codeTag));
        codes.add(codeType);
        String documentation = StringUtil.stripCell(detail.getProperty("documentation"));
        if (documentation != null && !documentation.isEmpty()) {
          Annotation annotation = codeType.getAnnotation();
          if (annotation == null) {
            annotation = new Annotation();
            codeType.setAnnotation(annotation);
          }
          addDocumentation(documentation, annotation);
        }
      });
    } else if (context instanceof Documentation) {
      Documentation detail = (Documentation) context;
      String name = detail.getKey(NAME_POSITION);
      String scenario = scenarioOrDefault(detail.getKeyValue("scenario"));

      CodeSetType codeset = this.findCodesetByName(name, scenario);
      if (codeset != null) {
        Annotation annotation = codeset.getAnnotation();
        if (annotation == null) {
          annotation = new Annotation();
          codeset.setAnnotation(annotation);
        }
        updateDocumentation(detail.getDocumentation(), annotation);
      }
    }
  }

  private void addComponent(Context context) {
    if (context instanceof DetailTable) {
      DetailTable detailTable = (DetailTable) context;
      String name = detailTable.getKey(NAME_POSITION);
      int tag = tagToInt(detailTable.getKeyValue("tag"));
      String scenario = scenarioOrDefault(detailTable.getKeyValue("scenario"));

      ComponentType component = new ComponentType();
      ComponentType refComponent = null;
      if (reference != null) {
        refComponent = reference.findComponentByName(name, scenario);
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
      List<Object> members = component.getComponentRefOrGroupRefOrFieldRef();
      addMembers(detailTable.rows().get(), members);
      repository.getComponents().getComponent().add(component);

    } else if (context instanceof Documentation) {
      Documentation detail = (Documentation) context;
      String name = detail.getKey(NAME_POSITION);
      String scenario = scenarioOrDefault(detail.getKeyValue("scenario"));

      ComponentType component = this.findComponentByName(name, scenario);
      if (component != null) {
        Annotation annotation = component.getAnnotation();
        if (annotation == null) {
          annotation = new Annotation();
          component.setAnnotation(annotation);
        }
        updateDocumentation(detail.getDocumentation(), annotation);
      }
    }
  }

  private void addDatatype(Context context) {
    if (context instanceof Detail) {
      Detail detail = (Detail) context;
      String name = detail.getProperty("name");
      io.fixprotocol._2020.orchestra.repository.Datatype datatype =
          new io.fixprotocol._2020.orchestra.repository.Datatype();
      datatype.setName(name);
      repository.getDatatypes().getDatatype().add(datatype);
    }
  }

  private void addDocumentation(String doc, Annotation annotation) {
    List<Object> elements = annotation.getDocumentationOrAppinfo();
    io.fixprotocol._2020.orchestra.repository.Documentation documentation =
        new io.fixprotocol._2020.orchestra.repository.Documentation();
    documentation.setContentType(MARKDOWN_MEDIA_TYPE);
    documentation.getContent().add(doc);
    elements.add(documentation);
  }

  private void addField(Context context) {
    if (context instanceof Detail) {
      Detail detail = (Detail) context;
      int tag = tagToInt(detail.getProperty("tag"));
      String name = detail.getProperty("name");
      String scenario = scenarioOrDefault(StringUtil.stripCell(detail.getProperty("scenario")));
      final String type = detail.getProperty("type");
      FieldType field = new FieldType();
      field.setId(BigInteger.valueOf(tag));
      field.setName(name);

      if (!DEFAULT_SCENARIO.equals(scenario)) {
        field.setScenario(scenario);
      }

      String values = detail.getProperty("values");
      if (values != null && !values.isEmpty()) {
        String[] valueTokens = values.split("[ =\t]");
        String codesetName = name + "Codeset";
        createCodeset(codesetName, scenario, type, valueTokens);
        field.setType(codesetName);
      } else {
        field.setType(type);
      }

      String documentation = detail.getProperty("documentation");
      if (documentation != null) {
        Annotation annotation = field.getAnnotation();
        if (annotation == null) {
          annotation = new Annotation();
          field.setAnnotation(annotation);
        }
        addDocumentation(documentation, annotation);
      }
      addFieldAndType(field);
    }
  }

  private void addFieldAndType(FieldType field) {
    List<FieldType> fields = repository.getFields().getField();
    fields.add(field);

    final String type = field.getType();
    final String scenario = field.getScenario();
    addType(type, scenario);
  }

  private void addGroup(Context context) {
    if (context instanceof DetailTable) {
      DetailTable detailTable = (DetailTable) context;
      String name = detailTable.getKey(NAME_POSITION);
      int tag = tagToInt(detailTable.getKeyValue("tag"));
      String scenario = scenarioOrDefault(detailTable.getKeyValue("scenario"));

      GroupType group = new GroupType();
      GroupType refComponent = null;
      if (reference != null) {
        refComponent = reference.findGroupByName(name, scenario);
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
              () -> logger.error("RepositoryBuilder unknown NumInGroup for group; name={}", group.getName())
      );
      
      List<Object> members = group.getComponentRefOrGroupRefOrFieldRef();
      Stream<? extends DetailProperties> rows = detailTable.rows().get();
      // skip the row for NumInGroup, already processed
      Stream<? extends DetailProperties> remainingRows = rows.skip(1);
      addMembers(remainingRows, members);
      repository.getGroups().getGroup().add(group);
    } else if (context instanceof Documentation) {
      Documentation detail = (Documentation) context;
      String name = detail.getKey(NAME_POSITION);
      String scenario = scenarioOrDefault(detail.getKeyValue("scenario"));

      GroupType group = this.findGroupByName(name, scenario);
      if (group != null) {
        Annotation annotation = group.getAnnotation();
        if (annotation == null) {
          annotation = new Annotation();
          group.setAnnotation(annotation);
        }
        updateDocumentation(detail.getDocumentation(), annotation);
      }
    }
  }

  private void addMembers(Stream<? extends DetailProperties> stream, List<Object> members)
      throws IllegalArgumentException {
    stream.forEach(detail -> {
      String tagStr = StringUtil.stripCell(detail.getProperty("tag"));
      if (tagStr != null && !tagStr.isEmpty()) {
        if ("group".startsWith(tagStr.toLowerCase())) {
          GroupRefType groupRefType = populateGroupRef(detail);
          members.add(groupRefType);
        } else if ("component".startsWith(tagStr.toLowerCase())) {
          ComponentRefType componentRefType = populateComponentRef(detail);
          members.add(componentRefType);
        } else {
          FieldRefType fieldRefType = populateFieldRef(detail);
          members.add(fieldRefType);
        }
      } else {
        FieldRefType fieldRefType = populateFieldRef(detail);
        members.add(fieldRefType);
      }
    });
  }

  private void addMessage(Context context) {
    if (context instanceof DetailTable) {
      DetailTable detailTable = (DetailTable) context;
      String name = detailTable.getKey(NAME_POSITION);
      int tag = tagToInt(detailTable.getKeyValue("tag"));
      String scenario = scenarioOrDefault(detailTable.getKeyValue("scenario"));
      String msgType = detailTable.getKeyValue("type");

      MessageType message = new MessageType();
      MessageType refMessage = null;
      if (reference != null) {
        refMessage = reference.findMessageByName(name, scenario);
      }

      if (tag == -1 && refMessage != null) {
        tag = refMessage.getId().intValue();
      }
      if (tag == -1) {
        tag = assignId();
      }
      message.setId(BigInteger.valueOf(tag));
      message.setName(name);
      if (!DEFAULT_SCENARIO.equals(scenario)) {
        message.setScenario(scenario);
      }
      if (msgType == null && refMessage != null) {
        msgType = refMessage.getMsgType();
      }
      if (msgType != null) {
        message.setMsgType(msgType);
      }
      final MessageType.Structure structure = new MessageType.Structure();
      message.setStructure(structure);
      List<Object> members = structure.getComponentRefOrGroupRefOrFieldRef();
      addMembers(detailTable.rows().get(), members);
      repository.getMessages().getMessage().add(message);

    } else if (context instanceof Documentation) {
      Documentation detail = (Documentation) context;
      String name = detail.getKey(NAME_POSITION);
      String scenario = scenarioOrDefault(detail.getKeyValue("scenario"));

      MessageType message = this.findMessageByName(name, scenario);
      if (message != null) {
        Annotation annotation = message.getAnnotation();
        if (annotation == null) {
          annotation = new Annotation();
          message.setAnnotation(annotation);
        }
        updateDocumentation(detail.getDocumentation(), annotation);
      }
    }
  }

  private void addMetadata(Context context) {
    final String name = String.join(" ", context.getKeys());
    repository.setName(name);
    String version = context.getKeyValue("version");
    if (version != null) {
      repository.setVersion(version);
    }
    if (context instanceof Documentation) {
      Documentation detail = (Documentation) context;
      ElementOrRefinementContainer container = repository.getMetadata();
      List<JAXBElement<SimpleLiteral>> literals = container.getAny();
      ObjectFactory objectFactory = new ObjectFactory();
      SimpleLiteral title = new SimpleLiteral();
      title.getContent().add(name);
      literals.add(objectFactory.createTitle(title));

      SimpleLiteral description = new SimpleLiteral();
      description.getContent().add(detail.getDocumentation());
      literals.add(objectFactory.createDescription(description));
    }
  }

  private void addType(final String type, String scenario) {
    boolean found = false;
    io.fixprotocol._2020.orchestra.repository.Datatype datatype = this.findDatatypeByName(type);
    if (datatype != null) {
      found = true;
    } else if (reference != null) {
      datatype = reference.findDatatypeByName(type);
      if (datatype != null) {
        copyDatatype(datatype);
        found = true;
      }
    }
    if (!found) {
      CodeSetType codeset = this.findCodesetByName(type, scenario);
      if (codeset == null && reference != null) {
        codeset = reference.findCodesetByName(type, scenario);
        if (codeset != null) {
          copyCodeset(codeset);
        }
      }
    }
  }

  private int assignId() {
    id++;
    return id;
  }

  private void copyCodeset(CodeSetType codeset) {
    repository.getCodeSets().getCodeSet().add((CodeSetType) codeset.clone());
  }

  private void copyComponent(ComponentType componentType) {
    repository.getComponents().getComponent().add((ComponentType) componentType.clone());
    List<Object> members = componentType.getComponentRefOrGroupRefOrFieldRef();
    copyMembers(members);
  }

  private void copyMembers(List<Object> members) {
    for (Object member : members) {
      if (member instanceof FieldRefType) {
        FieldRefType fieldRef = (FieldRefType) member;
        FieldType field = this.findFieldByTag(fieldRef.getId().intValue(), fieldRef.getScenario());
        if (field == null) {
          field = reference.findFieldByTag(fieldRef.getId().intValue(), fieldRef.getScenario());
          if (field != null) {
            copyFieldAndType(field);
          } else {
            logger.error("RepositoryBuilder unknown field; id={} scenario={}", 
                fieldRef.getId().intValue(), fieldRef.getScenario());
          }
        }
      } else if (member instanceof GroupRefType) {
        GroupRefType groupRef = (GroupRefType) member;
        GroupType group = this.findGroupByTag(groupRef.getId().intValue(), groupRef.getScenario());
        if (group == null) {
          group = reference.findGroupByTag(groupRef.getId().intValue(), groupRef.getScenario());
          if (group != null) {
            copyGroup(group);
          } else {
            logger.error("RepositoryBuilder unknown group; id={} scenario={}", 
                groupRef.getId().intValue(), groupRef.getScenario());
          }
        }
        }else if (member instanceof ComponentRefType) {
        ComponentRefType componentRef = (ComponentRefType) member;
        ComponentType component = this.findComponentByTag(componentRef.getId().intValue(), componentRef.getScenario());
        if (component == null) {
          component = reference.findComponentByTag(componentRef.getId().intValue(), componentRef.getScenario());
          if (component != null) {
            copyComponent(component);
          } else {
            logger.error("RepositoryBuilder unknown component; id={} scenario={}", 
                componentRef.getId().intValue(), componentRef.getScenario());
          }
        }
      }
    }
  }

  private void copyDatatype(Datatype datatype) {
    repository.getDatatypes().getDatatype().add((Datatype) datatype.clone());
  }

  private void copyFieldAndType(FieldType fieldType) {
    repository.getFields().getField().add((FieldType) fieldType.clone());
    final String type = fieldType.getType();
    final String scenario = fieldType.getScenario();
    addType(type, scenario);
  }

  private void copyGroup(GroupType componentType) {
    repository.getGroups().getGroup().add((GroupType) componentType.clone());
    List<Object> members = componentType.getComponentRefOrGroupRefOrFieldRef();
    copyMembers(members);
  }

  private void createCodeset(String codesetName, String scenario, String type,
      String[] valueTokens) {
    CodeSetType codeset = new CodeSetType();
    codeset.setId(BigInteger.valueOf(assignId()));
    codeset.setName(codesetName);
    if (!DEFAULT_SCENARIO.equals(scenario)) {
      codeset.setScenario(scenario);
    }
    if (type != null) {
      codeset.setType(type);
    }
    List<CodeType> codes = codeset.getCode();
    for (int i = 0; i < valueTokens.length; i += 2) {
      final CodeType code = new CodeType();
      code.setValue(valueTokens[i]);
      code.setName(valueTokens[i + 1]);
      codes.add(code);
    }
    repository.getCodeSets().getCodeSet().add(codeset);
  }

  private Repository createRepository() {
    Repository repository = new Repository();
    repository.setMetadata(new ElementOrRefinementContainer());
    repository.setDatatypes(new Datatypes());
    repository.setCodeSets(new CodeSets());
    repository.setFields(new Fields());
    repository.setMessages(new Messages());
    repository.setComponents(new Components());
    repository.setGroups(new Groups());
    return repository;
  }

  private ComponentRefType populateComponentRef(DetailProperties detail) {
    String name = StringUtil.stripCell(detail.getProperty("name"));
    String scenario = scenarioOrDefault(StringUtil.stripCell(detail.getProperty("scenario")));
    ComponentRefType componentRefType = new ComponentRefType();

    ComponentType componentType = this.findComponentByName(name, scenario);
    if (componentType != null) {
      componentRefType.setId(componentType.getId());
    } else if (reference != null) {
      componentType = reference.findComponentByName(name, scenario);
      if (componentType != null) {
        componentRefType.setId(componentType.getId());
        copyComponent(componentType);
      } else {
        // Component not found, but write reference to be corrected later
        componentRefType.setId(BigInteger.ZERO);
        logger.error("RepositoryBuilder unknown componentRef id; name={}", name);
      }
    }

    List<ComponentRuleType> rules = componentRefType.getRule();

    String presenceString = detail.getProperty("presence");
    String[] presenceWords = presenceString.split("[ \t]");
    PresenceT presence = null;
    boolean inWhen = false;
    List<String> whenWords = new ArrayList<>();
    for (String word : presenceWords) {
      if (isPresence(word)) {
        if (!whenWords.isEmpty()) {
          ComponentRuleType rule = new ComponentRuleType();
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
      ComponentRuleType rule = new ComponentRuleType();
      rule.setPresence(presence);
      rule.setWhen(String.join(" ", whenWords));
      rules.add(rule);
    }

    if (!DEFAULT_SCENARIO.equals(scenario)) {
      componentRefType.setScenario(scenario);
    }

    return componentRefType;
  }

  private FieldRefType populateFieldRef(DetailProperties detail) {
    String name = StringUtil.stripCell(detail.getProperty("name"));
    String scenario = scenarioOrDefault(StringUtil.stripCell(detail.getProperty("scenario")));
    int tag = tagToInt(detail.getProperty("tag"));

    FieldRefType fieldRefType = new FieldRefType();

    FieldType fieldType;
    if (tag != -1) {
      fieldType = this.findFieldByTag(tag, scenario);
    } else {
      fieldType = this.findFieldByName(name, scenario);
    }

    if (fieldType == null && reference != null) {
      fieldType = reference.findFieldByName(name, scenario);
      if (fieldType != null) {
        copyFieldAndType(fieldType);
      }
    }

    if (tag == -1 && fieldType != null) {
      tag = fieldType.getId().intValue();
    }

    if (tag != -1) {
      fieldRefType.setId(BigInteger.valueOf(tag));
    } else {
      // Field not found, but write reference to be corrected later
      fieldRefType.setId(BigInteger.ZERO);
      logger.error("RepositoryBuilder unknown fieldRef id; name={}", name);
    }

    List<FieldRuleType> rules = fieldRefType.getRule();

    String presenceString = detail.getProperty("presence");
    String[] presenceWords = presenceString.split("[ \t]");
    PresenceT presence = null;
    boolean inWhen = false;
    List<String> whenWords = new ArrayList<>();
    for (String word : presenceWords) {
      if (isPresence(word)) {
        if (!whenWords.isEmpty()) {
          FieldRuleType rule = new FieldRuleType();
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
      fieldRefType.setPresence(presence);
    } else if (!whenWords.isEmpty()) {
      FieldRuleType rule = new FieldRuleType();
      rule.setPresence(presence);
      rule.setWhen(String.join(" ", whenWords));
      rules.add(rule);
    }

    String values = StringUtil.stripCell(detail.getProperty("values"));
    if (values != null && !values.isEmpty()) {
      int keywordPos = values.indexOf(ASSIGN_KEYWORD);
      if (keywordPos != -1) {       
        fieldRefType.setAssign(values.substring(keywordPos + ASSIGN_KEYWORD.length() + 1));
      } else if (values != null && !values.isEmpty()) {
        if (presence == PresenceT.CONSTANT) {
          fieldRefType.setValue(values);
          if (!DEFAULT_SCENARIO.equals(scenario)) {
            fieldRefType.setScenario(scenario);
          }
        } else {
          // use the scenario of the parent element
          if (DEFAULT_SCENARIO.equals(scenario)) {
            scenario = detail.getContext().getKeyValue("scenario");
          }
          String[] valueTokens = values.split("[ =\t]");
          String codesetName = name + "Codeset";
          createCodeset(codesetName, scenario, DEFAULT_CODE_TYPE, valueTokens);
          logger.warn("RepositoryBuilder unknown codeset datatype; name={} scenario={}",
              codesetName, scenario);
        }
      }
    }

    if (!DEFAULT_SCENARIO.equals(scenario)) {
      fieldRefType.setScenario(scenario);
    }

    return fieldRefType;
  }

  private GroupRefType populateGroupRef(DetailProperties detail) {
    String name = StringUtil.stripCell(detail.getProperty("name"));
    String scenario = scenarioOrDefault(StringUtil.stripCell(detail.getProperty("scenario")));
    GroupRefType groupRefType = new GroupRefType();

    GroupType groupType = this.findGroupByName(name, scenario);
    if (groupType != null) {
      groupRefType.setId(groupType.getId());
    } else if (reference != null) {
      groupType = reference.findGroupByName(name, scenario);
      if (groupType != null) {
        groupRefType.setId(groupType.getId());
        copyGroup(groupType);
      } else {
        // Group not found, but write reference to be corrected later
        groupRefType.setId(BigInteger.ZERO);
        logger.error("RepositoryBuilder unknown groupRef id; name={}", name);
      }
    }

    List<ComponentRuleType> rules = groupRefType.getRule();

    String presenceString = detail.getProperty("presence");
    String[] presenceWords = presenceString.split("[ \t]");
    PresenceT presence = null;
    boolean inWhen = false;
    List<String> whenWords = new ArrayList<>();
    for (String word : presenceWords) {
      if (isPresence(word)) {
        if (!whenWords.isEmpty()) {
          ComponentRuleType rule = new ComponentRuleType();
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
      ComponentRuleType rule = new ComponentRuleType();
      rule.setPresence(presence);
      rule.setWhen(String.join(" ", whenWords));
      rules.add(rule);
    }

    if (!DEFAULT_SCENARIO.equals(scenario)) {
      groupRefType.setScenario(scenario);
    }

    return groupRefType;
  }

  private void populateNumInGroup(DetailProperties detail, GroupType group) {
    FieldRefType numInGroup = populateFieldRef(detail);
    group.setNumInGroup(numInGroup);
  }

  private PresenceT stringToPresence(String word) {
    if (word == null || word.isEmpty()) {
      return PresenceT.OPTIONAL;
    } else {
      String lcWord = word.toLowerCase();
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
  
  private boolean isPresence(String word) {
    if (word == null || word.isEmpty()) {
      return false;
    } else {
      String lcWord = word.toLowerCase();
      return "required".startsWith(lcWord) || "optional".startsWith(lcWord)
          || "forbidden".startsWith(lcWord) || "ignored".startsWith(lcWord)
          || "constant".startsWith(lcWord);
    }
  }

  private String scenarioOrDefault(String scenario) {
    return (scenario != null && !scenario.isEmpty()) ? scenario : DEFAULT_SCENARIO;
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
    String str2 = ((begin > 0) || (end < strLen)) ? str.substring(begin, end + 1) : str;

    if (str2.isEmpty()) {
      return -1;
    } else {
      return Integer.parseInt(str2);
    }
  }

  private Repository unmarshal(InputStream is) throws JAXBException {
    final JAXBContext jaxbContext = JAXBContext.newInstance(Repository.class);
    final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    return (Repository) jaxbUnmarshaller.unmarshal(is);
  }

  private void updateDocumentation(String doc, Annotation annotation) {
    List<Object> elements = annotation.getDocumentationOrAppinfo();
    elements.clear();
    addDocumentation(doc, annotation);
  }

  CodeSetType findCodesetByName(String name, String scenario) {
    List<CodeSetType> codesets = repository.getCodeSets().getCodeSet();
    for (CodeSetType codeset : codesets) {
      if (codeset.getName().equals(name) && codeset.getScenario().equals(scenario)) {
        return codeset;
      }
    }
    return null;
  }

  ComponentType findComponentByName(String name, String scenario) {
    List<ComponentType> components = repository.getComponents().getComponent();
    for (ComponentType component : components) {
      if (component.getName().equals(name) && component.getScenario().equals(scenario)) {
        return component;
      }
    }
    return null;
  }
  
  ComponentType findComponentByTag(int tag, String scenario) {
    List<ComponentType> components = repository.getComponents().getComponent();
    for (ComponentType component : components) {
      if (component.getId().intValue() == tag && component.getScenario().equals(scenario)) {
        return component;
      }
    }
    return null;
  }

  io.fixprotocol._2020.orchestra.repository.Datatype findDatatypeByName(String name) {
    List<io.fixprotocol._2020.orchestra.repository.Datatype> datatypes =
        repository.getDatatypes().getDatatype();
    for (io.fixprotocol._2020.orchestra.repository.Datatype datatype : datatypes) {
      if (datatype.getName().equals(name)) {
        return datatype;
      }
    }
    return null;
  }

  FieldType findFieldByName(String name, String scenario) {
    List<FieldType> fields = repository.getFields().getField();
    for (FieldType field : fields) {
      if (field.getName().equals(name) && field.getScenario().equals(scenario)) {
        return field;
      }
    }
    return null;
  }

  FieldType findFieldByTag(int tag, String scenario) {
    List<FieldType> fields = repository.getFields().getField();
    for (FieldType field : fields) {
      if (field.getId().intValue() == tag && field.getScenario().equals(scenario)) {
        return field;
      }
    }
    return null;
  }

  GroupType findGroupByName(String name, String scenario) {
    List<GroupType> components = repository.getGroups().getGroup();
    for (GroupType component : components) {
      if (component.getName().equals(name) && component.getScenario().equals(scenario)) {
        return component;
      }
    }
    return null;
  }
  
  GroupType findGroupByTag(int tag, String scenario) {
    List<GroupType> components = repository.getGroups().getGroup();
    for (GroupType component : components) {
      if (component.getId().intValue() == tag && component.getScenario().equals(scenario)) {
        return component;
      }
    }
    return null;
  }

  MessageType findMessageByName(String name, String scenario) {
    List<MessageType> messages = repository.getMessages().getMessage();
    for (MessageType message : messages) {
      if (message.getName().equals(name) && message.getScenario().equals(scenario)) {
        return message;
      }
    }
    return null;
  }
}
