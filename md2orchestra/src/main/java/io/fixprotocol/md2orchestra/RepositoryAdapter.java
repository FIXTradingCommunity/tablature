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
import java.util.Arrays;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import org.purl.dc.elements._1.SimpleLiteral;
import org.purl.dc.terms.ElementOrRefinementContainer;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import io.fixprotocol._2020.orchestra.repository.ActorType;
import io.fixprotocol._2020.orchestra.repository.Actors;
import io.fixprotocol._2020.orchestra.repository.Annotation;
import io.fixprotocol._2020.orchestra.repository.Appinfo;
import io.fixprotocol._2020.orchestra.repository.CodeSetType;
import io.fixprotocol._2020.orchestra.repository.CodeSets;
import io.fixprotocol._2020.orchestra.repository.ComponentType;
import io.fixprotocol._2020.orchestra.repository.Components;
import io.fixprotocol._2020.orchestra.repository.Datatype;
import io.fixprotocol._2020.orchestra.repository.Datatypes;
import io.fixprotocol._2020.orchestra.repository.FieldType;
import io.fixprotocol._2020.orchestra.repository.Fields;
import io.fixprotocol._2020.orchestra.repository.FlowType;
import io.fixprotocol._2020.orchestra.repository.GroupType;
import io.fixprotocol._2020.orchestra.repository.Groups;
import io.fixprotocol._2020.orchestra.repository.MessageType;
import io.fixprotocol._2020.orchestra.repository.Messages;
import io.fixprotocol._2020.orchestra.repository.Repository;
import io.fixprotocol._2020.orchestra.repository.StateMachineType;
import io.fixprotocol.md.event.MarkdownUtil;
import io.fixprotocol.orchestra.event.EventListener;

/**
 * Access methods for Repository
 * 
 * Preferred XML namespace prefixes: fixr = http://fixprotocol.io/2020/orchestra/repository dcterms
 * = http://purl.org/dc/terms/ dc = http://purl.org/dc/elements/1.1/
 * 
 * @author Don Mendelson
 *
 */
class RepositoryAdapter {

  private final EventListener eventLogger;
  
  /**
   * Provide deterministic XML namespace prefixes
   *
   * NamespacePrefixMapper class is declared in the XML processor implementation -- not portable!!!
   *
   * The implementation makes no guarantee that it will actually use the preferred prefix.
   */
  private static class RepositoryNamespacePrefixMapper extends NamespacePrefixMapper {

    @Override
    public String getPreferredPrefix(String namespaceUri, String suggestion,
        boolean requirePrefix) {
      switch (namespaceUri) {
        case "http://fixprotocol.io/2020/orchestra/repository":
          return "fixr";
        case "http://purl.org/dc/elements/1.1/":
          return "dc";
        case "http://purl.org/dc/terms/":
          return "dcterms";
        default:
          return null;
      }
    }
  }

  // sorted array of valid Dublin Core Terms
  private static final String[] DC_TERMS = new String[] {"accessRights", "accrualMethod",
      "accrualPeriodicity", "accrualPolicy", "abstract", "alternative", "audience", "avaliable",
      "bibliographicCitation", "conformsTo", "contributor", "coverage", "created", "creator",
      "date", "dateAccepted", "dateCopyrighted", "dateSubmitted", "description", "educationLevel",
      "extent", "format", "hasFormat", "hasPart", "hasVersion", "identifier", "instructionalMethod",
      "issued", "isFormatOf", "isPartOf", "isReferencedBy", "isReplacedBy", "isRequiredBy",
      "isVersionOf", "language", "license", "mediator", "medium", "modified", "provenance",
      "publisher", "references", "relation", "replaces", "requires", "rights", "rightsHolder",
      "source", "spatial", "subject", "tableOfContents", "temporal", "title", "type", "valid"};

  static String substitute(String markdown, String token, String replacement) {
    return markdown.replace(token, replacement);
  }

  private Repository repository;

  void addActor(final ActorType actor) {
    Actors actors = repository.getActors();
    if (actors == null) {
      actors = new Actors();
      repository.setActors(actors);
    }
    actors.getActorOrFlow().add(actor);
  }

  void addAppinfo(String markdown, String purpose, Annotation annotation) {
    final List<Object> elements = annotation.getDocumentationOrAppinfo();
    final Appinfo appinfo = new Appinfo();
    final List<Object> contents = appinfo.getContent();
    contents.add(markdown);
    if (purpose != null) {
      appinfo.setPurpose(purpose);
    }
    elements.add(appinfo);
  }

  void addAppinfo(String markdown, String paragraphDelmiter, String purpose,
      Annotation annotation) {
    addAppinfo(substitute(markdown, paragraphDelmiter, "\n\n"), purpose, annotation);
  }


  void addCodeset(final CodeSetType codeset) {
    repository.getCodeSets().getCodeSet().add(codeset);
  }

  void addComponent(final ComponentType component) {
    repository.getComponents().getComponent().add(component);
  }

  void addDatatype(io.fixprotocol._2020.orchestra.repository.Datatype datatype) {
    repository.getDatatypes().getDatatype().add(datatype);
  }

