/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Apr 11, 2002
 * Time: 7:51:16 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.intellij.codeInsight.daemon;

import consulo.language.editor.impl.internal.daemon.DaemonCodeAnalyzerEx;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.ide.impl.idea.codeInspection.LossyEncodingInspection;
import consulo.document.FileDocumentManager;
import consulo.util.io.CharsetToolkit;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.encoding.EncodingManager;
import consulo.virtualFileSystem.encoding.EncodingProjectManager;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;

public abstract class LossyEncodingTest extends LightDaemonAnalyzerTestCase {
  @NonNls private static final String BASE_PATH = "/codeInsight/daemonCodeAnalyzer/lossyEncoding";

  @Nonnull
  @Override
  protected LocalInspectionTool[] configureLocalInspectionTools() {
    return new LocalInspectionTool[]{new LossyEncodingInspection()};
  }

  public void testText() throws Exception {
    doTest("Text.txt");
    Charset ascii = CharsetToolkit.forName("US-ASCII");
    EncodingManager.getInstance().setEncoding(myVFile, ascii);
    assertEquals(ascii, myVFile.getCharset());
    int start = myEditor.getCaretModel().getOffset();
    type((char)0x445);
    type((char)0x438);
    int end = myEditor.getCaretModel().getOffset();

    Collection<HighlightInfo> infos = doHighlighting();
    HighlightInfo info = assertOneElement(infos);
    assertEquals("Unsupported characters for the charset 'US-ASCII'", info.getDescription());
    assertEquals(start, info.startOffset);
    assertEquals(end, info.endOffset);

    backspace();
    backspace();
    doTestConfiguredFile(true, false, null);
  }

  public void testNativeConversion() throws Exception {
    configureFromFileText("x.properties","a=<caret>v");
    EncodingProjectManager.getInstance(getProject()).setNative2AsciiForPropertiesFiles(null, true);
    UIUtil.dispatchAllInvocationEvents();  //reload files

    type('\\');
    type('\\');

    Collection<HighlightInfo> infos = doHighlighting();
    assertEquals(0, infos.size());
  }

  public void testTyping() throws Exception {
    doTest("Simple.xml");
    type("US-ASCII");

    Collection<HighlightInfo> infos = doHighlighting();
    assertEquals(1, infos.size());
    boolean found = false;

    for(HighlightInfo info:infos) {
      if (info.getDescription().equals("Unsupported characters for the charset 'US-ASCII'")) {
        found = true;
        break;
      }
    }
    assertTrue(found);
  }

  public void testMultipleRanges() throws Exception {
    configureByFile(BASE_PATH + "/" + "MultipleRanges.xml");
    type("US-ASCII");

    doTestConfiguredFile(true, false, null);
  }

  private void doTest(@NonNls String filePath) throws Exception {
    doTest(BASE_PATH + "/" + filePath, true, false);
  }

  public void testNativeEncoding() throws Exception {
    EncodingManager.getInstance().setNative2AsciiForPropertiesFiles(null, true);
    configureByFile(BASE_PATH + "/" + "NativeEncoding.properties");

    doTestConfiguredFile(true, false, null);
  }

  public void testDetectWrongEncoding() throws Exception {
    String threeNotoriousRussianLetters = "\u0416\u041e\u041f";
    configureFromFileText("Win1251.txt", threeNotoriousRussianLetters);
    VirtualFile virtualFile = getFile().getVirtualFile();
    assertEquals(CharsetToolkit.UTF8_CHARSET, virtualFile.getCharset());
    Charset WINDOWS_1251 = Charset.forName("windows-1251");
    virtualFile.setCharset(WINDOWS_1251);
    FileDocumentManager.getInstance().saveAllDocuments();
    assertEquals(WINDOWS_1251, virtualFile.getCharset());
    assertEquals(threeNotoriousRussianLetters, new String(virtualFile.contentsToByteArray(), WINDOWS_1251));
    virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);

    doHighlighting();
    List<HighlightInfo> infos = DaemonCodeAnalyzerEx.getInstanceEx(getProject()).getFileLevelHighlights(getProject(), getFile());
    HighlightInfo info = assertOneElement(infos);
    assertEquals("File was loaded in the wrong encoding: 'UTF-8'", info.getDescription());
  }
}
