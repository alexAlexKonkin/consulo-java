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
package com.intellij.debugger.ui.impl.watch;

import org.jetbrains.annotations.Nullable;
import com.intellij.debugger.DebuggerBundle;
import com.intellij.debugger.DebuggerContext;
import com.intellij.debugger.DebuggerInvocationUtil;
import com.intellij.debugger.EvaluatingComputable;
import com.intellij.debugger.engine.ContextUtil;
import com.intellij.debugger.engine.StackFrameContext;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.engine.evaluation.TextWithImports;
import com.intellij.debugger.engine.evaluation.expression.ExpressionEvaluator;
import com.intellij.debugger.engine.evaluation.expression.Modifier;
import com.intellij.debugger.engine.evaluation.expression.UnsupportedExpressionException;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.impl.PositionUtil;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiCodeFragment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionCodeFragment;
import com.intellij.psi.search.GlobalSearchScope;
import consulo.internal.com.sun.jdi.ObjectCollectedException;
import consulo.internal.com.sun.jdi.Value;

/**
 * @author lex
 */
public abstract class EvaluationDescriptor extends ValueDescriptorImpl
{
	private Modifier myModifier;
	protected TextWithImports myText;

	protected EvaluationDescriptor(TextWithImports text, Project project, Value value)
	{
		super(project, value);
		myText = text;
	}

	protected EvaluationDescriptor(TextWithImports text, Project project)
	{
		super(project);
		setLvalue(false);
		myText = text;
	}

	protected abstract EvaluationContextImpl getEvaluationContext(EvaluationContextImpl evaluationContext);

	protected abstract PsiCodeFragment getEvaluationCode(StackFrameContext context) throws EvaluateException;

	public PsiCodeFragment createCodeFragment(PsiElement context)
	{
		TextWithImports text = getEvaluationText();
		final PsiCodeFragment fragment = DebuggerUtilsEx.findAppropriateCodeFragmentFactory(text, context).createCodeFragment(text, context, myProject);
		fragment.forceResolveScope(GlobalSearchScope.allScope(myProject));
		return fragment;
	}

	@Override
	public final Value calcValue(final EvaluationContextImpl evaluationContext) throws EvaluateException
	{
		try
		{
			final EvaluationContextImpl thisEvaluationContext = getEvaluationContext(evaluationContext);

			ExpressionEvaluator evaluator = null;
			try
			{
				evaluator = DebuggerInvocationUtil.commitAndRunReadAction(myProject, new EvaluatingComputable<ExpressionEvaluator>()
				{
					@Override
					public ExpressionEvaluator compute() throws EvaluateException
					{
						final PsiElement psiContext = PositionUtil.getContextElement(evaluationContext);
						return DebuggerUtilsEx.findAppropriateCodeFragmentFactory(getEvaluationText(), psiContext).getEvaluatorBuilder().build(getEvaluationCode(thisEvaluationContext),
								ContextUtil.getSourcePosition(thisEvaluationContext));
					}
				});
			}
			catch(UnsupportedExpressionException ex)
			{
				throw ex;
			}

			if(!thisEvaluationContext.getDebugProcess().isAttached())
			{
				throw EvaluateExceptionUtil.PROCESS_EXITED;
			}
			StackFrameProxyImpl frameProxy = thisEvaluationContext.getFrameProxy();
			if(frameProxy == null)
			{
				throw EvaluateExceptionUtil.NULL_STACK_FRAME;
			}

			Value value = evaluator.evaluate(thisEvaluationContext);
			DebuggerUtilsEx.keep(value, thisEvaluationContext);

			myModifier = evaluator.getModifier();
			setLvalue(myModifier != null);

			return value;
		}
		catch(final EvaluateException ex)
		{
			throw new EvaluateException(ex.getLocalizedMessage(), ex);
		}
		catch(ObjectCollectedException ex)
		{
			throw EvaluateExceptionUtil.OBJECT_WAS_COLLECTED;
		}
	}

	@Override
	public PsiExpression getDescriptorEvaluation(DebuggerContext context) throws EvaluateException
	{
		PsiElement evaluationCode = getEvaluationCode(context);
		if(evaluationCode instanceof PsiExpressionCodeFragment)
		{
			return ((PsiExpressionCodeFragment) evaluationCode).getExpression();
		}
		else
		{
			throw new EvaluateException(DebuggerBundle.message("error.cannot.create.expression.from.code.fragment"), null);
		}
	}

	@Nullable
	public Modifier getModifier()
	{
		return myModifier;
	}

	@Override
	public boolean canSetValue()
	{
		return super.canSetValue() && myModifier != null && myModifier.canSetValue();
	}

	public TextWithImports getEvaluationText()
	{
		return myText;
	}
}
