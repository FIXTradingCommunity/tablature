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
package io.fixprotocol.md2interfaces;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.purl.dc.elements._1.SimpleLiteral;
import org.purl.dc.terms.ElementOrRefinementContainer;
import io.fixprotocol._2020.orchestra.interfaces.Annotation;
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
import io.fixprotocol.md.event.Context;
import io.fixprotocol.md.event.DetailProperties;
import io.fixprotocol.md.event.DetailTable;
import io.fixprotocol.md.event.Documentation;
import io.fixprotocol.md.event.MarkdownUtil;

class InterfacesBuilder implements Consumer<Context> {

  // sorted array of valid Dublin Core Terms
  private static final String[] dcTerms =
      new String[] {"contributor", "coverage", "creator", "date", "description", "format",
          "identifier", "language", "publisher", "relation", "rights", "source", "subject", "type"};

  private static final int NAME_POSITION = 1;

  private final Interfaces interfaces = new Interfaces();
  private final Logger logger = LogManager.getLogger(getClass());

  @Override
  public void accept(Context context) {
    final String type = context.getKey(0);
    if (type == null) {
      // log unknown type
    } else
      switch (type.toLowerCase()) {
        case "interface":
          addInterface(context);
          break;
        case "protocols":
          addProtocols(context);
          break;
        case "session":
          addSession(context);
          break;
        case "identifiers":
          addIdentifiers(context);
          break;
        default:
          if (context.getLevel() == 1) {
            addMetadata(context);
          } else {
            logger.warn("InterfacesBuilder received unknown context type {}", type);
          }
      }
  }

  public void marshal(OutputStream os) throws JAXBException {
    final JAXBContext jaxbContext = JAXBContext.newInstance(Interfaces.class);
    final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
    jaxbMarshaller.setProperty("jaxb.formatted.output", true);
    jaxbMarshaller.marshal(interfaces, os);
  }

  private void addDocumentation(String markdown, Annotation annotation) {
    final List<Object> elements = annotation.getDocumentationOrAppinfo();
    final io.fixprotocol._2020.orchestra.interfaces.Documentation documentation =
        new io.fixprotocol._2020.orchestra.interfaces.Documentation();
    documentation.setContentType(MarkdownUtil.MARKDOWN_MEDIA_TYPE);
    documentation.getContent().add(markdown);
    elements.add(documentation);
  }

  private void addIdentifiers(Context context) {
    final Context parent = context.getParent();
    final String sessionName = parent.getKey(NAME_POSITION);
    final Context grandParent = parent.getParent();
    if (grandParent != null) {
      final String interfaceName = grandParent.getKey(NAME_POSITION);
      final SessionType session = findSession(sessionName, interfaceName);
      if (session != null) {
        if (context instanceof DetailTable) {
          final DetailTable detailTable = (DetailTable) context;
          final List<IdentifierType> identifierList = session.getIdentifier();
          detailTable.rows().get().forEach(detail -> {
            IdentifierType identifier = new IdentifierType();
            identifier.setName(detail.getProperty("name"));
            identifier.setContent(detail.getProperty("value"));
            identifierList.add(identifier);
          });
        }
      } else {
        logger.error("InterfaceBuilder unknown session; name={} interface={}", sessionName,
            interfaceName);
      }
    } else {
      logger.error("InterfaceBuilder unknown parent interface for session id; name={}",
          sessionName);
    }
  }

  private void addInterface(Context context) {
    if (context instanceof Documentation) {
      final Documentation detail = (Documentation) context;
      final String name = detail.getKey(NAME_POSITION);
      final InterfaceType interfaceInstance = findInterface(name);
      if (interfaceInstance != null) {
        Annotation annotation = interfaceInstance.getAnnotation();
        if (annotation == null) {
          annotation = new Annotation();
          interfaceInstance.setAnnotation(annotation);
        }
        addDocumentation(detail.getDocumentation(), annotation);
      } else {
        logger.error("InterfaceBuilder unknown interface id; name={}", name);
      }
    } else {
      final String name = context.getKey(NAME_POSITION);
      final InterfaceType interfaceInstance = new InterfaceType();
      interfaceInstance.setName(name);

      interfaces.getInterface().add(interfaceInstance);
    }
  }

  private void addInterfaceProtocols(Context context, Context parent) {
    final String interfaceName = parent.getKey(NAME_POSITION);
    final InterfaceType interfaceInstance = findInterface(interfaceName);
    if (interfaceInstance != null) {
      if (context instanceof DetailTable) {
        final DetailTable detailTable = (DetailTable) context;
        detailTable.rows().get().forEach(detail -> {
          ProtocolType protocol = createProtocol(detail);
          interfaceInstance.getProtocol().add(protocol);
        });
      }
    } else {
      logger.error("InterfaceBuilder unknown interface id; name={}", interfaceName);
    }
  }

