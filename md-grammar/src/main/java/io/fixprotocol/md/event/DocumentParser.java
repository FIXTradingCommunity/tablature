/*
 * Copyright 2020 FIX Protocol Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package io.fixprotocol.md.event;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.fixprotocol.md.antlr.MarkdownBaseListener;
import io.fixprotocol.md.antlr.MarkdownEventSource;
import io.fixprotocol.md.antlr.MarkdownLexer;
import io.fixprotocol.md.antlr.MarkdownParser;
import io.fixprotocol.md.antlr.MarkdownParser.DocumentContext;

public final class DocumentParser {
  
  /**
   * Listens for parser errors
   */
  public interface ParserErrorListener {
    void parseError(int line, int charPositionInLine, String msg);
  }

  private static class SyntaxErrorListener extends BaseErrorListener {
    private final Logger logger = LogManager.getLogger(getClass());
    private int errors = 0;
    private final ParserErrorListener errorListener;

    public SyntaxErrorListener(ParserErrorListener errorListener) {
      this.errorListener = errorListener;
    }

    public int getErrors() {
      return errors;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
        int charPositionInLine, String msg, RecognitionException e) {
      errors++;
      logError(line, charPositionInLine, msg);
      if (errorListener != null) {
        errorListener.parseError(line, charPositionInLine, msg);
      }
    }

    void logError(int line, int charPositionInLine, String msg) {
      logger.error("Markdown parser failed at line {} position {} due to {}", line,
          charPositionInLine, msg);
    }
  }
  
  /**
   * Parse a markdown document
   *
   * @param inputStream input as markdown
   * @param contextConsumer consumer of document events
   * @return {@code true} if the document is fully parsed without errors
   * @throws IOException if the document cannot be read
   */
  public boolean parse(InputStream inputStream, Consumer<? super Contextual> contextConsumer)
      throws IOException {
    return parse(inputStream, contextConsumer, null);
  }

  /**
   * Parse a markdown document
   *
   * @param inputStream input as markdown
   * @param contextConsumer consumer of document events
   * @param parserListener listens for parser errors. May be {@code null}.
   * @return {@code true} if the document is fully parsed without errors
   * @throws IOException if the document cannot be read
   */
  public boolean parse(InputStream inputStream, Consumer<? super Contextual> contextConsumer, ParserErrorListener parserListener)
      throws IOException {
    final MarkdownLexer lexer = new MarkdownLexer(CharStreams.fromStream(inputStream));
    final MarkdownParser parser = new MarkdownParser(new CommonTokenStream(lexer));
    final SyntaxErrorListener errorListener = new SyntaxErrorListener(parserListener);
    parser.addErrorListener(errorListener);
    final ParseTreeListener listener = new MarkdownEventSource(contextConsumer);
    final ParseTreeWalker walker = new ParseTreeWalker();
    final DocumentContext documentContext = parser.document();
    walker.walk(listener, documentContext);

    final int errors = errorListener.getErrors();
    return (errors == 0);
  }

  /**
   * Validate a markdown document
   *
   * @param inputStream input as markdown
   * @param parserListener listens for parser errors.
   * @return {@code true} if the document is fully parsed without errors
   * @throws IOException if the document cannot be read
   */
  public boolean validate(InputStream inputStream, ParserErrorListener parserListener)
      throws IOException {
    final MarkdownLexer lexer = new MarkdownLexer(CharStreams.fromStream(inputStream));
    final MarkdownParser parser = new MarkdownParser(new CommonTokenStream(lexer));
    final SyntaxErrorListener errorListener = new SyntaxErrorListener(parserListener);
    parser.addErrorListener(errorListener);
    final ParseTreeWalker walker = new ParseTreeWalker();
    final DocumentContext documentContext = parser.document();
    walker.walk(new MarkdownBaseListener(), documentContext);

    final int errors = errorListener.getErrors();
    return (errors == 0);
  }
}
