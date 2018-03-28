/*
 * Copyright 2013-2017 consulo.io
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

package consulo.java.codeInsight.completion;

import javax.annotation.Nullable;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiType;
import com.intellij.util.PairFunction;
import com.siyeh.ig.psiutils.SideEffectChecker;

/**
 * @author VISTALL
 * @since 28-Dec-17
 */
public class JavaCompletionUtilCore
{
	public static final Key<PairFunction<PsiExpression, CompletionParameters, PsiType>> DYNAMIC_TYPE_EVALUATOR = Key.create("DYNAMIC_TYPE_EVALUATOR");

	public static boolean mayHaveSideEffects(@Nullable final PsiElement element)
	{
		return element instanceof PsiExpression && SideEffectChecker.mayHaveSideEffects((PsiExpression) element);
	}
}
