package com.intellij.codeInsight.daemon.quickFix;

public abstract class RenameWrongReferenceTest extends LightQuickFixAvailabilityTestCase {

  public void test() throws Exception { doAllTests(); }

  @Override
  protected String getBasePath() {
    return "/codeInsight/daemonCodeAnalyzer/quickFix/renameWrongReference";
  }
}

