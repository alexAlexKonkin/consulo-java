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
package com.intellij.java.debugger.impl.engine;

import java.util.List;

import com.intellij.java.debugger.SourcePosition;
import com.intellij.java.debugger.impl.engine.evaluation.DefaultCodeFragmentFactory;
import com.intellij.java.debugger.engine.evaluation.EvaluateException;
import com.intellij.java.debugger.engine.jdi.StackFrameProxy;
import com.intellij.java.debugger.impl.jdi.LocalVariableProxyImpl;
import com.intellij.java.debugger.impl.jdi.StackFrameProxyImpl;
import com.intellij.java.debugger.engine.StackFrameContext;
import consulo.application.ReadAction;
import consulo.logging.Logger;
import consulo.util.lang.Comparing;
import consulo.util.dataholder.Key;
import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiCodeBlock;
import com.intellij.java.language.psi.PsiDeclarationStatement;
import consulo.language.psi.PsiElement;
import com.intellij.java.language.psi.PsiElementFactory;
import com.intellij.java.language.psi.PsiResolveHelper;
import com.intellij.java.language.psi.PsiStatement;
import consulo.language.util.IncorrectOperationException;
import consulo.internal.com.sun.jdi.Location;
import jakarta.annotation.Nullable;

public class ContextUtil
{
	public static final Key<Boolean> IS_JSP_IMPLICIT = new Key<>("JspImplicit");
	private static final Logger LOG = Logger.getInstance("#com.intellij.java.debugger.impl.PositionUtil");

	@Nullable
	public static SourcePosition getSourcePosition(@Nullable final StackFrameContext context)
	{
		if(context == null)
		{
			return null;
		}
		DebugProcessImpl debugProcess = (DebugProcessImpl) context.getDebugProcess();
		if(debugProcess == null)
		{
			return null;
		}
		final StackFrameProxy frameProxy = context.getFrameProxy();
		if(frameProxy == null)
		{
			return null;
		}
		Location location = null;
		try
		{
			location = frameProxy.location();
		}
		catch(Throwable e)
		{
			LOG.debug(e);
		}
		if(location == null)
		{
			return null;
		}
		return debugProcess.getPositionManager().getSourcePosition(location);
	}

	@Nullable
	public static PsiElement getContextElement(final StackFrameContext context)
	{
		return getContextElement(context, getSourcePosition(context));
	}

	@Nullable
	public static PsiElement getContextElement(final StackFrameContext context, final SourcePosition position)
	{
		if(LOG.isDebugEnabled())
		{
			final SourcePosition sourcePosition = getSourcePosition(context);
			LOG.assertTrue(Comparing.equal(sourcePosition, position));
		}

		return ReadAction.compute(() ->
		{
			final PsiElement element = getContextElement(position);

			if(element == null)
			{
				return null;
			}

			// further code is java specific, actually
			if(element.getLanguage().getAssociatedFileType() != DefaultCodeFragmentFactory.getInstance().getFileType())
			{
				return element;
			}

			final StackFrameProxyImpl frameProxy = (StackFrameProxyImpl) context.getFrameProxy();

			if(frameProxy == null)
			{
				return element;
			}

			try
			{
				List<LocalVariableProxyImpl> list = frameProxy.visibleVariables();

				PsiResolveHelper resolveHelper = JavaPsiFacade.getInstance(element.getProject()).getResolveHelper();
				StringBuilder buf = null;
				for(LocalVariableProxyImpl localVariable : list)
				{
					final String varName = localVariable.name();
					if(resolveHelper.resolveReferencedVariable(varName, element) == null)
					{
						if(buf == null)
						{
							buf = new StringBuilder("{");
						}
						buf.append(localVariable.getVariable().typeName()).append(" ").append(varName).append(";");
					}
				}
				if(buf == null)
				{
					return element;
				}

				buf.append('}');

				final PsiElementFactory elementFactory = JavaPsiFacade.getInstance(element.getProject()).getElementFactory();
				final PsiCodeBlock codeBlockFromText = elementFactory.createCodeBlockFromText(buf.toString(), element);

				final PsiStatement[] statements = codeBlockFromText.getStatements();
				for(PsiStatement statement : statements)
				{
					if(statement instanceof PsiDeclarationStatement)
					{
						PsiDeclarationStatement declStatement = (PsiDeclarationStatement) statement;
						PsiElement[] declaredElements = declStatement.getDeclaredElements();
						for(PsiElement declaredElement : declaredElements)
						{
							declaredElement.putUserData(IS_JSP_IMPLICIT, Boolean.TRUE);
						}
					}
				}
				return codeBlockFromText;
			}
			catch(IncorrectOperationException | EvaluateException ignored)
			{
				return element;
			}
		});
	}

	@Nullable
	public static PsiElement getContextElement(@Nullable SourcePosition position)
	{
		return position == null ? null : position.getElementAt();
	}

	public static boolean isJspImplicit(PsiElement element)
	{
		return Boolean.TRUE.equals(element.getUserData(IS_JSP_IMPLICIT));
	}
}
