/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.java.impl.psi.formatter.java;

import static com.intellij.java.impl.psi.formatter.java.AbstractJavaBlock.newJavaBlock;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.language.codeStyle.Alignment;
import consulo.language.codeStyle.Block;
import consulo.language.codeStyle.FormattingMode;
import consulo.language.codeStyle.Indent;
import consulo.language.codeStyle.Wrap;
import consulo.language.codeStyle.AlignmentStrategy;
import consulo.language.ast.ASTNode;
import com.intellij.java.language.psi.JavaTokenType;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import com.intellij.java.impl.psi.codeStyle.JavaCodeStyleSettings;

public class CallChunkBlockBuilder
{

	private final CommonCodeStyleSettings mySettings;
	private final CommonCodeStyleSettings.IndentOptions myIndentSettings;
	private final JavaCodeStyleSettings myJavaSettings;
	private final FormattingMode myFormattingMode;

	public CallChunkBlockBuilder(@Nonnull CommonCodeStyleSettings settings, @Nonnull JavaCodeStyleSettings javaSettings,
								 @Nonnull FormattingMode formattingMode)
	{
		mySettings = settings;
		myIndentSettings = settings.getIndentOptions();
		myJavaSettings = javaSettings;
		myFormattingMode = formattingMode;
	}

	@Nonnull
	public Block create(@Nonnull final List<ASTNode> subNodes, final Wrap wrap, @Nullable final Alignment alignment)
	{
		final ArrayList<Block> subBlocks = new ArrayList<>();
		final ASTNode firstNode = subNodes.get(0);
		if(firstNode.getElementType() == JavaTokenType.DOT)
		{
			AlignmentStrategy strategy = AlignmentStrategy.getNullStrategy();
			Block block = newJavaBlock(firstNode, mySettings, myJavaSettings, Indent.getNoneIndent(), null, strategy, myFormattingMode);
			subBlocks.add(block);
			subNodes.remove(0);
			if(!subNodes.isEmpty())
			{
				subBlocks.add(create(subNodes, wrap, null));
			}
			return new SyntheticCodeBlock(subBlocks, alignment, mySettings, myJavaSettings, Indent.getContinuationIndent(myIndentSettings.USE_RELATIVE_INDENTS), wrap);
		}
		return new SyntheticCodeBlock(createJavaBlocks(subNodes), alignment, mySettings, myJavaSettings, Indent.getContinuationWithoutFirstIndent(myIndentSettings.USE_RELATIVE_INDENTS), null);
	}

	@Nonnull
	private List<Block> createJavaBlocks(@Nonnull final List<ASTNode> subNodes)
	{
		final ArrayList<Block> result = new ArrayList<>();
		for(ASTNode node : subNodes)
		{
			Indent indent = Indent.getContinuationWithoutFirstIndent(myIndentSettings.USE_RELATIVE_INDENTS);
			result.add(newJavaBlock(node, mySettings, myJavaSettings, indent, null, AlignmentStrategy.getNullStrategy(), myFormattingMode));
		}
		return result;
	}

}