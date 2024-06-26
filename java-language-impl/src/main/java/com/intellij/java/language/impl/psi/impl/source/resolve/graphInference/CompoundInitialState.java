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
package com.intellij.java.language.impl.psi.impl.source.resolve.graphInference;

import java.util.Map;

import com.intellij.java.language.psi.PsiCall;
import consulo.language.psi.PsiElement;
import com.intellij.java.language.psi.PsiSubstitutor;

class CompoundInitialState
{
	private PsiSubstitutor myInitialSubstitutor;
	private Map<PsiElement, InitialInferenceState> myInitialStates;

	CompoundInitialState(PsiSubstitutor initialSubstitutor, Map<PsiElement, InitialInferenceState> initialStates)
	{
		myInitialSubstitutor = initialSubstitutor;
		myInitialStates = initialStates;
	}

	PsiSubstitutor getInitialSubstitutor()
	{
		return myInitialSubstitutor;
	}

	Map<PsiElement, InitialInferenceState> getInitialStates()
	{
		return myInitialStates;
	}

	InitialInferenceState getInitialState(PsiCall call)
	{
		return myInitialStates.get(call);
	}
}
