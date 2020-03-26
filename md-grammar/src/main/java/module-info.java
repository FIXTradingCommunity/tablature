module md.grammar {
  exports io.fixprotocol.md.event;
  opens io.fixprotocol.md.event;
  exports io.fixprotocol.md.util;
  opens io.fixprotocol.md.util;
  requires org.apache.logging.log4j;
  requires org.antlr.antlr4.runtime;
}