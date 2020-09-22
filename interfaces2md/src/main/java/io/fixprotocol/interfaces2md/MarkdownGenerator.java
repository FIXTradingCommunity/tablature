package io.fixprotocol.interfaces2md;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.purl.dc.elements._1.SimpleLiteral;
import io.fixprotocol._2020.orchestra.interfaces.Annotation;
import io.fixprotocol._2020.orchestra.interfaces.BaseInterfaceType;
import io.fixprotocol._2020.orchestra.interfaces.EncodingType;
import io.fixprotocol._2020.orchestra.interfaces.IdentifierType;
import io.fixprotocol._2020.orchestra.interfaces.InterfaceType;
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
import io.fixprotocol._2020.orchestra.interfaces.InterfaceType.Sessions;
import io.fixprotocol.md.event.ContextFactory;
import io.fixprotocol.md.event.DocumentWriter;
import io.fixprotocol.md.event.MarkdownUtil;
import io.fixprotocol.md.event.MutableContext;
import io.fixprotocol.md.event.MutableDetailProperties;
import io.fixprotocol.md.event.MutableDetailTable;
import io.fixprotocol.md.event.MutableDocumentation;
import io.fixprotocol.orchestra.event.EventListener;
import io.fixprotocol.orchestra.event.EventListenerFactory;
import io.fixprotocol.orchestra.event.TeeEventListener;

public class MarkdownGenerator {

  private final ContextFactory contextFactory = new ContextFactory();
  // User facing event notifications should be written to eventLogger
  private TeeEventListener eventLogger;
  private final EventListenerFactory factory = new EventListenerFactory();
  private final Logger logger = LogManager.getLogger(getClass());


  void generate(InputStream inputStream, OutputStreamWriter outputWriter,
      OutputStream jsonOutputStream) throws Exception {
    Objects.requireNonNull(inputStream, "Input stream is missing");
    Objects.requireNonNull(outputWriter, "Output writer is missing");

    eventLogger = new TeeEventListener();
    final EventListener logEventLogger = factory.getInstance("LOG4J");
    logEventLogger.setResource(logger);
    eventLogger.addEventListener(logEventLogger);
    if (jsonOutputStream != null) {
      final EventListener jsonEventLogger = factory.getInstance("JSON");
      jsonEventLogger.setResource(jsonOutputStream);
      eventLogger.addEventListener(jsonEventLogger);
    }

    try (final DocumentWriter documentWriter = new DocumentWriter(outputWriter)) {
      final Interfaces interfaces = unmarshal(inputStream);
      generateMetadata(interfaces, documentWriter);
      final List<InterfaceType> interfaceList = interfaces.getInterface();
      for (final InterfaceType interfaceInstance : interfaceList) {
        generateInterface(interfaceInstance, documentWriter);
      }

    } catch (final JAXBException e) {
      logger.fatal("Interfaces2md failed to parse XML", e);
      throw new IOException(e);
    } catch (final Exception e1) {
      logger.fatal("Interfaces2md error", e1);
      throw e1;
    } finally {
      eventLogger.close();
    }
  }

  private void generateInterface(InterfaceType interfaceInstance, DocumentWriter documentWriter)
      throws IOException {
    MutableContext context = contextFactory.createContext(2);
    context.addPair("Interface", interfaceInstance.getName());
    documentWriter.write(context);
    final MutableDocumentation documentation =
        contextFactory.createDocumentation(getDocumentation(interfaceInstance.getAnnotation()));
    documentWriter.write(documentation);

    generateProtocolStack(interfaceInstance, documentWriter);

    final Sessions sessions = interfaceInstance.getSessions();
    if (sessions != null) {
      final List<SessionType> sessionList = sessions.getSession();
      for (final SessionType session : sessionList) {
        generateSession(session, documentWriter);
      }
    }
  }

  private void generateMetadata(Interfaces interfaces, DocumentWriter documentWriter)
      throws IOException {
    MutableContext context = contextFactory.createContext(1);
    context.addKey("Interfaces");
    documentWriter.write(context);
    final MutableDetailTable table = contextFactory.createDetailTable();

    final List<JAXBElement<SimpleLiteral>> elements = interfaces.getMetadata().getAny();
    for (final JAXBElement<SimpleLiteral> element : elements) {
      final MutableDetailProperties row = table.newRow();
      final String name = element.getName().getLocalPart();
      final String value = String.join(" ", element.getValue().getContent());
      row.addProperty("term", name);
      row.addProperty("value", value);
    }

    documentWriter.write(table);
  }

