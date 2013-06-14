/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.openlca.core.application.Messages;
import org.openlca.ui.UI;

/**
 * Action for confirming a deletion
 * 
 * @author Sebastian Greve
 * 
 */
public abstract class DeleteWithQuestionAction extends Action {

	/**
	 * Creates the question dialog
	 * 
	 * @return the question dialog
	 */
	private MessageDialog createMessageDialog() {
		return new MessageDialog(UI.shell(), Messages.Common_Delete, null,
				Messages.DeleteWithQuestionAction_Text, MessageDialog.QUESTION,
				new String[] { Messages.NavigationView_YesButton,
						Messages.NavigationView_NoButton }, 1) {

			@Override
			protected void createButtonsForButtonBar(final Composite parent) {
				int i = 0;
				for (final String s : getButtonLabels()) {
					final Button b = createButton(parent, i, s,
							i == getDefaultButtonIndex());
					if (i == getDefaultButtonIndex()) {
						b.setFocus();
					}
					i++;
				}
			}

		};
	}

	/**
	 * The delete action
	 */
	protected abstract void delete();

	@Override
	public final void run() {
		final MessageDialog dialog = createMessageDialog();
		if (dialog.open() == 0) {
			delete();
		}
	}

}
