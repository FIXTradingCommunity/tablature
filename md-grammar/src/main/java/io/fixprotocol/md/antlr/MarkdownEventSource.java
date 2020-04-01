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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.fixprotocol.md.antlr.MarkdownParser.BlockContext;
import io.fixprotocol.md.antlr.MarkdownParser.CellContext;
import io.fixprotocol.md.antlr.MarkdownParser.DocumentContext;
import io.fixprotocol.md.antlr.MarkdownParser.HeadingContext;
import io.fixprotocol.md.antlr.MarkdownParser.ListContext;
import io.fixprotocol.md.antlr.MarkdownParser.ListlineContext;
import io.fixprotocol.md.antlr.MarkdownParser.ParagraphContext;
import io.fixprotocol.md.antlr.MarkdownParser.ParagraphlineContext;
import io.fixprotocol.md.antlr.MarkdownParser.TableContext;
import io.fixprotocol.md.antlr.MarkdownParser.TabledelimiterrowContext;
import io.fixprotocol.md.antlr.MarkdownParser.TableheadingContext;
import io.fixprotocol.md.antlr.MarkdownParser.TablerowContext;
import io.fixprotocol.md.event.Context;
import io.fixprotocol.md.event.Documentation;
import io.fixprotocol.md.event.MutableDetailProperties;
import io.fixprotocol.md.event.mutable.ContextImpl;
import io.fixprotocol.md.event.mutable.DetailImpl;
import io.fixprotocol.md.event.mutable.DetailTableImpl;
import io.fixprotocol.md.event.mutable.DocumentationImpl;

/**
 * Generates events for document consumers
 *
 * @author Don Mendelson
 *
 */
public class MarkdownEventSource implements MarkdownListener {

  private static final String CELL_NONTEXT = " |\t";
  private static final String WHITESPACE_REGEX = "[ \t]";

  static String normalizeList(List<ListlineContext> textlines) {
    return textlines.stream().map(p -> p.LISTLINE().getText()).collect(Collectors.joining("\n"));
  }

  static String normalizeParagraph(List<ParagraphlineContext> textlines) {
    return textlines.stream().map(p -> p.PARAGRAPHLINE().getText())
        .collect(Collectors.joining(" "));
  }

  static String trimCell(String text) {
    int beginIndex = 0;
    int endIndex = text.length();
    for (; beginIndex < endIndex
        && (CELL_NONTEXT.indexOf(text.charAt(beginIndex)) != -1); beginIndex++);
    for (; endIndex > beginIndex
        && (CELL_NONTEXT.indexOf(text.charAt(endIndex - 1)) != -1); endIndex--);
    return text.substring(beginIndex, endIndex);
  }

  private final Consumer<Context> contextConsumer;
  private boolean inTableHeading = false;
  private final List<String> lastBlocks = new ArrayList<>();
  private int lastColumnNo;
  private int lastHeadingLevel = 0;
  private String[] lastHeadingWords = null;

  private final List<String> lastRowValues = new ArrayList<>();
  private final List<String> lastTableHeadings = new ArrayList<>();

  private final Logger logger = LogManager.getLogger(getClass());

  public MarkdownEventSource(Consumer<Context> contextConsumer) {
    this.contextConsumer = contextConsumer;
  }

  @Override
  public void enterBlock(BlockContext ctx) {
    // TODO Auto-generated method stub

  }

  @Override
  public void enterCell(CellContext ctx) {
    // TODO Auto-generated method stub

  }

  @Override
  public void enterDocument(DocumentContext ctx) {
    // TODO Auto-generated method stub

  }

  @Override
  public void enterEveryRule(ParserRuleContext ctx) {
    // TODO Auto-generated method stub

  }

  @Override
  public void enterHeading(HeadingContext ctx) {
    supplyLastDocumentation();
    lastBlocks.clear();
  }

  @Override
  public void enterList(ListContext ctx) {
    // TODO Auto-generated method stub

  }

  @Override
  public void enterListline(ListlineContext ctx) {
    // TODO Auto-generated method stub

  }

  @Override
  public void enterParagraph(ParagraphContext ctx) {

  }

  @Override
  public void enterParagraphline(ParagraphlineContext ctx) {
    // TODO Auto-generated method stub

  }

  @Override
  public void enterTable(TableContext ctx) {
    // TODO Auto-generated method stub

  }

