package com.intellij.lang.psi;

import com.intellij.lang.*;
import com.intellij.lang.impl.PsiBuilderImpl;
import com.intellij.lexer.JavaLexer;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.java.stubs.JavaStubElementTypes;
import com.intellij.psi.impl.source.DummyHolder;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.impl.source.tree.SharedImplUtil;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.text.BlockSupport;
import com.intellij.testFramework.LightIdeaTestCase;
import junit.framework.AssertionFailedError;

/**
 * Date: Jan 21, 2005
 *
 * @author max
 */
public class PsiBuilderTest extends LightIdeaTestCase {
  private PsiBuilderImpl myBuilder;

  public void testEmptyProgram() throws Exception {
    myBuilder = createBuilder("");
    final PsiBuilder.Marker fileMarker = myBuilder.mark();
    fileMarker.done(JavaStubElementTypes.JAVA_FILE);
    ASTNode fileNode = myBuilder.getTreeBuilt();
    assertNotNull(fileNode);
    assertEquals("", fileNode.getText());
  }

  public void testProgramWithSingleKeyword() throws Exception {
    myBuilder = createBuilder("package");

    final PsiBuilder.Marker fileMarker = myBuilder.mark();
    assertEquals("package", myBuilder.getTokenText());
    assertEquals(JavaTokenType.PACKAGE_KEYWORD, myBuilder.getTokenType());
    final PsiBuilder.Marker packageStatementMarker = myBuilder.mark();
    myBuilder.advanceLexer();
    assertTrue(myBuilder.eof());
    packageStatementMarker.done(JavaElementType.PACKAGE_STATEMENT);
    fileMarker.done(JavaStubElementTypes.JAVA_FILE);

    ASTNode fileNode = myBuilder.getTreeBuilt();
    assertNotNull(fileNode);
    assertEquals("package", fileNode.getText());
    assertSame(fileNode.getFirstChildNode(), fileNode.getLastChildNode());
    ASTNode packageNode = fileNode.getFirstChildNode();
    assertNotNull(packageNode);
    assertEquals("package", packageNode.getText());
    assertEquals(JavaElementType.PACKAGE_STATEMENT, packageNode.getElementType());

    ASTNode leaf = packageNode.getFirstChildNode();
    assertNotNull(leaf);
    assertEquals(JavaTokenType.PACKAGE_KEYWORD, leaf.getElementType());
  }

  private static PsiBuilderImpl createBuilder(final String text) {
    return createBuilder(text, null);
  }

  private static PsiBuilderImpl createBuilder(final String text, ASTNode originalTree) {
    final Language lang = JavaFileType.INSTANCE.getLanguage();
    final ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(lang);
    assertNotNull(parserDefinition);
    PsiFile psiFile = createFile("x.java", text);
    return new PsiBuilderImpl(getProject(), psiFile, parserDefinition, new JavaLexer(LanguageLevel.JDK_1_5), LanguageLevel.JDK_1_5,
                              SharedImplUtil.findCharTableByTree(psiFile.getNode()), text, originalTree, null);
  }

  public void testTrailingWhitespaces() throws Exception {
    myBuilder = createBuilder("foo\n\nx");
    final PsiBuilder.Marker marker = myBuilder.mark();
    while (!myBuilder.eof()) {
      myBuilder.advanceLexer();
    }
    marker.done(JavaStubElementTypes.JAVA_FILE);
    assertEquals("foo\n\nx", myBuilder.getTreeBuilt().getText());
  }

  public void testRollback() throws Exception {
    myBuilder = createBuilder("package");

    PsiBuilder.Marker fileMarker = myBuilder.mark();
    assertEquals("package", myBuilder.getTokenText());
    assertEquals(JavaTokenType.PACKAGE_KEYWORD, myBuilder.getTokenType());
    PsiBuilder.Marker packageStatementMarker = myBuilder.mark();
    myBuilder.advanceLexer();
    assertTrue(myBuilder.eof());
    packageStatementMarker.done(JavaElementType.PACKAGE_STATEMENT);

    fileMarker.rollbackTo();

    fileMarker = myBuilder.mark();
    assertEquals("package", myBuilder.getTokenText());
    assertEquals(JavaTokenType.PACKAGE_KEYWORD, myBuilder.getTokenType());
    packageStatementMarker = myBuilder.mark();
    myBuilder.advanceLexer();
    assertTrue(myBuilder.eof());
    packageStatementMarker.done(JavaElementType.PACKAGE_STATEMENT);
    fileMarker.done(JavaStubElementTypes.JAVA_FILE);

    ASTNode fileNode = myBuilder.getTreeBuilt();
    assertNotNull(fileNode);
    assertEquals("package", fileNode.getText());
    assertSame(fileNode.getFirstChildNode(), fileNode.getLastChildNode());
    ASTNode packageNode = fileNode.getFirstChildNode();
    assertNotNull(packageNode);
    assertEquals("package", packageNode.getText());
    assertEquals(JavaElementType.PACKAGE_STATEMENT, packageNode.getElementType());

    ASTNode leaf = packageNode.getFirstChildNode();
    assertNotNull(leaf);
    assertEquals(JavaTokenType.PACKAGE_KEYWORD, leaf.getElementType());
  }

