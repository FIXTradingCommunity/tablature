package io.fixprotocol.tablature.event.json;

import java.io.IOException;
import java.io.OutputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fixprotocol.tablature.event.Event;
import io.fixprotocol.tablature.event.EventListener;

public class JSONEventListener implements EventListener, AutoCloseable {

  private final OutputStream outputStream;
  private final ObjectMapper mapper = new ObjectMapper();


  public JSONEventListener(OutputStream outputStream) throws IOException {
    this.outputStream = outputStream;
  }

  @Override
  public void close() throws Exception {
    outputStream.close();
  }

  @Override
  public void event(Event event) {
    try {
      outputStream.write(mapper.writeValueAsBytes(event));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
