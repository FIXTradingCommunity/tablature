package io.fixprotocol.md.event;



public interface MutableContextual {

  /**
   * Set a parent Context to build a hierarchy
   *
   * @param parent parent Context
   */
  void setParent(Context parent);

}
