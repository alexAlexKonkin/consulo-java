/*
 * Copyright 2013-2018 consulo.io
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

package consulo.java.jam.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.ide.ServiceManager;
import consulo.language.psi.PsiElement;

/**
 * @author VISTALL
 * @since 2018-06-21
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface JamCommonService {
  static JamCommonService getInstance() {
    return ServiceManager.getService(JamCommonService.class);
  }

  @RequiredReadAction
  boolean isPlainJavaFile(PsiElement psiElement);

  @RequiredReadAction
  boolean isPlainXmlFile(PsiElement psiElement);
}
