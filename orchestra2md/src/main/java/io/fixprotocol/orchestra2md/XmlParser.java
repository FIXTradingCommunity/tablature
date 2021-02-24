package io.fixprotocol.orchestra2md;

import java.io.InputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import io.fixprotocol._2020.orchestra.repository.Repository;
import io.fixprotocol.orchestra.event.EventListener;

final class XmlParser {

  public static Repository unmarshal(InputStream is, EventListener eventLogger)
      throws JAXBException {
    final JAXBContext jaxbContext = JAXBContext.newInstance(Repository.class);
    final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    jaxbUnmarshaller.setEventHandler(new ValidationEventHandler() {

      @Override
      public boolean handleEvent(ValidationEvent event) {
        switch (event.getSeverity()) {
          case 0: // warning
            eventLogger.warn("Error parsing XML; {0} at line {1} col {2}", event.getMessage(),
                event.getLocator().getLineNumber(), event.getLocator().getColumnNumber());
            break;
          case 1: // error
            eventLogger.error("Error parsing XML; {0} at line {1} col {2}", event.getMessage(),
                event.getLocator().getLineNumber(), event.getLocator().getColumnNumber());
            break;
          case 2: // fatal error
            eventLogger.fatal("Error parsing XML; {0} at line {1} col {2}", event.getMessage(),
                event.getLocator().getLineNumber(), event.getLocator().getColumnNumber());
            return false;
        }
        return true; // continue
      }

    });
    return (Repository) jaxbUnmarshaller.unmarshal(is);
  }
}
