/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.codeInsight.lookup;

import javax.annotation.Nonnull;

import com.intellij.codeInsight.TailType;
import com.intellij.codeInsight.completion.JavaClassNameCompletionContributor;
import com.intellij.codeInsight.completion.JavaMethodCallElement;
import com.intellij.codeInsight.template.Template;
import consulo.logging.Logger;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiJavaPackage;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.meta.PsiMetaData;
import com.intellij.psi.util.PsiUtilCore;

/**
 * Created by IntelliJ IDEA.
 * User: ik
 * Date: 06.02.2003
 * Time: 16:05:20
 * To change this template use Options | File Templates.
 */
public class LookupItemUtil
{
	private static final Logger LOG = Logger.getInstance(LookupItemUtil.class);

	/**
	 * @see LookupElementBuilder
	 * @deprecated
	 */
	@Nonnull
	public static LookupElement objectToLookupItem(Object object)
	{
		if(object instanceof LookupElement)
		{
			return (LookupElement) object;
		}
		if(object instanceof PsiClass)
		{
			return JavaClassNameCompletionContributor.createClassLookupItem((PsiClass) object, true);
		}
		if(object instanceof PsiMethod)
		{
			return new JavaMethodCallElement((PsiMethod) object);
		}
		if(object instanceof PsiVariable)
		{
			return new VariableLookupItem((PsiVariable) object);
		}
		if(object instanceof PsiExpression)
		{
			return new ExpressionLookupItem((PsiExpression) object);
		}
		if(object instanceof PsiType)
		{
			return PsiTypeLookupItem.createLookupItem((PsiType) object, null);
		}
		if(object instanceof PsiJavaPackage)
		{
			return new PackageLookupItem((PsiJavaPackage) object);
		}

		String s = null;
		LookupItem item = new LookupItem(object, "");
		if(object instanceof PsiElement)
		{
			s = PsiUtilCore.getName((PsiElement) object);
		}
		TailType tailType = TailType.NONE;
		if(object instanceof PsiMetaData)
		{
			s = ((PsiMetaData) object).getName();
		}
		else if(object instanceof String)
		{
			s = (String) object;
		}
		else if(object instanceof Template)
		{
			s = ((Template) object).getKey();
		}
		else if(object instanceof PresentableLookupValue)
		{
			s = ((PresentableLookupValue) object).getPresentation();
		}

		if(object instanceof LookupValueWithUIHint && ((LookupValueWithUIHint) object).isBold())
		{
			item.setBold();
		}

		if(s == null)
		{
			LOG.error("Null string for object: " + object + " of class " + (object != null ? object.getClass() : null));
		}
		item.setLookupString(s);

		item.setTailType(tailType);
		return item;
	}
}
