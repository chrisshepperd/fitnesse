// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.responders.revisioncontrol;

import static fitnesse.revisioncontrol.RevisionControlOperation.CHECKIN;
import fitnesse.wiki.FileSystemPage;

public class CheckinResponder extends RevisionControlResponder {

  public CheckinResponder() {
    super(CHECKIN);
  }

  @Override
  protected void performOperation(FileSystemPage page) throws Exception {
    page.execute(CHECKIN);
  }

}
