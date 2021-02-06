// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.java.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.lang.LighterAST;
import com.intellij.lang.LighterASTNode;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiRecordComponent;
import com.intellij.psi.impl.cache.RecordUtil;
import com.intellij.psi.impl.cache.TypeInfo;
import com.intellij.psi.impl.java.stubs.impl.PsiRecordComponentStubImpl;
import com.intellij.psi.impl.source.PsiRecordComponentImpl;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.impl.source.tree.LightTreeUtil;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import javax.annotation.Nonnull;

import java.io.IOException;

public class JavaRecordComponentElementType extends JavaStubElementType<PsiRecordComponentStub, PsiRecordComponent>
{
	public JavaRecordComponentElementType()
	{
		super("RECORD_COMPONENT");
	}

	@Nonnull
	@Override
	public ASTNode createCompositeNode()
	{
		return new CompositeElement(this);
	}

	@Override
	public void serialize(@Nonnull PsiRecordComponentStub stub, @Nonnull StubOutputStream dataStream) throws IOException
	{
		dataStream.writeName(stub.getName());
		TypeInfo.writeTYPE(dataStream, stub.getType());
		dataStream.writeByte(((PsiRecordComponentStubImpl) stub).getFlags());
	}

	@Nonnull
	@Override
	public PsiRecordComponentStub deserialize(@Nonnull StubInputStream dataStream, StubElement parentStub) throws IOException
	{
		String name = dataStream.readNameString();
		TypeInfo type = TypeInfo.readTYPE(dataStream);
		byte flags = dataStream.readByte();
		return new PsiRecordComponentStubImpl(parentStub, name, type, flags);
	}

	@Override
	public void indexStub(@Nonnull PsiRecordComponentStub stub, @Nonnull IndexSink sink)
	{

	}

	@Override
	public PsiRecordComponent createPsi(@Nonnull PsiRecordComponentStub stub)
	{
		return getPsiFactory(stub).createRecordComponent(stub);
	}


	@Override
	public PsiRecordComponent createPsi(@Nonnull ASTNode node)
	{
		return new PsiRecordComponentImpl(node);
	}

	@Nonnull
	@Override
	public PsiRecordComponentStub createStub(@Nonnull LighterAST tree, @Nonnull LighterASTNode node, @Nonnull StubElement parentStub)
	{
		TypeInfo typeInfo = TypeInfo.create(tree, node, parentStub);
		LighterASTNode id = LightTreeUtil.requiredChildOfType(tree, node, JavaTokenType.IDENTIFIER);
		String name = RecordUtil.intern(tree.getCharTable(), id);

		LighterASTNode modifierList = LightTreeUtil.firstChildOfType(tree, node, JavaElementType.MODIFIER_LIST);
		boolean hasDeprecatedAnnotation = modifierList != null && RecordUtil.isDeprecatedByAnnotation(tree, modifierList);
		return new PsiRecordComponentStubImpl(parentStub, name, typeInfo, typeInfo.isEllipsis, hasDeprecatedAnnotation);
	}
}