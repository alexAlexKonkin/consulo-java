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
package com.intellij.java.impl.codeInsight;

import static com.intellij.java.impl.codeInsight.ExternalAnnotationsLineMarkerProvider.getAnnotationOwner;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nls;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.intention.BaseIntentionAction;
import com.intellij.java.impl.codeInsight.javadoc.AnnotationDocGenerator;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import com.intellij.java.language.psi.PsiModifierListOwner;
import consulo.language.util.IncorrectOperationException;
import consulo.util.collection.ContainerUtil;
import consulo.java.impl.codeInsight.JavaCodeInsightSettings;

/**
 * @author peter
 */
public class ToggleSourceInferredAnnotations extends BaseIntentionAction
{

	@Nls
	@Nonnull
	@Override
	public String getFamilyName()
	{
		return "Show/Hide Annotations Inferred from Source Code";
	}

	@Override
	public boolean isAvailable(@Nonnull final Project project, Editor editor, PsiFile file)
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
	public void invoke(@Nonnull final Project project, Editor editor, PsiFile file) throws IncorrectOperationException
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
