package io.fixprotocol.md.event;

public interface MutableDocumentation extends Documentation, MutableContext {

  MutableDocumentation documentation(String documentation);
}
