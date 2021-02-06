// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.compiled;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.impl.cache.TypeInfo;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import consulo.internal.org.objectweb.asm.*;
import consulo.util.lang.BitUtil;
import org.jetbrains.annotations.Contract;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Information retrieved during the first pass of a class file parsing
 */
class FirstPassData implements Function<String, String>
{
	private static final Logger LOG = Logger.getInstance(FirstPassData.class);

	private static class InnerClassEntry
	{
		final
		@Nonnull
		String myOuterName;
		final
		@Nullable
		String myInnerName;
		final boolean myStatic;

		private InnerClassEntry(@Nonnull String outerName, @Nullable String innerName, boolean aStatic)
		{
			myOuterName = outerName;
			myInnerName = innerName;
			myStatic = aStatic;
		}
	}

	private static final FirstPassData NO_DATA = new FirstPassData(Collections.emptyMap(), null, Collections.emptySet());
	private static final FirstPassData EMPTY = new FirstPassData(Collections.emptyMap(), null, Collections.emptySet());
	private final
	@Nonnull
	Map<String, InnerClassEntry> myMap;
	private final
	@Nonnull
	Set<String> myNonStatic;
	private final
	@Nonnull
	Set<ObjectMethod> mySyntheticMethods;
	private final
	@Nullable
	String myVarArgRecordComponent;

	private FirstPassData(@Nonnull Map<String, InnerClassEntry> map,
						  @Nullable String component,
						  @Nonnull Set<ObjectMethod> syntheticMethods)
	{
		myMap = map;
		myVarArgRecordComponent = component;
		mySyntheticMethods = syntheticMethods;
		if(!map.isEmpty())
		{
			List<String> jvmNames = map.entrySet().stream().filter(e -> !e.getValue().myStatic).map(Map.Entry::getKey).collect(Collectors.toList());

			myNonStatic = ContainerUtil.map2Set(jvmNames, this::mapJvmClassNameToJava);
		}
		else
		{
			myNonStatic = Collections.emptySet();
		}
	}

	@Override
	@Nonnull
	public String fun(@Nonnull String jvmName)
	{
		return mapJvmClassNameToJava(jvmName);
	}

	/**
	 * @param javaName java class name
	 * @return nesting level: number of enclosing classes for which this class is non-static
	 */
	int getInnerDepth(@Nonnull String javaName)
	{
		int depth = 0;
		while(!javaName.isEmpty() && myNonStatic.contains(javaName))
		{
			depth++;
			javaName = StringUtil.getPackageName(javaName);
		}
		return depth;
	}

	/**
	 * @param componentName record component name
	 * @return true if given component is var-arg
	 */
	boolean isVarArgComponent(@Nonnull String componentName)
	{
		return componentName.equals(myVarArgRecordComponent);
	}

	/**
	 * @param methodName method name
	 * @param methodDesc method descriptor
	 * @return true if given method is a synthetic method of the record (autogenerated equals, hashCode or toString)
	 */
	boolean isSyntheticRecordMethod(@Nonnull String methodName, @Nonnull String methodDesc)
	{
		return !mySyntheticMethods.isEmpty() && mySyntheticMethods.contains(ObjectMethod.from(methodName, methodDesc));
	}

	/**
	 * @param jvmNames array JVM type names (e.g. throws list, implements list)
	 * @return list of TypeInfo objects that correspond to given types. GUESSING_MAPPER is not used.
	 */
	@Contract("null -> null; !null -> !null")
	List<TypeInfo> createTypes(@Nullable String[] jvmNames)
	{
		return jvmNames == null ? null :
				ContainerUtil.map(jvmNames, jvmName -> new TypeInfo(mapJvmClassNameToJava(jvmName, false)));
	}

	/**
	 * @param jvmName JVM class name like java/util/Map$Entry
	 * @return Java class name like java.util.Map.Entry
	 */
	@Nonnull
	String mapJvmClassNameToJava(@Nonnull String jvmName)
	{
		return mapJvmClassNameToJava(jvmName, true);
	}

	/**
	 * @param jvmName    JVM class name like java/util/Map$Entry
	 * @param useGuesser if true, {@link StubBuildingVisitor#GUESSING_MAPPER} will be used in case if the entry was absent in
	 *                   InnerClasses table.
	 * @return Java class name like java.util.Map.Entry
	 */
	@Nonnull
	String mapJvmClassNameToJava(@Nonnull String jvmName, boolean useGuesser)
	{
		String className = jvmName;

		if(className.indexOf('$') >= 0)
		{
			InnerClassEntry p = myMap.get(className);
			if(p != null)
			{
				className = p.myOuterName;
				if(p.myInnerName != null)
				{
					className = mapJvmClassNameToJava(p.myOuterName) + '.' + p.myInnerName;
					myMap.put(className, new InnerClassEntry(className, null, true));
				}
			}
			else if(useGuesser)
			{
				return StubBuildingVisitor.GUESSING_MAPPER.fun(jvmName);
			}
		}

		return className.replace('/', '.');
	}

