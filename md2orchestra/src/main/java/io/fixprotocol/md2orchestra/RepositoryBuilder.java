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
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import io.fixprotocol._2020.orchestra.repository.MessageRefType;
import io.fixprotocol._2020.orchestra.repository.MessageType;
import io.fixprotocol._2020.orchestra.repository.Messages;
import io.fixprotocol._2020.orchestra.repository.PresenceT;
import io.fixprotocol._2020.orchestra.repository.Repository;
import io.fixprotocol._2020.orchestra.repository.ResponseType;
import io.fixprotocol.md.event.Context;
import io.fixprotocol.md.event.Detail;
import io.fixprotocol.md.event.DetailProperties;
import io.fixprotocol.md.event.DetailTable;
import io.fixprotocol.md.event.Documentation;

class RepositoryBuilder implements Consumer<Context> {

  public static final String ASSIGN_KEYWORD = "assign";
  // todo: integrate into markdown grammar
  public static final String WHEN_KEYWORD = "when";
  // sorted array of valid Dublin Core Terms
  private static final String[] dcTerms =
      new String[] {"contributor", "coverage", "creator", "date", "description", "format",
          "identifier", "language", "publisher", "relation", "rights", "source", "subject", "type"};
  private static final String DEFAULT_CODE_TYPE = "char";

  private static final String DEFAULT_SCENARIO = "base";
  private static final String MARKDOWN_MEDIA_TYPE = "text/markdown";

  private static final int NAME_POSITION = 1;
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
    final String type = context.getKey(0);
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
          addMessageMembers(context);
          break;
        case "responses":
          addMessageResponses(context);
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
      final DetailTable detailTable = (DetailTable) context;
      final String name = detailTable.getKey(NAME_POSITION);
      int tag = tagToInt(detailTable.getKeyValue("tag"));
      final String scenario = scenarioOrDefault(detailTable.getKeyValue("scenario"));

      final CodeSetType codeset = new CodeSetType();
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

