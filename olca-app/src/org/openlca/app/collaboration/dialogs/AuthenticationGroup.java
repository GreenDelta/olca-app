package org.openlca.app.collaboration.dialogs;

import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;

class AuthenticationGroup {

	private Runnable onChange;
	private boolean withUser;
	private boolean withPassword;
	private boolean withToken;
	private String user = "";
	private String password = "";
	private String token = "";

	AuthenticationGroup() {
	}

	AuthenticationGroup withUser() {
		return withUser("");
	}

	AuthenticationGroup withUser(String initialValue) {
		withUser = true;
		this.user = initialValue;
		return this;
	}

	AuthenticationGroup withPassword() {
		return withPassword("");
	}

	AuthenticationGroup withPassword(String initialValue) {
		withPassword = true;
		this.password = initialValue;
		return this;
	}

	AuthenticationGroup withToken() {
		withToken = true;
		return this;
	}

	AuthenticationGroup onChange(Runnable onChange) {
		this.onChange = onChange;
		return this;
	}

	AuthenticationGroup render(Composite parent, int flags) {
		var autoFocus = (flags & SWT.FOCUSED) != 0;
		var group = new Group(parent, SWT.NONE);
		group.setText("Authentication");
		UI.gridLayout(group, 2);
		UI.gridData(group, true, false);
		if (withUser) {
			var t = createText(group, SWT.NONE, "User:", user, text -> this.user = text);
			if (autoFocus && Strings.nullOrEmpty(user)) {
				t.setFocus();
			}
		}
		if (withPassword) {
			var t = createText(group, SWT.PASSWORD, "Password:", password, text -> this.password = text);
			if (autoFocus && !Strings.nullOrEmpty(user) && Strings.nullOrEmpty(password)) {
				t.setFocus();
			}
		}
		if (withToken) {
			var t = createText(group, SWT.NONE, "Token:", token, text -> this.token = text);
			if (autoFocus && !Strings.nullOrEmpty(user) && !Strings.nullOrEmpty(password)) {
				t.setFocus();
			}
		}
		return this;
	}

	private Text createText(Composite parent, int flags, String label, String initialValue, Consumer<String> process) {
		var text = UI.formText(parent, label, flags);
		if (initialValue != null) {
			text.setText(initialValue);
		}
		text.addModifyListener(e -> {
			process.accept(text.getText());
			if (onChange != null) {
				onChange.run();
			}
		});
		return text;
	}

	String user() {
		return user;
	}

	String password() {
		return password;
	}

	String token() {
		return token;
	}

	boolean isComplete() {
		if (withUser && Strings.nullOrEmpty(user))
			return false;
		if (withPassword && Strings.nullOrEmpty(password))
			return false;
		return true;
	}

}
