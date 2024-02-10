/*
 * Copyright 2013 Consulo.org
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
package consulo.java.language.impl.psi;

import com.intellij.java.language.impl.psi.impl.source.tree.CoreJavaASTLeafFactory;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.ast.IElementType;
import consulo.language.impl.ast.LeafElement;
import consulo.language.impl.psi.PsiCommentImpl;
import consulo.language.version.LanguageVersion;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 21:13/09.07.13
 */
@ExtensionImpl
public class JavaASTLeafFactory extends CoreJavaASTLeafFactory {
  @Override
  @Nonnull
  public LeafElement createLeaf(@Nonnull final IElementType type, @Nonnull LanguageVersion languageVersion, @Nonnull final CharSequence text) {
    if (type == C_STYLE_COMMENT || type == END_OF_LINE_COMMENT) {
      return new PsiCommentImpl(type, text);
    }

    return super.createLeaf(type, languageVersion, text);
  }
}
