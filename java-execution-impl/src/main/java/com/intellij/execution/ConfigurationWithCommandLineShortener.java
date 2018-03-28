// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution;

import com.intellij.openapi.project.Project;

public interface ConfigurationWithCommandLineShortener
{
	/**
	 * @return null if option was not selected explicitly, legacy user-local options to be used
	 */
	@javax.annotation.Nullable
	ShortenCommandLine getShortenCommandLine();

	/**
	 * Called from UI, when user explicitly selects method to be used to shorten the command line or from the deserialization
	 */
	void setShortenCommandLine(ShortenCommandLine mode);

	Project getProject();
}
