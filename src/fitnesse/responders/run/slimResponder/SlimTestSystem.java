// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.responders.run.slimResponder;

import fitnesse.components.CommandRunner;
import fitnesse.components.CommandRunnerGroup;
import fitnesse.responders.run.ExecutionLog;
import fitnesse.responders.run.TestSummary;
import fitnesse.responders.run.TestSystem;
import fitnesse.responders.run.TestSystemListener;
import fitnesse.slim.*;
import fitnesse.testutil.MockCommandRunner;
import fitnesse.wiki.PageData;
import fitnesse.wiki.WikiPage;
import fitnesse.slimTables.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SlimTestSystem extends TestSystem implements SlimTestContext {
  private CommandRunnerGroup slimRunners = new CommandRunnerGroup();
  private SlimClientGroup slimClientGroup = new SlimClientGroup();
  private String defaultCommandLine;
  private boolean started;
  protected TableScanner tableScanner;
  protected PageData testResults;
  protected Map<String, Object> instructionResults;
  protected List<SlimTable> testTables = new ArrayList<SlimTable>();
  protected Map<String, String> exceptions = new HashMap<String, String>();
  private Map<String, String> symbols = new HashMap<String, String>();
  protected TestSummary testSummary;
  private static AtomicInteger slimSocketOffset = new AtomicInteger(0);
  protected final Pattern exceptionMessagePattern = Pattern.compile("message:<<(.*)>>");
  private Map<String, ScenarioTable> scenarios = new HashMap<String, ScenarioTable>();
  private List<SlimTable.Expectation> expectations = new ArrayList<SlimTable.Expectation>();
  private String classPath;
  private Descriptor descriptor;

  public SlimTestSystem(WikiPage page, TestSystemListener listener) {
    super(page, listener);
    testSummary = new TestSummary(0, 0, 0, 0);
  }

  public String getSymbol(String symbolName) {
    return symbols.get(symbolName);
  }

  public void setSymbol(String symbolName, String value) {
    symbols.put(symbolName, value);
  }

  public void addScenario(String scenarioName, ScenarioTable scenarioTable) {
    scenarios.put(scenarioName, scenarioTable);
  }

  public ScenarioTable getScenario(String scenarioName) {
    return scenarios.get(scenarioName);
  }

  public void addExpectation(SlimTable.Expectation e) {
    expectations.add(e);
  }

  public boolean isSuccessfullyStarted() {
    return started;
  }

  public void kill() throws Exception {
    if (slimRunners != null)
      slimRunners.kill();
    if (slimClientGroup != null)
      slimClientGroup.close();
  }

  String getSlimFlags() throws Exception {
    String slimFlags = page.getData().getVariable("SLIM_FLAGS");
    if (slimFlags == null)
      slimFlags = "";
    return slimFlags;
  }

  protected ExecutionLog createExecutionLog(String classPath, Descriptor descriptor) throws Exception {
    this.classPath = classPath;
    this.descriptor = descriptor;

    createRunner(SlimTable.DEFAULT_PORT_TYPE, descriptor, false);
    defaultCommandLine = slimRunners.getFirstCommandRunner().getCommand();
    slimRunners.getCommandRunners().clear();
    slimClientGroup.getSlimClients().clear();

    return new ExecutionLog(page, slimRunners);
  }

  private void createRunner(String portType, Descriptor descriptor, boolean useAsServer) throws Exception {
    String slimArguments = getSlimFlags();
    int slimSocket = getNextSlimSocket();
    String slimCommand = buildCommand(descriptor, classPath, slimArguments, slimSocket+"");
    CommandRunner slimRunner;

    if (fastTest) {
      slimRunner = new MockCommandRunner();
      createSlimService(String.format("%s %d", slimArguments, slimSocket));
    } else {
      slimRunner = new CommandRunner(slimCommand, "");
    }

    slimRunners.addCommandRunner(portType, slimRunner);
    slimClientGroup.addSlimClient(portType, new SlimClient("localhost", slimSocket, useAsServer));
  }

  private boolean useFitNesseAsServer(String portType, PageData pageData) throws Exception {
    String useAsServer = pageData.getVariable(String.format("%s_%s", portType.toUpperCase(), "USE_AS_SERVER"));
    return useAsServer != null && Boolean.valueOf(useAsServer);
  }

  public int getNextSlimSocket() {
    synchronized (slimSocketOffset) {
      int base = slimSocketOffset.get();
      base++;
      if (base >= 10)
        base = 0;
      slimSocketOffset.set(base);
      return base + 8085;
    }
  }

  public void start() throws Exception {
    slimRunners.asynchronousStart();
    try {
      waitForConnection();
      started = true;
    } catch (SlimError e) {
      testSystemListener.exceptionOccurred(e);
    }
  }

  public String getDefaultCommandLine() {
    return defaultCommandLine;
  }

  public void bye() throws Exception {
    slimClientGroup.sendBye();
    if (!fastTest)
      slimRunners.kill();
  }

  //For testing only.  Makes responder faster.
  void createSlimService(String args) throws Exception {
    while (!tryCreateSlimService(args))
      Thread.sleep(10);
  }

  private boolean tryCreateSlimService(String args) throws Exception {
    try {
      SlimService.main(args.trim().split(" "));
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  void waitForConnection() throws Exception {
    while (!isConnected())
      Thread.sleep(50);
  }

  private boolean isConnected() throws Exception {
    try {
      slimClientGroup.connect();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public String runTestsAndGenerateHtml(PageData pageData) throws Exception {
    testTables.clear();
    symbols.clear();
    exceptions.clear();
    testSummary.clear();
    runTestsOnPage(pageData);
    testResults = pageData;
    String html = createHtmlResults();
    acceptOutputFirst(html);
    acceptResultsLast(testSummary);
    return html;
  }

  protected abstract String createHtmlResults() throws Exception;

  void runTestsOnPage(PageData pageData) throws Exception {
    tableScanner = scanTheTables(pageData);
    Map<String, List<Object>> instructionMap = createInstructions(this);
    instructionResults = new HashMap<String, Object>();
    for (String portType : instructionMap.keySet()) {
      if (!slimClientGroup.hasPortType(portType)) {
        Descriptor newDescriptor = descriptor.clone();
        newDescriptor.commandPattern = getCommandPattern(portType, pageData);
        createRunner(portType, newDescriptor, useFitNesseAsServer(portType, pageData));
        start();
      }
      instructionResults.putAll(slimClientGroup.invokeAndGetResponse(portType, instructionMap.get(portType)));
    }
  }

  protected static String getCommandPattern(String portType, PageData pageData) throws Exception {
    if (!SlimTable.DEFAULT_PORT_TYPE.equals(portType)) {
      String variable = String.format("%s_%s", portType.toUpperCase(), "COMMAND_PATTERN");
      String testRunner = pageData.getVariable(variable);
      if (testRunner != null)
        return testRunner;
    }
    return getCommandPattern(pageData);
  }

  protected abstract TableScanner scanTheTables(PageData pageData) throws Exception;

  private Map<String, List<Object>> createInstructions(SlimTestContext slimTestContext) {
    Map<String, List<Object>> instructionMap = new HashMap<String, List<Object>>();

    for (Table table : tableScanner) {
      String tableId = "" + testTables.size();
      SlimTable slimTable = makeSlimTable(table, tableId, slimTestContext);
      if (slimTable != null) {
        if (!instructionMap.containsKey(slimTable.getPortType()))
          instructionMap.put(slimTable.getPortType(), new ArrayList<Object>());

        slimTable.appendInstructions(instructionMap.get(slimTable.getPortType()));
        testTables.add(slimTable);
      }
    }

    return instructionMap;
  }

  private SlimTable makeSlimTable(Table table, String tableId, SlimTestContext slimTestContext) {
    String tableType = table.getCellContents(0, 0);
    if (beginsWith(tableType, "dt:") || beginsWith(tableType, "decision:"))
      return new DecisionTable(table, tableId, slimTestContext);
    else if (beginsWith(tableType, "dt-") || beginsWith(tableType, "decision-"))
      return new DecisionTable(table, tableId, slimTestContext, extractPortType(tableType));
    else if (beginsWith(tableType, "query-"))
      return new QueryTable(table, tableId, slimTestContext, extractPortType(tableType));
    else if (beginsWith(tableType, "query:"))
      return new QueryTable(table, tableId, slimTestContext);
    else if (beginsWith(tableType, "table-"))
      return new TableTable(table, tableId, slimTestContext, extractPortType(tableType));
    else if (beginsWith(tableType, "table:"))
      return new TableTable(table, tableId, slimTestContext);
    else if (beginsWith(tableType, "script-"))
      return new ScriptTable(table, tableId, slimTestContext, extractPortType(tableType));
    else if (tableType.equalsIgnoreCase("script"))
      return new ScriptTable(table, tableId, slimTestContext);
    else if (beginsWith(tableType, "scenario-"))
      return new ScenarioTable(table, tableId, slimTestContext, extractPortType(tableType));
    else if (tableType.equalsIgnoreCase("scenario"))
      return new ScenarioTable(table, tableId, slimTestContext);
    else if (tableType.equalsIgnoreCase("comment"))
      return null;
    else if (beginsWith(tableType, "import-"))
      return new ImportTable(table, tableId, slimTestContext, extractPortType(tableType));
    else if (tableType.equalsIgnoreCase("import"))
      return new ImportTable(table, tableId, slimTestContext);
    else if (doesNotHaveColon(tableType))
      return new DecisionTable(table, tableId, slimTestContext);
    else
      return new SlimErrorTable(table, tableId, slimTestContext);
  }

  private String extractPortType(String tableType) {
    if (!doesNotHaveColon(tableType))
      return tableType.substring(tableType.indexOf("-") + 1, tableType.indexOf(":"));
    else
      return tableType.substring(tableType.indexOf("-") + 1);
  }

  private boolean doesNotHaveColon(String tableType) {
    return tableType.indexOf(":") == -1;
  }

  private boolean beginsWith(String tableType, String typeCode) {
    return tableType.toUpperCase().startsWith(typeCode.toUpperCase());
  }

  static String translateExceptionMessage(String exceptionMessage) {
    String tokens[] = exceptionMessage.split(" ");
    if (tokens[0].equals("COULD_NOT_INVOKE_CONSTRUCTOR"))
      return "Could not invoke constructor for " + tokens[1];
    else if (tokens[0].equals("NO_METHOD_IN_CLASS"))
      return String.format("Method %s not found in %s", tokens[1], tokens[2]);
    else if (tokens[0].equals("NO_CONSTRUCTOR"))
      return String.format("Could not find constructor for %s", tokens[1]);
    else if (tokens[0].equals("NO_CONVERTER_FOR_ARGUMENT_NUMBER"))
      return String.format("No converter for %s", tokens[1]);
    else if (tokens[0].equals("NO_INSTANCE"))
      return String.format("The instance %s does not exist", tokens[1]);
    else if (tokens[0].equals("NO_CLASS"))
      return String.format("Could not find class %s", tokens[1]);
    else if (tokens[0].equals("MALFORMED_INSTRUCTION"))
      return String.format("The instruction %s is malformed", exceptionMessage.substring(exceptionMessage.indexOf(" ") + 1));

    return exceptionMessage;
  }

  public PageData getTestResults() {
    return testResults;
  }

  public static String exceptionToString(Throwable e) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter pw = new PrintWriter(stringWriter);
    e.printStackTrace(pw);
    return SlimServer.EXCEPTION_TAG + stringWriter.toString();
  }

  public TestSummary getTestSummary() {
    return testSummary;
  }

  protected void evaluateExpectations() {
    for (SlimTable.Expectation e : expectations) {
      try {
        e.evaluateExpectation(instructionResults);
      } catch (Throwable ex) {
        exceptions.put("ABORT", exceptionToString(ex));
        exceptionOccurred(ex);
      }
    }
  }

  protected void evaluateTables() {
    evaluateExpectations();
    for (SlimTable table : testTables)
      evaluateTable(table);
  }

  private void evaluateTable(SlimTable table) {
    try {
      table.evaluateReturnValues(instructionResults);
      testSummary.add(table.getTestSummary());
    } catch (Throwable e) {
      exceptions.put("ABORT", exceptionToString(e));
      exceptionOccurred(e);
    }
  }

  protected void replaceExceptionsWithLinks() {
    Set<String> resultKeys = instructionResults.keySet();
    for (String resultKey : resultKeys)
      replaceExceptionWithExceptionLink(resultKey);
  }

  private void replaceExceptionWithExceptionLink(String resultKey) {
    Object result = instructionResults.get(resultKey);
    if (result instanceof String)
      replaceIfUnignoredException(resultKey, (String) result);
  }

  private void replaceIfUnignoredException(String resultKey, String resultString) {
    if (resultString.indexOf(SlimServer.EXCEPTION_TAG) != -1) {
      if (shouldReportException(resultKey, resultString))
        replaceException(resultKey, resultString);
    }
  }

  private boolean shouldReportException(String resultKey, String resultString) {
    for (SlimTable table : testTables) {
      if (table.shouldIgnoreException(resultKey, resultString))
        return false;
    }
    return true;
  }

  private void replaceException(String resultKey, String resultString) {
    testSummary.exceptions++;
    Matcher exceptionMessageMatcher = exceptionMessagePattern.matcher(resultString);
    if (exceptionMessageMatcher.find()) {
      String exceptionMessage = exceptionMessageMatcher.group(1);
      instructionResults.put(resultKey, "!:" + translateExceptionMessage(exceptionMessage));
    } else {
      exceptions.put(resultKey, resultString);
      instructionResults.put(resultKey, exceptionResult(resultKey));
    }
  }

  private String exceptionResult(String resultKey) {
    return String.format("Exception: <a href=#%s>%s</a>", resultKey, resultKey);
  }

  public Map<String, ScenarioTable> getScenarios() {
    return scenarios;
  }

  static class ExceptionList {
    private Map<String, String> exceptions;
    public StringBuffer buffer;
    public Set<String> keys;

    private ExceptionList(Map<String, String> exceptions) {
      this.exceptions = exceptions;
      buffer = new StringBuffer();
      keys = exceptions.keySet();
    }

    private String toHtml() {
      header();
      exceptions();
      footer();
      return buffer.toString();
    }

    private void footer() {
      if (keys.size() > 0)
        buffer.append("<hr/>");
    }

    private void exceptions() {
      for (String key : keys) {
        buffer.append(String.format("<a name=\"%s\"/><b></b>", key));
        String collapsibleSectionFormat = "<div class=\"collapse_rim\">" +
          "<div style=\"float: right;\" class=\"meta\"><a href=\"javascript:expandAll();\">Expand All</a> | <a href=\"javascript:collapseAll();\">Collapse All</a></div>" +
          "<a href=\"javascript:toggleCollapsable('%d');\">" +
          "<img src=\"/files/images/collapsableClosed.gif\" class=\"left\" id=\"img%d\"/>" +
          "</a>" +
          "&nbsp;<span class=\"meta\">%s </span>\n" +
          "\n" +
          "\t<div class=\"hidden\" id=\"%d\"><pre>%s</pre></div>\n" +
          "</div>";
        long id = new Random().nextLong();
        buffer.append(String.format(collapsibleSectionFormat, id, id, key, id, exceptions.get(key)));
      }
    }

    private void header() {
      if (keys.size() > 0) {
        buffer.append("<H3> <span class=\"fail\">Exceptions</span></H3><br/>");
      }
    }

    protected static String toHtml(Map<String, String> exceptions) {
      return new ExceptionList(exceptions).toHtml();
    }
  }
}
