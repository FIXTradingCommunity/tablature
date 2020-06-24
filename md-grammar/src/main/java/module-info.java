module md.grammar {
  exports io.fixprotocol.md.event;

  opens io.fixprotocol.md.event;

  exports io.fixprotocol.md.antlr;

  opens io.fixprotocol.md.antlr;

  requires org.apache.logging.log4j;
  requires org.antlr.antlr4.runtime;
  requires antlr4;
}
