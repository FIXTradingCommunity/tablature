package io.fixprotocol.md.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AssociativeSetTest {

  @Test
  void testAdd() {
    AssociativeSet set =  new AssociativeSet();
    set.add("A", "Apple");
    set.add("E", "Elephant");
    set.add("X", "Xylophone");
    set.add("Z", "Zebra");
    assertEquals(4, set.size());
    assertEquals("Apple", set.get("A"));
    assertEquals("Z", set.getSecond("Zebra"));
    
    assertFalse(set.add("A", "Asparagus"));
    assertFalse(set.add("Q", "Zebra"));
  }

}
