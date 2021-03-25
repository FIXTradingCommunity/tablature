package io.fixprotocol.md.event;

public interface MutableTableColumn extends TableColumn {

  void setHeading(String display);

  int updateLength(int newLength);

}
