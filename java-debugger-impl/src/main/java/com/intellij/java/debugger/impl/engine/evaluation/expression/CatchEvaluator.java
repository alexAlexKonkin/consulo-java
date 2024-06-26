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

import com.intellij.java.debugger.engine.evaluation.EvaluateException;
import com.intellij.java.debugger.impl.engine.evaluation.EvaluationContextImpl;
import consulo.internal.com.sun.jdi.ObjectReference;

/**
 * @author egor
 */
public class CatchEvaluator implements Evaluator
{
	private final String myExceptionType;
	private final String myParamName;
	private final CodeFragmentEvaluator myEvaluator;

	public CatchEvaluator(String exceptionType, String paramName, CodeFragmentEvaluator evaluator)
	{
		myExceptionType = exceptionType;
		myParamName = paramName;
		myEvaluator = evaluator;
	}

	public Object evaluate(ObjectReference exception, EvaluationContextImpl context) throws EvaluateException
	{
		myEvaluator.setValue(myParamName, exception);
		return myEvaluator.evaluate(context);
	}

	@Override
	public Object evaluate(EvaluationContextImpl context) throws EvaluateException
	{
		throw new IllegalStateException("Use evaluate(ObjectReference exception, EvaluationContextImpl context)");
	}

	public String getExceptionType()
	{
		return myExceptionType;
	}
}
