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
package com.intellij.psi.impl.java.stubs;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;

import com.intellij.lang.ASTNode;
import com.intellij.lang.LighterAST;
import com.intellij.lang.LighterASTNode;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.java.language.psi.PsiPackageAccessibilityStatement;
import com.intellij.psi.impl.java.stubs.impl.PsiPackageAccessibilityStatementStubImpl;
import com.intellij.psi.impl.source.PackageAccessibilityStatementElement;
import com.intellij.psi.impl.source.PsiPackageAccessibilityStatementImpl;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.impl.source.tree.JavaSourceUtil;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.StringRef;

public class JavaPackageAccessibilityStatementElementType extends JavaStubElementType<PsiPackageAccessibilityStatementStub, PsiPackageAccessibilityStatement>
{
	public JavaPackageAccessibilityStatementElementType(@Nonnull String debugName)
	{
		super(debugName);
	}

	@Override
	public PsiPackageAccessibilityStatement createPsi(@Nonnull PsiPackageAccessibilityStatementStub stub)
	{
		return getPsiFactory(stub).createPackageAccessibilityStatement(stub);
	}

	@Override
	public PsiPackageAccessibilityStatement createPsi(@Nonnull ASTNode node)
	{
		return new PsiPackageAccessibilityStatementImpl(node);
	}

	@Nonnull
	@Override
	public ASTNode createCompositeNode()
	{
		return new PackageAccessibilityStatementElement(this);
	}

	@Override
	public PsiPackageAccessibilityStatementStub createStub(LighterAST tree, LighterASTNode node, StubElement parentStub)
	{
		String refText = null;
		List<String> to = ContainerUtil.newSmartList();

		for(LighterASTNode child : tree.getChildren(node))
		{
			IElementType type = child.getTokenType();
			if(type == JavaElementType.JAVA_CODE_REFERENCE)
			{
				refText = JavaSourceUtil.getReferenceText(tree, child);
			}
			else if(type == JavaElementType.MODULE_REFERENCE)
			{
				to.add(JavaSourceUtil.getReferenceText(tree, child));
			}
		}

		return new PsiPackageAccessibilityStatementStubImpl(parentStub, this, refText, to);
	}

	@Override
	public void serialize(@Nonnull PsiPackageAccessibilityStatementStub stub, @Nonnull StubOutputStream dataStream) throws IOException
	{
		dataStream.writeName(stub.getPackageName());
		dataStream.writeUTFFast(StringUtil.join(stub.getTargets(), "/"));
	}

	@Nonnull
	@Override
	public PsiPackageAccessibilityStatementStub deserialize(@Nonnull StubInputStream dataStream, StubElement parentStub) throws IOException
	{
		String packageName = StringRef.toString(dataStream.readName());
		List<String> targets = StringUtil.split(dataStream.readUTFFast(), "/");
		return new PsiPackageAccessibilityStatementStubImpl(parentStub, this, packageName, targets);
	}

	@Override
	public void indexStub(@Nonnull PsiPackageAccessibilityStatementStub stub, @Nonnull IndexSink sink)
	{
	}

	@Nonnull
	public static PsiPackageAccessibilityStatement.Role typeToRole(@Nonnull IElementType type)
	{
		if(type == JavaElementType.EXPORTS_STATEMENT)
		{
			return PsiPackageAccessibilityStatement.Role.EXPORTS;
		}
		if(type == JavaElementType.OPENS_STATEMENT)
		{
			return PsiPackageAccessibilityStatement.Role.OPENS;
		}
		throw new IllegalArgumentException("Unknown type: " + type);
	}
}