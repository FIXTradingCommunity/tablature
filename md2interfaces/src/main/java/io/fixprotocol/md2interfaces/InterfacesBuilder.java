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

import java.io.IOException;
import java.io.InputStream;
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
import io.fixprotocol._2020.orchestra.interfaces.BaseInterfaceType;
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
import io.fixprotocol.md.event.Contextual;
import io.fixprotocol.md.event.DetailProperties;
import io.fixprotocol.md.event.DetailTable;
import io.fixprotocol.md.event.DocumentParser;
import io.fixprotocol.md.event.Documentation;
import io.fixprotocol.md.event.MarkdownUtil;
import io.fixprotocol.orchestra.event.EventListener;
import io.fixprotocol.orchestra.event.EventListenerFactory;
import io.fixprotocol.orchestra.event.TeeEventListener;

public class InterfacesBuilder implements Consumer<Contextual> {

  public static final String IDENTIFIERS_KEYWORD = "identifiers";
  public static final String INTERFACE_KEYWORD = "interface";
  public static final String PROTOCOLS_KEYWORD = "protocols";
  public static final String SESSION_KEYWORD = "session";

  // sorted array of valid Dublin Core Terms
  private static final String[] dcTerms =
      new String[] {"contributor", "coverage", "creator", "date", "description", "format",
          "identifier", "language", "publisher", "relation", "rights", "source", "subject", "type"};

  private static final int KEY_POSITION = 0;
  private static final int NAME_POSITION = 1;

  private final String[] contextKeys =
      new String[] {IDENTIFIERS_KEYWORD, INTERFACE_KEYWORD, PROTOCOLS_KEYWORD, SESSION_KEYWORD};

  private TeeEventListener eventLogger;
  private final EventListenerFactory factory = new EventListenerFactory();
  private final Interfaces interfaces = new Interfaces();
  private final Logger logger = LogManager.getLogger(getClass());

  public InterfacesBuilder(OutputStream jsonOutputStream) throws Exception {
    createLogger(jsonOutputStream);
  }

  @Override
  public void accept(Contextual contextual) {
    final Context keyContext = getKeyContext(contextual);
    final String type = keyContext.getKey(KEY_POSITION);
    if (type == null) {
      logger.warn("InterfacesBuilder received element with unknown context of class {}",
          contextual.getClass());
    } else
      switch (type.toLowerCase()) {
        case INTERFACE_KEYWORD:
          addInterface(contextual, keyContext);
          break;
        case PROTOCOLS_KEYWORD:
          addProtocols(contextual, keyContext);
          break;
        case SESSION_KEYWORD:
          addSession(contextual, keyContext);
          break;
        case IDENTIFIERS_KEYWORD:
          addIdentifiers(contextual, keyContext);
          break;
        default:
          if (keyContext.getLevel() == 1) {
            addMetadata(contextual, keyContext);
          } else {
            logger.warn("InterfacesBuilder received unknown context type {}", type);
          }
      }
  }