	static
	@Nonnull
	FirstPassData create(Object classSource)
	{
		byte[] bytes = null;
		if(classSource instanceof ClsFileImpl.FileContentPair)
		{
			bytes = ((ClsFileImpl.FileContentPair) classSource).getContent();
		}
		else if(classSource instanceof VirtualFile)
		{
			try
			{
				bytes = ((VirtualFile) classSource).contentsToByteArray(false);
			}
			catch(IOException ignored)
			{
			}
		}

		if(bytes != null)
		{
			return fromClassBytes(bytes);
		}

		return NO_DATA;
	}

	@Nonnull
	private static FirstPassData fromClassBytes(byte[] classBytes)
	{

		class FirstPassVisitor extends ClassVisitor
		{
			final Map<String, InnerClassEntry> mapping = new HashMap<>();
			Set<String> varArgConstructors;
			Set<ObjectMethod> syntheticSignatures;
			StringBuilder canonicalSignature;
			String lastComponent;

			FirstPassVisitor()
			{
				super(Opcodes.API_VERSION);
			}

			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)
			{
				if(BitUtil.isSet(access, Opcodes.ACC_RECORD))
				{
					varArgConstructors = new HashSet<>();
					canonicalSignature = new StringBuilder("(");
					syntheticSignatures = EnumSet.noneOf(ObjectMethod.class);
				}
			}

			@Override
			public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature)
			{
				if(isRecord())
				{
					canonicalSignature.append(descriptor);
					lastComponent = name;
				}
				return null;
			}

			private boolean isRecord()
			{
				return varArgConstructors != null;
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions)
			{
				if(isRecord())
				{
					if(name.equals("<init>") && BitUtil.isSet(access, Opcodes.ACC_VARARGS))
					{
						varArgConstructors.add(descriptor);
					}
					ObjectMethod method = ObjectMethod.from(name, descriptor);
					if(method != null)
					{
						return new MethodVisitor(Opcodes.API_VERSION)
						{
							@Override
							public void visitInvokeDynamicInsn(String indyName,
															   String indyDescriptor,
															   Handle bootstrapMethodHandle,
															   Object... bootstrapMethodArguments)
							{
								if(indyName.equals(name) && bootstrapMethodHandle.getName().equals("bootstrap") &&
										bootstrapMethodHandle.getOwner().equals("java/lang/runtime/ObjectMethods"))
								{
									syntheticSignatures.add(method);
								}
							}
						};
					}
				}
				return null;
			}

			@Override
			public void visitInnerClass(String name, String outerName, String innerName, int access)
			{
				if(outerName != null && innerName != null)
				{
					mapping.put(name, new InnerClassEntry(outerName, innerName, BitUtil.isSet(access, Opcodes.ACC_STATIC)));
				}
			}
		}

		FirstPassVisitor visitor = new FirstPassVisitor();
		try
		{
			new ClassReader(classBytes).accept(visitor, ClsFileImpl.EMPTY_ATTRIBUTES, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		}
		catch(Exception ex)
		{
			LOG.debug(ex);
		}
		String varArgComponent = null;
		if(visitor.isRecord())
		{
			visitor.canonicalSignature.append(")V");
			if(visitor.varArgConstructors.contains(visitor.canonicalSignature.toString()))
			{
				varArgComponent = visitor.lastComponent;
			}
		}
		Set<ObjectMethod> syntheticMethods = visitor.syntheticSignatures == null ? Collections.emptySet() : visitor.syntheticSignatures;
		if(varArgComponent == null && visitor.mapping.isEmpty() && syntheticMethods.isEmpty())
		{
			return EMPTY;
		}
		return new FirstPassData(visitor.mapping, varArgComponent, syntheticMethods);
	}

	private enum ObjectMethod
	{
		EQUALS("equals", "(Ljava/lang/Object;)Z"),
		HASH_CODE("hashCode", "()I"),
		TO_STRING("toString", "()Ljava/lang/String;");

		private final
		@Nonnull
		String myName;
		private final
		@Nonnull
		String myDesc;

		ObjectMethod(@Nonnull String name, @Nonnull String desc)
		{
			myName = name;
			myDesc = desc;
		}

		static
		@Nullable
		ObjectMethod from(@Nonnull String name, @Nonnull String desc)
		{
			for(ObjectMethod method : values())
			{
				if(method.myName.equals(name) && method.myDesc.equals(desc))
				{
					return method;
				}
			}
			return null;
		}
	}
}
