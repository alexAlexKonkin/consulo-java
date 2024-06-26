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
package com.intellij.java.debugger.impl.engine.evaluation.expression;

import java.util.List;

import com.intellij.java.debugger.engine.DebuggerUtils;
import com.intellij.java.debugger.engine.evaluation.EvaluateException;
import com.intellij.java.debugger.impl.engine.evaluation.EvaluationContextImpl;
import consulo.internal.com.sun.jdi.ObjectReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author egor
 */
public class TryEvaluator implements Evaluator
{
	@Nonnull
	private final Evaluator myBodyEvaluator;
	private final List<CatchEvaluator> myCatchBlockEvaluators;
	@Nullable
	private final Evaluator myFinallyEvaluator;

	public TryEvaluator(@Nonnull Evaluator bodyEvaluator, List<CatchEvaluator> catchBlockEvaluators, @Nullable Evaluator finallyEvaluator)
	{
		myBodyEvaluator = bodyEvaluator;
		myCatchBlockEvaluators = catchBlockEvaluators;
		myFinallyEvaluator = finallyEvaluator;
	}

	@Override
	public Object evaluate(EvaluationContextImpl context) throws EvaluateException
	{
		Object result = context.getSuspendContext().getDebugProcess().getVirtualMachineProxy().mirrorOfVoid();
		try
		{
			result = myBodyEvaluator.evaluate(context);
		}
		catch(EvaluateException e)
		{
			boolean catched = false;
			ObjectReference vmException = e.getExceptionFromTargetVM();
			if(vmException != null)
			{
				for(CatchEvaluator evaluator : myCatchBlockEvaluators)
				{
					if(evaluator != null && DebuggerUtils.instanceOf(vmException.type(), evaluator.getExceptionType()))
					{
						result = evaluator.evaluate(vmException, context);
						catched = true;
						break;
					}
				}
			}
			if(!catched)
			{
				throw e;
			}
		}
		finally
		{
			if(myFinallyEvaluator != null)
			{
				result = myFinallyEvaluator.evaluate(context);
			}
		}
		return result;
	}
}
