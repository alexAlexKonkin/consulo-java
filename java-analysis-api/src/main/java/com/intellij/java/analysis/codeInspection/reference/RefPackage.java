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
package com.intellij.java.analysis.codeInspection.reference;

import consulo.language.editor.inspection.reference.RefEntity;

/**
 * A node in the reference graph corresponding to a Java package.
 *
 * @author anna
 * @since 6.0
 * @see RefJavaManager#getPackage
 */
public interface RefPackage extends RefEntity
{
  /**
   * Returns the full-qualified name for the package, or an empty string for the default package.
   *
   * @return the full-qualified name for the package.
   */
  @Override
  String getQualifiedName();
}