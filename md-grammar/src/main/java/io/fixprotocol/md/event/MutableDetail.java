package io.fixprotocol.md.event;

public interface MutableDetail extends Detail, MutableContext {

  void addIntProperty(String key, int value);

  void addProperty(String key, String value);

}
