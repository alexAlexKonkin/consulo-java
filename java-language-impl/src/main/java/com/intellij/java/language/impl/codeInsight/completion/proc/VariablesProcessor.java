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
 * Created by IntelliJ IDEA.
 * User: igork
 * Date: Nov 25, 2002
 * Time: 1:44:25 PM
 * To change this template use Options | File Templates.
 */
package com.intellij.java.language.impl.codeInsight.completion.proc;

import com.intellij.java.language.impl.psi.scope.ElementClassHint;
import com.intellij.java.language.impl.psi.scope.util.PsiScopesUtil;
import com.intellij.java.language.psi.PsiModifier;
import com.intellij.java.language.psi.PsiVariable;
import com.intellij.java.language.psi.scope.JavaScopeProcessorEvent;
import consulo.language.psi.PsiElement;
import consulo.language.psi.resolve.BaseScopeProcessor;
import consulo.language.psi.resolve.ResolveState;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;


/** Simple processor to get all visible variables
 * @see PsiScopesUtil
 */
public class VariablesProcessor
        extends BaseScopeProcessor implements ElementClassHint{
  private final String myPrefix;
  private boolean myStaticScopeFlag = false;
  private final boolean myStaticSensitiveFlag;
  private final List<PsiVariable> myResultList;

  /** Collecting _all_ variables in scope */
  public VariablesProcessor(String _prefix, boolean staticSensitiveFlag){
    this(_prefix, staticSensitiveFlag, new ArrayList<PsiVariable>());
  }

  /** Collecting _all_ variables in scope */
  public VariablesProcessor(String _prefix, boolean staticSensitiveFlag, List<PsiVariable> lst){
    myPrefix = _prefix;
    myStaticSensitiveFlag = staticSensitiveFlag;
    myResultList = lst;
  }

  @Override
  public boolean shouldProcess(DeclarationKind kind) {
    return kind == DeclarationKind.VARIABLE || kind == DeclarationKind.FIELD || kind == DeclarationKind.ENUM_CONST;
  }

  /** Always return true since we wanna get all vars in scope */
  @Override
  public boolean execute(@Nonnull PsiElement pe, ResolveState state){
    if(pe instanceof PsiVariable){
      final PsiVariable pvar = (PsiVariable)pe;
      final String pvar_name = pvar.getName();
      if(pvar_name.startsWith(myPrefix)){
        if(!myStaticSensitiveFlag || (!myStaticScopeFlag || pvar.hasModifierProperty(PsiModifier.STATIC))){
          myResultList.add(pvar);
        }
      }
    }

    return true;
  }

  @Override
  public final void handleEvent(Event event, Object associated){
    if(event == JavaScopeProcessorEvent.START_STATIC)
      myStaticScopeFlag = true;
  }

  /** sometimes it is important to get results as array */
  public PsiVariable[] getResultsAsArray(){
    PsiVariable[] ret = new PsiVariable[myResultList.size()];
    myResultList.toArray(ret);
    return ret;
  }

  @Override
  public <T> T getHint(@Nonnull Key<T> hintKey) {
    if (hintKey == ElementClassHint.KEY) {
      return (T)this;
    }

    return super.getHint(hintKey);
  }
}
