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

/*
 * User: anna
 * Date: 26-Jun-2007
 */
package com.intellij.codeInsight;

import java.util.List;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyKey;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.util.messages.Topic;

public abstract class ExternalAnnotationsManager
{
	@NonNls
	public static final String ANNOTATIONS_XML = "annotations.xml";

	public static final Topic<ExternalAnnotationsListener> TOPIC = Topic.create("external annotations", ExternalAnnotationsListener.class);

	public enum AnnotationPlace
	{
		IN_CODE,
		EXTERNAL,
		NOWHERE
	}

	private static final NotNullLazyKey<ExternalAnnotationsManager, Project> INSTANCE_KEY = ServiceManager.createLazyKey(ExternalAnnotationsManager
			.class);

	public static ExternalAnnotationsManager getInstance(@NotNull Project project)
	{
		return INSTANCE_KEY.getValue(project);
	}

	@Nullable
	public abstract PsiAnnotation findExternalAnnotation(@NotNull PsiModifierListOwner listOwner, @NotNull String annotationFQN);

	// Method used in Kotlin plugin
	public abstract boolean isExternalAnnotationWritable(@NotNull PsiModifierListOwner listOwner, @NotNull String annotationFQN);

	@Nullable
	public abstract PsiAnnotation[] findExternalAnnotations(@NotNull PsiModifierListOwner listOwner);

	public abstract void annotateExternally(
			@NotNull PsiModifierListOwner listOwner, @NotNull String annotationFQName, @NotNull PsiFile fromFile,
			@Nullable PsiNameValuePair[] value);

	public abstract boolean deannotate(@NotNull PsiModifierListOwner listOwner, @NotNull String annotationFQN);

	// Method used in Kotlin plugin when it is necessary to leave external annotation, but modify its arguments
	public abstract boolean editExternalAnnotation(
			@NotNull PsiModifierListOwner listOwner, @NotNull String annotationFQN, @Nullable PsiNameValuePair[] value);

	public abstract AnnotationPlace chooseAnnotationsPlace(@NotNull PsiElement element);

	@Nullable
	public abstract List<PsiFile> findExternalAnnotationsFiles(@NotNull PsiModifierListOwner listOwner);

	public abstract void dropCache();
}
