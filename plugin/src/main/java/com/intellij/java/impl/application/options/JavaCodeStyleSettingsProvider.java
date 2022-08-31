/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.java.impl.application.options;

import com.intellij.application.options.CodeStyleAbstractConfigurable;
import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.java.impl.psi.codeStyle.JavaCodeStyleSettings;
import com.intellij.java.language.JavaLanguage;
import com.intellij.lang.Language;
import com.intellij.openapi.options.Configurable;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Rustam Vishnyakov
 */
public class JavaCodeStyleSettingsProvider extends CodeStyleSettingsProvider {
  @Nonnull
  @Override
  public Configurable createSettingsPage(CodeStyleSettings settings, CodeStyleSettings originalSettings) {
    return new CodeStyleAbstractConfigurable(settings, originalSettings, "Java") {
      @Override
      protected CodeStyleAbstractPanel createPanel(final CodeStyleSettings settings) {
        return new JavaCodeStyleMainPanel(getCurrentSettings(), settings);
      }

      @Override
      public String getHelpTopic() {
        return "reference.settingsdialog.codestyle.java";
      }
    };
  }

  @Override
  public String getConfigurableDisplayName() {
    return "Java";
  }

  @Nullable
  @Override
  public Language getLanguage() {
    return JavaLanguage.INSTANCE;
  }

  @Nullable
  @Override
  public CustomCodeStyleSettings createCustomSettings(CodeStyleSettings settings) {
    return new JavaCodeStyleSettings(settings);
  }
}
