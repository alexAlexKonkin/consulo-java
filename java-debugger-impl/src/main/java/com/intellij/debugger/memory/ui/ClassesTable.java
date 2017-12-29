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
package com.intellij.debugger.memory.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.debugger.memory.component.InstancesTracker;
import com.intellij.debugger.memory.tracking.TrackerForNewInstances;
import com.intellij.debugger.memory.tracking.TrackingType;
import com.intellij.debugger.memory.utils.AbstractTableColumnDescriptor;
import com.intellij.debugger.memory.utils.AbstractTableModelWithColumns;
import com.intellij.debugger.memory.utils.InstancesProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.codeStyle.MinusculeMatcher;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.util.containers.FList;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBUI;
import consulo.internal.com.sun.jdi.ObjectReference;
import consulo.internal.com.sun.jdi.ReferenceType;

public class ClassesTable extends JBTable implements DataProvider, Disposable
{
	public static final Key<ReferenceType> SELECTED_CLASS_KEY = Key.create("ClassesTable.SelectedClass");
	public static final Key<InstancesProvider> NEW_INSTANCES_PROVIDER_KEY = Key.create("ClassesTable.NewInstances");
	public static final Key<ReferenceCountProvider> REF_COUNT_PROVIDER_KEY = Key.create("ClassesTable.ReferenceCountProvider");

	private static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder();

	private static final int CLASSES_COLUMN_PREFERRED_WIDTH = 250;
	private static final int COUNT_COLUMN_MIN_WIDTH = 80;
	private static final int DIFF_COLUMN_MIN_WIDTH = 80;
	private static final UnknownDiffValue UNKNOWN_VALUE = new UnknownDiffValue();

	private final DiffViewTableModel myModel = new DiffViewTableModel();
	private final Map<ReferenceType, DiffValue> myCounts = new ConcurrentHashMap<>();
	private final InstancesTracker myInstancesTracker;
	private final ClassesFilteredView myParent;
	private final ReferenceCountProvider myCountProvider;

	private boolean myOnlyWithDiff;
	private boolean myOnlyTracked;
	private boolean myOnlyWithInstances;
	private MinusculeMatcher myMatcher = NameUtil.buildMatcher("*").build();
	private String myFilteringPattern = "";

	private volatile List<ReferenceType> myItems = Collections.unmodifiableList(new ArrayList<>());
	private boolean myIsShowCounts = true;

