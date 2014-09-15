package org.openlca.app.devtools.sql;

import java.util.ArrayList;
import java.util.List;

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
import org.openlca.app.rcp.ImageType;
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
			List<String> statements = getStatements();
			List<String> results = new ArrayList<>();
			for (String st : statements) {
				String result = new SqlCommand().exec(st, Database.get());
				results.add(result);
			}
			if (results.size() == 1)
				resultText.setText(results.get(0));
			else {
				String text = "Executed " + results.size() + " statements:\n";
				int i = 1;
				for (String result : results) {
					text += "\n" + i + ". result: \n";
					text += org.openlca.util.Strings.cut(result, 1500);
					text += "\n";
					i++;
				}
				resultText.setText(text);
			}
		}

		private List<String> getStatements() {
			String statement = queryText.getText();
			List<String> statements = new ArrayList<>();
			if (!statement.contains(";"))
				statements.add(statement);
			else {
				String[] parts = statement.split(";");
				for (String part : parts) {
					if (part.trim().isEmpty())
						continue;
					statements.add(part);
				}
			}
			return statements;
		}
	}

	private class SyntaxStyler implements ModifyListener {

		private StyledText text;

		// complete SQL99 keywords, see
		// http://www.sql.org/sql-database/postgresql/manual/sql-keywords-appendix.html
		private String[] keywords = { "absolute", "action", "add", "admin",
				"after", "aggregate", "alias", "all", "allocate", "alter",
				"and", "any", "are", "array", "as", "asc", "assertion", "at",
				"authorization", "before", "begin", "binary", "bit", "blob",
				"boolean", "both", "breadth", "by", "call", "cascade",
				"cascaded", "case", "cast", "catalog", "char", "character",
				"check", "class", "clob", "close", "collate", "collation",
				"column", "commit", "completion", "connect", "connection",
				"constraint", "constraints", "constructor", "continue",
				"corresponding", "create", "cross", "cube", "current",
				"current_date", "current_path", "current_role", "current_time",
				"current_timestamp", "current_user", "cursor", "cycle", "data",
				"date", "day", "deallocate", "dec", "decimal", "declare",
				"default", "deferrable", "deferred", "delete", "depth",
				"deref", "desc", "describe", "descriptor", "destroy",
				"destructor", "deterministic", "diagnostics", "dictionary",
				"disconnect", "distinct", "domain", "double", "drop",
				"dynamic", "each", "else", "end", "end-exec", "equals",
				"escape", "every", "except", "exception", "exec", "execute",
				"external", "false", "fetch", "first", "float", "for",
				"foreign", "found", "free", "from", "full", "function",
				"general", "get", "global", "go", "goto", "grant", "group",
				"grouping", "having", "host", "hour", "identity", "ignore",
				"immediate", "in", "indicator", "initialize", "initially",
				"inner", "inout", "input", "insert", "int", "integer",
				"intersect", "interval", "into", "is", "isolation", "iterate",
				"join", "key", "language", "large", "last", "lateral",
				"leading", "left", "less", "level", "like", "limit", "local",
				"localtime", "localtimestamp", "locator", "map", "match",
				"minute", "modifies", "modify", "module", "month", "names",
				"national", "natural", "nchar", "nclob", "new", "next", "no",
				"none", "not", "null", "numeric", "object", "of", "off", "old",
				"on", "only", "open", "operation", "option", "or", "order",
				"ordinality", "out", "outer", "output", "pad", "parameter",
				"parameters", "partial", "path", "postfix", "precision",
				"prefix", "preorder", "prepare", "preserve", "primary",
				"prior", "privileges", "procedure", "public", "read", "reads",
				"real", "recursive", "ref", "references", "referencing",
				"relative", "restrict", "result", "return", "returns",
				"revoke", "right", "role", "rollback", "rollup", "routine",
				"row", "rows", "savepoint", "schema", "scope", "scroll",
				"search", "second", "section", "select", "sequence", "session",
				"session_user", "set", "sets", "size", "smallint", "some",
				"space", "specific", "specifictype", "sql", "sqlexception",
				"sqlstate", "sqlwarning", "start", "state", "statement",
				"static", "structure", "system_user", "table", "temporary",
				"terminate", "than", "then", "time", "timestamp",
				"timezone_hour", "timezone_minute", "to", "trailing",
				"transaction", "translation", "treat", "trigger", "true",
				"under", "union", "unique", "unknown", "unnest", "update",
				"usage", "user", "using", "value", "values", "varchar",
				"variable", "varying", "view", "when", "whenever", "where",
				"with", "without", "work", "write", "year", "zone" };

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
					setWordStyle(word, wordStart);
					word = null;
					wordStart = -1;
				}
			}
			setWordStyle(word, wordStart);
		}

		private void setWordStyle(StringBuilder word, int wordStart) {
			if (word == null)
				return;
			if (isKeyWord(word))
				setKeywordStyle(wordStart, word.length());
			else
				setDefaultStyle(wordStart, word.length());
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

		private void setKeywordStyle(int wordStart, int length) {
			StyleRange styleRange = new StyleRange();
			styleRange.start = wordStart;
			styleRange.length = length;
			styleRange.fontStyle = SWT.BOLD;
			styleRange.foreground = Colors.getColor(0, 0, 255);
			text.setStyleRange(styleRange);
		}

		private void setDefaultStyle(int wordStart, int length) {
			StyleRange styleRange = new StyleRange();
			styleRange.start = wordStart;
			styleRange.length = length;
			text.setStyleRange(styleRange);
		}
	}

}
