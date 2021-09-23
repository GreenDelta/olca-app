package org.openlca.app.devtools;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
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
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.ipc.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpcDialog extends FormDialog {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private Text portText;
	private Button button;
	private Label statusLabel;
	private Button grpcCheck;

	private Server server;
	private org.openlca.proto.io.server.Server grpcServer;

	public static int show() {
		if (Database.get() == null) {
			MsgBox.error(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return Window.CANCEL;
		}
		return new IpcDialog().open();
	}

	public IpcDialog() {
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
		newShell.setText(M.StartIPCServer);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 300);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.formBody(mForm.getForm(), tk);

		// port text
		var comp = tk.createComposite(body);
		UI.gridData(comp, true, false);
		UI.gridLayout(comp, 3);
		tk.createLabel(comp, M.Port);
		portText = tk.createText(comp, "8080", SWT.NONE);
		UI.gridData(portText, true, false);

		// start-stop button
		button = tk.createButton(comp, "", SWT.NONE);
		button.setImage(Icon.RUN.get());
		Controls.onSelect(button, e -> {
			if (server == null && grpcServer == null) {
				onStart();
			} else {
				onStop();
			}
		});
		UI.filler(comp, tk);
		grpcCheck = tk.createButton(comp,
				"Start as gRPC service (experimental)",
				SWT.CHECK);

		// status text and grpc check
		var statComp = tk.createComposite(body);
		UI.gridData(statComp, true, true);
		UI.gridLayout(statComp, 1);
		statusLabel = tk.createLabel(statComp, M.StartIPCInfo, SWT.WRAP);
		UI.gridData(statusLabel, true, true);

	}

	private void onStart() {
		try {
			int port = Integer.parseInt(portText.getText());
			var db = Database.get();
			var grpc = grpcCheck.getSelection();
			App.run(
					"Start server ...",
					() -> {
						if (grpc) {
							grpcServer = new org.openlca.proto.io.server.Server(db, port);
							new Thread(() -> grpcServer.start()).start();
						} else {
							server = new Server(port)
									.withDefaultHandlers(db, App.getSolver());
							server.start();
						}
					},
					() -> {
						log.info("Started IPC server @{}", port);
						portText.setEnabled(false);
						var image = PlatformUI.getWorkbench()
								.getSharedImages()
								.getImage(ISharedImages.IMG_ELCL_STOP);
						button.setImage(image);
						grpcCheck.setEnabled(false);
						statusLabel.setText(M.StopIPCInfo);
						statusLabel.getParent().requestLayout();
					});
		} catch (Exception e) {
			MsgBox.error("Failed to start the IPC server",
					e.getMessage());
			if (server != null && server.isAlive()) {
				try {
					server.stop();
				} catch (Exception ex) {
					log.error("Failed to stop IPC server", ex);
				}
			}
			server = null;
		}
	}

	private void onStop() {
		try {
			App.run(
					"Stop server ...",
					() -> {
						if (server != null) {
							server.stop();
						}
						if (grpcServer != null) {
							grpcServer.stop();
						}
						server = null;
						grpcServer = null;
					},
					() -> {
						log.info("Stopped IPC server");
						portText.setEnabled(true);
						button.setImage(Icon.RUN.get());
						grpcCheck.setEnabled(true);
						statusLabel.setText(M.StartIPCInfo);
						statusLabel.getParent().requestLayout();
					});
		} catch (Exception e) {
			MsgBox.error("Failed to stop the IPC server",
					e.getMessage());
		}
	}

	@Override
	public boolean close() {
		try {
			if (server != null && server.isAlive()) {
				server.stop();
			}
			if (grpcServer != null) {
				grpcServer.stop();
			}
			log.info("stopped the IPC server");
		} catch (Exception e) {
			log.error("Failed to stop IPC server", e);
		}

		return super.close();
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		var btn = createButton(parent, IDialogConstants.CLOSE_ID,
				IDialogConstants.CLOSE_LABEL, false);
		Controls.onSelect(btn, e -> this.close());
	}

}
