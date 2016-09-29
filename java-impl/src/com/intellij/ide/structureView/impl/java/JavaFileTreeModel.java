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
package com.intellij.ide.structureView.impl.java;

import java.util.Arrays;
import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.TextEditorBasedStructureViewModel;
import com.intellij.ide.util.treeView.smartTree.Filter;
import com.intellij.ide.util.treeView.smartTree.Grouper;
import com.intellij.ide.util.treeView.smartTree.NodeProvider;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import com.intellij.ide.util.treeView.smartTree.TreeStructureUtil;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.PlaceHolder;

public class JavaFileTreeModel extends TextEditorBasedStructureViewModel implements StructureViewModel.ElementInfoProvider, PlaceHolder<String>
{
	private static final Collection<NodeProvider> NODE_PROVIDERS = Arrays.<NodeProvider>asList(new JavaInheritedMembersNodeProvider(), new JavaAnonymousClassesNodeProvider());
	private final PsiClassOwner myFile;
	private String myPlace;

	public JavaFileTreeModel(@NotNull PsiClassOwner file)
	{
		super(file);
		myFile = file;
	}

	@Override
	@NotNull
	public Filter[] getFilters()
	{
		return new Filter[]{
				new FieldsFilter(),
				new PublicElementsFilter()
		};
	}

	@NotNull
	@Override
	public Collection<NodeProvider> getNodeProviders()
	{
		return NODE_PROVIDERS;
	}

	@Override
	@NotNull
	public Grouper[] getGroupers()
	{
		return new Grouper[]{
				new SuperTypesGrouper(),
				new PropertiesGrouper()
		};
	}

	@Override
	@NotNull
	public StructureViewTreeElement getRoot()
	{
		return new JavaFileTreeElement(myFile);
	}

	@Override
	public boolean shouldEnterElement(final Object element)
	{
		return element instanceof PsiClass;
	}

	@Override
	@NotNull
	public Sorter[] getSorters()
	{
		return new Sorter[]{
				TreeStructureUtil.isInStructureViewPopup(this) ? KindSorter.POPUP_INSTANCE : KindSorter.INSTANCE,
				VisibilitySorter.INSTANCE,
				AnonymousClassesSorter.INSTANCE,
				Sorter.ALPHA_SORTER
		};
	}

	@Override
	protected PsiFile getPsiFile()
	{
		return myFile;
	}

	@Override
	public boolean isAlwaysShowsPlus(StructureViewTreeElement element)
	{
		Object value = element.getValue();
		return value instanceof PsiClass || value instanceof PsiFile;
	}

	@Override
	public boolean isAlwaysLeaf(StructureViewTreeElement element)
	{
		Object value = element.getValue();
		return value instanceof PsiMethod || value instanceof PsiField;
	}

	@Override
	protected boolean isSuitable(final PsiElement element)
	{
		if(super.isSuitable(element))
		{
			if(element instanceof PsiMethod)
			{
				PsiMethod method = (PsiMethod) element;
				PsiClass parent = method.getContainingClass();
				return parent != null && (parent.getQualifiedName() != null || parent instanceof PsiAnonymousClass);
			}

			if(element instanceof PsiField)
			{
				PsiField field = (PsiField) element;
				PsiClass parent = field.getContainingClass();
				return parent != null && parent.getQualifiedName() != null;
			}

			if(element instanceof PsiClass)
			{
				return ((PsiClass) element).getQualifiedName() != null;
			}
		}
		return false;
	}

	@Override
	@NotNull
	protected Class[] getSuitableClasses()
	{
		return new Class[]{
				PsiClass.class,
				PsiMethod.class,
				PsiField.class,
				PsiJavaFile.class
		};
	}

	@Override
	public void setPlace(String place)
	{
		myPlace = place;
	}

	@Override
	public String getPlace()
	{
		return myPlace;
	}
}
