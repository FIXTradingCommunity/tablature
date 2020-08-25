package io.fixprotocol.tablature.event.log4j2;

import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.fixprotocol.tablature.event.Event;
import io.fixprotocol.tablature.event.EventListener;

/**
 * Serializes events using log4j2
 *
 * @author Don Mendelson
 *
 */
public class EventLogger implements EventListener {

  private final Logger logger;
  private volatile boolean isOpen = true;

  /**
   * Uses a Logger qualified by this class name
   */
  EventLogger() {
    logger = LogManager.getLogger(EventLogger.class);
  }

  /**
   * Uses a supplied Logger
   *
   * @param logger a log4j2 Logger
   */
  EventLogger(Logger logger) {
    this.logger = logger;
  }

  @Override
  public void close() throws Exception {
    isOpen = false;
  }

  @Override
  public void event(Event event) {
    if (isOpen) {
      switch (event.getSeverity()) {
        case INFO:
          logger.info(event.getMessage());
          break;
        case ERROR:
          logger.error(event.getMessage());
          break;
        case FATAL:
          logger.fatal(event.getMessage());
          break;
        case WARN:
          logger.warn(event.getMessage());
          break;
      }
    }
  }

}
