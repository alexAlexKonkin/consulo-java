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
package com.intellij.psi.impl.source;

import static com.intellij.psi.impl.source.tree.JavaElementType.ANONYMOUS_CLASS;
import static com.intellij.psi.impl.source.tree.JavaElementType.EXPRESSION_LIST;

import java.util.List;

import org.jetbrains.annotations.Contract;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.intellij.lang.LighterAST;
import com.intellij.lang.LighterASTNode;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.impl.cache.RecordUtil;
import com.intellij.psi.impl.source.tree.ElementType;
import com.intellij.psi.impl.source.tree.LightTreeUtil;

/**
 * @author peter
 */
public class JavaLightTreeUtil
{
	@Nullable
	@Contract("_,null->null")
	public static List<LighterASTNode> getArgList(@Nonnull LighterAST tree, @javax.annotation.Nullable LighterASTNode call)
	{
		LighterASTNode anonClass = LightTreeUtil.firstChildOfType(tree, call, ANONYMOUS_CLASS);
		LighterASTNode exprList = LightTreeUtil.firstChildOfType(tree, anonClass != null ? anonClass : call, EXPRESSION_LIST);
		return exprList == null ? null : getExpressionChildren(tree, exprList);
	}

	@javax.annotation.Nullable
	@Contract("_,null->null")
	public static String getNameIdentifierText(@Nonnull LighterAST tree, @javax.annotation.Nullable LighterASTNode idOwner)
	{
		LighterASTNode id = LightTreeUtil.firstChildOfType(tree, idOwner, JavaTokenType.IDENTIFIER);
		return id != null ? RecordUtil.intern(tree.getCharTable(), id) : null;
	}

	@Nonnull
	public static List<LighterASTNode> getExpressionChildren(@Nonnull LighterAST tree, @Nonnull LighterASTNode node)
	{
		return LightTreeUtil.getChildrenOfType(tree, node, ElementType.EXPRESSION_BIT_SET);
	}

	@javax.annotation.Nullable
	public static LighterASTNode findExpressionChild(@Nonnull LighterAST tree, @Nullable LighterASTNode node)
	{
		return LightTreeUtil.firstChildOfType(tree, node, ElementType.EXPRESSION_BIT_SET);
	}
}
