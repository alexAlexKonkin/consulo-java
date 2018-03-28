/*
 * Copyright 2013 Consulo.org
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
package consulo.java.module.extension;

import java.util.Set;

import javax.annotation.Nonnull;

import org.jdom.Element;
import com.intellij.compiler.impl.ModuleChunk;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import consulo.annotations.RequiredReadAction;
import consulo.module.extension.ModuleInheritableNamedPointer;
import consulo.module.extension.impl.ModuleExtensionWithSdkImpl;
import consulo.roots.ModuleRootLayer;

/**
 * @author VISTALL
 * @since 10:02/19.05.13
 */
public class JavaModuleExtensionImpl extends ModuleExtensionWithSdkImpl<JavaModuleExtensionImpl> implements
		JavaModuleExtension<JavaModuleExtensionImpl>
{
	private static final String SPECIAL_DIR_LOCATION = "special-dir-location";
	private static final String BYTECODE_VERSION = "bytecode-version";

	protected LanguageLevelModuleInheritableNamedPointerImpl myLanguageLevel;
	protected SpecialDirLocation mySpecialDirLocation = SpecialDirLocation.SOURCE_DIR;
	protected String myBytecodeVersion;

	private LazyValueBySdk<LanguageLevel> myLanguageLevelValue;

	public JavaModuleExtensionImpl(@Nonnull String id, @Nonnull ModuleRootLayer moduleRootLayer)
	{
		super(id, moduleRootLayer);
		myLanguageLevel = new LanguageLevelModuleInheritableNamedPointerImpl(moduleRootLayer, id);
		myLanguageLevelValue = new LazyValueBySdk<>(this, LanguageLevel.HIGHEST, sdk -> {
			JavaSdkVersion sdkVersion = JavaSdk.getInstance().getVersion(sdk);
			return sdkVersion == null ? LanguageLevel.HIGHEST : sdkVersion.getMaxLanguageLevel();
		});
	}

	@RequiredReadAction
	@Override
	public void commit(@Nonnull JavaModuleExtensionImpl mutableModuleExtension)
	{
		super.commit(mutableModuleExtension);

		myLanguageLevel.set(mutableModuleExtension.getInheritableLanguageLevel());
		mySpecialDirLocation = mutableModuleExtension.getSpecialDirLocation();
		myBytecodeVersion = mutableModuleExtension.getBytecodeVersion();
	}

	@Override
	@Nonnull
	public LanguageLevel getLanguageLevel()
	{
		return myLanguageLevel.isNull() ? myLanguageLevelValue.getValue() : myLanguageLevel.get();
	}

	@javax.annotation.Nullable
	@Override
	public LanguageLevel getLanguageLevelNoDefault()
	{
		return myLanguageLevel.get();
	}

	@Override
	@Nonnull
	public SpecialDirLocation getSpecialDirLocation()
	{
		return mySpecialDirLocation;
	}

	@javax.annotation.Nullable
	@Override
	public Sdk getSdkForCompilation()
	{
		return getSdk();
	}

	@javax.annotation.Nullable
	@Override
	public String getBytecodeVersion()
	{
		return myBytecodeVersion;
	}

	@Nonnull
	@Override
	public Set<VirtualFile> getCompilationClasspath(@Nonnull CompileContext compileContext, @Nonnull ModuleChunk moduleChunk)
	{
		return moduleChunk.getCompilationClasspathFiles(JavaSdk.getInstance());
	}

	@Nonnull
	@Override
	public Set<VirtualFile> getCompilationBootClasspath(@Nonnull CompileContext compileContext, @Nonnull ModuleChunk moduleChunk)
	{
		return moduleChunk.getCompilationBootClasspathFiles(JavaSdk.getInstance());
	}

	@Nonnull
	public ModuleInheritableNamedPointer<LanguageLevel> getInheritableLanguageLevel()
	{
		return myLanguageLevel;
	}

	@Nonnull
	@Override
	public Class<? extends SdkType> getSdkTypeClass()
	{
		return JavaSdk.class;
	}

	@Override
	protected void getStateImpl(@Nonnull Element element)
	{
		super.getStateImpl(element);

		myLanguageLevel.toXml(element);
		if(mySpecialDirLocation != SpecialDirLocation.SOURCE_DIR)
		{
			element.setAttribute(SPECIAL_DIR_LOCATION, mySpecialDirLocation.name());
		}
		if(!StringUtil.isEmpty(myBytecodeVersion))
		{
			element.setAttribute(BYTECODE_VERSION, myBytecodeVersion);
		}
	}

	@RequiredReadAction
	@Override
	protected void loadStateImpl(@Nonnull Element element)
	{
		super.loadStateImpl(element);

		myLanguageLevel.fromXml(element);
		mySpecialDirLocation = SpecialDirLocation.valueOf(element.getAttributeValue(SPECIAL_DIR_LOCATION, SpecialDirLocation.SOURCE_DIR.name()));
		myBytecodeVersion = element.getAttributeValue(BYTECODE_VERSION, (String) null);
	}
}