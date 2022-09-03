/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import static com.intellij.java.impl.psi.formatter.java.JavaFormatterUtil.getWrapType;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import consulo.language.codeStyle.Alignment;
import consulo.language.codeStyle.Block;
import consulo.language.codeStyle.FormattingMode;
import consulo.language.codeStyle.Indent;
import consulo.language.codeStyle.Wrap;
import consulo.language.ast.ASTNode;
import com.intellij.java.language.psi.JavaTokenType;
import consulo.language.psi.PsiComment;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import com.intellij.java.impl.psi.codeStyle.JavaCodeStyleSettings;
import com.intellij.java.language.impl.psi.impl.source.tree.JavaElementType;

class ChainMethodCallsBlockBuilder
{
	private final CommonCodeStyleSettings mySettings;
	private final CommonCodeStyleSettings.IndentOptions myIndentSettings;
	private final JavaCodeStyleSettings myJavaSettings;

	private final Wrap myBlockWrap;
	private final Alignment myBlockAlignment;
	private final Indent myBlockIndent;

	private final FormattingMode myFormattingMode;

	public ChainMethodCallsBlockBuilder(Alignment alignment,
										Wrap wrap,
										Indent indent,
										CommonCodeStyleSettings settings,
										JavaCodeStyleSettings javaSettings,
										@Nonnull FormattingMode formattingMode)
	{
		myBlockWrap = wrap;
		myBlockAlignment = alignment;
		myBlockIndent = indent;
		mySettings = settings;
		myIndentSettings = settings.getIndentOptions();
		myJavaSettings = javaSettings;
		myFormattingMode = formattingMode;
	}

	public Block build(List<ASTNode> nodes)
	{
		List<Block> blocks = buildBlocksFrom(nodes);

		Indent indent = myBlockIndent != null ? myBlockIndent : Indent.getContinuationWithoutFirstIndent(myIndentSettings.USE_RELATIVE_INDENTS);
		return new SyntheticCodeBlock(blocks, myBlockAlignment, mySettings, myJavaSettings, indent, myBlockWrap);
	}

	private List<Block> buildBlocksFrom(List<ASTNode> nodes)
	{
		List<ChainedCallChunk> methodCall = splitMethodCallOnChunksByDots(nodes);

		Wrap wrap = null;
		Alignment chainedCallsAlignment = null;

		List<Block> blocks = new ArrayList<>();

		for(int i = 0; i < methodCall.size(); i++)
		{
			ChainedCallChunk currentCallChunk = methodCall.get(i);
			if(isMethodCall(currentCallChunk) || isComment(currentCallChunk))
			{
				if(wrap == null)
				{
					wrap = createCallChunkWrap(i, methodCall);
				}
				if(chainedCallsAlignment == null)
				{
					chainedCallsAlignment = createCallChunkAlignment(i, methodCall);
				}
			}
			else
			{
				wrap = null;
				chainedCallsAlignment = null;
			}

			CallChunkBlockBuilder builder = new CallChunkBlockBuilder(mySettings, myJavaSettings, myFormattingMode);
			blocks.add(builder.create(currentCallChunk.nodes, wrap, chainedCallsAlignment));
		}

		return blocks;
	}

	private static boolean isComment(ChainedCallChunk chunk)
	{
		List<ASTNode> nodes = chunk.nodes;
		if(nodes.size() == 1)
		{
			return nodes.get(0).getPsi() instanceof PsiComment;
		}
		return false;
	}

	private Wrap createCallChunkWrap(int chunkIndex, @Nonnull List<ChainedCallChunk> methodCall)
	{
		if(mySettings.WRAP_FIRST_METHOD_IN_CALL_CHAIN)
		{
			ChainedCallChunk next = chunkIndex + 1 < methodCall.size() ? methodCall.get(chunkIndex + 1) : null;
			if(next != null && isMethodCall(next))
			{
				return Wrap.createWrap(getWrapType(mySettings.METHOD_CALL_CHAIN_WRAP), true);
			}
		}

		return Wrap.createWrap(getWrapType(mySettings.METHOD_CALL_CHAIN_WRAP), false);
	}

	private boolean shouldAlignMethod(ChainedCallChunk currentMethodChunk, List<ChainedCallChunk> methodCall)
	{
		return mySettings.ALIGN_MULTILINE_CHAINED_METHODS
				&& !currentMethodChunk.isEmpty()
				&& !chunkIsFirstInChainMethodCall(currentMethodChunk, methodCall);
	}

	private static boolean chunkIsFirstInChainMethodCall(@Nonnull ChainedCallChunk callChunk, @Nonnull List<ChainedCallChunk> methodCall)
	{
		return !methodCall.isEmpty() && callChunk == methodCall.get(0);
	}

	@Nonnull
	private static List<ChainedCallChunk> splitMethodCallOnChunksByDots(@Nonnull List<ASTNode> nodes)
	{
		List<ChainedCallChunk> result = new ArrayList<>();

		List<ASTNode> current = new ArrayList<>();
		for(ASTNode node : nodes)
		{
			if(node.getElementType() == JavaTokenType.DOT || node.getPsi() instanceof PsiComment)
			{
				if(!current.isEmpty())
				{
					result.add(new ChainedCallChunk(current));
				}
				current = new ArrayList<>();
			}
			current.add(node);
		}

		if(!current.isEmpty())
		{
			result.add(new ChainedCallChunk(current));
		}

		return result;
	}

	private Alignment createCallChunkAlignment(int chunkIndex, @Nonnull List<ChainedCallChunk> methodCall)
	{
		ChainedCallChunk current = methodCall.get(chunkIndex);
		return shouldAlignMethod(current, methodCall)
				? AbstractJavaBlock.createAlignment(mySettings.ALIGN_MULTILINE_CHAINED_METHODS, null)
				: null;
	}

	private static boolean isMethodCall(@Nonnull ChainedCallChunk callChunk)
	{
		List<ASTNode> nodes = callChunk.nodes;
		return nodes.size() >= 3 && nodes.get(2).getElementType() == JavaElementType.EXPRESSION_LIST;
	}
}

class ChainedCallChunk
{
	@Nonnull
	final List<ASTNode> nodes;

	ChainedCallChunk(@Nonnull List<ASTNode> nodes)
	{
		this.nodes = nodes;
	}

	boolean isEmpty()
	{
		return nodes.isEmpty();
	}
}
