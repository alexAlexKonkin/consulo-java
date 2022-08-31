/*
 * Copyright 2013-2014 must-be.org
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

package consulo.java.ide.newProjectOrModule;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import consulo.annotation.access.RequiredReadAction;
import consulo.ide.impl.UnzipNewModuleBuilderProcessor;
import consulo.ide.newProject.NewModuleBuilder;
import consulo.ide.newProject.NewModuleBuilderProcessor;
import consulo.ide.newProject.NewModuleContext;
import consulo.ide.newProject.node.NewModuleContextGroup;
import consulo.java.language.module.extension.JavaMutableModuleExtension;
import consulo.localize.LocalizeValue;
import consulo.roots.impl.ProductionContentFolderTypeProvider;
import consulo.ui.wizard.WizardStep;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 05.06.14
 */
public class JavaNewModuleBuilder implements NewModuleBuilder
{
	@Override
	public void setupContext(@Nonnull NewModuleContext context)
	{
		NewModuleContextGroup jvmGroup = context.addGroup("jvm", LocalizeValue.localizeTODO("Java Platform"));

		NewModuleContextGroup javaGroup = jvmGroup.addGroup("jvm.java", LocalizeValue.localizeTODO("Java"));

		javaGroup.add(LocalizeValue.localizeTODO("Empty"), AllIcons.FileTypes.Any_type, new NewModuleBuilderProcessor<JavaNewModuleWizardContext>()
		{
			@Nonnull
			@Override
			public JavaNewModuleWizardContext createContext(boolean isNewProject)
			{
				return new JavaNewModuleWizardContext(isNewProject);
			}

			@Override
			public void buildSteps(@Nonnull Consumer<WizardStep<JavaNewModuleWizardContext>> consumer, @Nonnull JavaNewModuleWizardContext context)
			{
				consumer.accept(new JavaSdkSelectStep(context));
			}

			@RequiredReadAction
			@Override
			public void process(@Nonnull JavaNewModuleWizardContext context, @Nonnull ContentEntry contentEntry, @Nonnull ModifiableRootModel modifiableRootModel)
			{
				setupModule(context, contentEntry, modifiableRootModel);
			}
		});

		javaGroup.add(LocalizeValue.localizeTODO("Console Application"), AllIcons.RunConfigurations.Application, new UnzipNewModuleBuilderProcessor<JavaNewModuleWizardContext>("/moduleTemplates/#JavaHelloWorld.zip")
		{
			@Nonnull
			@Override
			public JavaNewModuleWizardContext createContext(boolean isNewProject)
			{
				return new JavaNewModuleWizardContext(isNewProject);
			}

			@Override
			public void buildSteps(@Nonnull Consumer<WizardStep<JavaNewModuleWizardContext>> consumer, @Nonnull JavaNewModuleWizardContext context)
			{
				consumer.accept(new JavaSdkSelectStep(context));
			}

			@RequiredReadAction
			@Override
			public void process(@Nonnull JavaNewModuleWizardContext context, @Nonnull ContentEntry contentEntry, @Nonnull ModifiableRootModel modifiableRootModel)
			{
				unzip(modifiableRootModel);

				setupModule(context, contentEntry, modifiableRootModel);
			}
		});
	}

	@RequiredReadAction
	private static void setupModule(@Nonnull JavaNewModuleWizardContext context, @Nonnull ContentEntry contentEntry, @Nonnull ModifiableRootModel modifiableRootModel)
	{
		// need get by id - due, extension can be from original Java impl, or from other plugin, like IKVM.NET
		JavaMutableModuleExtension<?> javaMutableModuleExtension = modifiableRootModel.getExtensionWithoutCheck("java");
		assert javaMutableModuleExtension != null;

		javaMutableModuleExtension.setEnabled(true);

		Sdk sdk = context.getSdk();
		if(sdk != null)
		{
			javaMutableModuleExtension.getInheritableSdk().set(null, sdk);
			modifiableRootModel.addModuleExtensionSdkEntry(javaMutableModuleExtension);
		}
		contentEntry.addFolder(contentEntry.getUrl() + "/src", ProductionContentFolderTypeProvider.getInstance());
	}
}
