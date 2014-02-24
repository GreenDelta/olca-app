package org.openlca.app.db.sql;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.db.Database;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;

public class SqlEditorPage extends FormPage {

	private Text resultText;
	private StyledText queryText;

	public SqlEditorPage(FormEditor editor) {
		super(editor, "SqlEditorPage", "SQL Query Browser");
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, "SQL Query Browser");
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		createStatementSection(body, toolkit);
		createResultSection(body, toolkit);
	}

	private void createStatementSection(Composite body, FormToolkit toolkit) {
		Section section = UI.section(body, toolkit, "SQL Statement");
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		queryText = new StyledText(composite, SWT.BORDER);
		toolkit.adapt(queryText);
		UI.gridData(queryText, true, false).heightHint = 150;
		queryText.addModifyListener(new SyntaxStyler(queryText));
		Actions.bind(section, new RunAction());
	}

	private void createResultSection(Composite body, FormToolkit toolkit) {
		Section section = UI.section(body, toolkit, "Results");
		UI.gridData(section, true, true);
		Composite composite = UI.sectionClient(section, toolkit);
		composite.setLayout(new FillLayout());
		resultText = toolkit.createText(composite, null, SWT.MULTI
				| SWT.H_SCROLL | SWT.V_SCROLL);
	}

	private class RunAction extends Action {

		RunAction() {
			setToolTipText("Run SQL statement");
			setText("Run SQL statement");
			setImageDescriptor(ImageType.RUN_SMALL.getDescriptor());
		}

		@Override
		public void run() {
			String statement = queryText.getText();
			String result = new SqlCommand().exec(statement, Database.get());
			resultText.setText(result);
		}
	}

	private class SyntaxStyler implements ModifyListener {

		private StyledText text;

		// we only support a small set of keywords to make the queries a bit nicer
		// see http://www.sql.org/sql-database/postgresql/manual/sql-keywords-appendix.html
		// for the complete keyword list
		private String[] keywords = {"alter", "and", "as", "asc", "column",
				"create", "select", "from", "insert",
				"update", "delete", "where", "show", "set"};

		SyntaxStyler(StyledText text) {
			this.text = text;
		}

		@Override
		public void modifyText(ModifyEvent modifyEvent) {
			String content = text.getText();
			if (content == null)
				return;
			StringBuilder word = null;
			int wordStart = -1;
			for (int i = 0; i < content.length(); i++) {
				char c = content.charAt(i);
				if (!Character.isWhitespace(c)) {
					if (word == null) {
						word = new StringBuilder();
						wordStart = i;
					}
					word.append(c);
					continue;
				}
				if (word == null)
					continue;
				else {
					if (isKeyWord(word))
						styleRange(wordStart, word.length());
					word = null;
					wordStart = -1;
				}
			}
			if (isKeyWord(word))
				styleRange(wordStart, word.length());
		}

		private boolean isKeyWord(StringBuilder word) {
			if (word == null || word.length() == 0)
				return false;
			String s = word.toString();
			for (String keyword : keywords) {
				if (s.equalsIgnoreCase(keyword))
					return true;
			}
			return false;
		}

		private void styleRange(int wordStart, int length) {
			StyleRange styleRange = new StyleRange();
			styleRange.start = wordStart;
			styleRange.length = length;
			styleRange.fontStyle = SWT.BOLD;
			styleRange.foreground = Colors.getColor(0, 0, 255);
			text.setStyleRange(styleRange);
		}
	}


}
