/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.psi;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.lang.jvm.JvmModifiersOwner;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;

import static com.intellij.psi.PsiJvmConversionHelper.getListAnnotations;
import static com.intellij.psi.PsiJvmConversionHelper.getListModifiers;

/**
 * Represents a PSI element which has a list of modifiers (public/private/protected/etc.)
 * and annotations.
 */
public interface PsiModifierListOwner extends PsiElement, JvmModifiersOwner
{
	/**
	 * Returns the list of modifiers for the element.
	 *
	 * @return the list of modifiers, or null if the element (for example, an anonymous
	 * inner class) does not have the list of modifiers.
	 */
	@Nullable
	PsiModifierList getModifierList();

	/**
	 * Checks if the element has the specified modifier. Possible modifiers are defined
	 * as constants in the {@link PsiModifier} class.
	 *
	 * @param name the name of the modifier to check.
	 * @return true if the element has the modifier, false otherwise
	 */
	boolean hasModifierProperty(@PsiModifier.ModifierConstant @NonNls @Nonnull String name);

	@Nonnull
	@Override
	default PsiAnnotation[] getAnnotations()
	{
		return getListAnnotations(this);
	}

	@Nonnull
	@Override
	default JvmModifier[] getModifiers()
	{
		return getListModifiers(this);
	}

	@Nullable
	default PsiAnnotation getAnnotation(@Nonnull String fqn)
	{
		return PsiJvmConversionHelper.getListAnnotation(this, fqn);
	}

	default boolean hasAnnotation(@Nonnull String fqn)
	{
		return PsiJvmConversionHelper.hasListAnnotation(this, fqn);
	}

	@Nonnull
	@Override
	default PsiElement getSourceElement()
	{
		return this;
	}
}
