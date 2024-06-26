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

/**
 * created at Jan 10, 2002
 *
 * @author Jeka
 */
package com.intellij.java.compiler.impl.classParsing;

import com.intellij.java.compiler.impl.cache.JavaCacheUtils;
import com.intellij.java.compiler.impl.cache.SymbolTable;
import com.intellij.java.compiler.impl.util.cls.ClsUtil;
import consulo.compiler.CacheCorruptedException;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class MethodInfo extends MemberInfo
{
	private static final Logger LOG = Logger.getInstance(MethodInfo.class);

	private static final int[] EXCEPTION_INFO_UNAVAILABLE = ArrayUtil.EMPTY_INT_ARRAY;
	public static final MethodInfo[] EMPTY_ARRAY = new MethodInfo[0];

	private final int[] myThrownExceptions;
	// cached (lazy initialized) data
	private String mySignature = null;
	private String[] myParameterDescriptors = null;
	private String myReturnTypeSignature = null;
	private final boolean myIsConstructor;
	private final AnnotationConstantValue[][] myRuntimeVisibleParameterAnnotations;
	private final AnnotationConstantValue[][] myRuntimeInvisibleParameterAnnotations;
	@Nonnull
	private final ConstantValue myAnnotationDefault;

	public MethodInfo(int name, int descriptor, boolean isConstructor)
	{
		super(name, descriptor);
		myIsConstructor = isConstructor;
		myThrownExceptions = EXCEPTION_INFO_UNAVAILABLE;
		myRuntimeVisibleParameterAnnotations = AnnotationConstantValue.EMPTY_ARRAY_ARRAY;
		myRuntimeInvisibleParameterAnnotations = AnnotationConstantValue.EMPTY_ARRAY_ARRAY;
		myAnnotationDefault = ConstantValue.EMPTY_CONSTANT_VALUE;
	}

	public MethodInfo(int name,
					  int descriptor,
					  final int genericSignature,
					  int flags,
					  int[] exceptions,
					  boolean isConstructor,
					  final AnnotationConstantValue[] runtimeVisibleAnnotations,
					  final AnnotationConstantValue[] runtimeInvisibleAnnotations,
					  final AnnotationConstantValue[][] runtimeVisibleParameterAnnotations,
					  final AnnotationConstantValue[][] runtimeInvisibleParameterAnnotations, ConstantValue annotationDefault)
	{

		super(name, descriptor, genericSignature, flags, runtimeVisibleAnnotations, runtimeInvisibleAnnotations);
		myThrownExceptions = exceptions != null ? exceptions : ArrayUtil.EMPTY_INT_ARRAY;
		myIsConstructor = isConstructor;
		myRuntimeVisibleParameterAnnotations = runtimeVisibleParameterAnnotations == null ? AnnotationConstantValue.EMPTY_ARRAY_ARRAY : runtimeVisibleParameterAnnotations;
		myRuntimeInvisibleParameterAnnotations = runtimeInvisibleParameterAnnotations == null ? AnnotationConstantValue.EMPTY_ARRAY_ARRAY : runtimeInvisibleParameterAnnotations;
		myAnnotationDefault = ObjectUtil.notNull(annotationDefault, ConstantValue.EMPTY_CONSTANT_VALUE);
	}

	public MethodInfo(DataInput in) throws IOException
	{
		super(in);
		myIsConstructor = in.readBoolean();
		int count = in.readInt();
		if(count == -1)
		{
			myThrownExceptions = EXCEPTION_INFO_UNAVAILABLE;
		}
		else
		{
			myThrownExceptions = ArrayUtil.newIntArray(count);
			for(int idx = 0; idx < count; idx++)
			{
				myThrownExceptions[idx] = in.readInt();
			}
		}
		myRuntimeVisibleParameterAnnotations = loadParameterAnnotations(in);
		myRuntimeInvisibleParameterAnnotations = loadParameterAnnotations(in);
		myAnnotationDefault = MemberInfoExternalizer.loadConstantValue(in);
	}

	@Override
	public void save(DataOutput out) throws IOException
	{
		super.save(out);
		out.writeBoolean(myIsConstructor);
		if(isExceptionInfoAvailable())
		{
			out.writeInt(myThrownExceptions.length);
		}
		else
		{
			out.writeInt(-1);
		}
		for(int thrownException : myThrownExceptions)
		{
			out.writeInt(thrownException);
		}
		saveParameterAnnotations(out, myRuntimeVisibleParameterAnnotations);
		saveParameterAnnotations(out, myRuntimeInvisibleParameterAnnotations);
		MemberInfoExternalizer.saveConstantValue(out, myAnnotationDefault);
	}

	private boolean isExceptionInfoAvailable()
	{
		return myThrownExceptions != EXCEPTION_INFO_UNAVAILABLE;
	}

	public boolean areExceptionsEqual(MethodInfo info)
	{
		if(myThrownExceptions.length != info.myThrownExceptions.length)
		{
			return false;
		}
		if(myThrownExceptions.length != 0)
		{ // optimization
			IntSet exceptionsSet = IntSets.newHashSet();
			for(int thrownException : myThrownExceptions)
			{
				exceptionsSet.add(thrownException);
			}
			for(int exception : info.myThrownExceptions)
			{
				if(!exceptionsSet.contains(exception))
				{
					return false;
				}
			}
		}
		return true;
	}

	public int[] getThrownExceptions()
	{
		return myThrownExceptions;
	}

	public String getDescriptor(SymbolTable symbolTable) throws CacheCorruptedException
	{
		if(mySignature == null)
		{
			final String descriptor = symbolTable.getSymbol(getDescriptor());
			final String name = symbolTable.getSymbol(getName());
			mySignature = JavaCacheUtils.getMethodSignature(name, descriptor);
		}
		return mySignature;
	}

	public String getReturnTypeDescriptor(SymbolTable symbolTable) throws CacheCorruptedException
	{
		if(myReturnTypeSignature == null)
		{
			String descriptor = symbolTable.getSymbol(getDescriptor());
			myReturnTypeSignature = descriptor.substring(descriptor.indexOf(')') + 1, descriptor.length());
		}
		return myReturnTypeSignature;
	}

	public String[] getParameterDescriptors(SymbolTable symbolTable) throws CacheCorruptedException
	{
		if(myParameterDescriptors == null)
		{
			String descriptor = symbolTable.getSymbol(getDescriptor());
			int endIndex = descriptor.indexOf(')');
			if(endIndex <= 0)
			{
				LOG.error("Corrupted method descriptor: " + descriptor);
			}
			myParameterDescriptors = parseParameterDescriptors(descriptor.substring(1, endIndex));
		}
		return myParameterDescriptors;
	}

	public boolean isAbstract()
	{
		return ClsUtil.isAbstract(getFlags());
	}

	public boolean isConstructor()
	{
		return myIsConstructor;
	}

	private String[] parseParameterDescriptors(String signature)
	{
		ArrayList<String> list = new ArrayList<String>();
		String paramSignature = parseFieldType(signature);
		while(paramSignature != null && !"".equals(paramSignature))
		{
			list.add(paramSignature);
			signature = signature.substring(paramSignature.length());
			paramSignature = parseFieldType(signature);
		}
		return ArrayUtil.toStringArray(list);
	}

	private
	@NonNls
	String parseFieldType(@NonNls String signature)
	{
		if(signature.length() == 0)
		{
			return null;
		}
		if(signature.charAt(0) == 'B')
		{
			return "B";
		}
		if(signature.charAt(0) == 'C')
		{
			return "C";
		}
		if(signature.charAt(0) == 'D')
		{
			return "D";
		}
		if(signature.charAt(0) == 'F')
		{
			return "F";
		}
		if(signature.charAt(0) == 'I')
		{
			return "I";
		}
		if(signature.charAt(0) == 'J')
		{
			return "J";
		}
		if(signature.charAt(0) == 'S')
		{
			return "S";
		}
		if(signature.charAt(0) == 'Z')
		{
			return "Z";
		}
		if(signature.charAt(0) == 'L')
		{
			return signature.substring(0, signature.indexOf(";") + 1);
		}
		if(signature.charAt(0) == '[')
		{
			String s = parseFieldType(signature.substring(1));
			return (s != null) ? ("[" + s) : null;
		}
		return null;
	}

	public AnnotationConstantValue[][] getRuntimeVisibleParameterAnnotations()
	{
		return myRuntimeVisibleParameterAnnotations;
	}

	public AnnotationConstantValue[][] getRuntimeInvisibleParameterAnnotations()
	{
		return myRuntimeInvisibleParameterAnnotations;
	}

	public String toString()
	{
		return mySignature;
	}

	private AnnotationConstantValue[][] loadParameterAnnotations(DataInput in) throws IOException
	{
		final int size = in.readInt();
		if(size == 0)
		{
			return AnnotationConstantValue.EMPTY_ARRAY_ARRAY;
		}
		final AnnotationConstantValue[][] paramAnnotations = new AnnotationConstantValue[size][];
		for(int idx = 0; idx < size; idx++)
		{
			paramAnnotations[idx] = loadAnnotations(in);
		}
		return paramAnnotations;
	}

	private void saveParameterAnnotations(DataOutput out, AnnotationConstantValue[][] parameterAnnotations) throws IOException
	{
		out.writeInt(parameterAnnotations.length);
		for(AnnotationConstantValue[] parameterAnnotation : parameterAnnotations)
		{
			saveAnnotations(out, parameterAnnotation);
		}
	}

	@Nonnull
	public ConstantValue getAnnotationDefault()
	{
		return myAnnotationDefault;
	}

	@Override
	public boolean equals(Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(o == null || getClass() != o.getClass())
		{
			return false;
		}
		if(!super.equals(o))
		{
			return false;
		}
		MethodInfo that = (MethodInfo) o;
		boolean rv = Arrays.equals(myRuntimeVisibleParameterAnnotations, that.myRuntimeVisibleParameterAnnotations);
		boolean ri = Arrays.equals(myRuntimeInvisibleParameterAnnotations, that.myRuntimeInvisibleParameterAnnotations);
		return myIsConstructor == that.myIsConstructor &&
				Arrays.equals(myThrownExceptions, that.myThrownExceptions) &&
				Objects.equals(mySignature, that.mySignature) &&
				Arrays.equals(myParameterDescriptors, that.myParameterDescriptors) &&
				Objects.equals(myReturnTypeSignature, that.myReturnTypeSignature) &&
				Arrays.equals(myRuntimeVisibleParameterAnnotations, that.myRuntimeVisibleParameterAnnotations) &&
				Arrays.equals(myRuntimeInvisibleParameterAnnotations, that.myRuntimeInvisibleParameterAnnotations) &&
				Objects.equals(myAnnotationDefault, that.myAnnotationDefault);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), myThrownExceptions, mySignature, myParameterDescriptors, myReturnTypeSignature, myIsConstructor, myRuntimeVisibleParameterAnnotations,
				myRuntimeInvisibleParameterAnnotations, myAnnotationDefault);
	}
}
