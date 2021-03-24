module md2orchestra {
  exports io.fixprotocol.md2orchestra;
  exports io.fixprotocol.md2orchestra.util;

  opens io.fixprotocol.md2orchestra;
  opens io.fixprotocol.md2orchestra.util;

  requires java.xml.bind;
  requires jaxb2.basics.runtime;
  requires md.grammar;
  requires orchestra.repository;
  requires commons.cli;
  requires transitive org.apache.logging.log4j;
  requires org.apache.logging.log4j.core;
  requires jaxb.impl;
  requires orchestra.common;
}
