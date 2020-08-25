package io.fixprotocol.tablature.event;

import static io.fixprotocol.tablature.event.Event.Severity.ERROR;
import static io.fixprotocol.tablature.event.Event.Severity.WARN;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.fixprotocol.tablature.event.json.JSONEventListener;

class TeeEventListenerTest {

  private TeeEventListener tee;

  @BeforeEach
  void setUp() throws Exception {
    tee = new TeeEventListener();
  }

  @Test
  void testEvent() throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(4096);
    JSONEventListener json = new JSONEventListener(outputStream);   
    tee.addEventListener(json);
    Event event1 = new Event(WARN, "Warning message");
    Event event2 = new Event(ERROR, "Error message {0}", 23);
    tee.event(event1);
    tee.event(event2);
    tee.close();
    assertTrue(outputStream.toString().length() > 0);
  }

}
