package io.fixprotocol.md.event;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ConsumerTest {

  @ParameterizedTest
  @ValueSource(strings = {"md2orchestra-proto.md"})
  void consume(String fileName) throws IOException {
    Consumer<Context> contextConsumer = new Consumer<Context>() {

      @Override
      public void accept(Context context) {
        if (context instanceof Detail) {
          Detail detail = (Detail) context;
          final String[] keys = detail.getKeys();
          System.out.format("Detail context=%s level=%d%n", keys.length > 0 ? keys[0] : "None",
              detail.getLevel());
          detail.getProperties().forEach(property -> System.out.format("Property key=%s value=%s%n",
              property.getKey(), property.getValue()));
        } else if (context instanceof Documentation) {
          Documentation documentation = (Documentation) context;
          final String[] keys = documentation.getKeys();
          System.out.format("Documentation context=%s level=%d %s%n",
              keys.length > 0 ? keys[0] : "None", documentation.getLevel(),
              documentation.getDocumentation());
        } else {
          final String[] keys = context.getKeys();
          System.out.format("Context=%s level=%d%n", keys.length > 0 ? keys[0] : "None",
              context.getLevel());
        }
      }
    };


    InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
    DocumentParser parser = new DocumentParser();
    parser.parse(inputStream, contextConsumer);
  }

}
