package io.fixprotocol.md.event.mutable;

import java.util.Arrays;
import io.fixprotocol.md.event.MutableDocumentation;

public class DocumentationImpl extends ContextImpl implements MutableDocumentation {

  private String documentation;

  public DocumentationImpl(int level) {
    super(EMPTY_CONTEXT, level);
  }

  public DocumentationImpl(String documentation) {
    this(EMPTY_CONTEXT, 0, documentation);
  }

  public DocumentationImpl(String[] keys, int level) {
    super(keys, level);
  }

  public DocumentationImpl(String[] keys, int level, String documentation) {
    super(keys, level);
    this.documentation = documentation;
  }

  public DocumentationImpl(String[] keys, String documentation) {
    this(keys, 0, documentation);
  }

  @Override
  public MutableDocumentation documentation(String documentation) {
    this.documentation = documentation;
    return this;
  }

  @Override
  public String getDocumentation() {
    return documentation;
  }

  @Override
  public String toString() {
    return "DocumentationImpl [documentation=" + documentation + ", getKeys()="
        + Arrays.toString(getKeys()) + ", getLevel()=" + getLevel() + "]";
  }

}
