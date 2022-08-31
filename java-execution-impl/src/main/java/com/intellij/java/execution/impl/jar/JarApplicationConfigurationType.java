/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.java.execution.impl.jar;

import javax.annotation.Nonnull;

import com.intellij.execution.configuration.ConfigurationFactoryEx;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import consulo.java.execution.JavaExecutionBundle;
import consulo.java.language.module.extension.JavaModuleExtension;
import consulo.module.extension.ModuleExtensionHelper;

public class JarApplicationConfigurationType extends ConfigurationTypeBase implements ConfigurationType
{
	@Nonnull
	public static JarApplicationConfigurationType getInstance()
	{
		return ConfigurationTypeUtil.findConfigurationType(JarApplicationConfigurationType.class);
	}

	public JarApplicationConfigurationType()
	{
		super("JarApplication", JavaExecutionBundle.message("jar.application.configuration.name"), JavaExecutionBundle.message("jar.application.configuration.description"), AllIcons.FileTypes.Archive);
		addFactory(new ConfigurationFactoryEx(this)
		{
			@Override
			public void onNewConfigurationCreated(@Nonnull RunConfiguration configuration)
			{
				JarApplicationConfiguration jarApplicationConfiguration = (JarApplicationConfiguration) configuration;
				if(StringUtil.isEmpty(jarApplicationConfiguration.getWorkingDirectory()))
				{
					String baseDir = FileUtil.toSystemIndependentName(StringUtil.notNullize(configuration.getProject().getBasePath()));
					jarApplicationConfiguration.setWorkingDirectory(baseDir);
				}
			}

			@Override
			@Nonnull
			public RunConfiguration createTemplateConfiguration(Project project)
			{
				return new JarApplicationConfiguration(project, this, "");
			}

			@Override
			public boolean isApplicable(@Nonnull Project project)
			{
				return ModuleExtensionHelper.getInstance(project).hasModuleExtension(JavaModuleExtension.class);
			}
		});
	}
}
