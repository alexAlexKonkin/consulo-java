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
package com.siyeh.ig.performance;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nonnull;
import javax.swing.JComponent;

import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import com.intellij.codeInspection.ui.ListTable;
import com.intellij.codeInspection.ui.ListWrappingTableModel;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import consulo.java.module.util.JavaClassNames;
import com.intellij.util.SmartList;
import com.siyeh.ig.ui.UiUtils;
import consulo.java.analysis.impl.JavaQuickFixBundle;

/**
 * @author Dmitry Batkovich
 */
public abstract class CollectionsListSettings
{
	@NonNls
	public static final SortedSet<String> DEFAULT_COLLECTION_LIST;

	static
	{
		final SortedSet<String> set = new TreeSet<>();
		set.add("java.util.concurrent.ConcurrentHashMap");
		set.add("java.util.concurrent.PriorityBlockingQueue");
		set.add("java.util.ArrayDeque");
		set.add("java.util.ArrayList");
		set.add("java.util.HashMap");
		set.add("java.util.Hashtable");
		set.add("java.util.HashSet");
		set.add("java.util.IdentityHashMap");
		set.add("java.util.LinkedHashMap");
		set.add("java.util.LinkedHashSet");
		set.add("java.util.PriorityQueue");
		set.add("java.util.Vector");
		set.add("java.util.WeakHashMap");
		DEFAULT_COLLECTION_LIST = Collections.unmodifiableSortedSet(set);
	}

	private final List<String> myCollectionClassesRequiringCapacity;

	public CollectionsListSettings()
	{
		myCollectionClassesRequiringCapacity = new SmartList<>(getDefaultSettings());
	}

	public void readSettings(@Nonnull Element node) throws InvalidDataException
	{
		myCollectionClassesRequiringCapacity.clear();
		myCollectionClassesRequiringCapacity.addAll(getDefaultSettings());
		for(Element classElement : node.getChildren("cls"))
		{
			final String className = classElement.getText();
			if(classElement.getAttributeValue("remove", Boolean.FALSE.toString()).equals(Boolean.TRUE.toString()))
			{
				myCollectionClassesRequiringCapacity.remove(className);
			}
			else
			{
				myCollectionClassesRequiringCapacity.add(className);
			}
		}
	}

	public void writeSettings(@Nonnull Element node) throws WriteExternalException
	{
		final Collection<String> defaultToRemoveSettings = new HashSet<>(getDefaultSettings());
		defaultToRemoveSettings.removeAll(myCollectionClassesRequiringCapacity);

		final Set<String> toAdd = new HashSet<>(myCollectionClassesRequiringCapacity);
		toAdd.removeAll(getDefaultSettings());

		for(String className : defaultToRemoveSettings)
		{
			node.addContent(new Element("cls").setText(className).setAttribute("remove", Boolean.TRUE.toString()));
		}
		for(String className : toAdd)
		{
			node.addContent(new Element("cls").setText(className));
		}
	}

	protected abstract Collection<String> getDefaultSettings();

	public Collection<String> getCollectionClassesRequiringCapacity()
	{
		return myCollectionClassesRequiringCapacity;
	}

	public JComponent createOptionsPanel()
	{
		final String title = JavaQuickFixBundle.message("collection.addall.can.be.replaced.with.constructor.fix.options.title");
		final ListTable table = new ListTable(new ListWrappingTableModel(myCollectionClassesRequiringCapacity, title));
		return UiUtils.createAddRemoveTreeClassChooserPanel(table, title, JavaClassNames.JAVA_LANG_OBJECT);
	}
}
