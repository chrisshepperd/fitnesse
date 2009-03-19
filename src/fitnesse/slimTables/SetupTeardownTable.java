// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.slimTables;

import fitnesse.responders.run.slimResponder.SlimTestContext;

import java.util.*;

public class SetupTeardownTable extends DecisionTable {

  public SetupTeardownTable(Table table, String id, SlimTestContext context) {
    super(table, id, context);
  }

  public SetupTeardownTable(Table table, String id, SlimTestContext context, String portType) {
    super(table, id, context, portType);
  }

  @Override
  public boolean isTableTypeGlobal() {
    return true;
  }
}