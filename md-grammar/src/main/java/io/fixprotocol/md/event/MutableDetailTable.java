package io.fixprotocol.md.event;

public interface MutableDetailTable extends DetailTable, MutableDocumentation {

  /**
   * Adds a collection key-value pairs to an array
   * 
   * @param detailProperties a collection key-value pairs
   * @return the added collection
   */
  DetailProperties addProperties(DetailProperties detailProperties);

  /**
   * Creates a new row and adds it to this table
   * 
   * @return a new row instance
   */
  MutableDetailProperties newRow();

}
