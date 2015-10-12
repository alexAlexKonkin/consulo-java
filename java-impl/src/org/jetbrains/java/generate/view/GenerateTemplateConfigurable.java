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

/*
 * @author max
 */
package org.jetbrains.java.generate.view;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.java.generate.element.ClassElement;
import org.jetbrains.java.generate.element.FieldElement;
import org.jetbrains.java.generate.element.GenerationHelper;
import org.jetbrains.java.generate.template.TemplateResource;
import org.jetbrains.java.generate.template.TemplatesManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ex.MultiLineLabel;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiType;
import com.intellij.util.LocalTimeCounter;
import com.intellij.util.ui.JBUI;

public class GenerateTemplateConfigurable implements UnnamedConfigurable
{
	private final TemplateResource template;
	private final Editor myEditor;
	private final List<String> availableImplicits = new ArrayList<String>();

	public GenerateTemplateConfigurable(TemplateResource template, Map<String, PsiType> contextMap, Project project)
	{
		this(template, contextMap, project, true);
	}

	public GenerateTemplateConfigurable(TemplateResource template, Map<String, PsiType> contextMap, Project project, boolean multipleFields)
	{
		this.template = template;
		final EditorFactory factory = EditorFactory.getInstance();
		Document doc = factory.createDocument(template.getTemplate());
		final FileType ftl = FileTypeManager.getInstance().findFileTypeByName("VTL");
		if(project != null && ftl != null)
		{
			final PsiFile file = PsiFileFactory.getInstance(project).createFileFromText(template.getFileName(), ftl, template.getTemplate(), LocalTimeCounter.currentTime(), true);
			if(!template.isDefault())
			{
				final HashMap<String, PsiType> map = new LinkedHashMap<String, PsiType>();
				map.put("java_version", PsiType.INT);
				map.put("class", TemplatesManager.createElementType(project, ClassElement.class));
				if(multipleFields)
				{
					map.put("fields", TemplatesManager.createFieldListElementType(project));
				}
				else
				{
					map.put("field", TemplatesManager.createElementType(project, FieldElement.class));
				}
				map.put("helper", TemplatesManager.createElementType(project, GenerationHelper.class));
				map.put("settings", PsiType.NULL);
				map.putAll(contextMap);
				availableImplicits.addAll(map.keySet());
				file.getViewProvider().putUserData(TemplatesManager.TEMPLATE_IMPLICITS, map);
			}
			final Document document = PsiDocumentManager.getInstance(project).getDocument(file);
			if(document != null)
			{
				doc = document;
			}
		}
		myEditor = factory.createEditor(doc, project, ftl != null ? ftl : PlainTextFileType.INSTANCE, template.isDefault());
	}

	@Override
	public JComponent createComponent()
	{
		final JComponent component = myEditor.getComponent();
		if(availableImplicits.isEmpty())
		{
			return component;
		}
		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(component, BorderLayout.CENTER);
		MultiLineLabel label = new MultiLineLabel("<html>Available implicit variables:\n" + StringUtil.join(availableImplicits, ", ") + "</html>");
		label.setPreferredSize(JBUI.size(250, 30));
		panel.add(label, BorderLayout.SOUTH);
		return panel;
	}

	@Override
	public boolean isModified()
	{
		return !Comparing.equal(myEditor.getDocument().getText(), template.getTemplate());
	}

	@Override
	public void apply() throws ConfigurationException
	{
		template.setTemplate(myEditor.getDocument().getText());
	}

	@Override
	public void reset()
	{
		new WriteCommandAction(null)
		{
			@Override
			protected void run(@NotNull Result result) throws Throwable
			{
				myEditor.getDocument().setText(template.getTemplate());
			}
		}.execute();
	}

	@Override
	public void disposeUIResources()
	{
		EditorFactory.getInstance().releaseEditor(myEditor);
	}
}