package io.fixprotocol.md.event;


/**
 * Something that has a Context
 *
 * @author Don Mendelson
 *
 */
public interface Contextual {

  /**
   * Returns the Context to which an object belongs, or a broader Context
   *
   * @return a parent Context or {@code null} if there is no parent (root context)
   */
  Context getParent();

}
