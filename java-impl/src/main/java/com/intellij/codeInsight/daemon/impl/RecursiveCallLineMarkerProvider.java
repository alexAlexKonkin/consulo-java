/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.codeInsight.daemon.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiThisExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.FunctionUtil;
import consulo.annotations.RequiredReadAction;
import consulo.ui.image.Image;

/**
 * @author Danila Ponomarenko
 */
public class RecursiveCallLineMarkerProvider extends LineMarkerProviderDescriptor
{
	@RequiredReadAction
	@Override
	public LineMarkerInfo getLineMarkerInfo(@Nonnull PsiElement element)
	{
		return null; //do nothing
	}

	@RequiredReadAction
	@Override
	public void collectSlowLineMarkers(@Nonnull List<PsiElement> elements, @Nonnull Collection<LineMarkerInfo> result)
	{
		final Set<PsiStatement> statements = new HashSet<>();

		for(PsiElement element : elements)
		{
			ProgressManager.checkCanceled();
			if(element instanceof PsiMethodCallExpression)
			{
				final PsiMethodCallExpression methodCall = (PsiMethodCallExpression) element;
				final PsiStatement statement = PsiTreeUtil.getParentOfType(methodCall, PsiStatement.class, true, PsiMethod.class);
				if(!statements.contains(statement) && isRecursiveMethodCall(methodCall))
				{
					statements.add(statement);
					result.add(new RecursiveMethodCallMarkerInfo(methodCall));
				}
			}
		}
	}

	public static boolean isRecursiveMethodCall(@Nonnull PsiMethodCallExpression methodCall)
	{
		final PsiExpression qualifier = methodCall.getMethodExpression().getQualifierExpression();
		if(qualifier != null && !(qualifier instanceof PsiThisExpression))
		{
			return false;
		}

		final PsiMethod method = PsiTreeUtil.getParentOfType(methodCall, PsiMethod.class, true, PsiLambdaExpression.class, PsiClass.class);
		if(method == null || !method.getName().equals(methodCall.getMethodExpression().getReferenceName()))
		{
			return false;
		}

		return Comparing.equal(method, methodCall.resolveMethod());
	}

	@Nonnull
	@Override
	public String getName()
	{
		return "Recursive call";
	}

	@Nullable
	@Override
	public Image getIcon()
	{
		return AllIcons.Gutter.RecursiveMethod;
	}

	private static class RecursiveMethodCallMarkerInfo extends LineMarkerInfo<PsiMethodCallExpression>
	{
		private RecursiveMethodCallMarkerInfo(@Nonnull PsiMethodCallExpression methodCall)
		{
			super(methodCall, methodCall.getTextRange(), AllIcons.Gutter.RecursiveMethod, Pass.LINE_MARKERS, FunctionUtil.constant("Recursive call"), null, GutterIconRenderer.Alignment.RIGHT);
		}

		@Override
		public GutterIconRenderer createGutterRenderer()
		{
			if(myIcon == null)
			{
				return null;
			}
			return new LineMarkerGutterIconRenderer<PsiMethodCallExpression>(this)
			{
				@Override
				public AnAction getClickAction()
				{
					return null; // to place breakpoint on mouse click
				}
			};
		}
	}
}

