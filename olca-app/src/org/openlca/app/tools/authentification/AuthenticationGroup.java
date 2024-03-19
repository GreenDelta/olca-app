package org.openlca.app.tools.authentification;

import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;

public class AuthenticationGroup {

	private Runnable onChange;
	private boolean withAnonymousOption;
	private boolean withUser;
	private boolean withPassword;
	private boolean withToken;
	private boolean anonymous;
	private String user = "";
	private String password = "";
	private String token = "";
	private Text userText;
	private Text passwordText;
	private Text tokenText;

	public AuthenticationGroup() {
	}

	public AuthenticationGroup withAnonymousOption() {
		withAnonymousOption = true;
		return this;
	}

	public AuthenticationGroup withUser() {
		return withUser("");
	}

	public AuthenticationGroup withUser(String initialValue) {
		withUser = true;
		this.user = initialValue;
		return this;
	}

	public AuthenticationGroup withPassword() {
		return withPassword("");
	}

	public AuthenticationGroup withPassword(String initialValue) {
		withPassword = true;
		this.password = initialValue;
		return this;
	}

	public AuthenticationGroup withToken() {
		withToken = true;
		return this;
	}

	public AuthenticationGroup onChange(Runnable onChange) {
		this.onChange = onChange;
		return this;
	}

	public AuthenticationGroup render(Composite parent, FormToolkit tk, int flags) {
		return render(parent, tk, flags, M.User);
	}

	public AuthenticationGroup render(Composite parent, FormToolkit tk, int flags, String userLabel) {
		var autoFocus = (flags & SWT.FOCUSED) != 0;
		var group = UI.group(parent, tk);
		group.setText(M.Authentication);
		UI.gridLayout(group, 1);
		UI.gridData(group, true, false);
		if (withAnonymousOption) {
			UI.radioGroup(group, tk, new String[] { "Authenticated", "Anonymous" }, selected -> {
				anonymous = selected == 1;
				updateDisabled(userText);
				updateDisabled(passwordText);
				updateDisabled(tokenText);
				if (onChange != null) {
					onChange.run();
				}
			});
		}
		var container = UI.composite(group, tk);
		UI.gridLayout(container, 2);
		UI.gridData(container, true, false);
		if (withUser) {
			userText = createText(container, tk, SWT.NONE, userLabel, user, text -> this.user = text);
			if (autoFocus && Strings.nullOrEmpty(user)) {
				userText.setFocus();
			}
		}
		if (withPassword) {
			passwordText = createText(container, tk, SWT.PASSWORD, M.Password, password, text -> this.password = text);
			if (autoFocus && !Strings.nullOrEmpty(user) && Strings.nullOrEmpty(password)) {
				passwordText.setFocus();
			}
		}
		if (withToken) {
			tokenText = createText(container, tk, SWT.NONE, "Token", token, text -> this.token = text);
			if (autoFocus && !Strings.nullOrEmpty(user) && !Strings.nullOrEmpty(password)) {
				tokenText.setFocus();
			}
		}
		return this;
	}

	private void updateDisabled(Text text) {
		if (text == null)
			return;
		text.setEnabled(!anonymous);
	}

	private Text createText(Composite parent, FormToolkit tk, int flags,
			String label, String initialValue, Consumer<String> process) {
		var text = UI.labeledText(parent, tk, label, flags);
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

	public boolean anonymous() {
		return anonymous;
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

	public boolean isComplete() {
		if (withUser && Strings.nullOrEmpty(user))
			return false;
		if (withPassword && Strings.nullOrEmpty(password))
			return false;
		return true;
	}

}
