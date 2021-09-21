package org.openlca.app.devtools.sql;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.devtools.ScriptingEditor;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Colors;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.python.google.common.base.Strings;

public class SqlEditor extends ScriptingEditor {

	private Page page;

	public static void open() {
		var id = UUID.randomUUID() + "_new";
		var input = new SimpleEditorInput(id, "SQL");
		Editors.open(input, "SqlEditor");
	}

	public static void open(File file) {
		if (file == null || !file.exists())
			return;
		var id = file.getAbsolutePath();
		var input = new SimpleEditorInput(id, "SQL");
		Editors.open(input, "SqlEditor");
	}

	@Override
	public void eval() {
		page.runAction.run();
	}

	@Override
	protected FormPage getPage() {
		return page = new Page();
	}

	public void clearResults() {
		page.resultText.setText("");
	}

	private class Page extends FormPage {

		private Text resultText;
		private StyledText queryText;
		private RunAction runAction;

		public Page() {
			super(SqlEditor.this, "SqlEditorPage", "SQL Query Browser");
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var form = UI.formHeader(mform, "SQL Query Browser", Icon.SQL.get());
			var tk = mform.getToolkit();
			var body = UI.formBody(form, tk);
			createStatementSection(body, tk);
			createResultSection(body, tk);
		}

		private void createStatementSection(Composite body, FormToolkit toolkit) {
			Section section = UI.section(body, toolkit, "SQL Statement");
			Composite composite = UI.sectionClient(section, toolkit, 1);
			queryText = new StyledText(composite, SWT.BORDER);
			toolkit.adapt(queryText);
			UI.gridData(queryText, true, false).heightHint = 150;
			queryText.setText(script == null ? "" : script);
			var styler = new SyntaxStyler(queryText);
			styler.styleIt();
			queryText.addModifyListener($ -> {
				styler.styleIt();
				script = queryText.getText();
				setDirty();
			});

			// bind actions
			runAction = new RunAction();
			var saveAs = Actions.create(
				M.SaveAs, Icon.SAVE_AS.descriptor(), () -> getEditor().doSaveAs());
			Actions.bind(section, runAction, saveAs);
		}

		private void createResultSection(Composite body, FormToolkit toolkit) {
			Section section = UI.section(body, toolkit, "Results");
			UI.gridData(section, true, true);
			Composite composite = UI.sectionClient(section, toolkit, 1);
			composite.setLayout(new FillLayout());
			resultText = toolkit.createText(composite, null, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		}

		private class RunAction extends Action {

			RunAction() {
				setToolTipText("Run SQL statement");
				setText("Run SQL statement");
				setImageDescriptor(Icon.RUN.descriptor());
			}

			@Override
			public void run() {
				if (Database.get() == null) {
					MsgBox.error(M.NoDatabaseOpened, M.NeedOpenDatabase);
					return;
				}
				var statements = getStatements();
				var results = new ArrayList<String>();
				for (String st : statements) {
					var result = new SqlCommand().exec(st, Database.get());
					results.add(result);
				}
				if (results.size() == 1)
					resultText.setText(results.get(0));
				else {
					var buff = new StringBuilder();
					buff.append("Executed ")
							.append(results.size())
							.append(" statements:\n");
					int i = 1;
					for (String result : results) {
						buff.append('\n')
								.append(i)
								.append(". result: \n")
								.append(org.openlca.util.Strings.cut(result, 1500))
								.append('\n');
						i++;
					}
					resultText.setText(buff.toString());
				}
			}

			private List<String> getStatements() {
				var text = queryText.getText();
				if (Strings.isNullOrEmpty(text))
					return Collections.emptyList();
				var statements = new ArrayList<String>();
				boolean inQuote = false;
				var buff = new StringBuilder();
				for (char c : text.toCharArray()) {

					if (c == '\'') {
						inQuote = !inQuote;
						buff.append(c);
						continue;
					}

					if (c == ';' && !inQuote) {
						var next = buff.toString().trim();
						if (!next.isEmpty()) {
							statements.add(next);
						}
						buff.setLength(0);
						continue;
					}

					if (Character.isWhitespace(c) && buff.length() > 0) {
						buff.append(' ');
					} else {
						buff.append(c);
					}
				}
				if (buff.length() > 0) {
					var next = buff.toString().trim();
					if (!next.isEmpty()) {
						statements.add(next);
					}
				}
				return statements;
			}
		}

		private class SyntaxStyler {

			private final StyledText text;

			// complete SQL99 keywords, see
			// http://www.sql.org/sql-database/postgresql/manual/sql-keywords-appendix.html
			private final String[] keywords = {"absolute", "action", "add", "admin",
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
					"with", "without", "work", "write", "year", "zone"};

			SyntaxStyler(StyledText text) {
				this.text = text;
			}

			void styleIt() {
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
					if (word != null) {
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
				styleRange.foreground = Colors.get(0, 0, 255);
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

}
