package io.fixprotocol.md.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class StringUtilTest {

  @Test
  void testConvertToTitleCase() {
    assertEquals("Synopsis", StringUtil.convertToTitleCase("synopsis"));
    assertEquals("My Data", StringUtil.convertToTitleCase("my data"));
    assertEquals("Synopsis", StringUtil.convertToTitleCase("SYNOPSIS"));
    assertEquals("My Data", StringUtil.convertToTitleCase("MY DATA"));
  }

}
