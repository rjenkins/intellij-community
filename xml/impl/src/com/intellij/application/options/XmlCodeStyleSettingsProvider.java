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
package com.intellij.application.options;

import com.intellij.psi.codeStyle.CodeStyleSettingsProvider;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;
import com.intellij.psi.formatter.xml.XmlCodeStyleSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author yole
 */
public class XmlCodeStyleSettingsProvider extends CodeStyleSettingsProvider {
  @NotNull
  public Configurable createSettingsPage(final CodeStyleSettings settings, final CodeStyleSettings originalSettings) {
    return new CodeStyleAbstractConfigurable(settings, originalSettings, ApplicationBundle.message("title.xml")){
      protected CodeStyleAbstractPanel createPanel(final CodeStyleSettings settings) {
        return new XmlCodeStyleMainPanel(getCurrentSettings(), settings);
      }
      public Icon getIcon() {
        return StdFileTypes.XML.getIcon();
      }

      public String getHelpTopic() {
        return "reference.settingsdialog.IDE.globalcodestyle.xml";
      }
    };
  }

  @Override
  public String getConfigurableDisplayName() {
    return ApplicationBundle.message("title.xml");
  }

  @Override
  public CustomCodeStyleSettings createCustomSettings(CodeStyleSettings settings) {
    return new XmlCodeStyleSettings(settings);
  }
}
