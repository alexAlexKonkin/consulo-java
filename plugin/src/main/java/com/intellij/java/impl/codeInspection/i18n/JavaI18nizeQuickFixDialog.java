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

/**
 * @author cdr
 */
package com.intellij.java.impl.codeInspection.i18n;

import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.intention.IntentionAction;
import consulo.fileTemplate.FileTemplate;
import consulo.fileTemplate.FileTemplateManager;
import consulo.ide.impl.idea.ide.fileTemplates.impl.FileTemplateConfigurable;
import com.intellij.java.language.impl.JavaFileType;
import com.intellij.java.language.psi.*;
import com.intellij.java.impl.lang.properties.psi.I18nizedTextGenerator;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.java.impl.lang.properties.psi.PropertyCreationHandler;
import com.intellij.java.impl.lang.properties.psi.ResourceBundleManager;
import com.intellij.lang.properties.references.I18nizeQuickFixDialog;
import consulo.document.Document;
import consulo.document.event.DocumentAdapter;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.project.Project;
import consulo.ui.ex.awt.HyperlinkLabel;
import consulo.ui.ex.awt.Messages;
import consulo.ide.impl.idea.openapi.ui.ex.MultiLineLabel;
import consulo.util.lang.StringUtil;
import com.intellij.psi.*;
import consulo.language.editor.ui.awt.EditorComboBox;
import consulo.util.collection.ArrayUtil;
import consulo.language.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JavaI18nizeQuickFixDialog extends I18nizeQuickFixDialog
{
	private final PsiLiteralExpression myLiteralExpression;

	private final JLabel myPreviewLabel;
	private final JPanel myHyperLinkPanel;
	private final JPanel myResourceBundleSuggester;
	private EditorComboBox myRBEditorTextField;
	private final JPanel myJavaCodeInfoPanel;
	private final JPanel myPreviewPanel;
	private PsiClassType myResourceBundleType;
	protected final ResourceBundleManager myResourceBundleManager;

	private final boolean myShowJavaCodeInfo;
	private final boolean myShowPreview;

	@NonNls
	public static final String PROPERTY_KEY_OPTION_KEY = "PROPERTY_KEY";
	@NonNls
	private static final String RESOURCE_BUNDLE_OPTION_KEY = "RESOURCE_BUNDLE";
	@NonNls
	public static final String PROPERTY_VALUE_ATTR = "PROPERTY_VALUE";

	public JavaI18nizeQuickFixDialog(@Nonnull Project project,
									 @Nonnull final PsiFile context,
									 @Nullable final PsiLiteralExpression literalExpression,
									 String defaultPropertyValue,
									 DialogCustomization customization,
									 final boolean showJavaCodeInfo,
									 final boolean showPreview)
	{
		super(project, context, defaultPropertyValue, customization, true);

		ResourceBundleManager resourceBundleManager = null;
		try
		{
			resourceBundleManager = ResourceBundleManager.getManager(context);
			LOG.assertTrue(resourceBundleManager != null);
		}
		catch(ResourceBundleManager.ResourceBundleNotFoundException e)
		{
			LOG.error(e);
		}
		myResourceBundleManager = resourceBundleManager;

		JavaExtensibilityData data = new JavaExtensibilityData();
		myExtensibilityPanel.setLayout(new BorderLayout());
		myExtensibilityPanel.add(data.myPanel, BorderLayout.CENTER);
		myJavaCodeInfoPanel = data.myJavaCodeInfoPanel;
		myPreviewPanel = data.myPreviewPanel;
		myHyperLinkPanel = data.myHyperLinkPanel;
		myResourceBundleSuggester = data.myResourceBundleSuggester;
		myPreviewLabel = data.myPreviewLabel;

		myLiteralExpression = literalExpression;
		myShowPreview = showPreview;

		myResourceBundleSuggester.setLayout(new BorderLayout());
		PsiElementFactory factory = JavaPsiFacade.getInstance(myProject).getElementFactory();
		PsiClass resourceBundle = myResourceBundleManager.getResourceBundle();

		myShowJavaCodeInfo = showJavaCodeInfo && myResourceBundleManager.canShowJavaCodeInfo();

		if(myShowJavaCodeInfo)
		{
			LOG.assertTrue(resourceBundle != null);
			myResourceBundleType = factory.createType(resourceBundle);
			@NonNls String defaultVarName = "resourceBundle";
			final JavaCodeFragmentFactory codeFragmentFactory = JavaCodeFragmentFactory.getInstance(project);
			PsiExpressionCodeFragment expressionCodeFragment =
					codeFragmentFactory.createExpressionCodeFragment(defaultVarName, myLiteralExpression, myResourceBundleType, true);
			Document document = PsiDocumentManager.getInstance(myProject).getDocument(expressionCodeFragment);
			myRBEditorTextField = new EditorComboBox(document, myProject, JavaFileType.INSTANCE);
			myResourceBundleSuggester.add(myRBEditorTextField, BorderLayout.CENTER);
			suggestAvailableResourceBundleExpressions();
			myRBEditorTextField.addDocumentListener(new DocumentAdapter()
			{
				@Override
				public void documentChanged(com.intellij.openapi.editor.event.DocumentEvent e)
				{
					somethingChanged();
				}
			});
		}

		myHyperLinkPanel.setLayout(new BorderLayout());
		final String templateName = getTemplateName();

		if(templateName != null)
		{
			HyperlinkLabel link = new HyperlinkLabel(CodeInsightBundle.message("i18nize.dialog.template.link.label"));
			link.addHyperlinkListener(new HyperlinkListener()
			{
				@Override
				public void hyperlinkUpdate(HyperlinkEvent e)
				{
					final FileTemplateConfigurable configurable = new FileTemplateConfigurable(project);
					final FileTemplate template = FileTemplateManager.getInstance(project).getCodeTemplate(templateName);
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							configurable.setTemplate(template, null);
						}
					});
					ShowSettingsUtil.getInstance().editConfigurable(myPanel, configurable).doWhenDone(() -> {
						somethingChanged();
						if(myShowJavaCodeInfo)
						{
							suggestAvailableResourceBundleExpressions();
						}
					});
				}
			});
			myHyperLinkPanel.add(link, BorderLayout.CENTER);
		}

		if(!myShowJavaCodeInfo)
		{
			myJavaCodeInfoPanel.setVisible(false);
		}
		if(!myShowPreview)
		{
			myPreviewPanel.setVisible(false);
		}

		init();
	}

	public static boolean isAvailable(PsiFile file)
	{
		final Project project = file.getProject();
		final String title = CodeInsightBundle.message("i18nize.dialog.error.jdk.title");
		try
		{
			return ResourceBundleManager.getManager(file) != null;
		}
		catch(ResourceBundleManager.ResourceBundleNotFoundException e)
		{
			final IntentionAction fix = e.getFix();
			if(fix != null)
			{
				if(Messages.showOkCancelDialog(project, e.getMessage(), title, Messages.getErrorIcon()) == OK_EXIT_CODE)
				{
					try
					{
						fix.invoke(project, null, file);
						return false;
					}
					catch(IncorrectOperationException e1)
					{
						LOG.error(e1);
					}
				}
			}
			Messages.showErrorDialog(project, e.getMessage(), title);
			return false;
		}
	}

	public PropertyCreationHandler getPropertyCreationHandler()
	{
		PropertyCreationHandler handler = myResourceBundleManager.getPropertyCreationHandler();
		return handler != null ? handler : JavaI18nUtil.DEFAULT_PROPERTY_CREATION_HANDLER;
	}

	private void suggestAvailableResourceBundleExpressions()
	{
		String templateName = getTemplateName();
		if(templateName == null)
		{
			return;
		}

		if(myShowJavaCodeInfo)
		{
			FileTemplate template = FileTemplateManager.getInstance(myProject).getCodeTemplate(templateName);
			boolean showResourceBundleSuggester = template.getText().contains("${" + RESOURCE_BUNDLE_OPTION_KEY + "}");
			myJavaCodeInfoPanel.setVisible(showResourceBundleSuggester);
		}
		Set<String> result = JavaI18nUtil.suggestExpressionOfType(myResourceBundleType, myLiteralExpression);
		if(result.isEmpty())
		{
			result.add(getResourceBundleText());
		}

		myRBEditorTextField.setHistory(ArrayUtil.toStringArray(result));
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				myRBEditorTextField.setSelectedIndex(0);
			}
		});
	}

	@Override
	protected void somethingChanged()
	{
		if(myShowPreview)
		{
			myPreviewLabel.setText(getI18nizedText());
		}
		super.somethingChanged();
	}

	@Nullable
	protected String getTemplateName()
	{
		return myResourceBundleManager.getTemplateName();
	}

	@Override
	protected String defaultSuggestPropertyKey(String value)
	{
		return myResourceBundleManager.suggestPropertyKey(value);
	}

	@Override
	protected List<String> defaultSuggestPropertiesFiles()
	{
		return myResourceBundleManager.suggestPropertiesFiles();
	}

	public String getI18nizedText()
	{
		String propertyKey = StringUtil.escapeStringCharacters(getKey());
		I18nizedTextGenerator textGenerator = myResourceBundleManager.getI18nizedTextGenerator();
		if(textGenerator != null)
		{
			return generateText(textGenerator, propertyKey, getPropertiesFile(), myLiteralExpression);
		}

		String templateName = getTemplateName();
		LOG.assertTrue(templateName != null);
		FileTemplate template = FileTemplateManager.getInstance(myProject).getCodeTemplate(templateName);
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(PROPERTY_KEY_OPTION_KEY, propertyKey);
		attributes.put(RESOURCE_BUNDLE_OPTION_KEY, getResourceBundleText());
		attributes.put(PROPERTY_VALUE_ATTR, StringUtil.escapeStringCharacters(myDefaultPropertyValue));
		addAdditionalAttributes(attributes);
		String text = null;
		try
		{
			text = template.getText(attributes);
		}
		catch(IOException e)
		{
			LOG.error(e);
		}
		return text;
	}

	protected String generateText(final I18nizedTextGenerator textGenerator,
								  final String propertyKey,
								  final PropertiesFile propertiesFile,
								  final PsiLiteralExpression literalExpression)
	{
		return textGenerator.getI18nizedText(propertyKey, propertiesFile, literalExpression);
	}

	protected void addAdditionalAttributes(final Map<String, String> attributes)
	{
	}

	private String getResourceBundleText()
	{
		return myShowJavaCodeInfo ? myRBEditorTextField.getText() : null;
	}

	public PsiLiteralExpression getLiteralExpression()
	{
		return myLiteralExpression;
	}

	public PsiExpression[] getParameters()
	{
		return PsiExpression.EMPTY_ARRAY;
	}

	static class JavaExtensibilityData
	{
		private JPanel myPreviewPanel;
		private JPanel myJavaCodeInfoPanel;
		private JPanel myPanel;
		private JPanel myHyperLinkPanel;
		private MultiLineLabel myPreviewLabel;
		private JPanel myResourceBundleSuggester;
	}
}
