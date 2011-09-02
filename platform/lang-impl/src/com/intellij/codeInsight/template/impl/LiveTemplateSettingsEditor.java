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

package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.openapi.MnemonicHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class LiveTemplateSettingsEditor extends JPanel {
  private final TemplateImpl myTemplate;

  private final JTextField myKeyField;
  private final JTextField myDescription;
  private final Editor myTemplateEditor;

  private JComboBox myExpandByCombo;
  private final String myDefaultShortcutItem;
  private JCheckBox myCbReformat;

  private JButton myEditVariablesButton;

  private static final String SPACE = CodeInsightBundle.message("template.shortcut.space");
  private static final String TAB = CodeInsightBundle.message("template.shortcut.tab");
  private static final String ENTER = CodeInsightBundle.message("template.shortcut.enter");
  private final Map<TemplateOptionalProcessor, Boolean> myOptions;
  private final Map<TemplateContextType, Boolean> myContext;

  public LiveTemplateSettingsEditor(TemplateImpl template,
                                    final String defaultShortcut,
                                    Map<TemplateOptionalProcessor, Boolean> options,
                                    Map<TemplateContextType, Boolean> context, final Runnable nodeChanged) {
    super(new BorderLayout());
    myOptions = options;
    myContext = context;

    myTemplate = template;
    myDefaultShortcutItem = CodeInsightBundle.message("dialog.edit.template.shortcut.default", defaultShortcut);

    myKeyField=new JTextField();
    myDescription=new JTextField();
    myTemplateEditor = TemplateEditorUtil.createEditor(false, myTemplate.getString(), context);
    myTemplate.setId(null);

    createComponents();
    
    reset();

    com.intellij.ui.DocumentAdapter listener = new com.intellij.ui.DocumentAdapter() {
      @Override
      protected void textChanged(javax.swing.event.DocumentEvent e) {
        myTemplate.setKey(myKeyField.getText().trim());
        myTemplate.setDescription(myDescription.getText().trim());
        nodeChanged.run();
      }
    };
    myKeyField.getDocument().addDocumentListener(listener);
    myDescription.getDocument().addDocumentListener(listener);
  }

  public TemplateImpl getTemplate() {
    return myTemplate;
  }

  public void dispose() {
    EditorFactory.getInstance().releaseEditor(myTemplateEditor);
  }

  private void createComponents() {
    JPanel panel = new JPanel(new GridBagLayout());

    GridBag gb = new GridBag().setDefaultInsets(4, 4, 4, 4).setDefaultWeightY(1).setDefaultFill(GridBagConstraints.BOTH);
    
    JPanel editorPanel = new JPanel(new BorderLayout());
    editorPanel.setPreferredSize(new Dimension(250, 100));
    editorPanel.setMinimumSize(editorPanel.getPreferredSize());
    editorPanel.add(myTemplateEditor.getComponent(), BorderLayout.CENTER);
    panel.add(editorPanel, gb.nextLine().next().weighty(1).weightx(1).coverColumn(2));

    myEditVariablesButton = new JButton(CodeInsightBundle.message("dialog.edit.template.button.edit.variables"));
    myEditVariablesButton.setDefaultCapable(false);
    myEditVariablesButton.setMaximumSize(myEditVariablesButton.getPreferredSize());
    panel.add(myEditVariablesButton, gb.next().weighty(0));

    panel.add(createTemplateOptionsPanel(), gb.nextLine().next().next().coverColumn(2).weighty(1));

    panel.add(createShortContextPanel(), gb.nextLine().next().fillCellNone().anchor(GridBagConstraints.WEST));

    myTemplateEditor.getDocument().addDocumentListener(
      new DocumentAdapter() {
        public void documentChanged(DocumentEvent e) {
          validateEditVariablesButton();

          myTemplate.setString(myTemplateEditor.getDocument().getText());
          applyVariables(updateVariablesByTemplateText());
        }
      }
    );

    myEditVariablesButton.addActionListener(
      new ActionListener(){
        public void actionPerformed(ActionEvent e) {
          editVariables();
        }
      }
    );

    add(createNorthPanel(), BorderLayout.NORTH);
    add(panel, BorderLayout.CENTER);
    setBorder(IdeBorderFactory.createTitledBorder(CodeInsightBundle.message("dialog.edit.template.template.text.title"), false, false, true));
  }

  private void applyVariables(final List<Variable> variables) {
    myTemplate.removeAllParsed();
    for (Variable variable : variables) {
      myTemplate.addVariable(variable.getName(), variable.getExpressionString(), variable.getDefaultValueString(),
                             variable.isAlwaysStopAt());
    }
    myTemplate.parseSegments();
  }

  @Nullable
  private JComponent createNorthPanel() {
    JPanel panel = new JPanel(new GridBagLayout());

    GridBag gb = new GridBag().setDefaultInsets(4, 4, 4, 4).setDefaultWeightY(1).setDefaultFill(GridBagConstraints.BOTH);

    JLabel keyPrompt = new JLabel(CodeInsightBundle.message("dialog.edit.template.label.abbreviation"));
    keyPrompt.setLabelFor(myKeyField);
    panel.add(keyPrompt, gb.nextLine().next());

    panel.add(myKeyField, gb.next().weightx(1));

    JLabel descriptionPrompt = new JLabel(CodeInsightBundle.message("dialog.edit.template.label.description"));
    descriptionPrompt.setLabelFor(myDescription);
    panel.add(descriptionPrompt, gb.next());

    panel.add(myDescription, gb.next().weightx(3));
    return panel;
  }

  private JPanel createTemplateOptionsPanel() {
    JPanel panel = new JPanel();
    panel.setBorder(IdeBorderFactory.createTitledBorder(CodeInsightBundle.message("dialog.edit.template.options.title"),
                                                        false, true, true));
    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();
    gbConstraints.fill = GridBagConstraints.BOTH;

    gbConstraints.weighty = 0;
    gbConstraints.weightx = 0;
    gbConstraints.gridy = 0;
    panel.add(new JLabel(CodeInsightBundle.message("dialog.edit.template.label.expand.with")), gbConstraints);

    gbConstraints.gridx = 1;
    myExpandByCombo = new JComboBox(new Object[]{myDefaultShortcutItem, SPACE, TAB, ENTER});
    myExpandByCombo.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        Object selectedItem = myExpandByCombo.getSelectedItem();
        if(myDefaultShortcutItem.equals(selectedItem)) {
          myTemplate.setShortcutChar(TemplateSettings.DEFAULT_CHAR);
        }
        else if(TAB.equals(selectedItem)) {
          myTemplate.setShortcutChar(TemplateSettings.TAB_CHAR);
        }
        else if(ENTER.equals(selectedItem)) {
          myTemplate.setShortcutChar(TemplateSettings.ENTER_CHAR);
        }
        else {
          myTemplate.setShortcutChar(TemplateSettings.SPACE_CHAR);
        }
        
      }
    });
    
    panel.add(myExpandByCombo, gbConstraints);
    gbConstraints.weightx = 1;
    gbConstraints.gridx = 2;
    panel.add(new JPanel(), gbConstraints);

    gbConstraints.gridx = 0;
    gbConstraints.gridy++;
    gbConstraints.gridwidth = 3;
    myCbReformat = new JCheckBox(CodeInsightBundle.message("dialog.edit.template.checkbox.reformat.according.to.style"));
    panel.add(myCbReformat, gbConstraints);

    for (final TemplateOptionalProcessor processor: myOptions.keySet()) {
      if (!processor.isVisible(myTemplate)) continue;
      gbConstraints.gridy++;
      final JCheckBox cb = new JCheckBox(processor.getOptionName());
      panel.add(cb, gbConstraints);
      cb.setSelected(myOptions.get(processor).booleanValue());
      cb.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          myOptions.put(processor, cb.isSelected());
        }
      });
    }

    gbConstraints.weighty = 1;
    gbConstraints.gridy++;
    panel.add(new JPanel(), gbConstraints);          

    return panel;
  }

  private JPanel createShortContextPanel() {
    JPanel panel = new JPanel(new BorderLayout());

    final JLabel ctxLabel = new JLabel();
    final JLabel change = new JLabel();
    change.setForeground(Color.BLUE);
    change.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    panel.add(ctxLabel, BorderLayout.CENTER);
    panel.add(change, BorderLayout.EAST);

    final Runnable updateLabel = new Runnable() {
      public void run() {
        List<String> contexts = new ArrayList<String>();
        for (TemplateContextType type : myContext.keySet()) {
          if (myContext.get(type).booleanValue()) {
            contexts.add(UIUtil.removeMnemonic(type.getPresentableName()));
          }
        }
        ctxLabel.setText((contexts.isEmpty() ? "No applicable contexts yet" : "Applicable in " + StringUtil.join(contexts, ", ")) + ".  ");
        ctxLabel.setForeground(contexts.isEmpty() ? Color.RED : UIUtil.getLabelForeground());
        change.setText(contexts.isEmpty() ? "Define" : "Change");
      }
    };

    change.addMouseListener(new MouseAdapter() {
      private JBPopup myPopup;
      @Override
      public void mouseClicked(MouseEvent e) {
        if (myPopup != null && myPopup.isVisible()) {
          myPopup.cancel();
          myPopup = null;
          return;
        }

        JPanel content = createPopupContextPanel(updateLabel);
        myPopup = JBPopupFactory.getInstance().createComponentPopupBuilder(content, null).createPopup();
        myPopup.show(new RelativePoint(change, new Point(change.getWidth() , -content.getPreferredSize().height - 10)));
      }
    });

    updateLabel.run();

    return panel;
  }

  private JPanel createPopupContextPanel(final Runnable onChange) {
    final Map<TemplateContextType, JCheckBox> contextComboBoxes = new HashMap<TemplateContextType, JCheckBox>();

    ChangeListener listener = new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        myExpandByCombo.setEnabled(isExpandableFromEditor());
      }

    };

    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();
    gbConstraints.fill = GridBagConstraints.BOTH;
    gbConstraints.weightx = 1;
    gbConstraints.weighty = 1;

    final Runnable updateContextTypesEnabledState = new Runnable() {
      public void run() {
        for (Map.Entry<TemplateContextType, JCheckBox> entry : contextComboBoxes.entrySet()) {
          TemplateContextType contextType = entry.getKey();
          TemplateContextType baseContextType = contextType.getBaseContextType();
          boolean enabled = baseContextType == null || !contextComboBoxes.get(baseContextType).isSelected();
          entry.getValue().setEnabled(enabled);
        }
      }
    };

    int row = 0;
    int col = 0;
    for (final TemplateContextType contextType : myContext.keySet()) {
      gbConstraints.gridy = row;
      gbConstraints.gridx = col;
      final JCheckBox cb = new JCheckBox(contextType.getPresentableName());
      cb.getModel().addChangeListener(listener);
      panel.add(cb, gbConstraints);
      contextComboBoxes.put(contextType, cb);

      if (row == (myContext.size() + 1) / 2 - 1) {
        row = 0;
        col = 1;
      }
      else {
        row++;
      }
      cb.setSelected(myContext.get(contextType).booleanValue());

      cb.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          myContext.put(contextType, cb.isSelected());
          updateContextTypesEnabledState.run();
          updateHighlighter();
          onChange.run();
        }
      }
      );
    }
    
    updateContextTypesEnabledState.run();

    new MnemonicHelper().register(panel);

    return panel;
  }

  private boolean isExpandableFromEditor() {
    boolean hasNonExpandable = false;
    for (TemplateContextType type : myContext.keySet()) {
      if (myContext.get(type)) {
        if (type.isExpandableFromEditor()) {
          return true;
        }
        hasNonExpandable = true;
      }
    }
    
    return !hasNonExpandable;
  }

  private void updateHighlighter() {
    TemplateContext templateContext = new TemplateContext();
    TemplateEditorUtil.setHighlighter(myTemplateEditor, templateContext);
    ((EditorEx) myTemplateEditor).repaint(0, myTemplateEditor.getDocument().getTextLength());
  }

  private void validateEditVariablesButton() {
    myEditVariablesButton.setEnabled(!parseVariables(myTemplateEditor.getDocument().getCharsSequence(), false).isEmpty());
  }

  private void reset() {
    myKeyField.setText(myTemplate.getKey());
    myDescription.setText(myTemplate.getDescription());

    if(myTemplate.getShortcutChar() == TemplateSettings.DEFAULT_CHAR) {
      myExpandByCombo.setSelectedItem(myDefaultShortcutItem);
    }
    else if(myTemplate.getShortcutChar() == TemplateSettings.TAB_CHAR) {
      myExpandByCombo.setSelectedItem(TAB);
    }
    else if(myTemplate.getShortcutChar() == TemplateSettings.ENTER_CHAR) {
      myExpandByCombo.setSelectedItem(ENTER);
    }
    else {
      myExpandByCombo.setSelectedItem(SPACE);
    }

    CommandProcessor.getInstance().executeCommand(
      null, new Runnable() {
      public void run() {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          public void run() {
            final Document document = myTemplateEditor.getDocument();
            document.replaceString(0, document.getTextLength(), myTemplate.getString());
          }
        });
      }
    },
      "",
      null
    );

    myCbReformat.setSelected(myTemplate.isToReformat());
    myCbReformat.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myTemplate.setToReformat(myCbReformat.isSelected());
      }
    });

    myExpandByCombo.setEnabled(isExpandableFromEditor());

    updateHighlighter();
    validateEditVariablesButton();
  }

  private void editVariables() {
    ArrayList<Variable> newVariables = updateVariablesByTemplateText();

    EditVariableDialog editVariableDialog = new EditVariableDialog(myTemplateEditor, myEditVariablesButton, newVariables);
    editVariableDialog.show();
    if (editVariableDialog.isOK()) {
      applyVariables(newVariables);
    }
  }

  private ArrayList<Variable> updateVariablesByTemplateText() {
    List<Variable> oldVariables = getCurrentVariables();
    
    Set<String> oldVariableNames = ContainerUtil.map2Set(oldVariables, new Function<Variable, String>() {
      @Override
      public String fun(Variable variable) {
        return variable.getName();
      }
    });
    

    ArrayList<Variable> parsedVariables = parseVariables(myTemplateEditor.getDocument().getCharsSequence(), false);

    Map<String,String> newVariableNames = new HashMap<String, String>();
    for (Object parsedVariable : parsedVariables) {
      Variable newVariable = (Variable)parsedVariable;
      String name = newVariable.getName();
      newVariableNames.put(name, name);
    }

    int oldVariableNumber = 0;
    for(int i = 0; i < parsedVariables.size(); i++){
      Variable variable = parsedVariables.get(i);
      if(oldVariableNames.contains(variable.getName())) {
        Variable oldVariable = null;
        for(;oldVariableNumber<oldVariables.size(); oldVariableNumber++) {
          oldVariable = oldVariables.get(oldVariableNumber);
          if(newVariableNames.get(oldVariable.getName()) != null) {
            break;
          }
          oldVariable = null;
        }
        oldVariableNumber++;
        if(oldVariable != null) {
          parsedVariables.set(i, oldVariable);
        }
      }
    }

    return parsedVariables;
  }

  private List<Variable> getCurrentVariables() {
    List<Variable> myVariables = new ArrayList<Variable>();

    for(int i = 0; i < myTemplate.getVariableCount(); i++) {
      myVariables.add(new Variable(myTemplate.getVariableNameAt(i),
                                   myTemplate.getExpressionStringAt(i),
                                   myTemplate.getDefaultValueStringAt(i),
                                   myTemplate.isAlwaysStopAt(i)));
    }
    return myVariables;
  }

  public JTextField getKeyField() {
    return myKeyField;
  }

  public void focusKey() {
    myKeyField.selectAll();
    //todo[peter,kirillk] without these invokeLaters this requestFocus conflicts with com.intellij.openapi.ui.impl.DialogWrapperPeerImpl.MyDialog.MyWindowListener.windowOpened()
    IdeFocusManager.findInstanceByComponent(myKeyField).requestFocus(myKeyField, true);
    final ModalityState modalityState = ModalityState.stateForComponent(myKeyField);
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
              @Override
              public void run() {
                IdeFocusManager.findInstanceByComponent(myKeyField).requestFocus(myKeyField, true);
              }
            }, modalityState);
          }
        }, modalityState);
      }
    }, modalityState);
  }

  private static ArrayList<Variable> parseVariables(CharSequence text, boolean includeInternal) {
    ArrayList<Variable> variables = new ArrayList<Variable>();
    TemplateImplUtil.parseVariables(text, variables, TemplateImpl.INTERNAL_VARS_SET);
    if (!includeInternal) {
      for (Iterator<Variable> iterator = variables.iterator(); iterator.hasNext(); ) {
        if (TemplateImpl.INTERNAL_VARS_SET.contains(iterator.next().getName())) {
          iterator.remove();
        }
      }
    }
    return variables;
  }

}
