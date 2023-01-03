/*
 * Copyright 2006-2011 Dave Griffith, Bas Leijdekkers
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
package com.intellij.java.impl.ig.serialization;

import consulo.annotation.component.ExtensionImpl;
import consulo.java.language.module.util.JavaClassNames;
import com.intellij.java.language.psi.PsiAnonymousClass;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.util.InheritanceUtil;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.intellij.java.impl.ig.fixes.MakeSerializableFix;
import com.intellij.java.impl.ig.psiutils.SerializationUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@ExtensionImpl
public class ComparatorNotSerializableInspection extends BaseInspection {

  @Override
  @Nonnull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message(
      "comparator.not.serializable.display.name");
  }

  @Override
  @Nonnull
  public String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message(
      "comparator.not.serializable.problem.descriptor");
  }

  @Override
  @Nullable
  protected InspectionGadgetsFix buildFix(Object... infos) {
    return new MakeSerializableFix();
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new ComparatorNotSerializableVisitor();
  }

  private static class ComparatorNotSerializableVisitor
    extends BaseInspectionVisitor {

    @Override
    public void visitClass(PsiClass aClass) {
      //note, no call to super, avoiding drilldown
      if (aClass instanceof PsiAnonymousClass) {
        return;
      }
      if (!InheritanceUtil.isInheritor(aClass,
                                       JavaClassNames.JAVA_UTIL_COMPARATOR)) {
        return;
      }
      if (SerializationUtils.isSerializable(aClass)) {
        return;
      }
      registerClassError(aClass);
    }
  }
}