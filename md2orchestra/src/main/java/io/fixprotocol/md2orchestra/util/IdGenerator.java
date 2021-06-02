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
  public IdGenerator(final int minValue, final int maxValue) {
    this.minValue = minValue;
    this.maxValue = maxValue;
  }
  
  /**
   * Generate an ID using one or more strings as a seed
   * @param seeds names of some object to assign its ID
   * @return a numeric ID in the specified range
   */
  public int generate(final String... seeds) {
    return Math.abs(Arrays.hashCode(seeds)) % (maxValue - minValue) + minValue;
  }

}
