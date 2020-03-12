package io.fixprotocol.md.antlr;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarkdownSourceTest {

  @Test
  void testTrimCell() {
    String text = "| Cross (orders where counterparty is an exchange, valid for all messages *except* IOIs) ";
    String trimmed = MarkdownEventSource.trimCell(text);
    assertEquals('C', trimmed.charAt(0));
    assertEquals(')', trimmed.charAt(trimmed.length()-1));
  }

}
