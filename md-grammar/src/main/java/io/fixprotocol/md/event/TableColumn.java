package io.fixprotocol.md.event;


public interface TableColumn {
  
  enum Alignment {
    LEFT, CENTER, RIGHT
  }
  
  Alignment getAlignment();

  /**
   * Displayed table heading
   * @return displayable string
   */
  String getHeading();
  
  /**
   * Data key for values displayed in this column
   * @return data key
   */
  String getKey();

  /**
   * Width of the column
   * @return the column width
   */
  int getWidth();

}
