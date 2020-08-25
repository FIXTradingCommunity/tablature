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
  private Event event1;
  private Event event2;
  
  JSONEventListenerTest() {
    event1 = new Event(WARN, "Warning message");
    event2 = new Event(ERROR, "Error message {0}", 23);
  }

  @BeforeEach
  void setUp() throws Exception {
    outputStream = new ByteArrayOutputStream(4096);
    listener = new JSONEventListener(outputStream);
  }

  @Test
  void message() throws Exception {
    listener.event(event1);
    listener.close();
    assertEquals("{\"events\":[{\"severity\":\"WARN\",\"message\":\"Warning message\"}]}", 
        outputStream.toString());
  }
  
  @Test
  void withArguments() throws Exception {
    listener.event(event2);
    listener.close();
    assertEquals("{\"events\":[{\"severity\":\"ERROR\",\"message\":\"Error message 23\"}]}", 
        outputStream.toString());
  }
  
  @Test
  void multiple() throws Exception {
    listener.event(event1);
    listener.event(event2);
    listener.close();
    assertEquals("{\"events\":[{\"severity\":\"WARN\",\"message\":\"Warning message\"},{\"severity\":\"ERROR\",\"message\":\"Error message 23\"}]}", 
        outputStream.toString());
  }

}
