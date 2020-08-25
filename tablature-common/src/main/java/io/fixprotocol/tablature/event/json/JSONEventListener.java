package io.fixprotocol.tablature.event.json;

import java.io.IOException;
import java.io.OutputStream;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.fixprotocol.tablature.event.Event;
import io.fixprotocol.tablature.event.EventListener;

public class JSONEventListener implements EventListener {

  private final JsonGenerator generator;

  public JSONEventListener(OutputStream outputStream) throws IOException {
    final JsonFactory factory = new JsonFactory();
    generator = factory.createGenerator(outputStream);
    generator.writeStartObject();
    generator.writeArrayFieldStart("events");
  }

  @Override
  public void close() throws Exception {
    generator.close();
  }

  @Override
  public void event(Event event) {
    try {
      generator.writeStartObject();
      generator.writeObjectField("severity", event.getSeverity().name());
      generator.writeObjectField("message", event.getMessage());
      generator.writeEndObject();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

}
