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
package com.intellij.java.debugger.impl.ui.impl.watch;

import jakarta.annotation.Nullable;
import com.intellij.java.debugger.DebuggerBundle;
import com.intellij.java.debugger.DebuggerContext;
import com.intellij.java.debugger.impl.DebuggerManagerEx;
import com.intellij.java.debugger.engine.DebuggerUtils;
import com.intellij.java.debugger.impl.engine.JavaValue;
import com.intellij.java.debugger.impl.engine.JavaValueModifier;
import com.intellij.java.debugger.engine.evaluation.EvaluateException;
import com.intellij.java.debugger.impl.engine.evaluation.EvaluationContextImpl;
import com.intellij.java.debugger.impl.DebuggerContextImpl;
import com.intellij.java.debugger.impl.PositionUtil;
import com.intellij.java.debugger.impl.jdi.LocalVariableProxyImpl;
import com.intellij.java.debugger.impl.jdi.StackFrameProxyImpl;
import com.intellij.java.debugger.impl.ui.tree.LocalVariableDescriptor;
import com.intellij.java.debugger.ui.tree.NodeDescriptor;
import consulo.project.Project;
import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiElementFactory;
import com.intellij.java.language.psi.PsiExpression;
import consulo.language.util.IncorrectOperationException;
import consulo.execution.debug.frame.XValueModifier;
import consulo.internal.com.sun.jdi.ClassNotLoadedException;
import consulo.internal.com.sun.jdi.IncompatibleThreadStateException;
import consulo.internal.com.sun.jdi.InvalidTypeException;
import consulo.internal.com.sun.jdi.InvocationException;
import consulo.internal.com.sun.jdi.ReferenceType;
import consulo.internal.com.sun.jdi.Value;
import jakarta.annotation.Nonnull;

public class LocalVariableDescriptorImpl extends ValueDescriptorImpl implements LocalVariableDescriptor
{
	private final StackFrameProxyImpl myFrameProxy;
	private final LocalVariableProxyImpl myLocalVariable;

	private String myTypeName = DebuggerBundle.message("label.unknown.value");
	private boolean myIsPrimitive;

	private boolean myIsNewLocal = true;

	public LocalVariableDescriptorImpl(Project project, @Nonnull LocalVariableProxyImpl local)
	{
		super(project);
		setLvalue(true);
		myFrameProxy = local.getFrame();
		myLocalVariable = local;
	}

	@Override
	public LocalVariableProxyImpl getLocalVariable()
	{
		return myLocalVariable;
	}

	public boolean isNewLocal()
	{
		return myIsNewLocal;
	}

	@Override
	public boolean isPrimitive()
	{
		return myIsPrimitive;
	}

	@Override
	public Value calcValue(EvaluationContextImpl evaluationContext) throws EvaluateException
	{
		boolean isVisible = myFrameProxy.isLocalVariableVisible(getLocalVariable());
		if(isVisible)
		{
			final String typeName = getLocalVariable().typeName();
			myTypeName = typeName;
			myIsPrimitive = DebuggerUtils.isPrimitiveType(typeName);
			return myFrameProxy.getValue(getLocalVariable());
		}

		return null;
	}

	public void setNewLocal(boolean aNew)
	{
		myIsNewLocal = aNew;
	}

	@Override
	public void displayAs(NodeDescriptor descriptor)
	{
		super.displayAs(descriptor);
		if(descriptor instanceof LocalVariableDescriptorImpl)
		{
			myIsNewLocal = ((LocalVariableDescriptorImpl) descriptor).myIsNewLocal;
		}
	}

	@Override
	public String getName()
	{
		return myLocalVariable.name();
	}

	@Nullable
	@Override
	public String getDeclaredType()
	{
		return myTypeName;
	}

	@Override
	public PsiExpression getDescriptorEvaluation(DebuggerContext context) throws EvaluateException
	{
		PsiElementFactory elementFactory = JavaPsiFacade.getInstance(myProject).getElementFactory();
		try
		{
			return elementFactory.createExpressionFromText(getName(), PositionUtil.getContextElement(context));
		}
		catch(IncorrectOperationException e)
		{
			throw new EvaluateException(DebuggerBundle.message("error.invalid.local.variable.name", getName()), e);
		}
	}

	@Override
	public XValueModifier getModifier(JavaValue value)
	{
		return new JavaValueModifier(value)
		{
			@Override
			protected void setValueImpl(@Nonnull String expression, @Nonnull XModificationCallback callback)
			{
				final LocalVariableProxyImpl local = LocalVariableDescriptorImpl.this.getLocalVariable();
				if(local != null)
				{
					final DebuggerContextImpl debuggerContext = DebuggerManagerEx.getInstanceEx(getProject()).getContext();
					set(expression, callback, debuggerContext, new SetValueRunnable()
					{
						public void setValue(EvaluationContextImpl evaluationContext, Value newValue) throws ClassNotLoadedException, InvalidTypeException, EvaluateException
						{
							debuggerContext.getFrameProxy().setValue(local, preprocessValue(evaluationContext, newValue, local.getType()));
							update(debuggerContext);
						}

						public ReferenceType loadClass(EvaluationContextImpl evaluationContext,
								String className) throws InvocationException, ClassNotLoadedException, IncompatibleThreadStateException, InvalidTypeException, EvaluateException
						{
							return evaluationContext.getDebugProcess().loadClass(evaluationContext, className, evaluationContext.getClassLoader());
						}
					});
				}
			}
		};
	}
}