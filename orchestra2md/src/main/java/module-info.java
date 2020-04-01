module orchestra2md {
  exports io.fixprotocol.orchestra2md;
  
  opens io.fixprotocol.orchestra2md;
  
  requires md.grammar;
  requires java.xml.bind;
  requires org.apache.logging.log4j;
  requires repository;
  requires commons.cli;
  requires org.apache.logging.log4j.core;
 }