  /**
   * Append input to a interfaces file
   *
   * @param inputStream an markdown file input
   * @throws IOException if an IO error occurs
   */
  public void appendInput(InputStream inputStream) throws IOException {
    final DocumentParser parser = new DocumentParser();
    parser.parse(inputStream, this);
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

  private void addIdentifiers(Contextual contextual, Context context) {
    if (contextual instanceof DetailTable) {
      final DetailTable detailTable = (DetailTable) contextual;
      final Context parent = context.getParent();
      final String sessionName = parent.getKey(NAME_POSITION);
      final Context grandParent = parent.getParent();
      if (grandParent != null) {
        final String interfaceName = grandParent.getKey(NAME_POSITION);
        final SessionType session = findSession(sessionName, interfaceName);
        if (session != null) {
          final List<IdentifierType> identifierList = session.getIdentifier();
          detailTable.rows().forEach(detail -> {
            final IdentifierType identifier = new IdentifierType();
            identifier.setName(detail.getProperty("name"));
            identifier.setContent(detail.getProperty("value"));
            identifierList.add(identifier);
          });
        } else {
          logger.error("InterfaceBuilder unknown session; name={} interface={}", sessionName,
              interfaceName);
        }
      } else {
        logger.error("InterfaceBuilder unknown parent interface for session id; name={}",
            sessionName);
      }
    }
  }

  private void addInterface(Contextual contextual, Context context) {
    final String name = context.getKey(NAME_POSITION);
    if (contextual instanceof Documentation) {
      final Documentation detail = (Documentation) contextual;

      final InterfaceType interfaceInstance = findInterface(name);
      if (interfaceInstance != null) {
        Annotation annotation = interfaceInstance.getAnnotation();
        if (annotation == null) {
          annotation = new Annotation();
          interfaceInstance.setAnnotation(annotation);
        }
        addDocumentation(detail.getDocumentation(), annotation);
      } else {
        logger.error("InterfaceBuilder unknown interface; name={}", name);
      }
    }
    if (contextual instanceof Context
        && INTERFACE_KEYWORD.equalsIgnoreCase(((Context) contextual).getKey(KEY_POSITION))) {
      final InterfaceType interfaceInstance = new InterfaceType();
      interfaceInstance.setName(name);

      interfaces.getInterface().add(interfaceInstance);
    }
  }

  private void addInterfaceProtocol(DetailProperties detail, BaseInterfaceType interfaceInstance) {
    final String layer = detail.getProperty("layer");
    ProtocolType protocol;
    switch (layer.toLowerCase()) {
      case "service":
      case "application":
        protocol = new ServiceType();
        interfaceInstance.getService().add((ServiceType) protocol);
        break;
      case "ui":
      case "userinterface":
        protocol = new UserIntefaceType();
        interfaceInstance.getUserInterface().add((UserIntefaceType) protocol);
        break;
      case "encoding":
      case "presentation":
        protocol = new EncodingType();
        interfaceInstance.getEncoding().add((EncodingType) protocol);
        break;
      case SESSION_KEYWORD:
        protocol = new SessionProtocolType();
        interfaceInstance.getSessionProtocol().add((SessionProtocolType) protocol);
        break;
      case "transport":
        protocol = new TransportProtocolType();
        interfaceInstance.getTransport().add((TransportProtocolType) protocol);
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
        interfaceInstance.getProtocol().add(protocol);
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

  }

  private void addInterfaceProtocols(Contextual contextual, Context parent) {
    final String interfaceName = parent.getKey(NAME_POSITION);
    final InterfaceType interfaceInstance = findInterface(interfaceName);
    if (interfaceInstance != null) {
      if (contextual instanceof DetailTable) {
        final DetailTable detailTable = (DetailTable) contextual;
        detailTable.rows().forEach(detail -> addInterfaceProtocol(detail, interfaceInstance));
      }
    } else {
      logger.error("InterfaceBuilder unknown interface; name={}", interfaceName);
    }
  }

  private void addMetadata(Contextual contextual, Context context) {
    if (contextual instanceof DetailTable) {
      final DetailTable detailTable = (DetailTable) contextual;
      final ElementOrRefinementContainer container = new ElementOrRefinementContainer();
      interfaces.setMetadata(container);
      final List<JAXBElement<SimpleLiteral>> literals = container.getAny();

      detailTable.rows().forEach(detail -> {
        final String term = detail.getProperty("term");
        if (Arrays.binarySearch(dcTerms, term) == -1) {
          logger.error("InterfaceBuilder invalid metadata term {}", term);
        } else {
          final String value = detail.getProperty("value");
          final SimpleLiteral literal = new SimpleLiteral();
          literal.getContent().add(value);

          final QName qname = new QName("http://purl.org/dc/elements/1.1/", term);
          final JAXBElement<SimpleLiteral> element =
              new JAXBElement<>(qname, SimpleLiteral.class, null, literal);
          literals.add(element);
        }
      });
    }
  }

  private void addProtocols(Contextual contextual, Context context) {
    final Context parent = context.getParent();
    final String parentType = parent.getKey(KEY_POSITION);
    if (parentType == null) {
      // log unknown type
    } else
      switch (parentType.toLowerCase()) {
        case INTERFACE_KEYWORD:
          addInterfaceProtocols(contextual, parent);
          break;
        case SESSION_KEYWORD:
          addSessionProtocols(contextual, parent);
          break;
      }
  }

  private void addSession(Contextual contextual, Context context) {
    final String name = context.getKey(NAME_POSITION);
    if (contextual instanceof Documentation) {
      final Documentation detail = (Documentation) contextual;
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
    } else if (contextual instanceof Context
        && SESSION_KEYWORD.equalsIgnoreCase(((Context) contextual).getKey(KEY_POSITION))) {
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

  private void addSessionProtocols(Contextual contextual, Context parent) {
    final String sesionName = parent.getKey(NAME_POSITION);
    final Context grandParent = parent.getParent();
    if (grandParent != null) {
      final String interfaceName = grandParent.getKey(NAME_POSITION);
      final SessionType session = findSession(sesionName, interfaceName);
      if (session != null) {
        if (contextual instanceof DetailTable) {
          final DetailTable detailTable = (DetailTable) contextual;
          detailTable.rows().forEach(detail -> addInterfaceProtocol(detail, session));
        }
      } else {
        logger.error("InterfaceBuilder unknown session; name={} interface={}", sesionName,
            interfaceName);
      }
    } else {
      logger.error("InterfaceBuilder unknown parent interface for session id; name={}", sesionName);
    }
  }

  private void createLogger(OutputStream jsonOutputStream) throws Exception {
    eventLogger = new TeeEventListener();
    final EventListener logEventLogger = factory.getInstance("LOG4J");
    logEventLogger.setResource(logger);
    eventLogger.addEventListener(logEventLogger);
    if (jsonOutputStream != null) {
      final EventListener jsonEventLogger = factory.getInstance("JSON");
      jsonEventLogger.setResource(jsonOutputStream);
      eventLogger.addEventListener(jsonEventLogger);
    }
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
}
