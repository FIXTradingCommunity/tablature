package io.fixprotocol.md2orchestra.util;

import java.util.Arrays;

public class IdGenerator {
  
  private final int minValue;
  private final int maxValue;

  /**
   * Default range is zero to maximum integer value
   */
  public IdGenerator() {
    this(0, Integer.MAX_VALUE);
  }
  
  /**
   * Constructor with a range
   * @param minValue minimum ID value
   * @param maxValue maximum ID value
   */
  public IdGenerator(int minValue, int maxValue) {
    this.minValue = minValue;
    this.maxValue = maxValue;
  }
  
  /**
   * Generate an ID using one or more strings as a seed
   * @param seed names of some object to assign its ID
   * @return a numeric ID in the specified range
   */
  public int generate(String... seeds) {  
    return Math.abs(Arrays.hashCode(seeds)) % (maxValue - minValue) + minValue;
  }

}
