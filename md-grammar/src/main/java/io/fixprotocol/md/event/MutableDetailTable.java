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

public interface MutableDetailTable extends DetailTable, MutableDocumentation {

  /**
   * Adds a collection key-value pairs to an array
   *
   * @param detailProperties a collection key-value pairs
   * @return the added collection
   */
  DetailProperties addProperties(DetailProperties detailProperties);

  /**
   * Creates a new row and adds it to this table
   *
   * @return a new row instance
   */
  MutableDetailProperties newRow();

}
