// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.responders.run;

import fitnesse.testutil.MockCommandRunner;
import static fitnesse.testutil.RegexTestCase.assertSubString;
import fitnesse.wiki.InMemoryPage;
import fitnesse.wiki.WikiPage;
import fitnesse.components.CommandRunnerGroup;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

public class CompositeExecutionLogTest {
  private static String ErrorLogName = ExecutionLog.ErrorLogName;
  private WikiPage testPage;
  private MockCommandRunner runner;
  private CompositeExecutionLog log;
  private WikiPage root;

  @Before
  public void setUp() throws Exception {
    root = InMemoryPage.makeRoot("RooT");
    testPage = root.addChildPage("TestPage");
    runner = new MockCommandRunner("some command", 123);
    log = new CompositeExecutionLog(testPage);
  }

  @Test
  public void publish() throws Exception {
    log.add("testSystem1", new ExecutionLog(testPage, new CommandRunnerGroup(runner)));
    log.add("testSystem2", new ExecutionLog(testPage, new CommandRunnerGroup(runner)));
    log.publish();
    WikiPage errorLogPage = root.getChildPage(ErrorLogName);
    assertNotNull(errorLogPage);
    WikiPage testErrorLog = errorLogPage.getChildPage("TestPage");
    assertNotNull(testErrorLog);
    String content = testErrorLog.getData().getContent();

    assertSubString("!3 !-testSystem1", content);
    assertSubString("!3 !-testSystem2", content);
    assertSubString("'''Command: '''", content);
    assertSubString("!-some command-!", content);
    assertSubString("'''Exit code: '''", content);
    assertSubString("123", content);
    assertSubString("'''Date: '''", content);
    assertSubString("'''Time elapsed: '''", content);
  }

}