  public void testDrop() throws Exception {
    myBuilder = createBuilder("package");

    final PsiBuilder.Marker fileMarker = myBuilder.mark();
    assertEquals("package", myBuilder.getTokenText());
    assertEquals(JavaTokenType.PACKAGE_KEYWORD, myBuilder.getTokenType());
    final PsiBuilder.Marker packageStatementMarker = myBuilder.mark();
    myBuilder.advanceLexer();
    assertTrue(myBuilder.eof());
    packageStatementMarker.drop();
    fileMarker.done(JavaStubElementTypes.JAVA_FILE);

    ASTNode fileNode = myBuilder.getTreeBuilt();
    assertNotNull(fileNode);
    assertEquals("package", fileNode.getText());
    assertSame(fileNode.getFirstChildNode(), fileNode.getLastChildNode());

    ASTNode leaf = fileNode.getFirstChildNode();
    assertNotNull(leaf);
    assertEquals(JavaTokenType.PACKAGE_KEYWORD, leaf.getElementType());
    assertEquals("package", leaf.getText());
    assertNull(leaf.getFirstChildNode());
  }

  public void testAdvanceBeyondEof() {
    myBuilder = createBuilder("package");
    for (int i = 0; i < 20; i++) {
      myBuilder.eof();
      myBuilder.advanceLexer();
    }
    assertTrue(myBuilder.eof());
  }

  public void testAssertionFailureOnUnbalancedMarkers() {
    myBuilder = createBuilder("foo");
    myBuilder.setDebugMode(true);
    final PsiBuilder.Marker m = myBuilder.mark();
    final PsiBuilder.Marker m1 = myBuilder.mark();
    myBuilder.getTokenType();
    myBuilder.advanceLexer();
    try {
      m.done(JavaTokenType.PACKAGE_KEYWORD);
      fail("Assertion must fire");
    }
    catch (AssertionFailedError e) {
      throw e;
    }
    catch (Throwable e) {
      if (!e.getMessage().startsWith("Another not done marker")) {
        fail("Wrong assertion message");
      }
    }
  }

  public void testNotAllTokensProcessed() {
    myBuilder = createBuilder("foo");
    myBuilder.setDebugMode(true);
    final PsiBuilder.Marker m = myBuilder.mark();
    m.done(JavaTokenType.PACKAGE_KEYWORD);
    try {
      myBuilder.getTreeBuilt();
      fail("Assertion must fire");
    }
    catch (AssertionFailedError e) {
      throw e;
    }
    catch (Throwable e) {
      if (!e.getMessage().startsWith("Tokens [IDENTIFIER] were not inserted into the tree")) {
        fail("Wrong assertion message");
      }
    }
  }

  public void testMergeWhenEmptyElementAfterWhitespaceIsLastChild() throws Throwable {
    myBuilder = createBuilder(" foo bar");
    parseWhenEmptyElementAfterWhitespaceIsLastChild();
    final ASTNode tree = myBuilder.getTreeBuilt();
    new DummyHolder(getPsiManager(), (TreeElement)tree, null);

    myBuilder = createBuilder("  bar", tree);
    parseWhenEmptyElementAfterWhitespaceIsLastChild();
    try {
      myBuilder.getTreeBuilt();
      fail();
    }
    catch (BlockSupport.ReparsedSuccessfullyException e) {
      e.getDiffLog().performActualPsiChange(tree.getPsi().getContainingFile());
    }

    assertEquals("  bar", tree.getText());
  }

  private void parseWhenEmptyElementAfterWhitespaceIsLastChild() {
    final PsiBuilder.Marker root = myBuilder.mark();

    final PsiBuilder.Marker composite = myBuilder.mark();
    final PsiBuilder.Marker backup = myBuilder.mark();
    if ("foo".equals(myBuilder.getTokenText())) {
      myBuilder.advanceLexer();
      myBuilder.getTokenType();
      myBuilder.mark().done(JavaStubElementTypes.TYPE_PARAMETER_LIST);
      backup.done(JavaStubElementTypes.ANNOTATION_METHOD);
    }
    else {
      backup.rollbackTo();
    }
    composite.done(JavaStubElementTypes.ANONYMOUS_CLASS);

    myBuilder.getTokenType();
    myBuilder.advanceLexer();
    root.done(JavaStubElementTypes.ENUM_CONSTANT_INITIALIZER);
  }


}
