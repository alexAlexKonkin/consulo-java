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
package com.intellij.java.impl.codeInspection.deadCode;

import consulo.language.editor.inspection.GlobalInspectionContext;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.editor.inspection.ProblemDescriptionsProcessor;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.scheme.JobDescriptor;
import consulo.language.editor.scope.AnalysisScope;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 */
@SuppressWarnings("ExtensionImplIsNotAnnotatedInspection")
public class DummyEntryPointsTool extends UnusedDeclarationInspection {
  public DummyEntryPointsTool() {
  }

  @Override
  public void runInspection(@jakarta.annotation.Nonnull AnalysisScope scope,
                            @jakarta.annotation.Nonnull InspectionManager manager,
                            @jakarta.annotation.Nonnull GlobalInspectionContext globalContext,
                            @Nonnull ProblemDescriptionsProcessor problemDescriptionsProcessor,
                            Object state) {
  }

  @Nullable
  @Override
  public JobDescriptor[] getAdditionalJobs() {
    return JobDescriptor.EMPTY_ARRAY;
  }

  @Override
  @jakarta.annotation.Nonnull
  public String getDisplayName() {
    return InspectionsBundle.message("inspection.dead.code.entry.points.display.name");
  }

  @Override
  @jakarta.annotation.Nonnull
  public String getGroupDisplayName() {
    return "";
  }

  @Override
  @jakarta.annotation.Nonnull
  public String getShortName() {
    //noinspection InspectionDescriptionNotFoundInspection
    return "";
  }
}