	public ClassesTable(@NotNull InstancesTracker tracker, @NotNull ClassesFilteredView parent, boolean onlyWithDiff, boolean onlyWithInstances, boolean onlyTracked)
	{
		setModel(myModel);

		myOnlyWithDiff = onlyWithDiff;
		myOnlyWithInstances = onlyWithInstances;
		myOnlyTracked = onlyTracked;
		myInstancesTracker = tracker;
		myParent = parent;

		final TableColumnModel columnModel = getColumnModel();
		TableColumn classesColumn = columnModel.getColumn(DiffViewTableModel.CLASSNAME_COLUMN_INDEX);
		TableColumn countColumn = columnModel.getColumn(DiffViewTableModel.COUNT_COLUMN_INDEX);
		TableColumn diffColumn = columnModel.getColumn(DiffViewTableModel.DIFF_COLUMN_INDEX);

		setAutoResizeMode(AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		classesColumn.setPreferredWidth(JBUI.scale(CLASSES_COLUMN_PREFERRED_WIDTH));

		countColumn.setMinWidth(JBUI.scale(COUNT_COLUMN_MIN_WIDTH));

		diffColumn.setMinWidth(JBUI.scale(DIFF_COLUMN_MIN_WIDTH));

		setShowGrid(false);
		setIntercellSpacing(new JBDimension(0, 0));

		setDefaultRenderer(ReferenceType.class, new MyClassColumnRenderer());
		setDefaultRenderer(Long.class, new MyCountColumnRenderer());
		setDefaultRenderer(DiffValue.class, new MyDiffColumnRenderer());

		TableRowSorter<DiffViewTableModel> sorter = new TableRowSorter<>(myModel);
		sorter.setRowFilter(new RowFilter<DiffViewTableModel, Integer>()
		{
			@Override
			public boolean include(Entry<? extends DiffViewTableModel, ? extends Integer> entry)
			{
				int ix = entry.getIdentifier();
				ReferenceType ref = myItems.get(ix);
				DiffValue diff = myCounts.getOrDefault(ref, UNKNOWN_VALUE);

				boolean isFilteringOptionsRefused = myOnlyWithDiff && diff.diff() == 0 || myOnlyWithInstances && !diff.hasInstance() || myOnlyTracked && myParent.getStrategy(ref) == null;
				return !(isFilteringOptionsRefused) && myMatcher.matches(ref.name());
			}
		});

		List<RowSorter.SortKey> myDefaultSortingKeys = Arrays.asList(new RowSorter.SortKey(DiffViewTableModel.DIFF_COLUMN_INDEX, SortOrder.DESCENDING), new RowSorter.SortKey(DiffViewTableModel
				.COUNT_COLUMN_INDEX, SortOrder.DESCENDING), new RowSorter.SortKey(DiffViewTableModel.CLASSNAME_COLUMN_INDEX, SortOrder.ASCENDING));
		sorter.setSortKeys(myDefaultSortingKeys);
		setRowSorter(sorter);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		myCountProvider = new ReferenceCountProvider()
		{
			@Override
			public int getTotalCount(@NotNull ReferenceType ref)
			{
				return (int) myCounts.get(ref).myCurrentCount;
			}

			@Override
			public int getDiffCount(@NotNull ReferenceType ref)
			{
				return (int) myCounts.get(ref).diff();
			}

			@Override
			public int getNewInstancesCount(@NotNull ReferenceType ref)
			{
				TrackerForNewInstances strategy = myParent.getStrategy(ref);
				return strategy == null || !strategy.isReady() ? -1 : strategy.getCount();
			}
		};
	}

	public interface ReferenceCountProvider
	{

		int getTotalCount(@NotNull ReferenceType ref);

		int getDiffCount(@NotNull ReferenceType ref);

		int getNewInstancesCount(@NotNull ReferenceType ref);
	}

	@Nullable
	ReferenceType getSelectedClass()
	{
		int selectedRow = getSelectedRow();
		if(selectedRow != -1)
		{
			int ix = convertRowIndexToModel(selectedRow);
			return myItems.get(ix);
		}

		return null;
	}

	@Nullable
	ReferenceType getClassByName(@NotNull String name)
	{
		for(ReferenceType ref : myItems)
		{
			if(name.equals(ref.name()))
			{
				return ref;
			}
		}

		return null;
	}

	void setBusy(boolean value)
	{
		setPaintBusy(value);
	}

	void setFilterPattern(String pattern)
	{
		if(!myFilteringPattern.equals(pattern))
		{
			myFilteringPattern = pattern;
			myMatcher = NameUtil.buildMatcher("*" + pattern).build();
			fireTableDataChanged();
			if(getSelectedClass() == null && getRowCount() > 0)
			{
				getSelectionModel().setSelectionInterval(0, 0);
			}
		}
	}

	void setFilteringByInstanceExists(boolean value)
	{
		if(value != myOnlyWithInstances)
		{
			myOnlyWithInstances = value;
			fireTableDataChanged();
		}
	}

	void setFilteringByDiffNonZero(boolean value)
	{
		if(myOnlyWithDiff != value)
		{
			myOnlyWithDiff = value;
			fireTableDataChanged();
		}
	}

	void setFilteringByTrackingState(boolean value)
	{
		if(myOnlyTracked != value)
		{
			myOnlyTracked = value;
			fireTableDataChanged();
		}
	}

	public void updateClassesOnly(@NotNull List<ReferenceType> classes)
	{
		myIsShowCounts = false;
		final LinkedHashMap<ReferenceType, Long> class2Count = new LinkedHashMap<>();
		classes.forEach(x -> class2Count.put(x, 0L));
		updateCountsInternal(class2Count);
	}

	public void updateContent(@NotNull Map<ReferenceType, Long> class2Count)
	{
		myIsShowCounts = true;
		updateCountsInternal(class2Count);
	}

	void hideContent()
	{
		myModel.hide();
	}

	private void showContent()
	{
		myModel.show();
	}

	private void updateCountsInternal(@NotNull Map<ReferenceType, Long> class2Count)
	{
		final ReferenceType selectedClass = myModel.getSelectedClassBeforeHide();
		int newSelectedIndex = -1;
		final boolean isInitialized = !myItems.isEmpty();
		myItems = Collections.unmodifiableList(new ArrayList<>(class2Count.keySet()));

		int i = 0;
		for(final ReferenceType ref : class2Count.keySet())
		{
			if(ref.equals(selectedClass))
			{
				newSelectedIndex = i;
			}

			final DiffValue oldValue = isInitialized && !myCounts.containsKey(ref) ? new DiffValue(0, 0) : myCounts.getOrDefault(ref, UNKNOWN_VALUE);
			myCounts.put(ref, oldValue.update(class2Count.get(ref)));

			i++;
		}

		showContent();

		if(newSelectedIndex != -1 && !myModel.isHidden())
		{
			final int ix = convertRowIndexToView(newSelectedIndex);
			changeSelection(ix, DiffViewTableModel.CLASSNAME_COLUMN_INDEX, false, false);
		}

		fireTableDataChanged();
	}

	@Nullable
	@Override
	public Object getData(@NonNls Key dataId)
	{
		if(SELECTED_CLASS_KEY == dataId)
		{
			return getSelectedClass();
		}
		if(NEW_INSTANCES_PROVIDER_KEY == dataId)
		{
			ReferenceType selectedClass = getSelectedClass();
			if(selectedClass != null)
			{
				TrackerForNewInstances strategy = myParent.getStrategy(selectedClass);
				if(strategy != null && strategy.isReady())
				{
					List<ObjectReference> newInstances = strategy.getNewInstances();
					return (InstancesProvider) limit -> newInstances;
				}
			}
		}

		if(REF_COUNT_PROVIDER_KEY == dataId)
		{
			return myCountProvider;
		}

		return null;
	}

	public void clean()
	{
		clearSelection();
		myItems = Collections.emptyList();
		myCounts.clear();
		myModel.mySelectedClassWhenHidden = null;
		fireTableDataChanged();
	}

	@Override
	public void dispose()
	{
		ApplicationManager.getApplication().invokeLater(this::clean);
	}

	@Nullable
	private TrackingType getTrackingType(int row)
	{
		ReferenceType ref = (ReferenceType) getValueAt(row, convertColumnIndexToView(DiffViewTableModel.CLASSNAME_COLUMN_INDEX));
		return myInstancesTracker.getTrackingType(ref.name());
	}

	private void fireTableDataChanged()
	{
		myModel.fireTableDataChanged();
	}

	class DiffViewTableModel extends AbstractTableModelWithColumns
	{
		final static int CLASSNAME_COLUMN_INDEX = 0;
		final static int COUNT_COLUMN_INDEX = 1;
		final static int DIFF_COLUMN_INDEX = 2;

		// Workaround: save selection after content of classes table has been hided
		private ReferenceType mySelectedClassWhenHidden = null;
		private boolean myIsWithContent = false;

		DiffViewTableModel()
		{
			super(new AbstractTableColumnDescriptor[]{
					new AbstractTableColumnDescriptor("Class", ReferenceType.class)
					{
						@Override
						public Object getValue(int ix)
						{
							return myItems.get(ix);
						}
					},
					new AbstractTableColumnDescriptor("Count", Long.class)
					{
						@Override
						public Object getValue(int ix)
						{
							return myCounts.getOrDefault(myItems.get(ix), UNKNOWN_VALUE).myCurrentCount;
						}
					},
					new AbstractTableColumnDescriptor("Diff", DiffValue.class)
					{
						@Override
						public Object getValue(int ix)
						{
							return myCounts.getOrDefault(myItems.get(ix), UNKNOWN_VALUE);
						}
					}
			});
		}

		ReferenceType getSelectedClassBeforeHide()
		{
			return mySelectedClassWhenHidden;
		}

		void hide()
		{
			if(myIsWithContent)
			{
				mySelectedClassWhenHidden = getSelectedClass();
				myIsWithContent = false;
				clearSelection();
				fireTableDataChanged();
			}
		}

		void show()
		{
			if(!myIsWithContent)
			{
				myIsWithContent = true;
				fireTableDataChanged();
			}
		}

		boolean isHidden()
		{
			return !myIsWithContent;
		}

		@Override
		public int getRowCount()
		{
			return myIsWithContent ? myItems.size() : 0;
		}
	}

	/**
	 * State transmissions for DiffValue and UnknownDiffValue
	 * unknown -> diff
	 * diff -> diff
	 * <p>
	 * State descriptions:
	 * Unknown - instances count never executed
	 * Diff - actual value
	 */
	private static class UnknownDiffValue extends DiffValue
	{
		UnknownDiffValue()
		{
			super(-1);
		}

		@Override
		boolean hasInstance()
		{
			return true;
		}

		@Override
		DiffValue update(long count)
		{
			return new DiffValue(count);
		}
	}

	private static class DiffValue implements Comparable<DiffValue>
	{
		private long myOldCount;

		private long myCurrentCount;

		DiffValue(long count)
		{
			this(count, count);
		}

		DiffValue(long old, long current)
		{
			myCurrentCount = current;
			myOldCount = old;
		}

		DiffValue update(long count)
		{
			myOldCount = myCurrentCount;
			myCurrentCount = count;
			return this;
		}

		boolean hasInstance()
		{
			return myCurrentCount > 0;
		}

		long diff()
		{
			return myCurrentCount - myOldCount;
		}

		@Override
		public int compareTo(@NotNull DiffValue o)
		{
			return Long.compare(diff(), o.diff());
		}
	}

	private abstract static class MyTableCellRenderer extends ColoredTableCellRenderer
	{
		@Override
		protected void customizeCellRenderer(JTable table, @Nullable Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{

			if(hasFocus)
			{
				setBorder(EMPTY_BORDER);
			}

			if(value != null)
			{
				addText(value, isSelected, row);
			}
		}

		protected abstract void addText(@NotNull Object value, boolean isSelected, int row);
	}

	private class MyClassColumnRenderer extends MyTableCellRenderer
	{
		@Override
		protected void addText(@NotNull Object value, boolean isSelected, int row)
		{
			String presentation = ((ReferenceType) value).name();
			append(" ");
			if(isSelected)
			{
				FList<TextRange> textRanges = myMatcher.matchingFragments(presentation);
				if(textRanges != null)
				{
					SimpleTextAttributes attributes = new SimpleTextAttributes(getBackground(), getForeground(), null, SimpleTextAttributes.STYLE_SEARCH_MATCH);
					SpeedSearchUtil.appendColoredFragments(this, presentation, textRanges, SimpleTextAttributes.REGULAR_ATTRIBUTES, attributes);
				}
			}
			else
			{
				append(String.format("%s", presentation), SimpleTextAttributes.REGULAR_ATTRIBUTES);
			}
		}
	}

	private abstract class MyNumericRenderer extends MyTableCellRenderer
	{
		@Override
		protected void addText(@NotNull Object value, boolean isSelected, int row)
		{
			if(myIsShowCounts)
			{
				setTextAlign(SwingConstants.RIGHT);
				appendText(value, row);
			}
		}

		abstract void appendText(@NotNull Object value, int row);
	}

	private class MyCountColumnRenderer extends MyNumericRenderer
	{
		@Override
		void appendText(@NotNull Object value, int row)
		{
			append(value.toString());
		}
	}

	private class MyDiffColumnRenderer extends MyNumericRenderer
	{
		private final SimpleTextAttributes myClickableCellAttributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_UNDERLINE, JBColor.BLUE);

		@Override
		void appendText(@NotNull Object value, int row)
		{
			TrackingType trackingType = getTrackingType(row);
			if(trackingType != null)
			{
				setIcon(AllIcons.Debugger.MemoryView.ClassTracked);
				setTransparentIconBackground(true);
			}

			ReferenceType ref = myItems.get(convertRowIndexToModel(row));

			long diff = myCountProvider.getDiffCount(ref);
			String text = String.format("%s%d", diff > 0 ? "+" : "", diff);

			int newInstancesCount = myCountProvider.getNewInstancesCount(ref);
			if(newInstancesCount >= 0)
			{
				if(newInstancesCount == diff)
				{
					append(text, diff == 0 ? SimpleTextAttributes.REGULAR_ATTRIBUTES : myClickableCellAttributes);
				}
				else
				{
					append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
					if(newInstancesCount != 0)
					{
						append(String.format(" (%d)", newInstancesCount), myClickableCellAttributes);
					}
				}
			}
			else
			{
				append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
			}
		}
	}
}