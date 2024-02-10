/*
 * Copyright 2006 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.java.regexp.impl;

import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.application.AllIcons;
import consulo.language.psi.PsiElement;
import com.intellij.java.language.psi.PsiLiteralExpression;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import org.intellij.plugins.intelliLang.util.RegExpUtil;
import consulo.java.impl.intelliLang.util.StringLiteralReference;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Set;

/**
 * Provides completion suggestions for enum-like regular expression patterns such as
 * <pre>@Pattern("abc|xyz|123")</pre>.
 */
final class RegExpEnumReference extends StringLiteralReference {
  private final String myPattern;

  public RegExpEnumReference(PsiLiteralExpression expression, @Nonnull String pattern) {
    super(expression);
    myPattern = pattern;
  }

  @Nonnull
  public Object[] getVariants() {
    final Set<String> values = getEnumValues();
    if (values == null || values.size() == 0) {
      return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }
    return ContainerUtil.map2Array(values, s -> LookupElementBuilder.create(s).withIcon(AllIcons.Nodes.Enum));
  }

  public boolean isSoft() {
    return true;
  }

  @Nullable
  public PsiElement resolve() {
    final Set<String> values = getEnumValues();
    return values != null ? values.contains(getValue()) ? myValue : null : null;
  }

  @Nullable
  private Set<String> getEnumValues() {
    return RegExpUtil.getEnumValues(myValue.getProject(), myPattern);
  }
}
