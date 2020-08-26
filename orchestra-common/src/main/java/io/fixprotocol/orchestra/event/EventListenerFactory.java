package io.fixprotocol.orchestra.event;

import io.fixprotocol.orchestra.event.json.JSONEventListener;
import io.fixprotocol.orchestra.event.log4j2.EventLogger;

/**
 * Creates instances of EventListener implementations
 * 
 * Todo: use a Provider class to plug in new service implementations. For now, keys are hard-coded.
 * 
 * @author Don Mendelson
 */
public class EventListenerFactory {

  /**
   * Create an instance of EventListener
   * 
   * Available types:
   * <ul>
   * <li>JSON</li>
   * <li>LOG4J</li>
   * </ul>
   * 
   * @param type key to an EventListener implementation
   * @return a new instance of EventListener
   * @throws IllegalArgumentException if an unknown type is requested
   */
  public EventListener getInstance(String type) {
    switch (type) {
      case "JSON":
        return new JSONEventListener();
      case "LOG4J":
        return new EventLogger();
      default:
        throw new IllegalArgumentException("Unknown type");
    }
  }
}
