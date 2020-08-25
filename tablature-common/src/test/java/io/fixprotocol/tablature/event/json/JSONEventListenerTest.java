package io.fixprotocol.tablature.event.json;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.fixprotocol.tablature.event.Event;
import static io.fixprotocol.tablature.event.Event.Severity.*;

/**
 * Serializes events as JSON
 * 
 * @author Don Mendelson
 *
 */
class JSONEventListenerTest {

  private JSONEventListener listener;
  private ByteArrayOutputStream outputStream;
  
  @BeforeEach
  void setUp() throws Exception {
    outputStream = new ByteArrayOutputStream(4096);
    listener = new JSONEventListener(outputStream);
  }

  @Test
  void message() throws Exception {
    Event event1 = new Event(ERROR, "Error message");
    listener.event(event1);
    listener.close();
    assertEquals("{\"severity\":\"ERROR\",\"message\":\"Error message\"}", 
        outputStream.toString());
  }
  
  @Test
  void withArguments() throws Exception {
    Event event1 = new Event(ERROR, "Error message {0}", 23);
    listener.event(event1);
    listener.close();
    assertEquals("{\"severity\":\"ERROR\",\"message\":\"Error message 23\"}", 
        outputStream.toString());
  }

}
