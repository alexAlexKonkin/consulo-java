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
package com.intellij.java.debugger.impl.engine;

import com.intellij.java.language.psi.PsiClass;
import com.intellij.util.Range;

/**
 * @author egor
 */
public class ConstructorStepMethodFilter extends BasicStepMethodFilter
{
	public ConstructorStepMethodFilter(JVMName classJvmName, Range<Integer> callingExpressionLines)
	{
		super(classJvmName, JVMNameUtil.CONSTRUCTOR_NAME, null, callingExpressionLines);
	}

	public ConstructorStepMethodFilter(PsiClass psiClass, Range<Integer> callingExpressionLines)
	{
		this(JVMNameUtil.getJVMQualifiedName(psiClass), callingExpressionLines);
	}
}
