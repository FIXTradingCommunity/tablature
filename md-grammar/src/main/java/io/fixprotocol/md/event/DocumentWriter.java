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
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DocumentWriter implements AutoCloseable {
  enum Alignment {
    CENTER, LEFT, RIGHT
  }

  private static final char[] CELL_PREFIX = "| ".toCharArray();
  private static final char[] HEADING_LEVELS = "######".toCharArray();
  private static final char[] HYPHENS = new char[128];
  private static final char[] SPACES = new char[128];

  private final Logger logger = LogManager.getLogger(getClass());
  private final Writer writer;

  public DocumentWriter(Writer writer) {
    this.writer = writer;
    Arrays.fill(SPACES, ' ');
    Arrays.fill(HYPHENS, '-');
  }

  @Override
  public void close() throws Exception {
    writer.close();
  }

  public void write(Context context) throws IOException {
    writer.write(HEADING_LEVELS, 0, context.getLevel());
    writer.write(" ");
    writer.write(String.join(" ", context.getKeys()));
    writer.write("\n\n");
  }

  public void write(Detail t) throws IOException {
    // TODO Auto-generated method stub

  }

  // todo: alignment based on column class - align right if numeric, etc.
  public void write(DetailTable detailTable) throws IOException {
    try {
      write((Documentation) detailTable);
      final Collection<? extends TableColumn> tableColumns = detailTable.getTableColumns();
      writeTableHeadings(tableColumns);
      writeTableDelimiters(tableColumns);
      detailTable.rows().get().forEach(row -> {
        try {
          writeTableRow(tableColumns, row);
        } catch (final IOException e) {
          throw new RuntimeException(e);
        }
      });
    } catch (final RuntimeException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof IOException) {
        throw (IOException) cause;
      } else {
        throw e;
      }
    }
    writer.write("\n");
  }

  public void write(Documentation context) throws IOException {
    write((Context) context);
    final String documentation = context.getDocumentation();
    if (documentation != null) {
      writer.write(documentation);
      writer.write("\n\n");
    }
  }

  public void write(Iterable<? extends Context> contextSupplier) throws IOException {
    final Consumer<? super Context> action = (Consumer<Context>) t -> {
      try {
        if (t instanceof Documentation) {
          write((Documentation) t);
        } else if (t instanceof DetailTable) {
          write((DetailTable) t);
        } else if (t instanceof Detail) {
          write((Detail) t);
        } else {
          write(t);
        }

      } catch (final IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    };
    contextSupplier.forEach(action);
  }

  private void writeCell(String value, int length) throws IOException {
    writer.write(CELL_PREFIX);
    writer.write(value);
    final int spaces = Math.min(length - value.length() + 1, SPACES.length - 1);
    writer.write(SPACES, 0, spaces);
  }

  private void writeTableDelimiters(Collection<? extends TableColumn> tableColumns)
      throws IOException {
    for (final TableColumn column : tableColumns) {
      writer.write("|");
      final int hyphens = Math.min(column.getWidth() + 2, HYPHENS.length - 1);
      writer.write(HYPHENS, 0, hyphens);
    }
    writer.write("|\n");
  }

  private void writeTableHeadings(Collection<? extends TableColumn> tableColumns)
      throws IOException {
    for (final TableColumn column : tableColumns) {
      writer.write(CELL_PREFIX);
      writer.write(column.getHeading());
      final int spaces =
          Math.min(column.getWidth() - column.getHeading().length() + 1, SPACES.length - 1);
      writer.write(SPACES, 0, spaces);
    }
    writer.write("|\n");
  }

  private void writeTableRow(Collection<? extends TableColumn> tableColumns, DetailProperties row)
      throws IOException {
    for (final TableColumn column : tableColumns) {
      final String key = column.getKey();
      String value = row.getProperty(key);
      if (value == null) {
        value = "";
      }
      writeCell(value, column.getWidth());
    }
    writer.write("|\n");
  }
}
