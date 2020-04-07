module md2orchestra {
  exports io.fixprotocol.md2orchestra;

  opens io.fixprotocol.md2orchestra;

  requires md.grammar;
  requires orchestra.repository;
  requires commons.cli;
  requires org.apache.logging.log4j;
  requires org.apache.logging.log4j.core;
}
