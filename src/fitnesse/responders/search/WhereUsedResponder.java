// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.responders.search;

import fitnesse.components.WhereUsed;
import fitnesse.html.HtmlElement;
import fitnesse.html.HtmlTag;
import fitnesse.html.HtmlUtil;

public class WhereUsedResponder extends ResultResponder {
  protected String getPageFooterInfo(int hits) throws Exception {
    //return HtmlUtil.makeLink(getRenderedPath(), page.getName()).html() + " is used in " + hits + " page(s).";
    HtmlTag tag = HtmlUtil.makeLink(getRenderedPath(), page.getName());
    tag.tail = " is used in " + hits + " page(s).";
    return tag.html().replaceAll(HtmlElement.endl, "");
  }

  protected void startSearching() throws Exception {
    new WhereUsed(root).searchForReferencingPages(page, this);
  }

  protected String getTitle() throws Exception {
    return "Where Used Results";
  }

}
