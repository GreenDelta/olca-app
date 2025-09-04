package org.openlca.app.devtools.agent;

import java.io.File;
import java.util.UUID;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.devtools.ScriptingEditor;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.logging.Console;
import org.openlca.app.preferences.Theme;
import org.openlca.app.rcp.HtmlFolder;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;

public class AgentEditor extends ScriptingEditor {

	private Page page;

	public static void open() {
		var id = UUID.randomUUID() + "_new";
		var input = new SimpleEditorInput(id, "Agent");
		Editors.open(input, "AgentEditor");
	}

	public static void open(File file) {
		if (file == null || !file.exists())
			return;
		var id = file.getAbsolutePath();
		var input = new SimpleEditorInput(id, "Agent");
		Editors.open(input, "AgentEditor");
	}

	@Override
	public void eval() {
		// For the agent, we don't have a traditional "eval" like Python
		// Instead, we could trigger a chat action or refresh the interface
		Console.show();
		App.run("Agent Action", () -> {
			// This could trigger specific agent actions
			// For now, just log that the action was triggered
			System.out.println("Agent action triggered");
		});
	}

	@Override
	protected FormPage getPage() {
		setTitleImage(Icon.PYTHON.get()); // Using Python icon for now, could create a custom agent icon
		return page = new Page();
	}

	private class Page extends FormPage {

		private Browser browser;

		public Page() {
			super(AgentEditor.this, "AgentEditorPage", "Agent");
		}

		@Override
		protected void createFormContent(IManagedForm mForm) {
			var form = UI.header(mForm, getTitle(), Icon.PYTHON.get());
			var tk = mForm.getToolkit();
			var body = UI.body(form, tk);
			body.setLayout(new FillLayout());
			try {
				browser = new Browser(body, SWT.NONE);
				browser.setJavascriptEnabled(true);

				// initialize the agent interface
				UI.onLoaded(browser, HtmlFolder.getUrl("agent.html"), () -> {

					browser.getDisplay();
					// set the theme - default to dark mode
					if (Theme.isDark()) {
						browser.execute("window.setTheme(true)");
					} else {
						browser.execute("window.setTheme(true)"); // Default to dark mode
					}

					// add the _onChange listener for chat messages
					UI.bindFunction(browser, "_onChange", (args) -> {
						if (args == null || args.length == 0)
							return null;
						var arg = args[0] == null
								? null
								: args[0].toString();
						if (arg != null) {
							// Handle chat message changes
							// This could be used to save chat history or trigger actions
							setDirty();
						}
						return null;
					});

					// add the _onSave listener, called when Ctrl+s is pressed
					UI.bindFunction(browser, "_onSave", (args) -> {
						var editor = AgentEditor.this;
						if (!editor.isDirty()) {
							if (editor.file == null) {
								editor.doSaveAs();
							}
							return null;
						}
						var progress = new ProgressMonitorDialog(UI.shell());
						progress.setOpenOnRun(true);
						editor.doSave(progress.getProgressMonitor());
						progress.close();
						return null;
					});

					// add the _onChatMessage listener for handling chat interactions
					UI.bindFunction(browser, "_onChatMessage", (args) -> {
						if (args == null || args.length == 0)
							return null;
						var message = args[0] == null ? null : args[0].toString();
						if (message != null) {
							// Handle chat message - this could integrate with AI services
							handleChatMessage(message);
						}
						return null;
					});

					// add the _onClearChat listener
					UI.bindFunction(browser, "_onClearChat", (args) -> {
						// Clear chat history
						browser.execute("window.clearChat()");
						setDirty();
						return null;
					});
				});

			} catch (Exception e) {
				ErrorReporter.on("failed to create browser in Agent editor", e);
			}
		}

		/**
		 * Handle chat messages from the React interface
		 */
		private void handleChatMessage(String message) {
			// This is where you would integrate with AI services
			// For now, just log the message
			System.out.println("Agent received message: " + message);
			
			// You could:
			// 1. Send to an AI service
			// 2. Process LCA-specific queries
			// 3. Integrate with openLCA's data and APIs
			// 4. Return responses back to the React interface
		}

		/**
		 * Get the current chat messages from the React interface
		 */
		public String getChatMessages() {
			try {
				var messages = browser.evaluate("return JSON.stringify(window.getMessages());");
				return messages != null ? messages.toString() : "[]";
			} catch (Exception e) {
				ErrorReporter.on("failed to get chat messages", e);
				return "[]";
			}
		}

		/**
		 * Set chat messages in the React interface
		 */
		public void setChatMessages(String messagesJson) {
			try {
				browser.execute("window.setMessages(" + messagesJson + ")");
			} catch (Exception e) {
				ErrorReporter.on("failed to set chat messages", e);
			}
		}

		/**
		 * Clear the chat interface
		 */
		public void clearChat() {
			try {
				browser.execute("window.clearChat()");
			} catch (Exception e) {
				ErrorReporter.on("failed to clear chat", e);
			}
		}
	}

	@Override
	protected void doSave(org.eclipse.core.runtime.IProgressMonitor monitor) {
		// Save chat history or agent configuration
		if (page != null) {
			var messages = page.getChatMessages();
			// Save the messages to the file or database
			// For now, just mark as saved
			setDirty(false);
		}
	}

	@Override
	protected void doSaveAs() {
		// Implement save as functionality for agent configurations
		// This could save chat history, agent settings, etc.
		super.doSaveAs();
	}
}
