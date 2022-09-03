/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

/*
 * Created by IntelliJ IDEA.
 * User: cdr
 * Date: Jul 20, 2007
 * Time: 2:57:38 PM
 */
package com.intellij.java.impl.codeInsight.intention.impl;

import com.intellij.java.language.codeInsight.NullableNotNullManager;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.util.collection.ArrayUtil;
import javax.annotation.Nonnull;

import java.util.List;

public class AddNotNullAnnotationIntention extends AddAnnotationIntention {
  @Nonnull
  @Override
  public Pair<String, String[]> getAnnotations(@Nonnull Project project) {
    return new Pair<String, String[]>(NullableNotNullManager.getInstance(project).getDefaultNotNull(), getNullables(project));
  }

  @Nonnull
  private static String[] getNullables(@Nonnull Project project) {
    final List<String> nullables = NullableNotNullManager.getInstance(project).getNullables();
    return ArrayUtil.toStringArray(nullables);
  }
}
