package io.fixprotocol.md.event;

import io.fixprotocol.md.event.mutable.ContextImpl;
import io.fixprotocol.md.event.mutable.DetailImpl;
import io.fixprotocol.md.event.mutable.DetailTableImpl;
import io.fixprotocol.md.event.mutable.DocumentationImpl;

public class ContextFactory {
  public MutableContext createContext(int level) {
    return new ContextImpl(level);
  }

  public MutableContext createContext(String[] keys, int level) {
    return new ContextImpl(keys, level);
  }

  public MutableDetail createDetail(int level) {
    return new DetailImpl(level);
  }

  public MutableDetail createDetail(String[] keys, int level) {
    return new DetailImpl(keys, level);
  }

  public MutableDetailTable createDetailTable(int level) {
    return new DetailTableImpl(level);
  }

  public MutableDetailTable createDetailTable(String[] keys, int level) {
    return new DetailTableImpl(keys, level);
  }

  public MutableDocumentation createDocumentation(int level) {
    return new DocumentationImpl(level);
  }

  public MutableDocumentation createDocumentation(String[] keys, int level) {
    return new DocumentationImpl(keys, level);
  }
}
