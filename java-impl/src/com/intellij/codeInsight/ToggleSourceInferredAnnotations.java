/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.codeInsight;

import static com.intellij.codeInsight.ExternalAnnotationsLineMarkerProvider.getAnnotationOwner;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.codeInsight.javadoc.AnnotationDocGenerator;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import consulo.java.codeInsight.JavaCodeInsightSettings;

/**
 * @author peter
 */
public class ToggleSourceInferredAnnotations extends BaseIntentionAction
{

	@Nls
	@NotNull
	@Override
	public String getFamilyName()
	{
		return "Show/Hide Annotations Inferred from Source Code";
	}

	@Override
	public boolean isAvailable(@NotNull final Project project, Editor editor, PsiFile file)
	{
		final PsiElement leaf = file.findElementAt(editor.getCaretModel().getOffset());
		final PsiModifierListOwner owner = getAnnotationOwner(leaf);
		if(owner != null)
		{
			boolean hasSrcInferredAnnotation = ContainerUtil.exists(AnnotationDocGenerator.getAnnotationsToShow(owner), AnnotationDocGenerator::isInferredFromSource);
			if(hasSrcInferredAnnotation)
			{
				setText((JavaCodeInsightSettings.getInstance().SHOW_SOURCE_INFERRED_ANNOTATIONS ? "Hide" : "Show") + " annotations inferred from source code");
				return true;
			}
		}

		return false;
	}

	@Override
	public void invoke(@NotNull final Project project, Editor editor, PsiFile file) throws IncorrectOperationException
	{
		JavaCodeInsightSettings.getInstance().SHOW_SOURCE_INFERRED_ANNOTATIONS = !JavaCodeInsightSettings.getInstance().SHOW_SOURCE_INFERRED_ANNOTATIONS;
		DaemonCodeAnalyzer.getInstance(project).restart(file);
	}

	@Override
	public boolean startInWriteAction()
	{
		return false;
	}
}
