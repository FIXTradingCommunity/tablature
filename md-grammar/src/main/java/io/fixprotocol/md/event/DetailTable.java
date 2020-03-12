package io.fixprotocol.md.event;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A Context with a table of values
 * 
 * @author Don Mendelson
 *
 */
public interface DetailTable extends Context {
  
  /**
   * 
   * @return a collection of TableColumn that describes this table
   */
  Collection<? extends TableColumn> getTableColumns();

  /**
   * Supplies a Stream of row values
   * 
   * @return a Stream of DetailProperties
   */
  Supplier<Stream<? extends DetailProperties>> rows();
}
