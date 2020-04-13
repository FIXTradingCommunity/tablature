module md2orchestra {
  exports io.fixprotocol.md2orchestra;

  opens io.fixprotocol.md2orchestra;

  requires java.xml.bind;
  requires jaxb2.basics.runtime;
  requires md.grammar;
  requires repository;
  requires commons.cli;
  requires org.apache.logging.log4j;
  requires org.apache.logging.log4j.core;
}
