package org.openlca.app.tools.mcp;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.mcp.McpService.ToolCall;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;

/// A dialog for starting and stopping the MCP server. In contrast to the IPC
/// dialog, the server keeps running when the dialog is closed; when the
/// dialog is opened again, it shows the state of the running server.
public class McpDialog extends FormDialog {

	private static final int DEFAULT_PORT = 8090;
	private static final DateTimeFormatter TIME = DateTimeFormatter
			.ofPattern("HH:mm:ss")
			.withZone(ZoneId.systemDefault());

	private Text portText;
	private Button button;
	private Text urlText;
	private Label statusLabel;
	private TableViewer callTable;

	private final List<ToolCall> calls = new ArrayList<>();
	private final Consumer<ToolCall> callListener = this::onToolCall;
	private McpService service;
	private Display display;

	public static int show() {
		if (Database.get() == null) {
			MsgBox.error(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return Window.CANCEL;
		}
		return new McpDialog().open();
	}

	private McpDialog() {
		super(UI.shell());
		setBlockOnOpen(false);
		setShellStyle(SWT.CLOSE
				| SWT.MODELESS
				| SWT.BORDER
				| SWT.TITLE
				| SWT.RESIZE
				| SWT.MIN);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(M.McpServer);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(650, 600);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.dialogBody(mForm.getForm(), tk);
		display = body.getDisplay();

		// port & start/stop button
		var comp = UI.composite(body, tk);
		UI.gridData(comp, true, false);
		UI.gridLayout(comp, 3);
		portText = UI.labeledText(comp, tk, M.Port);
		portText.setText(Integer.toString(DEFAULT_PORT));
		UI.gridData(portText, true, false);
		button = UI.button(comp, tk, "");
		Controls.onSelect(button, _ -> {
			if (service == null) {
				onStart();
			} else {
				onStop();
			}
		});

		// URL
		urlText = UI.labeledText(comp, tk, M.URL, SWT.READ_ONLY);
		UI.filler(comp, tk);

		// status
		statusLabel = UI.label(body, tk, "", SWT.WRAP);
		UI.gridData(statusLabel, true, false).widthHint = 500;

		// tool calls; the table fills the remaining space of the dialog and
		// scrolls itself, instead of growing the form
		UI.label(body, tk, M.ToolCalls);
		callTable = Tables.createViewer(body,
				new String[]{M.Time, M.Name, M.Status},
				new CallLabel());
		UI.stretchXY(callTable.getTable());
		Tables.bindColumnWidths(callTable, 0.2, 0.5, 0.3);
		callTable.setInput(calls);

		// show the state of an already running server
		service = McpService.current().orElse(null);
		if (service != null) {
			portText.setText(Integer.toString(service.port()));
			calls.addAll(service.history().reversed());
			callTable.refresh();
			service.addListener(callListener);
		}
		updateState();
	}

	private void onStart() {
		int port;
		try {
			port = Integer.parseInt(portText.getText().strip());
		} catch (NumberFormatException e) {
			MsgBox.error(M.FailedToStartMcpServer,
					"Invalid port: " + portText.getText());
			return;
		}
		var db = Database.get();
		if (db == null) {
			MsgBox.error(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		var sync = new AppDataSync(display);
		var error = new Exception[1];
		App.exec(M.StartServerDots, () -> {
			try {
				service = McpService.start(db, port, sync);
			} catch (Exception e) {
				error[0] = e;
			}
		});
		if (error[0] != null || service == null) {
			MsgBox.error(M.FailedToStartMcpServer, error[0]);
			service = null;
			return;
		}
		calls.clear();
		callTable.refresh();
		service.addListener(callListener);
		updateState();
	}

	private void onStop() {
		if (service != null) {
			service.removeListener(callListener);
		}
		try {
			App.exec(M.StopServerDots, McpService::stopCurrent);
		} catch (Exception e) {
			MsgBox.error(M.FailedToStopMcpServer, e);
		}
		service = null;
		updateState();
	}

	/// Called from the server threads; dispatches the update to the UI thread.
	private void onToolCall(ToolCall call) {
		if (display == null || display.isDisposed())
			return;
		display.asyncExec(() -> {
			if (callTable.getTable().isDisposed())
				return;
			calls.addFirst(call);
			if (calls.size() > 200) {
				calls.removeLast();
			}
			callTable.refresh();
		});
	}

	private void updateState() {
		boolean running = service != null;
		portText.setEnabled(!running);
		button.setImage(running
				? PlatformUI.getWorkbench()
				.getSharedImages()
				.getImage(ISharedImages.IMG_ELCL_STOP)
				: Icon.RUN.get());
		statusLabel.setText(running ? M.StopMcpInfo : M.StartMcpInfo);
		urlText.setText(running ? service.url() : "");
		statusLabel.getParent().layout(true, true);
	}

	@Override
	public boolean close() {
		// the server keeps running; we only stop listening for tool calls
		if (service != null) {
			service.removeListener(callListener);
		}
		return super.close();
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		var btn = createButton(parent, IDialogConstants.CLOSE_ID,
				IDialogConstants.CLOSE_LABEL, false);
		Controls.onSelect(btn, _ -> this.close());
	}

	private static class CallLabel extends LabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (col != 2 || !(obj instanceof ToolCall call))
				return null;
			return call.isError()
					? Icon.ERROR.get()
					: Icon.ACCEPT.get();
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ToolCall call))
				return null;
			return switch (col) {
				case 0 -> TIME.format(call.time());
				case 1 -> call.tool();
				case 2 -> (call.isError() ? M.Error : M.OK)
						+ " (" + call.millis() + " ms)";
				default -> null;
			};
		}
	}
}
