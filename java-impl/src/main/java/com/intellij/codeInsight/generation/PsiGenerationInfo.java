/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.codeInsight.generation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.util.IncorrectOperationException;

/**
 * @author peter
 */
public class PsiGenerationInfo<T extends PsiMember> extends GenerationInfoBase implements GenerationInfo
{
	private T myMember;
	private final boolean myMergeIfExists;
	private static final Logger LOG = Logger.getInstance("#" + PsiGenerationInfo.class.getName());

	public PsiGenerationInfo(@Nonnull final T member)
	{
		myMember = member;
		myMergeIfExists = true;
	}

	public PsiGenerationInfo(@Nonnull T member, boolean mergeIfExists)
	{
		myMember = member;
		myMergeIfExists = mergeIfExists;
	}

	@Override
	public final T getPsiMember()
	{
		return myMember;
	}

	@Override
	public void insert(@Nonnull final PsiClass aClass, @Nullable PsiElement anchor, boolean before) throws IncorrectOperationException
	{
		final PsiMember existingMember;
		if(myMember instanceof PsiField)
		{
			existingMember = aClass.findFieldByName(myMember.getName(), false);
		}
		else if(myMember instanceof PsiMethod)
		{
			existingMember = aClass.findMethodBySignature((PsiMethod) myMember, false);
		}
		else
		{
			existingMember = null;
		}
		if(existingMember == null || !myMergeIfExists)
		{
			PsiElement newMember = GenerateMembersUtil.insert(aClass, myMember, anchor, before);
			myMember = (T) JavaCodeStyleManager.getInstance(aClass.getProject()).shortenClassReferences(newMember);
			LOG.assertTrue(myMember.isValid(), myMember);
		}
		else
		{
			final PsiModifierList modifierList = myMember.getModifierList();
			final PsiModifierList existingModifierList = existingMember.getModifierList();
			if(modifierList != null && existingModifierList != null)
			{
				final PsiAnnotation[] psiAnnotations = modifierList.getAnnotations();
				PsiElement annoAnchor = existingModifierList.getAnnotations().length > 0 ? existingModifierList.getAnnotations()[0] : existingModifierList.getFirstChild();
				if(psiAnnotations.length > 0)
				{
					for(PsiAnnotation annotation : psiAnnotations)
					{
						final PsiAnnotation existingAnno = existingModifierList.findAnnotation(annotation.getQualifiedName());
						if(existingAnno != null)
						{
							annoAnchor = existingAnno.replace(annotation);
						}
						else
						{
							existingModifierList.addBefore(annotation, annoAnchor);
						}
					}
				}
			}
			myMember = (T) existingMember;
			if(!myMember.isValid())
			{
				LOG.error("invalid member: " + myMember +
						" existing member: " + existingMember.isValid() +
						" self modified list: " + modifierList +
						" existing modified list: " + existingModifierList);
			}
		}
	}
}
