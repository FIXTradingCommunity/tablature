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
 */
package io.fixprotocol.md.antlr;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.antlr.v4.gui.TestRig;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import io.fixprotocol.md.antlr.MarkdownLexer;
import io.fixprotocol.md.antlr.MarkdownParser;
import io.fixprotocol.md.antlr.MarkdownParser.BlockContext;
import io.fixprotocol.md.antlr.MarkdownParser.CellContext;
import io.fixprotocol.md.antlr.MarkdownParser.DocumentContext;
import io.fixprotocol.md.antlr.MarkdownParser.HeadingContext;
import io.fixprotocol.md.antlr.MarkdownParser.ParagraphContext;
import io.fixprotocol.md.antlr.MarkdownParser.ParagraphlineContext;
import io.fixprotocol.md.antlr.MarkdownParser.TableContext;
import io.fixprotocol.md.antlr.MarkdownParser.TableheadingContext;
import io.fixprotocol.md.antlr.MarkdownParser.TablerowContext;

class MdGrammarTest {

  @ParameterizedTest
  @ValueSource(strings = {"md2orchestra-proto.md"})
  void schemaFile(String fileName) throws IOException {
    MarkdownLexer lexer = new MarkdownLexer(CharStreams.fromStream(new FileInputStream(fileName)));
    MarkdownParser parser = new MarkdownParser(new CommonTokenStream(lexer));
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
          int charPositionInLine, String msg, RecognitionException e) {
        throw new IllegalStateException(String.format(
            "Failed to parse at line %d position %d due to %s", line, charPositionInLine, msg), e);
      }
    });
    DocumentContext document = parser.document();
    List<BlockContext> blocks = document.block();
    for (BlockContext block : blocks) {
      HeadingContext heading = block.heading();
      if (heading != null) {
        // String level = heading.headinglevel().getText();
        String textline = heading.HEADINGLINE().getText();
        System.out.format("Heading text: %s%n", textline);
      } else {
        ParagraphContext paragraph = block.paragraph();
        if (paragraph != null) {
          List<ParagraphlineContext> textlines = paragraph.paragraphline();
          String paragraphText = textlines.stream().map(p -> p.PARAGRAPHLINE().getText())
              .collect(Collectors.joining(" "));
          System.out.format("Paragraph text: %s%n", paragraphText);
        } else {
          TableContext table = block.table();
          if (table != null) {
            TableheadingContext tableHeading = table.tableheading();
            TablerowContext headingRow = tableHeading.tablerow();
            List<CellContext> colHeadings = headingRow.cell();
            for (CellContext colHeading : colHeadings) {
              System.out.format("Column heading: %s%n", colHeading.getText());
            }
            List<TablerowContext> rows = table.tablerow();
            for (TablerowContext row : rows) {
              List<CellContext> cells = row.cell();
              for (CellContext cell : cells) {
                String celltext = cell.CELLTEXT().getText();
                System.out.format("Cell: %s%n", celltext);
              }
            }
          }
        }
      }
    }
  }


  @Disabled
  @ParameterizedTest
  @ValueSource(strings = {"md2orchestra-proto.md"})
  void testRig(String fileName) throws Exception {
    String[] args = new String[] {"io.fixprotocol.md.antlr.Markdown", "document", "-gui", "-tree",
        "-tokens", fileName};
    TestRig testRig = new TestRig(args);
    testRig.process();
  }
}
