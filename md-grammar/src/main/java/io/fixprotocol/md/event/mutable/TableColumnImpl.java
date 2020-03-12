package io.fixprotocol.md.event.mutable;

import io.fixprotocol.md.event.TableColumn;

class TableColumnImpl implements TableColumn {
  private final Alignment alignment;
  private String display = null;
  private final String key;
  private int length;

  public TableColumnImpl(String key) {
    this(key, 0, Alignment.LEFT);
  }

  public TableColumnImpl(String key, int length) {
    this(key, length, Alignment.LEFT);
  }

  public TableColumnImpl(String key, int length, Alignment alignment) {
    this.key = key;
    this.length = length;
    this.alignment = alignment;
  }

  @Override
  public Alignment getAlignment() {
    return alignment;
  }

  @Override
  public String getHeading() {
    if (display == null) {
      StringBuilder sb = new StringBuilder();
      sb.append(Character.toUpperCase(key.charAt(0)));
      for (int i = 1; i < key.length(); i++) {
        if (Character.isWhitespace(key.charAt(i - 1))) {
          sb.append(Character.toUpperCase(key.charAt(0)));
        } else {
          sb.append(key.charAt(i));
        }
      }
      display = sb.toString();
    }
    return display;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public int getWidth() {
    return length;
  }

  public void setHeading(String display) {
    this.display = display;
    updateLength(display.length());
  }

  public int updateLength(int newLength) {
    this.length = Math.max(length, newLength);
    return length;
  }

}
