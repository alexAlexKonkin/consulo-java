/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.codeInspection.bytecodeAnalysis;

import static com.intellij.codeInspection.bytecodeAnalysis.PResults.meet;

import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException;

class NotNullInterpreter extends NullityInterpreter
{
	NotNullInterpreter()
	{
		super(false, Direction.In.NOT_NULL_MASK);
	}

	@Override
	PResults.PResult combine(PResults.PResult res1, PResults.PResult res2) throws AnalyzerException
	{
		return meet(res1, res2);
	}
}