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
package com.intellij.java.language.impl.psi.filters.classes;

import com.intellij.java.language.psi.PsiClass;
import consulo.language.psi.PsiElement;
import consulo.language.psi.filter.ElementFilter;
import consulo.util.lang.reflect.ReflectionUtil;

/**
 * Created by IntelliJ IDEA.
 * User: ik
 * Date: 28.01.2003
 * Time: 21:00:45
 * To change this template use Options | File Templates.
 */
public class AnnotationTypeFilter
  implements ElementFilter{

  @Override
  public boolean isClassAcceptable(Class hintClass){
    return ReflectionUtil.isAssignable(PsiClass.class, hintClass);
  }

  @Override
  public boolean isAcceptable(Object element, PsiElement context){
    return element instanceof PsiClass && ((PsiClass)element).isAnnotationType();
  }

  public String toString(){
    return "annotationType";
  }
}
