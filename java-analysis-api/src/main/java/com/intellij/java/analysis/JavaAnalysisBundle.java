// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.java.analysis;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.PropertyKey;

public final class JavaAnalysisBundle extends AbstractBundle
{
	@NonNls
	public static final String BUNDLE = "messages.JavaAnalysisBundle";
	private static final JavaAnalysisBundle INSTANCE = new JavaAnalysisBundle();

	private JavaAnalysisBundle()
	{
		super(BUNDLE);
	}

	@Nonnull
	public static String message(@Nonnull @PropertyKey(resourceBundle = BUNDLE) String key, @Nonnull Object  ...params)
	{
		return INSTANCE.getMessage(key, params);
	}
}