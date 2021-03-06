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
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.LanguageLevelModuleExtension;

import javax.swing.*;
import java.awt.*;

/**
 * @author Eugene Zhuravlev
 *         Date: Oct 4, 2003
 *         Time: 6:54:57 PM
 */
public class ContentEntriesEditor extends JavaContentEntriesEditor {
  private LanguageLevelConfigurable myLanguageLevelConfigurable;

  public ContentEntriesEditor(String moduleName, final ModuleConfigurationState state) {
    super(moduleName, state);
  }

  public void disposeUIResources() {
    if (myLanguageLevelConfigurable != null) myLanguageLevelConfigurable.disposeUIResources();
    super.disposeUIResources();
  }

  public boolean isModified() {
    return super.isModified() || myLanguageLevelConfigurable != null && myLanguageLevelConfigurable.isModified();
  }

  protected void addAdditionalSettingsToPanel(final JPanel mainPanel) {
    myLanguageLevelConfigurable = new LanguageLevelConfigurable() {
      @Override
      public LanguageLevelModuleExtension getLanguageLevelExtension() {
        return getModel().getModuleExtension(LanguageLevelModuleExtension.class);
      }
    };
    mainPanel.add(myLanguageLevelConfigurable.createComponent(), BorderLayout.NORTH);
    myLanguageLevelConfigurable.reset();
  }

  public void apply() throws ConfigurationException {
    myLanguageLevelConfigurable.apply();
    super.apply();
  }
}
