## Conventions for the internationalization of strings

All messages, titles, questions, labels should be translated (or at least
translatable) in openLCA. This means that instead of using hardcoded
strings, one should create a message in `org.openlca.app.M` and provide a value
in English in `messages.properties`.

As a general rule, keys should be **short** and **descriptive** and **parameters in
strings should be avoided**.

### Punctuations

Punctuation's marks should be part of the strings as much as possible as it
differs from one language to another. For example, _Delete?_ is _Supprimer ?_
in French, _¿Borrar?_ in Spanish and _يمسح؟_ in Arabic. For that reason, the
following suffixes should be added to the keys:

- `Dots` for the ellipsis _..._,
- `Q` for the question mark _?_,
- `Bang` for the exclamation mark _!_ (not used yet),
- `Amp` for the ampersand _&_ (`AmpFile=&File`),
- no suffix for the full stop _._.

For example `M.DeleteQ` is used for _Delete?_.

### Long strings

In dialogues or message boxes, long strings are often used. To avoid using long
descriptive keys, one can instead add the suffixes `Info`, `Question` and `Err`.

For example:
```java
var a = Question.ask(M.DeleteLibraryQ, M.DeleteLibraryQuestion);
```

### Parameters within a string

While it is advised not to use variables within strings, it is sometimes hard to
avoid. Here are some good practices:

- Avoid using variables in strings 😇,
- Retain from using colons `:` as they translate badly to other languages. It is
preferable to use
  - hyphens `-` (`M.OpenInEditor + " - " + name`),
  - or parenthesis `()` (`M.UpstreamTotal + " (" + percentage + "%)"`) instead.
- Avoid differentiating plural words if possible (`DaysAgo=Day(s) ago`).

### Organize messages and removed unused

When deleting messages from `org.openlca.app.M`, it is recommended to remove the
corresponding translations in the `messages_*.properties` files. Also, it is
cleaner to order messages alphabetically in the `M.java` file. For that purpose,
one can use the `olca-app/message_property.py` script (it is recommended to make
a copy of the `messages_*.properties` first).

### Search for strings

It is possible to search for strings that are not internationalized by checking
usage of the most common methods using messages (see below). Another option is
to use the following regex:

```regexp
(?<!ErrorReporter\.on\()(?<!error\()(?<!warn\()(?<!info\()(?<!trace\()(?<!debug\()(?<!super\(table, )(?<!var id = )(?<!setId\()(?<!Exception\()(?<!endsWith\()(?<!getString\()(?<!addProperty\()(?<!getBoolean\()(?<!getDouble\()(?<!contains\()(?<!equals\()(?<!startsWith\()(?<!equalsIgnoreCase\()(?<!SuppressWarnings\()(?<!=)(?<!super\(tool, )(?<!super\(editor, )(?<!Error\()(?<!println\()(?<!setDebugLabel\()(?<!@import url\()(?<!=\\)(?<!getBoolean\(o, )(?<!getLong\(o, )(?<!getString\(o, )(?<!getDate\(o, )(?<!\.put\()(?<!\.get\()(?<!=)(?<!"")"[A-z](?![a-z]*[A-Z])(?![a-z]*_)(?![a-z]*-)(?![a-z]*[A-Z])(?![a-z]*\.)(?![a-z]*\/)(?!elect)(?!ttp)(?![a-z]*\()(?![a-z]*\[)
```

#### Common methods using messages

