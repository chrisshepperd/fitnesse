// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.slimTables;

import static fitnesse.util.ListUtility.list;
import fitnesse.responders.run.slimResponder.SlimTestContext;

import java.util.List;
import java.util.Map;

public class ImportTable extends SlimTable {
  public ImportTable(Table table, String id, SlimTestContext testContext) {
    super(table, id, testContext);
  }

  public ImportTable(Table table, String id, SlimTestContext testContext, String portType) {
    super(table, id, testContext, portType);
  }

  protected String getTableType() {
    return "import";
  }

  public boolean isTableTypeGlobal() {
    return true;
  }

  public void appendInstructions() {
    int rows = table.getRowCount();
    if (rows < 2)
      throw new SlimTable.SyntaxError("Import tables must have at least two rows.");

    for (int row = 1; row < rows; row++) {
      String importString = table.getCellContents(0, row);
      if (importString.length() > 0) {
        List<Object> importInstruction = prepareInstruction();
        importInstruction.addAll(list("import", importString));
        addInstruction(importInstruction);
      }
    }
  }

  public void evaluateReturnValues(Map<String, Object> returnValues) {
  }

}
