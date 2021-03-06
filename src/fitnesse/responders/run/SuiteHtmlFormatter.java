// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.responders.run;

import fitnesse.html.HtmlPage;
import fitnesse.html.HtmlTag;
import fitnesse.html.HtmlUtil;
import fitnesse.html.TagGroup;

public class SuiteHtmlFormatter extends TestHtmlFormatter {
  private static final String cssSuffix1 = "1";
  private static final String cssSuffix2 = "2";

  private TestSummary pageCounts = new TestSummary();

  private String cssSuffix = cssSuffix1;
  private TagGroup testResultsGroup = new TagGroup();
  private HtmlTag currentOutputDiv;
  private int pageNumber = 0;

  public SuiteHtmlFormatter(HtmlPage page) throws Exception {
    super(page);

    HtmlTag outputTitle = new HtmlTag("h2", "Test Output");
    outputTitle.addAttribute("class", "centered");
    testResultsGroup.add(outputTitle);
  }

  protected String testPageSummary() {
    return "<strong>Test Pages:</strong> " + pageCounts.toString() + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
  }

  public void setPageAssertions(TestSummary pageSummary) {
    this.pageCounts = pageSummary;
  }

  public String acceptResults(String relativePageName, TestSummary testSummary) throws Exception {
    switchCssSuffix();
    HtmlTag mainDiv = HtmlUtil.makeDivTag("alternating_row_" + cssSuffix);

    mainDiv.add(HtmlUtil.makeSpanTag("test_summary_results " + cssClassFor(testSummary), testSummary.toString()));

    HtmlTag link = HtmlUtil.makeLink("#" + relativePageName + pageNumber, relativePageName);
    link.addAttribute("class", "test_summary_link");
    mainDiv.add(link);

    pageCounts.tallyPageCounts(testSummary);

    return mainDiv.html(2);
  }

  public void startOutputForNewTest(String relativePageName, String qualifiedPageName) throws Exception {
    pageNumber++;
    HtmlTag pageNameBar = HtmlUtil.makeDivTag("test_output_name");
    HtmlTag anchor = HtmlUtil.makeLink(qualifiedPageName, relativePageName);
    anchor.addAttribute("id", relativePageName + pageNumber);
    pageNameBar.add(anchor);
    testResultsGroup.add(pageNameBar);
    currentOutputDiv = HtmlUtil.makeDivTag("alternating_block_" + cssSuffix);
    testResultsGroup.add(currentOutputDiv);
  }

  public void acceptOutput(String output) {
    currentOutputDiv.add(output);
  }

  public String testOutput() throws Exception {
    return testResultsGroup.html();
  }

  private void switchCssSuffix() {
    if (cssSuffix1.equals(cssSuffix))
      cssSuffix = cssSuffix2;
    else
      cssSuffix = cssSuffix1;
  }

  public void announceTestSystem(String testSystemName) {
    HtmlTag outputTitle = new HtmlTag("h2", String.format("Test System: %s", testSystemName));
    outputTitle.addAttribute("class", "centered");
    testResultsGroup.add(outputTitle);
  }

  public String getTestSystemHeader(String testSystemName) {
    return String.format("<h3>%s</h3>\n", testSystemName);
  }
}
