/*
 * Copyright 2003-2007 Dave Griffith, Bas Leijdekkers
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
package com.intellij.java.impl.ig.bugs;

import consulo.java.language.module.util.JavaClassNames;
import com.intellij.java.language.psi.PsiMethod;
import com.siyeh.HardcodedMethodConstants;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.intellij.java.impl.ig.psiutils.IteratorUtils;
import com.siyeh.ig.psiutils.MethodUtils;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

public abstract class IteratorHasNextCallsIteratorNextInspection
  extends BaseInspection {

  @Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message(
      "iterator.hasnext.which.calls.next.display.name");
  }

  @Nonnull
  public String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message(
      "iterator.hasnext.which.calls.next.problem.descriptor");
  }

  public BaseInspectionVisitor buildVisitor() {
    return new IteratorHasNextCallsIteratorNext();
  }

  private static class IteratorHasNextCallsIteratorNext
    extends BaseInspectionVisitor {

    @Override
    public void visitMethod(@Nonnull PsiMethod method) {
      // note: no call to super
      @NonNls final String name = method.getName();
      if (!MethodUtils.methodMatches(method, JavaClassNames.JAVA_UTIL_ITERATOR, null,
                                     HardcodedMethodConstants.HAS_NEXT)) {
        return;
      }
      if (!IteratorUtils.containsCallToIteratorNext(method, null, true)) {
        return;
      }
      registerMethodError(method);
    }
  }
}