      final List<CodeType> codes = codeset.getCode();
      detailTable.rows().get().forEach(detail -> {
        final String codeName = detail.getProperty("name");
        final String codeValue = detail.getProperty("value");
        int codeTag = tagToInt(detail.getProperty("tag"));
        if (codeTag == -1) {
          codeTag = assignId();
        }
        final CodeType codeType = new CodeType();
        codeType.setName(codeName);
        codeType.setValue(codeValue);
        codeType.setId(BigInteger.valueOf(codeTag));
        codes.add(codeType);
        final String markdown = detail.getProperty("documentation");
        if (markdown != null && !markdown.isEmpty()) {
          Annotation annotation = codeType.getAnnotation();
          if (annotation == null) {
            annotation = new Annotation();
            codeType.setAnnotation(annotation);
          }
          addDocumentation(markdown, annotation);
        }
      });
    } else if (context instanceof Documentation) {
      final Documentation detail = (Documentation) context;
      final String name = detail.getKey(NAME_POSITION);
      final String scenario = scenarioOrDefault(detail.getKeyValue("scenario"));

      final CodeSetType codeset = this.findCodesetByName(name, scenario);
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
      final DetailTable detailTable = (DetailTable) context;
      final String name = detailTable.getKey(NAME_POSITION);
      int tag = tagToInt(detailTable.getKeyValue("tag"));
      final String scenario = scenarioOrDefault(detailTable.getKeyValue("scenario"));

      final ComponentType component = new ComponentType();
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
      final List<Object> members = component.getComponentRefOrGroupRefOrFieldRef();
      addMembers(detailTable.rows().get(), members);
      repository.getComponents().getComponent().add(component);

    } else if (context instanceof Documentation) {
      final Documentation detail = (Documentation) context;
      final String name = detail.getKey(NAME_POSITION);
      final String scenario = scenarioOrDefault(detail.getKeyValue("scenario"));

      final ComponentType component = this.findComponentByName(name, scenario);
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
      final Detail detail = (Detail) context;
      final String name = detail.getProperty("name");
      final io.fixprotocol._2020.orchestra.repository.Datatype datatype =
          new io.fixprotocol._2020.orchestra.repository.Datatype();
      datatype.setName(name);
      repository.getDatatypes().getDatatype().add(datatype);
    }
  }

  private void addDocumentation(String markdown, Annotation annotation) {
    final List<Object> elements = annotation.getDocumentationOrAppinfo();
    final io.fixprotocol._2020.orchestra.repository.Documentation documentation =
        new io.fixprotocol._2020.orchestra.repository.Documentation();
    documentation.setContentType(MARKDOWN_MEDIA_TYPE);
    documentation.getContent().add(markdown);
    elements.add(documentation);
  }

  private void addField(Context context) {
    if (context instanceof Detail) {
      final Detail detail = (Detail) context;
      final int tag = tagToInt(detail.getProperty("tag"));
      final String name = detail.getProperty("name");
      final String scenario = scenarioOrDefault(detail.getProperty("scenario"));
      final String type = detail.getProperty("type");
      final FieldType field = new FieldType();
      field.setId(BigInteger.valueOf(tag));
      field.setName(name);

      if (!DEFAULT_SCENARIO.equals(scenario)) {
        field.setScenario(scenario);
      }

      final String values = detail.getProperty("values");
      if (values != null && !values.isEmpty()) {
        final String[] valueTokens = values.split("[ =\t]");
        final String codesetName = name + "Codeset";
        createCodeset(codesetName, scenario, type, valueTokens);
        field.setType(codesetName);
      } else {
        field.setType(type);
      }

      final String encoding = detail.getProperty("encoding");
      if (encoding != null) {
        field.setEncoding(encoding);
      }

      final Integer minLength = detail.getIntProperty("implMinLength");
      if (minLength != null) {
        field.setImplMinLength(minLength.shortValue());
      }

      final Integer maxLength = detail.getIntProperty("implMaxLength");
      if (maxLength != null) {
        field.setImplMaxLength(maxLength.shortValue());
      }

      final Integer length = detail.getIntProperty("implLength");
      if (maxLength != null) {
        field.setImplLength(length.shortValue());
      }

      final String markdown = detail.getProperty("documentation");
      if (markdown != null) {
        Annotation annotation = field.getAnnotation();
        if (annotation == null) {
          annotation = new Annotation();
          field.setAnnotation(annotation);
        }
        addDocumentation(markdown, annotation);
      }
      addFieldAndType(field);
    }
  }

  private void addFieldAndType(FieldType field) {
    final List<FieldType> fields = repository.getFields().getField();
    fields.add(field);

    final String type = field.getType();
    final String scenario = field.getScenario();
    addType(type, scenario);
  }

  private void addGroup(Context context) {
    if (context instanceof DetailTable) {
      final DetailTable detailTable = (DetailTable) context;
      final String name = detailTable.getKey(NAME_POSITION);
      int tag = tagToInt(detailTable.getKeyValue("tag"));
      final String scenario = scenarioOrDefault(detailTable.getKeyValue("scenario"));

      final GroupType group = new GroupType();
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
          () -> logger.error("RepositoryBuilder unknown NumInGroup for group; name={}",
              group.getName()));

      final List<Object> members = group.getComponentRefOrGroupRefOrFieldRef();
      final Stream<? extends DetailProperties> rows = detailTable.rows().get();
      // skip the row for NumInGroup, already processed
      final Stream<? extends DetailProperties> remainingRows = rows.skip(1);
      addMembers(remainingRows, members);
      repository.getGroups().getGroup().add(group);
    } else if (context instanceof Documentation) {
      final Documentation detail = (Documentation) context;
      final String name = detail.getKey(NAME_POSITION);
      final String scenario = scenarioOrDefault(detail.getKeyValue("scenario"));

      final GroupType group = this.findGroupByName(name, scenario);
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
      int tag = tagToInt(detailTable.getKeyValue("tag"));
      final String scenario = scenarioOrDefault(detailTable.getKeyValue("scenario"));
      String msgType = detailTable.getKeyValue("type");
      MessageType message = getOrAddMessage(name, scenario, tag, msgType);

      final MessageType.Structure structure = new MessageType.Structure();
      message.setStructure(structure);
      final List<Object> members = structure.getComponentRefOrGroupRefOrFieldRef();
      addMembers(detailTable.rows().get(), members);
    } else if (context instanceof Documentation) {
      final Documentation detail = (Documentation) context;
      final String name = detail.getKey(NAME_POSITION);
      final String scenario = scenarioOrDefault(detail.getKeyValue("scenario"));
      final String scenarioOrDefault = scenarioOrDefault(scenario);
      final MessageType message = this.findMessageByName(name, scenarioOrDefault);
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

  private MessageType getOrAddMessage(String name, String scenario, int tag, String msgType) {
    final String scenarioOrDefault = scenarioOrDefault(scenario);
    MessageType message = this.findMessageByName(name, scenarioOrDefault);
    if (message == null) {
      message = new MessageType();
      MessageType refMessage = null;
      if (reference != null) {
        refMessage = reference.findMessageByName(name, scenarioOrDefault);
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
      if (!repository.getMessages().getMessage().contains(message)) {
        repository.getMessages().getMessage().add(message);
      }
    }
    return message;
  }

  private void addMessageResponses(Context context) {
    if (context instanceof DetailTable) {
      Context messageContext = context.getParent();
      if (messageContext != null) {
        String messageName = messageContext.getKeyValue("message");
        int tag = tagToInt(messageContext.getKeyValue("tag"));
        String scenario = scenarioOrDefault(messageContext.getKeyValue("scenario"));
        String msgType = messageContext.getKeyValue("type");

        MessageType message = getOrAddMessage(messageName, scenario, tag, msgType);
        MessageType.Responses responses = new MessageType.Responses();
        message.setResponses(responses);
        List<ResponseType> responseList = responses.getResponse();
        final DetailTable detailTable = (DetailTable) context;
        detailTable.rows().get().forEach(detail -> {
          ResponseType response = new ResponseType();
          final List<Object> responseRefs = response.getMessageRefOrAssignOrTrigger();
          MessageRefType messageRef = new MessageRefType();
          messageRef.setName(detail.getProperty("name"));

          final String refScenario = detail.getProperty("scenario");
          if (!DEFAULT_SCENARIO.equals(refScenario) && !refScenario.isBlank()) {
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
            addDocumentation(markdown, annotation);
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
    repository.setName(name);
    final String version = context.getKeyValue("version");
    if (version != null) {
      repository.setVersion(version);
    }
    if (context instanceof DetailTable) {
      DetailTable detailTable = (DetailTable) context;
      final ElementOrRefinementContainer container = repository.getMetadata();
      final List<JAXBElement<SimpleLiteral>> literals = container.getAny();

      detailTable.rows().get().forEach(detail -> {
        final String term = detail.getProperty("term");
        if (Arrays.binarySearch(dcTerms, term) == -1) {
          logger.error("RepositoryBuilder invalid metadata term {}", term);
        } else {
          final String value = detail.getProperty("value");
          final SimpleLiteral literal = new SimpleLiteral();
          literal.getContent().add(value);

          QName qname = new QName("http://purl.org/dc/elements/1.1/", term);
          JAXBElement<SimpleLiteral> element =
                  new JAXBElement<>(qname, SimpleLiteral.class, null, literal);
          literals.add(element);
        }
      });
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
    final List<Object> members = componentType.getComponentRefOrGroupRefOrFieldRef();
    copyMembers(members);
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
    final List<Object> members = componentType.getComponentRefOrGroupRefOrFieldRef();
    copyMembers(members);
  }

  private void copyMembers(List<Object> members) {
    for (final Object member : members) {
      if (member instanceof FieldRefType) {
        final FieldRefType fieldRef = (FieldRefType) member;
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
        final GroupRefType groupRef = (GroupRefType) member;
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
      } else if (member instanceof ComponentRefType) {
        final ComponentRefType componentRef = (ComponentRefType) member;
        ComponentType component =
            this.findComponentByTag(componentRef.getId().intValue(), componentRef.getScenario());
        if (component == null) {
          component = reference.findComponentByTag(componentRef.getId().intValue(),
              componentRef.getScenario());
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
    repository.getCodeSets().getCodeSet().add(codeset);
  }

  private Repository createRepository() {
    final Repository repository = new Repository();
    repository.setMetadata(new ElementOrRefinementContainer());
    repository.setDatatypes(new Datatypes());
    repository.setCodeSets(new CodeSets());
    repository.setFields(new Fields());
    repository.setMessages(new Messages());
    repository.setComponents(new Components());
    repository.setGroups(new Groups());
    return repository;
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

    final List<ComponentRuleType> rules = componentRefType.getRule();

    final String presenceString = detail.getProperty("presence");
    final String[] presenceWords = presenceString.split("[ \t]");
    PresenceT presence = null;
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

    if (!DEFAULT_SCENARIO.equals(scenario)) {
      componentRefType.setScenario(scenario);
    }

    return componentRefType;
  }

  private FieldRefType populateFieldRef(DetailProperties detail) {
    final String name = detail.getProperty("name");
    String scenario = scenarioOrDefault(detail.getProperty("scenario"));
    int tag = tagToInt(detail.getProperty("tag"));

    final FieldRefType fieldRefType = new FieldRefType();

    FieldType fieldType = null;
    if (tag != -1) {
      fieldType = this.findFieldByTag(tag, scenario);
    } else if (name != null) {
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

    final List<FieldRuleType> rules = fieldRefType.getRule();

    PresenceT presence = PresenceT.OPTIONAL;
    final String presenceString = detail.getProperty("presence");
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

    final String values = detail.getProperty("values");
    if (values != null && !values.isEmpty()) {
      final int keywordPos = values.indexOf(ASSIGN_KEYWORD);
      if (keywordPos != -1) {
        fieldRefType.setAssign(values.substring(keywordPos + ASSIGN_KEYWORD.length() + 1));
      } else {
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
          final String[] valueTokens = values.split("[ =\t]");
          final String codesetName = name + "Codeset";
          createCodeset(codesetName, scenario, DEFAULT_CODE_TYPE, valueTokens);
          logger.warn("RepositoryBuilder unknown codeset datatype; name={} scenario={}",
              codesetName, scenario);
        }
      }
    }

    if (!DEFAULT_SCENARIO.equals(scenario)) {
      fieldRefType.setScenario(scenario);
    }

    final String encoding = detail.getProperty("encoding");
    if (encoding != null) {
      fieldRefType.setEncoding(encoding);
    }

    final Integer minLength = detail.getIntProperty("implMinLength");
    if (minLength != null) {
      fieldRefType.setImplMinLength(minLength.shortValue());
    }

    final Integer maxLength = detail.getIntProperty("implMaxLength");
    if (maxLength != null) {
      fieldRefType.setImplMaxLength(maxLength.shortValue());
    }

    final Integer length = detail.getIntProperty("implLength");
    if (length != null) {
      fieldRefType.setImplLength(length.shortValue());
    }

    return fieldRefType;
  }

  private GroupRefType populateGroupRef(DetailProperties detail) {
    final String name = detail.getProperty("name");
    final String scenario = scenarioOrDefault(detail.getProperty("scenario"));
    final GroupRefType groupRefType = new GroupRefType();

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

    final List<ComponentRuleType> rules = groupRefType.getRule();

    final String presenceString = detail.getProperty("presence");
    final String[] presenceWords = presenceString.split("[ \t]");
    PresenceT presence = null;
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

  private Repository unmarshal(InputStream is) throws JAXBException {
    final JAXBContext jaxbContext = JAXBContext.newInstance(Repository.class);
    final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    return (Repository) jaxbUnmarshaller.unmarshal(is);
  }

  private void updateDocumentation(String doc, Annotation annotation) {
    final List<Object> elements = annotation.getDocumentationOrAppinfo();
    elements.clear();
    addDocumentation(doc, annotation);
  }

  CodeSetType findCodesetByName(String name, String scenario) {
    final List<CodeSetType> codesets = repository.getCodeSets().getCodeSet();
    for (final CodeSetType codeset : codesets) {
      if (codeset.getName().equals(name) && codeset.getScenario().equals(scenario)) {
        return codeset;
      }
    }
    return null;
  }

  ComponentType findComponentByName(String name, String scenario) {
    final List<ComponentType> components = repository.getComponents().getComponent();
    for (final ComponentType component : components) {
      if (component.getName().equals(name) && component.getScenario().equals(scenario)) {
        return component;
      }
    }
    return null;
  }

  ComponentType findComponentByTag(int tag, String scenario) {
    final List<ComponentType> components = repository.getComponents().getComponent();
    for (final ComponentType component : components) {
      if (component.getId().intValue() == tag && component.getScenario().equals(scenario)) {
        return component;
      }
    }
    return null;
  }

  io.fixprotocol._2020.orchestra.repository.Datatype findDatatypeByName(String name) {
    final List<io.fixprotocol._2020.orchestra.repository.Datatype> datatypes =
        repository.getDatatypes().getDatatype();
    for (final io.fixprotocol._2020.orchestra.repository.Datatype datatype : datatypes) {
      if (datatype.getName().equals(name)) {
        return datatype;
      }
    }
    return null;
  }

  FieldType findFieldByName(String name, String scenario) {
    final List<FieldType> fields = repository.getFields().getField();
    for (final FieldType field : fields) {
      if (field.getName().equals(name) && field.getScenario().equals(scenario)) {
        return field;
      }
    }
    return null;
  }

  FieldType findFieldByTag(int tag, String scenario) {
    final List<FieldType> fields = repository.getFields().getField();
    for (final FieldType field : fields) {
      if (field.getId().intValue() == tag && field.getScenario().equals(scenario)) {
        return field;
      }
    }
    return null;
  }

  GroupType findGroupByName(String name, String scenario) {
    final List<GroupType> components = repository.getGroups().getGroup();
    for (final GroupType component : components) {
      if (component.getName().equals(name) && component.getScenario().equals(scenario)) {
        return component;
      }
    }
    return null;
  }

  GroupType findGroupByTag(int tag, String scenario) {
    final List<GroupType> components = repository.getGroups().getGroup();
    for (final GroupType component : components) {
      if (component.getId().intValue() == tag && component.getScenario().equals(scenario)) {
        return component;
      }
    }
    return null;
  }

  MessageType findMessageByName(String name, String scenario) {
    final List<MessageType> messages = repository.getMessages().getMessage();
    for (final MessageType message : messages) {
      if (name.equals(message.getName()) && message.getScenario().equals(scenario)) {
        return message;
      }
    }
    return null;
  }
}
