package io.fixprotocol.md.antlr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

// black box testing for now
@Disabled
class MarkdownSourceTest {

  @Test
  void testTrimCell() {
    String text = "| Cross (orders where counterparty is an exchange, valid for all messages *except* IOIs) ";
    String trimmed = MarkdownEventSource.trimCell(text);
    assertEquals('C', trimmed.charAt(0));
    assertEquals(')', trimmed.charAt(trimmed.length()-1));
  }

}