  void addDocumentation(String markdown, String purpose, Annotation annotation) {
    final List<Object> elements = annotation.getDocumentationOrAppinfo();
    final io.fixprotocol._2020.orchestra.repository.Documentation documentation =
        new io.fixprotocol._2020.orchestra.repository.Documentation();
    documentation.setContentType(MarkdownUtil.MARKDOWN_MEDIA_TYPE);
    documentation.getContent().add(markdown);
    if (purpose != null) {
      documentation.setPurpose(purpose);
    }
    elements.add(documentation);
  }

  void addDocumentation(String markdown, String paragraphDelmiter, String purpose,
      Annotation annotation) {
    addDocumentation(substitute(markdown, paragraphDelmiter, "\n\n"), purpose, annotation);
  }

  void addField(FieldType field) {
    repository.getFields().getField().add(field);
  }

  void addFlow(final FlowType flow) {
    Actors actors = repository.getActors();
    if (actors == null) {
      actors = new Actors();
      repository.setActors(actors);
    }
    actors.getActorOrFlow().add(flow);
  }

  void addGroup(final GroupType group) {
    repository.getGroups().getGroup().add(group);
  }

  void addMessage(MessageType message) {
    repository.getMessages().getMessage().add(message);
  }

  CodeSetType copyCodeset(CodeSetType source) {
    final CodeSetType codeset = (CodeSetType) source.clone();
    repository.getCodeSets().getCodeSet().add(codeset);
    return codeset;
  }

  ComponentType copyComponent(ComponentType source) {
    final ComponentType component = (ComponentType) source.clone();
    repository.getComponents().getComponent().add(component);
    return component;
  }

  Datatype copyDatatype(Datatype source) {
    final Datatype datatype = (Datatype) source.clone();
    repository.getDatatypes().getDatatype().add(datatype);
    return datatype;
  }

  FieldType copyField(FieldType source) {
    final FieldType field = (FieldType) source.clone();
    repository.getFields().getField().add(field);
    return field;
  }

  GroupType copyGroup(GroupType source) {
    final GroupType group = (GroupType) source.clone();
    repository.getGroups().getGroup().add(group);
    return group;
  }

  void createRepository() {
    repository = new Repository();
    repository.setMetadata(new ElementOrRefinementContainer());
    repository.setDatatypes(new Datatypes());
    repository.setCodeSets(new CodeSets());
    repository.setFields(new Fields());
    repository.setMessages(new Messages());
    repository.setComponents(new Components());
    repository.setGroups(new Groups());
  }

  ActorType findActorByName(String name) {
    final Actors actors = repository.getActors();
    if (actors != null) {
      final List<Object> objects = actors.getActorOrFlow();
      for (final Object object : objects) {
        if (object instanceof ActorType) {
          final ActorType actor = (ActorType) object;
          if (actor.getName().equals(name)) {
            return actor;
          }
        }
      }
    }
    return null;
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


  FlowType findFlowByName(String name) {
    final Actors actors = repository.getActors();
    if (actors != null) {
      final List<Object> objects = actors.getActorOrFlow();
      for (final Object object : objects) {
        if (object instanceof FlowType) {
          final FlowType flow = (FlowType) object;
          if (flow.getName().equals(name)) {
            return flow;
          }
        }
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

  StateMachineType findStatemachineByName(ActorType actor, String name) {
    final List<Object> objects = actor.getFieldOrFieldRefOrComponent();
    for (final Object object : objects) {
      if (object instanceof StateMachineType) {
        final StateMachineType statemachine = (StateMachineType) object;
        if (statemachine.getName().equals(name)) {
          return statemachine;
        }
      }
    }
    return null;
  }

  void marshal(OutputStream os) throws JAXBException {
    final JAXBContext jaxbContext = JAXBContext.newInstance(Repository.class);
    final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    // warning: this is implementation specific !!!
    try {
      jaxbMarshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper",
          new RepositoryNamespacePrefixMapper());
    } catch (final PropertyException e) {     
      eventLogger.warn("RepositoryBuilder namespace prefix mapper not supported by XML implementation");
    }
    jaxbMarshaller.marshal(repository, os);
  }

  void setMetadata(String term, String value) {
    final ElementOrRefinementContainer container = repository.getMetadata();
    final List<JAXBElement<SimpleLiteral>> literals = container.getAny();
    if (Arrays.binarySearch(DC_TERMS, term) < 0) {
      eventLogger.error("RepositoryBuilder invalid metadata term {0}", term);
    } else {
      final SimpleLiteral literal = new SimpleLiteral();
      literal.getContent().add(value);

      final QName qname = new QName("http://purl.org/dc/terms/", term);
      final JAXBElement<SimpleLiteral> element =
          new JAXBElement<>(qname, SimpleLiteral.class, null, literal);
      literals.add(element);
    }
  }

  void setName(final String name) {
    repository.setName(name);
  }

  void setVersion(final String version) {
    repository.setVersion(version);
  }

  RepositoryAdapter(EventListener eventLogger) {
    this.eventLogger = eventLogger;
  }

  void unmarshal(InputStream is) throws JAXBException {
    final JAXBContext jaxbContext = JAXBContext.newInstance(Repository.class);
    final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    // this.repository = (Repository) jaxbUnmarshaller.unmarshal(is);
    final Object obj = jaxbUnmarshaller.unmarshal(is);
    if (obj instanceof Repository) {
      this.repository = (Repository) obj;
    }
  }
}
