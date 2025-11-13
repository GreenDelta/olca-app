package org.openlca.app.components;

import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.util.UI;
import org.openlca.commons.Strings;

public class AuthenticationGroup extends Composite {

	private final FormToolkit toolkit;
	private final boolean autoFocus;
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

	public AuthenticationGroup(Composite parent, FormToolkit toolkit, int flags) {
		super(parent, SWT.NONE);
		this.toolkit = toolkit;
		this.autoFocus = (flags & SWT.FOCUSED) != 0;
		UI.gridLayout(this, 1, 0, 0);
		UI.gridData(this, true, false);
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

	public void render() {
		var group = UI.group(this, toolkit);
		group.setText(M.Authentication);
		UI.gridLayout(group, 1);
		UI.gridData(group, true, false);
		if (withAnonymousOption) {
			UI.radioGroup(group, toolkit, new String[] { M.Authenticated, M.Anonymous }, selected -> {
				anonymous = selected == 1;
				updateDisabled(userText);
				updateDisabled(passwordText);
				updateDisabled(tokenText);
				if (onChange != null) {
					onChange.run();
				}
			});
		}
		var container = UI.composite(group, toolkit);
		UI.gridLayout(container, 2);
		UI.gridData(container, true, false);
		if (withUser) {
			userText = createText(container, toolkit, SWT.NONE, M.EmailOrUsername, user, text -> this.user = text);
			if (autoFocus && Strings.isBlank(user)) {
				userText.setFocus();
			}
		}
		if (withPassword) {
			passwordText = createText(container, toolkit, SWT.PASSWORD | SWT.BORDER, M.Password, password,
					text -> this.password = text);
			if (autoFocus && Strings.isNotBlank(user) && Strings.isBlank(password)) {
				passwordText.setFocus();
			}
		}
		if (withToken) {
			tokenText = createText(container, toolkit, SWT.NONE, M.Token, token, text -> this.token = text);
			if (autoFocus && Strings.isNotBlank(user) && Strings.isNotBlank(password)) {
				tokenText.setFocus();
			}
		}
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

	public void user(String user) {
		if (userText == null)
			return;
		if (Objects.equals(user, userText.getText()))
			return;
		userText.setText(user);
	}

	public String user() {
		return user;
	}

	public String password() {
		return password;
	}

	public void password(String password) {
		if (passwordText == null)
			return;
		if (Objects.equals(password, passwordText.getText()))
			return;
		passwordText.setText(password);
	}

	public String token() {
		return token;
	}

	public boolean isComplete() {
		if (withUser && Strings.isBlank(user))
			return false;
		if (withPassword && Strings.isBlank(password))
			return false;
		return true;
	}

}
