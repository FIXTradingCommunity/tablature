package io.fixprotocol.tablature.event;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Delegates events to multiple listeners
 * 
 * @author Don Mendelson
 *
 */
public class TeeEventListener implements EventListener {

  List<EventListener> listeners = new CopyOnWriteArrayList<>();

  public void addEventListener(EventListener listener) {
    listeners.add(Objects.requireNonNull(listener, "EventListener missing"));
  }

  @Override
  public void close() throws Exception {
    for (final EventListener listener : listeners) {
      listener.close();
    }
  }

  @Override
  public void event(Event event) {
    for (final EventListener listener : listeners) {
      listener.event(event);
    }
  }

  public void removeEventListener(EventListener listener) {
    listeners.remove(listener);
  }

}
