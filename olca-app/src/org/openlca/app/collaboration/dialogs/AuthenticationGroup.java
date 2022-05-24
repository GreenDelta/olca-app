package org.openlca.app.collaboration.dialogs;

import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.openlca.app.util.UI;

class AuthenticationGroup {

	private String user;
	private String password;
	private String token;
	private Runnable onChange;

	private AuthenticationGroup(Runnable onChange) {
		this.onChange = onChange;
	}

	static AuthenticationGroup create(Composite parent, Runnable onChange) {
		return new AuthenticationGroup(onChange).render(parent);
	}

	private AuthenticationGroup render(Composite parent) {
		var group = new Group(parent, SWT.NONE);
		group.setText("Authentication");
		UI.gridLayout(group, 2);
		UI.gridData(group, true, false);
		createText(group, SWT.NONE, "User:", text -> this.user = text);
		createText(group, SWT.PASSWORD, "Password:", text -> this.password = text);
		createText(group, SWT.NONE, "(Optional) Token:", text -> this.token = text);
		return this;
	}

	private void createText(Composite parent, int flags, String label, Consumer<String> process) {
		var text = UI.formText(parent, label, flags);
		text.addModifyListener(e -> {
			process.accept(text.getText());
			if (onChange != null) {
				onChange.run();
			}
		});
	}

	public String user() {
		return user;
	}

	public String password() {
		return password;
	}

	public String token() {
		return token;
	}

}