  private void addMetadata(Context context) {
    if (context instanceof DetailTable) {
      final DetailTable detailTable = (DetailTable) context;
      final ElementOrRefinementContainer container = new ElementOrRefinementContainer();
      interfaces.setMetadata(container);
      final List<JAXBElement<SimpleLiteral>> literals = container.getAny();

      detailTable.rows().get().forEach(detail -> {
        final String term = detail.getProperty("term");
        if (Arrays.binarySearch(dcTerms, term) == -1) {
          logger.error("InterfaceBuilder invalid metadata term {}", term);
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

  private void addProtocols(Context context) {
    final Context parent = context.getParent();
    final String parentType = parent.getKey(0);
    if (parentType == null) {
      // log unknown type
    } else
      switch (parentType.toLowerCase()) {
        case "interface":
          addInterfaceProtocols(context, parent);
          break;
        case "session":
          addSessionProtocols(context, parent);
          break;
      }
  }

  private void addSession(Context context) {
    if (context instanceof Documentation) {
      final Documentation detail = (Documentation) context;
      final String name = detail.getKey(NAME_POSITION);
      final Context parent = context.getParent();
      if (parent != null) {
        final String interfaceName = parent.getKey(NAME_POSITION);
        final SessionType session = findSession(name, interfaceName);
        if (session != null) {
          Annotation annotation = session.getAnnotation();
          if (annotation == null) {
            annotation = new Annotation();
            session.setAnnotation(annotation);
          }
          addDocumentation(detail.getDocumentation(), annotation);
        } else {
          logger.error("InterfaceBuilder unknown session; name={} interface={}", name,
              interfaceName);
        }
      } else {
        logger.error("InterfaceBuilder unknown parent interface for session id; name={}", name);
      }
    } else {
      final String name = context.getKey(NAME_POSITION);

      final Context parent = context.getParent();
      if (parent != null) {
        final String interfaceName = parent.getKey(NAME_POSITION);
        final InterfaceType interfaceInstance = findInterface(interfaceName);
        if (interfaceInstance != null) {
          final SessionType session = new SessionType();
          session.setName(name);
          Sessions sessions = interfaceInstance.getSessions();
          if (sessions == null) {
            sessions = new Sessions();
            interfaceInstance.setSessions(sessions);
          }
          final List<SessionType> sessionList = sessions.getSession();
          sessionList.add(session);
        }
      } else {
        logger.error("InterfaceBuilder unknown parent interface for session {}", name);
      }
    }
  }

  private void addSessionProtocols(Context context, Context parent) {
    final String sesionName = parent.getKey(NAME_POSITION);
    final Context grandParent = parent.getParent();
    if (grandParent != null) {
      final String interfaceName = grandParent.getKey(NAME_POSITION);
      final SessionType session = findSession(sesionName, interfaceName);
      if (session != null) {
        if (context instanceof DetailTable) {
          final DetailTable detailTable = (DetailTable) context;
          detailTable.rows().get().forEach(detail -> {
            ProtocolType protocol = createProtocol(detail);
            session.getProtocol().add(protocol);
          });
        }
      } else {
        logger.error("InterfaceBuilder unknown session; name={} interface={}", sesionName,
            interfaceName);
      }
    } else {
      logger.error("InterfaceBuilder unknown parent interface for session id; name={}", sesionName);
    }
  }

  private ProtocolType createProtocol(DetailProperties detail) {
    final String layer = detail.getProperty("layer");
    ProtocolType protocol;
    switch (layer.toLowerCase()) {
      case "service":
      case "application":
        protocol = new ServiceType();
        break;
      case "ui":
      case "userinterface":
        protocol = new UserIntefaceType();
        break;
      case "encoding":
      case "presentation":
        protocol = new EncodingType();
        break;
      case "session":
        protocol = new SessionProtocolType();
        break;
      case "transport":
        protocol = new TransportProtocolType();
        final String address = detail.getProperty("address");
        if (address != null) {
          ((TransportProtocolType) protocol).setAddress(address);
        }
        final String use = detail.getProperty("use");
        if (use != null) {
          ((TransportProtocolType) protocol).setUse(use);
        }
        final String messageCast = detail.getProperty("messageCast");
        if (messageCast != null) {
          final MessageCastT messagecastt = MessageCastT.fromValue(messageCast.toLowerCase());
          ((TransportProtocolType) protocol).setMessageCast(messagecastt);
        }
        break;
      default:
        protocol = new ProtocolType();
        final LayerT layert = LayerT.fromValue(layer.toLowerCase());
        protocol.setLayer(layert);
    }
    protocol.setName(detail.getProperty("name"));

    final String version = detail.getProperty("version");
    if (version != null) {
      protocol.setVersion(version);
    }

    final String reliability = detail.getProperty("reliability");
    if (reliability != null && !reliability.isEmpty()) {
      final ReliabilityT reliabilityt = ReliabilityT.fromValue(enumValue(reliability));
      protocol.setReliability(reliabilityt);
    }

    final String orchestration = detail.getProperty("orchestration");
    if (orchestration != null) {
      protocol.setOrchestration(orchestration);
    }

    return protocol;
  }

  private String enumValue(String text) {
    final StringBuilder sb = new StringBuilder();
    int fromIndex = 0;
    do {
      final int underScorePos = text.indexOf('_', fromIndex);
      if (underScorePos == -1) {
        sb.append(text.substring(fromIndex).toLowerCase());
        break;
      } else {
        sb.append(text.substring(fromIndex, underScorePos).toLowerCase());
        sb.append(text.charAt(underScorePos + 1));
        fromIndex = underScorePos + 2;
      }
    } while (fromIndex < text.length());
    return sb.toString();
  }

  private InterfaceType findInterface(String name) {
    final List<InterfaceType> interfaceList = interfaces.getInterface();
    for (final InterfaceType interfaceInstance : interfaceList) {
      if (interfaceInstance.getName().equals(name)) {
        return interfaceInstance;
      }
    }
    return null;
  }

  private SessionType findSession(String name, String interfaceName) {
    final InterfaceType interfaceInstance = findInterface(interfaceName);
    if (interfaceInstance != null) {
      final Sessions sessions = interfaceInstance.getSessions();
      if (sessions != null) {
        final List<SessionType> sessionList = sessions.getSession();
        for (final SessionType session : sessionList) {
          if (session.getName().equals(name)) {
            return session;
          }
        }
      }
    }
    return null;
  }
}