  private void generateProtocolStack(BaseInterfaceType interfaceInstance,
      DocumentWriter documentWriter) throws IOException {
    final List<ServiceType> services = interfaceInstance.getService();
    final List<UserIntefaceType> uis = interfaceInstance.getUserInterface();
    final List<EncodingType> encodings = interfaceInstance.getEncoding();
    final List<SessionProtocolType> sessionProtocols = interfaceInstance.getSessionProtocol();
    final List<TransportProtocolType> transports = interfaceInstance.getTransport();
    final List<ProtocolType> protocols = interfaceInstance.getProtocol();
    if (!(services.isEmpty() && uis.isEmpty() && encodings.isEmpty() && sessionProtocols.isEmpty()
        && transports.isEmpty()) && protocols.isEmpty()) {

      MutableContext context = contextFactory.createContext(4);
      context.addKey("Protocols");
      documentWriter.write(context);

      final MutableDetailTable table = contextFactory.createDetailTable();
      for (final ServiceType service : services) {
        final MutableDetailProperties row = table.newRow();
        row.addProperty("layer", "Service");
        populateProtocol(row, service);
      }

      for (final UserIntefaceType ui : uis) {
        final MutableDetailProperties row = table.newRow();
        row.addProperty("layer", "UI");
        populateProtocol(row, ui);
      }

      for (final EncodingType encoding : encodings) {
        final MutableDetailProperties row = table.newRow();
        row.addProperty("layer", "Encoding");
        populateProtocol(row, encoding);
      }

      for (final SessionProtocolType sessionProtocol : sessionProtocols) {
        final MutableDetailProperties row = table.newRow();
        row.addProperty("layer", "Session");
        populateProtocol(row, sessionProtocol);
      }

      for (final TransportProtocolType transport : transports) {
        final MutableDetailProperties row = table.newRow();
        row.addProperty("layer", "Transport");
        final String use = transport.getUse();
        if (use != null) {
          row.addProperty("use", use);
        }
        final String address = transport.getAddress();
        if (address != null) {
          row.addProperty("address", address);
        }
        final MessageCastT messageCast = transport.getMessageCast();
        if (messageCast != null) {
          row.addProperty("messageCast", messageCast.name());
        }
        populateProtocol(row, transport);
      }

      for (final ProtocolType protocol : protocols) {
        final MutableDetailProperties row = table.newRow();
        row.addProperty("layer", protocol.getLayer().name());
        populateProtocol(row, protocol);
      }
      documentWriter.write(table);
    }
  }

  private void generateSession(SessionType session, DocumentWriter documentWriter)
      throws IOException {
    final MutableContext context = contextFactory.createContext(3);
    context.addKey("Session");
    context.addKey(session.getName());
    documentWriter.write(context);

    generateSessionIdentifiers(session, documentWriter);
    generateProtocolStack(session, documentWriter);
  }

  private void generateSessionIdentifiers(SessionType session, DocumentWriter documentWriter)
      throws IOException {
    final List<IdentifierType> ids = session.getIdentifier();
    if (!ids.isEmpty()) {
      MutableContext context = contextFactory.createContext(4);
      context.addKey("Identifiers");
      documentWriter.write(context);

      final MutableDetailTable table = contextFactory.createDetailTable();
      for (final IdentifierType id : ids) {
        final MutableDetailProperties row = table.newRow();
        row.addProperty("name", id.getName());
        row.addProperty("value", id.getContent());
      }
      documentWriter.write(table);
    }
  }

  private String getDocumentation(Annotation annotation) {
    if (annotation == null) {
      return "";
    } else {
      final List<Object> objects = annotation.getDocumentationOrAppinfo();
      return objects.stream()
          .filter(o -> o instanceof io.fixprotocol._2020.orchestra.interfaces.Documentation)
          .map(o -> (io.fixprotocol._2020.orchestra.interfaces.Documentation) o).map(d -> {
            if (d.getContentType().contentEquals(MarkdownUtil.MARKDOWN_MEDIA_TYPE)) {
              return d.getContent().stream().map(Object::toString).collect(Collectors.joining(" "));
            } else
              return d.getContent().stream()
                  .map(c -> MarkdownUtil.plainTextToMarkdown(c.toString()))
                  .collect(Collectors.joining(" "));
          }).collect(Collectors.joining(" "));
    }
  }

  private void populateProtocol(final MutableDetailProperties row, ProtocolType protocol) {
    final LayerT layer = protocol.getLayer();
    if (layer != null) {
      row.addProperty("layer", layer.name());
    }
    final String name = protocol.getName();
    if (name != null) {
      row.addProperty("name", name);
    }
    final String version = protocol.getVersion();
    if (version != null) {
      row.addProperty("version", version);
    }
    final ReliabilityT reliability = protocol.getReliability();
    if (reliability != null) {
      row.addProperty("reliability", reliability.name());
    }

    final String orchestration = protocol.getOrchestration();
    if (orchestration != null) {
      row.addProperty("orchestration", orchestration);
    }
  }

  private Interfaces unmarshal(InputStream is) throws JAXBException {
    final JAXBContext jaxbContext = JAXBContext.newInstance(Interfaces.class);
    final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    return (Interfaces) jaxbUnmarshaller.unmarshal(is);
  }

}
