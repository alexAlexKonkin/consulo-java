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
package com.intellij.codeInspection.defaultFileTemplateUsage;

import gnu.trove.TIntObjectHashMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.ide.fileTemplates.JavaTemplateUtil;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author cdr
 */
public class FileHeaderChecker
{
	private static final Logger LOG = Logger.getInstance("#com.intellij.codeInspection.defaultFileTemplateUsage.FileHeaderChecker");

	static ProblemDescriptor checkFileHeader(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean onTheFly)
	{
		TIntObjectHashMap<String> offsetToProperty = new TIntObjectHashMap<>();
		FileTemplate defaultTemplate = FileTemplateManager.getInstance(file.getProject()).getDefaultTemplate(JavaTemplateUtil.FILE_HEADER_TEMPLATE_NAME);
		Pattern pattern = FileTemplateUtil.getTemplatePattern(defaultTemplate, file.getProject(), offsetToProperty);
		Matcher matcher = pattern.matcher(file.getViewProvider().getContents());
		if(!matcher.matches())
		{
			return null;
		}

		PsiComment element = PsiTreeUtil.findElementOfClassAtRange(file, matcher.start(1), matcher.end(1), PsiComment.class);
		if(element == null)
		{
			return null;
		}

		LocalQuickFix[] fixes = createQuickFix(matcher, offsetToProperty, file.getProject(), onTheFly);
		String description = InspectionsBundle.message("default.file.template.description");
		return manager.createProblemDescriptor(element, description, onTheFly, fixes, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
	}


	private static Properties computeProperties(final Matcher matcher, final TIntObjectHashMap<String> offsetToProperty, Project project)
	{
		Properties properties = new Properties(FileTemplateManager.getInstance(project).getDefaultProperties());

		int[] offsets = offsetToProperty.keys();
		Arrays.sort(offsets);
		for(int i = 0; i < offsets.length; i++)
		{
			final int offset = offsets[i];
			String propName = offsetToProperty.get(offset);
			int groupNum = i + 2; // first group is whole doc comment
			String propValue = matcher.group(groupNum);
			properties.setProperty(propName, propValue);
		}

		return properties;
	}

	private static LocalQuickFix[] createQuickFix(final Matcher matcher, final TIntObjectHashMap<String> offsetToProperty, Project project, boolean onTheFly)
	{
		final FileTemplate template = FileTemplateManager.getInstance(project).getPattern(JavaTemplateUtil.FILE_HEADER_TEMPLATE_NAME);

		ReplaceWithFileTemplateFix replaceTemplateFix = new ReplaceWithFileTemplateFix()
		{
			@Override
			public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor)
			{
				PsiElement element = descriptor.getPsiElement();
				if(element == null)
				{
					return;
				}

				String newText;
				try
				{
					newText = template.getText(computeProperties(matcher, offsetToProperty, project)).trim();
				}
				catch(IOException e)
				{
					LOG.error(e);
					return;
				}

				if(!newText.isEmpty())
				{
					PsiElement parent = element.getParent();
					PsiFile tempFile = PsiFileFactory.getInstance(project).createFileFromText("template.java", JavaFileType.INSTANCE, newText);
					for(PsiElement child : tempFile.getChildren())
					{
						if(child.getTextLength() > 0)
						{
							parent.addBefore(child, element);
						}
					}
				}

				element.delete();
			}
		};

		if(onTheFly)
		{
			LocalQuickFix editFileTemplateFix = DefaultFileTemplateUsageInspection.createEditFileTemplateFix(template, replaceTemplateFix);
			return template.isDefault() ? new LocalQuickFix[]{editFileTemplateFix} : new LocalQuickFix[]{
					replaceTemplateFix,
					editFileTemplateFix
			};
		}
		return template.isDefault() ? null : new LocalQuickFix[]{replaceTemplateFix};
	}
}