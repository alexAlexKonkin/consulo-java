/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.java.impl.spellchecker;

import com.intellij.java.language.psi.PsiIdentifier;
import com.intellij.java.language.psi.PsiTypeElement;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.spellcheker.tokenizer.TokenConsumer;
import consulo.language.spellcheker.tokenizer.Tokenizer;

import jakarta.annotation.Nonnull;

/**
 * Created by IntelliJ IDEA.
 *
 * @author shkate@jetbrains.com
 */
public class NamedElementTokenizer<T extends PsiNamedElement> extends Tokenizer<T> {
  private final Tokenizer<PsiIdentifier> myIdentifierTokenizer = new PsiIdentifierTokenizer();
  private final PsiTypeTokenizer myTypeTokenizer = new PsiTypeTokenizer();

  @Override
   public void tokenize(@Nonnull T element, TokenConsumer consumer) {
    final PsiIdentifier psiIdentifier = PsiTreeUtil.getChildOfType(element, PsiIdentifier.class);
    final PsiTypeElement psiType = PsiTreeUtil.getChildOfType(element, PsiTypeElement.class);

    if (psiIdentifier == null) {
      return;
    }

    final String identifier = psiIdentifier.getText();
    final String type = psiType==null?null:psiType.getText();

    if (identifier == null) {
      return;
    }

    if (type == null || !type.equalsIgnoreCase(identifier)) {
      myIdentifierTokenizer.tokenize(psiIdentifier, consumer);      
    }
    if (psiType != null) {
      myTypeTokenizer.tokenize(psiType, consumer);
    }
  }
}


