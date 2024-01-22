/*
 * Copyright (c) 2007-2009, Osmorc Development Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright notice, this list
 *       of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this
 *       list of conditions and the following disclaimer in the documentation and/or other
 *       materials provided with the distribution.
 *     * Neither the name of 'Osmorc Development Team' nor the names of its contributors may be
 *       used to endorse or promote products derived from this software without specific
 *       prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.osmorc.manifest.lang.psi;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.psi.AbstractElementManipulator;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.util.IncorrectOperationException;
import jakarta.annotation.Nonnull;
import org.osmorc.manifest.lang.ManifestFileType;

/**
 * @author Robert F. Beeger (robert@beeger.net)
 */
@ExtensionImpl
public class HeaderValuePartManipulator extends AbstractElementManipulator<HeaderValuePart> {
  @Override
  public HeaderValuePart handleContentChange(HeaderValuePart element, TextRange range, String newContent)
      throws IncorrectOperationException {
    String newText = range.replace(element.getText(), newContent);
    PsiFileFactory fileFactory = PsiFileFactory.getInstance(element.getProject());
    PsiFile fromText = fileFactory.createFileFromText("DUMMY.MF", ManifestFileType.INSTANCE, " " + newText);
    Clause clause = (Clause) fromText.getFirstChild().getFirstChild().getNextSibling();
    HeaderValuePart headerValue = (HeaderValuePart) clause.getFirstChild();

    return (HeaderValuePart) element.replace(headerValue);
  }

  @Nonnull
  @Override
  public Class<HeaderValuePart> getElementClass() {
    return HeaderValuePart.class;
  }
}