  @Override
  public void enterTabledelimiterrow(TabledelimiterrowContext ctx) {
    // TODO Auto-generated method stub

  }

  @Override
  public void enterTableheading(TableheadingContext ctx) {
    lastTableHeadings.clear();
    inTableHeading = true;
  }

  @Override
  public void enterTablerow(TablerowContext ctx) {
    lastColumnNo = 0;
    lastRowValues.clear();
  }

  @Override
  public void exitBlock(BlockContext ctx) {
    // TODO Auto-generated method stub

  }

  @Override
  public void exitCell(CellContext ctx) {
    final String cellText = trimCell(ctx.CELLTEXT().getText());
    if (inTableHeading) {
      lastTableHeadings.add(cellText);
    } else {
      lastRowValues.add(cellText);
    }
    lastColumnNo++;
  }

  @Override
  public void exitDocument(DocumentContext ctx) {
    supplyLastDocumentation();
  }

  @Override
  public void exitEveryRule(ParserRuleContext ctx) {
    // TODO Auto-generated method stub

  }

  @Override
  public void exitHeading(HeadingContext ctx) {
    final String headingLine = ctx.HEADINGLINE().getText();
    // Heading level is length of first word formed with '#'
    lastHeadingLevel = headingLine.indexOf(" ");
    lastHeadingWords = headingLine.substring(lastHeadingLevel + 1).split(WHITESPACE_REGEX);
    contextConsumer.accept(new ContextImpl(lastHeadingWords, lastHeadingLevel));
  }

  @Override
  public void exitList(ListContext ctx) {
    final List<ListlineContext> textlines = ctx.listline();
    lastBlocks.add(normalizeList(textlines));
  }

  @Override
  public void exitListline(ListlineContext ctx) {
    // TODO Auto-generated method stub

  }

  @Override
  public void exitParagraph(ParagraphContext ctx) {
    final List<ParagraphlineContext> textlines = ctx.paragraphline();
    lastBlocks.add(normalizeParagraph(textlines));
  }

  @Override
  public void exitParagraphline(ParagraphlineContext ctx) {
    // TODO Auto-generated method stub

  }

  @Override
  public void exitTable(TableContext ctx) {
    if (!inTableHeading) {
      final DetailTableImpl detailTable = new DetailTableImpl(lastHeadingWords, lastHeadingLevel);
      final List<TablerowContext> tablerows = ctx.tablerow();

      for (final TablerowContext tablerow : tablerows) {
        final MutableDetailProperties detail = detailTable.newRow();

        for (int i = 0; i < lastColumnNo && i < lastTableHeadings.size(); i++) {
          final CellContext cell = tablerow.cell(i);
          if (cell != null) {
            detail.addProperty(lastTableHeadings.get(i), cell.getText());
          } else {
            logger.error("MarkdownEventSource table cell missing in column {}", i);
          }
        }
      }
      if (contextConsumer != null) {
        contextConsumer.accept(detailTable);
      }
    }
  }

  @Override
  public void exitTabledelimiterrow(TabledelimiterrowContext ctx) {
    // TODO Auto-generated method stub

  }

  @Override
  public void exitTableheading(TableheadingContext ctx) {
    inTableHeading = false;
  }

  @Override
  public void exitTablerow(TablerowContext ctx) {
    if (!inTableHeading) {
      final DetailImpl detail = new DetailImpl(lastHeadingWords, lastHeadingLevel);
      for (int i = 0; i < lastColumnNo && i < lastTableHeadings.size(); i++) {
        detail.addProperty(lastTableHeadings.get(i), lastRowValues.get(i));
      }
      if (contextConsumer != null) {
        contextConsumer.accept(detail);
      }
    }
  }

  @Override
  public void visitErrorNode(ErrorNode node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visitTerminal(TerminalNode node) {
    // TODO Auto-generated method stub

  }

  private void supplyLastDocumentation() {
    if (!lastBlocks.isEmpty()) {
      final String paragraphs = normalizeParagraphs();
      final Documentation documentation =
          new DocumentationImpl(lastHeadingWords, lastHeadingLevel, paragraphs);
      contextConsumer.accept(documentation);
    }
  }

  String normalizeParagraphs() {
    return String.join("\n", lastBlocks);
  }

}
