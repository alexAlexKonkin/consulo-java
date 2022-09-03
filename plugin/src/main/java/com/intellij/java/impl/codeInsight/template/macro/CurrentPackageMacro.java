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
package com.intellij.java.impl.codeInsight.template.macro;

import consulo.language.editor.CodeInsightBundle;
import com.intellij.codeInsight.template.*;
import com.intellij.java.impl.codeInsight.template.JavaCodeContextType;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import com.intellij.java.language.psi.PsiJavaFile;
import javax.annotation.Nonnull;


/**
 * Created by IntelliJ IDEA.
 * User: ven
 * Date: May 13, 2003
 * Time: 8:36:42 PM
 * To change this template use Options | File Templates.
 */
class CurrentPackageMacro extends Macro {
  @Override
  public String getName() {
    return "currentPackage";
  }

  @Override
  public String getPresentableName() {
    return CodeInsightBundle.message("macro.current.package");
  }

  @Override
  public Result calculateResult(@Nonnull Expression[] params, ExpressionContext context) {
    Project project = context.getProject();
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(context.getEditor().getDocument());
    if (!(file instanceof PsiJavaFile)) return new TextResult ("");
    return new TextResult (((PsiJavaFile)file).getPackageName());
  }

  @Override
  public Result calculateQuickResult(@Nonnull Expression[] params, ExpressionContext context) {
    return calculateResult(params, context);
  }

  @Override
  public boolean isAcceptableInContext(TemplateContextType context) {
    return context instanceof JavaCodeContextType;
  }


}
