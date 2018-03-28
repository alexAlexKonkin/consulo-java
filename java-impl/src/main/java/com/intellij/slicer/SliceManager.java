/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.slicer;

import java.awt.Font;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.intellij.analysis.AnalysisUIOptions;
import com.intellij.ide.impl.ContentManagerWatcher;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.ElementDescriptionUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.refactoring.util.RefactoringDescriptionLocation;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.ui.UIUtil;

@State(name = "SliceManager", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
public class SliceManager implements PersistentStateComponent<SliceManager.StoredSettingsBean>
{
	private final Project myProject;
	private ContentManager myBackContentManager;
	private ContentManager myForthContentManager;
	private final StoredSettingsBean myStoredSettings = new StoredSettingsBean();
	private static final String BACK_TOOLWINDOW_ID = "Analyze Dataflow to";
	private static final String FORTH_TOOLWINDOW_ID = "Analyze Dataflow from";

	public static class StoredSettingsBean
	{
		public boolean showDereferences = true; // to show in dataflow/from dialog
		public AnalysisUIOptions analysisUIOptions = new AnalysisUIOptions();
	}

	public static SliceManager getInstance(@Nonnull Project project)
	{
		return ServiceManager.getService(project, SliceManager.class);
	}

	public SliceManager(@Nonnull Project project, PsiManager psiManager)
	{
		myProject = project;
	}

	@Nonnull
	private Disposable addPsiListener(@Nonnull final ProgressIndicator indicator)
	{
		Disposable disposable = Disposer.newDisposable();
		PsiManager.getInstance(myProject).addPsiTreeChangeListener(new PsiTreeChangeAdapter()
		{
			@Override
			public void beforeChildAddition(@Nonnull PsiTreeChangeEvent event)
			{
				indicator.cancel();
			}

			@Override
			public void beforeChildRemoval(@Nonnull PsiTreeChangeEvent event)
			{
				indicator.cancel();
			}

			@Override
			public void beforeChildReplacement(@Nonnull PsiTreeChangeEvent event)
			{
				indicator.cancel();
			}

			@Override
			public void beforeChildMovement(@Nonnull PsiTreeChangeEvent event)
			{
				indicator.cancel();
			}

			@Override
			public void beforeChildrenChange(@Nonnull PsiTreeChangeEvent event)
			{
				indicator.cancel();
			}

			@Override
			public void beforePropertyChange(@Nonnull PsiTreeChangeEvent event)
			{
				indicator.cancel();
			}
		}, disposable);
		return disposable;
	}

	private ContentManager getContentManager(boolean dataFlowToThis)
	{
		if(dataFlowToThis)
		{
			if(myBackContentManager == null)
			{
				ToolWindow backToolWindow = ToolWindowManager.getInstance(myProject).registerToolWindow(BACK_TOOLWINDOW_ID, true, ToolWindowAnchor.BOTTOM, myProject);
				myBackContentManager = backToolWindow.getContentManager();
				new ContentManagerWatcher(backToolWindow, myBackContentManager);
			}
			return myBackContentManager;
		}

		if(myForthContentManager == null)
		{
			ToolWindow forthToolWindow = ToolWindowManager.getInstance(myProject).registerToolWindow(FORTH_TOOLWINDOW_ID, true, ToolWindowAnchor.BOTTOM, myProject);
			myForthContentManager = forthToolWindow.getContentManager();
			new ContentManagerWatcher(forthToolWindow, myForthContentManager);
		}
		return myForthContentManager;
	}

	public void slice(@Nonnull PsiElement element, boolean dataFlowToThis, @Nonnull SliceHandler handler)
	{
		String dialogTitle = getElementDescription((dataFlowToThis ? BACK_TOOLWINDOW_ID : FORTH_TOOLWINDOW_ID) + " ", element, null);

		dialogTitle = Pattern.compile("(<style>.*</style>)|<[^<>]*>").matcher(dialogTitle).replaceAll("");
		SliceAnalysisParams params = handler.askForParams(element, dataFlowToThis, myStoredSettings, dialogTitle);
		if(params == null)
		{
			return;
		}

		SliceRootNode rootNode = new SliceRootNode(myProject, new DuplicateMap(), SliceUsage.createRootUsage(element, params));

		createToolWindow(dataFlowToThis, rootNode, false, getElementDescription(null, element, null));
	}

	public void createToolWindow(boolean dataFlowToThis, @Nonnull SliceRootNode rootNode, boolean splitByLeafExpressions, @Nonnull String displayName)
	{
		final SliceToolwindowSettings sliceToolwindowSettings = SliceToolwindowSettings.getInstance(myProject);
		final ContentManager contentManager = getContentManager(dataFlowToThis);
		final Content[] myContent = new Content[1];
		ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(dataFlowToThis ? BACK_TOOLWINDOW_ID : FORTH_TOOLWINDOW_ID);
		final SlicePanel slicePanel = new SlicePanel(myProject, dataFlowToThis, rootNode, splitByLeafExpressions, toolWindow)
		{
			@Override
			protected void close()
			{
				super.close();
				contentManager.removeContent(myContent[0], true);
			}

			@Override
			public boolean isAutoScroll()
			{
				return sliceToolwindowSettings.isAutoScroll();
			}

			@Override
			public void setAutoScroll(boolean autoScroll)
			{
				sliceToolwindowSettings.setAutoScroll(autoScroll);
			}

			@Override
			public boolean isPreview()
			{
				return sliceToolwindowSettings.isPreview();
			}

			@Override
			public void setPreview(boolean preview)
			{
				sliceToolwindowSettings.setPreview(preview);
			}
		};

		myContent[0] = contentManager.getFactory().createContent(slicePanel, displayName, true);
		contentManager.addContent(myContent[0]);
		contentManager.setSelectedContent(myContent[0]);

		toolWindow.activate(null);
	}

	public static String getElementDescription(String prefix, PsiElement element, String suffix)
	{
		PsiElement elementToSlice = element;
		if(element instanceof PsiReferenceExpression)
		{
			elementToSlice = ((PsiReferenceExpression) element).resolve();
		}
		if(elementToSlice == null)
		{
			elementToSlice = element;
		}
		String desc = ElementDescriptionUtil.getElementDescription(elementToSlice, RefactoringDescriptionLocation.WITHOUT_PARENT);
		return "<html><head>" + UIUtil.getCssFontDeclaration(getLabelFont()) + "</head><body>" + (prefix == null ? "" : prefix) + StringUtil.first(desc, 100, true) + (suffix == null ? "" : suffix) +
				"</body></html>";
	}

	// copy from com.intellij.openapi.wm.impl.content.BaseLabel since desktop libraries is not exported
	public static Font getLabelFont()
	{
		Font f = UIUtil.getLabelFont();
		return f.deriveFont(f.getStyle(), Math.max(11, f.getSize() - 2));
	}

	public void runInterruptibly(@Nonnull ProgressIndicator progress, @Nonnull Runnable onCancel, @Nonnull Runnable runnable) throws ProcessCanceledException
	{
		Disposable disposable = addPsiListener(progress);
		try
		{
			progress.checkCanceled();
			ProgressManager.getInstance().executeProcessUnderProgress(runnable, progress);
		}
		catch(ProcessCanceledException e)
		{
			progress.cancel();
			//reschedule for later
			onCancel.run();
			throw e;
		}
		finally
		{
			Disposer.dispose(disposable);
		}
	}

	@Override
	public StoredSettingsBean getState()
	{
		return myStoredSettings;
	}

	@Override
	public void loadState(StoredSettingsBean state)
	{
		myStoredSettings.analysisUIOptions.save(state.analysisUIOptions);
	}
}