```bash
org.openlca.app.util.UI.header
org.eclipse.swt.widgets.Button.setText
org.eclipse.swt.widgets.Group.setText
org.openlca.app.util.UI.labeledText
org.eclipse.jface.wizard.WizardPage.setErrorMessage
org.eclipse.jface.action.IAction.setText
org.openlca.app.collaboration.viewers.json.olca.PropertyLabels.put
org.openlca.app.editors.ModelPage.multiText
org.openlca.app.viewers.trees.Trees.createViewer
org.openlca.app.util.UI.formSection
org.eclipse.jface.action.Action.setText
org.eclipse.jface.viewers.ColumnLabelProvider.getText
org.eclipse.core.runtime.IProgressMonitor.subTask
org.openlca.app.util.Question.ask
org.openlca.app.viewers.tables.AbstractTableViewer.getColumnHeaders
org.eclipse.jface.viewers.ITableLabelProvider.getColumnText
org.eclipse.jface.viewers.LabelProvider.getText
org.eclipse.ui.forms.widgets.Hyperlink.setText
org.eclipse.gef.commands.Command.setLabel
org.eclipse.jface.viewers.ColumnViewer.setColumnProperties
org.eclipse.ui.forms.widgets.FormToolkit.createButton
org.eclipse.ui.actions.RetargetAction.RetargetAction
org.eclipse.jface.action.Action.setToolTipText
org.openlca.app.util.UI.button
org.openlca.app.util.DQUI.appendTableHeaders
org.openlca.app.editors.ModelPage.text
org.openlca.app.results.InfoSection.text
org.eclipse.swt.widgets.Label.setText
org.eclipse.ui.forms.editor.FormPage.FormPage
org.openlca.app.viewers.tables.modify.ModifySupport.bind
org.eclipse.draw2d.Label
org.eclipse.jface.viewers.ILabelProvider.getText
org.openlca.app.util.UI.checkbox
org.eclipse.jface.wizard.WizardPage.setTitle
org.openlca.app.navigation.elements.Group.of
org.openlca.app.navigation.elements.Group.Group
org.eclipse.ui.forms.widgets.FormToolkit.createLabel
org.openlca.app.util.UI.radio
org.eclipse.core.runtime.IProgressMonitor.beginTask
org.eclipse.jface.action.MenuManager
org.eclipse.swt.widgets.Shell.setText
org.openlca.app.App.runWithProgress
org.eclipse.jface.wizard.Wizard.setWindowTitle
org.eclipse.jface.wizard.WizardPage.setDescription
org.openlca.app.util.MsgBox.BoxJob
org.eclipse.jface.dialogs.Dialog.createButton
org.eclipse.jface.dialogs.MessageDialogWithToggle.openError
org.openlca.app.util.Question.askWithAll
org.openlca.app.util.UI.section
org.openlca.app.util.Actions.create
org.openlca.app.App.run
org.eclipse.ui.forms.widgets.Hyperlink.setToolTipText
org.openlca.app.util.UI.multiText
org.openlca.app.editors.SimpleEditorInput
org.openlca.app.collaboration.navigation.RepositoryMenu.subMenu
org.openlca.app.editors.ModelPage.doubleText
org.openlca.app.editors.graphical.model.commands.CreateLinkCommand.getLabel
org.eclipse.gef.commands.Command.getLabel
org.eclipse.jface.dialogs.DialogPage.setMessage
org.openlca.app.editors.ModelPage.modelLink
org.openlca.app.navigation.actions.db.DbActivateAction.ActivationCallback.error
org.openlca.app.util.Popup.info
org.eclipse.jface.dialogs.MessageDialogWithToggle.openError
org.eclipse.osgi.util.NLS.bind
org.openlca.app.util.UI.labeledCombo
org.eclipse.swt.widgets.Combo.add
org.eclipse.jface.viewers.ColumnViewer.setColumnProperties
org.openlca.app.preferences.ConfigMemCheck.showError
org.openlca.app.util.MsgBox.error
org.openlca.app.util.UI.labeledCheckbox
org.openlca.app.components.FileChooser.forSavingFile
org.eclipse.jface.dialogs.MessageDialog.openQuestion
org.openlca.app.util.MsgBox.info
org.openlca.app.results.InfoSection.link
org.eclipse.ui.forms.widgets.ExpandableComposite.setText
org.openlca.app.viewers.BaseLabelProvider
org.openlca.app.util.MsgBox.warning
org.openlca.app.util.UI.labeledRadio
org.openlca.app.viewers.trees.TreeClipboard.Provider.getHeader
org.openlca.app.viewers.trees.TreeClipboard.Provider.getLabel
org.eclipse.nebula.widgets.opal.notifier.Notifier.notify
org.openlca.app.editors.ModelPage.checkBox
org.openlca.app.collaboration.viewers.json.olca.ValueLabels.get
org.eclipse.ui.forms.IMessageManager.addMessage
org.eclipse.swt.widgets.Control.setToolTipText
java.util.function.Consumer.accept
org.eclipse.jface.preference.ComboFieldEditor
org.eclipse.jface.action.IAction.getText
org.eclipse.ui.part.MultiPageEditorPart.setPageText
org.eclipse.ui.IEditorInput.getName
org.eclipse.ui.forms.widgets.ScrolledForm.setText
org.eclipse.swt.widgets.MenuItem.setText
org.eclipse.jface.dialogs.InputDialog
org.openlca.app.db.Database.validateNewName
org.openlca.app.navigation.actions.CreateModelAction.getText
org.openlca.app.wizards.AbstractWizard.getTitle
org.eclipse.swt.widgets.Text.setText
org.openlca.app.results.contributions.locations.TreeLabel.getLabel
org.eclipse.jface.preference.PreferencePage.createNoteComposite
org.openlca.app.components.replace.ReplaceProvidersDialog.createFlowViewer
org.eclipse.swt.widgets.Dialog.setText
org.eclipse.gef.commands.Command.setLabel
org.eclipse.ui.progress.UIJob.UIJob
org.openlca.app.results.contributions.ChartLegend.getText
org.openlca.core.results.ProcessGrouping.name
org.openlca.core.results.ProcessGrouping.applyOn
org.eclipse.swt.widgets.Combo.setItems
org.eclipse.jface.preference.StringFieldEditor
org.openlca.app.tools.authentification.AuthenticationGroup.createText
org.openlca.app.wizards.ProductSystemWizard.addCreationInfo
org.openlca.app.wizards.LinkingConfigPanel.createRadioGroup
org.openlca.app.wizards.ProductSystemWizard.getLinkingInfo
org.openlca.app.editors.ModelPage.link
org.openlca.app.results.analysis.sankey.actions.OpenEditorAction.getObjectName
org.openlca.app.editors.flows.FlowUseSection.renderLinks
org.openlca.app.components.ModelLink.renderOn
org.openlca.app.components.replace.ReplaceFlowsDialog.createFlowViewer
org.openlca.app.results.ResultEditor.nameOf
org.openlca.app.search.ResultPage
org.openlca.app.util.Question.askWithCancel
org.openlca.app.results.requirements.TotalRequirementsSection.renderTotalCosts
org.openlca.app.wizards.UnitGroupWizard.Page.failCheck
org.openlca.app.collaboration.viewers.json.olca.PropertyLabels
org.openlca.app.tools.authentification.AuthenticationGroup.render
org.openlca.app.components.replace.ReplaceProvidersDialog.createProcessViewer
org.openlca.app.editors.ModelPage.shortText
org.openlca.app.viewers.tables.Tables.createViewer
org.openlca.app.components.FileChooser.OpenBuilder.withTitle
org.eclipse.swt.widgets.Text.setMessage
org.openlca.app.editors.ModelPage.ModelPage
org.openlca.app.App.exec
org.openlca.app.WrappedJob.run
org.eclipse.jface.action.IContributionManager.insertAfter
org.openlca.app.collaboration.viewers.json.MenuBar.createButton
org.eclipse.swt.widgets.TableColumn.setToolTipText
org.openlca.app.viewers.tables.Tables.createColumns
org.openlca.app.viewers.trees.Trees.createColumns
org.eclipse.swt.widgets.DirectoryDialog.setMessage
org.openlca.app.App.runInUI
org.openlca.app.util.UI.labeledMultiText
org.openlca.app.navigation.actions.nexus.XNexusEcoinventIndexExportAction.Entry
org.openlca.app.tools.openepd.output.Upload.errorAsk
org.openlca.app.collaboration.navigation.actions.Actions.handleException
```
