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
package com.intellij.java.debugger.impl.engine.evaluation;

import javax.annotation.Nonnull;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoFilter;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.psi.PsiFile;

public class DebuggerHighlightFilter implements HighlightInfoFilter
{
	@Override
	public boolean accept(@Nonnull HighlightInfo highlightInfo, PsiFile file)
	{
		return highlightInfo.type != HighlightInfoType.UNHANDLED_EXCEPTION ||
				file == null ||
				!DefaultCodeFragmentFactory.isDebuggerFile(file);
	}
}