package io.fixprotocol.md2orchestra.util;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class IdGeneratorTest {

  @Test
  void defaultRange() {
    IdGenerator generator = new IdGenerator();
    List<String> seeds =  List.of("AdvSideCodeSet", "", "OverTheDay", "CancelOnConnectionLoss", "NewOrderSingle", "AllocationInstructionAck");
    List<Integer> ids = new ArrayList<>();
    for (String seed: seeds) {
      ids.add(generator.generate(seed));
    }
    for (Integer id : ids) {
      assertTrue(id >= 0);
    }
    List<Integer> distinct = ids.stream().distinct().collect(Collectors.toList());
    assertEquals(seeds.size(), distinct.size());
  }
  
  @Test
  void withRange() {
    int max = 39999;
    int min = 5000;
    IdGenerator generator = new IdGenerator(min, max);
    List<String> seeds =  List.of("AdvSideCodeSet", "", "OverTheDay", "CancelOnConnectionLoss", "NewOrderSingle", "AllocationInstructionAck");
    List<Integer> ids = new ArrayList<>();
    for (String seed: seeds) {
      ids.add(generator.generate(seed));
    }
    for (Integer id : ids) {
      assertTrue(id >= min);
      assertTrue(id <= max);
    }
    List<Integer> distinct = ids.stream().distinct().collect(Collectors.toList());
    assertEquals(seeds.size(), distinct.size());
  }

}
