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
package com.intellij.psi.impl.migration;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.intellij.openapi.components.ServiceManager;
import consulo.logging.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.psi.PsiManager;
import com.intellij.java.language.psi.PsiMigration;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.PsiManagerImpl;

@Singleton
public class PsiMigrationManager {
  private static final Logger LOG = Logger.getInstance(PsiMigrationManager.class);

  public static PsiMigrationManager getInstance(Project project) {
    return ServiceManager.getService(project, PsiMigrationManager.class);
  }
  
  private final Project myProject;
  private PsiMigrationImpl myCurrentMigration;

  @Inject
  public PsiMigrationManager(Project project) {
    myProject = project;
  }

  public PsiMigrationImpl getCurrentMigration() {
    return myCurrentMigration;
  }

  /**
   * Initiates a migrate refactoring. The refactoring is finished when
   * {@link PsiMigration#finish()} is called.
   *
   * @return the migrate operation object.
   */
  @Nonnull
  public PsiMigration startMigration() {
    LOG.assertTrue(myCurrentMigration == null);
    myCurrentMigration = new PsiMigrationImpl(this, JavaPsiFacade.getInstance(myProject),
                                              (PsiManagerImpl)PsiManager.getInstance(myProject));
    return myCurrentMigration;
  }

  public void migrationModified(boolean terminated) {
    if (terminated) {
      myCurrentMigration = null;
    }

    ((PsiManagerEx)PsiManager.getInstance(myProject)).beforeChange(true);
  }
}
