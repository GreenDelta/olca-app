/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.model.ConnectionLink;

public class DeleteLinkCommand extends Command {

	private ConnectionLink link;
	private boolean linkWasVisible = false;

	DeleteLinkCommand() {
	}

	@Override
	public boolean canExecute() {
		return link != null;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		linkWasVisible = link.isVisible();
		this.link.unlink();
		link.getSourceNode().getParent().getProductSystem().getProcessLinks()
				.remove(link.getProcessLink());
	}

	@Override
	public String getLabel() {
		return Messages.Systems_ProcessLinkDeleteCommand_Text;
	}

	@Override
	public void redo() {
		link.getSourceNode().getParent().getProductSystem().getProcessLinks()
				.remove(link.getProcessLink());
		this.link.unlink();
	}

	@Override
	public void undo() {
		link.getSourceNode().getParent().getProductSystem().getProcessLinks()
				.add(link.getProcessLink());
		link.link();
		link.setVisible(linkWasVisible);
	}

	void setLink(ConnectionLink link) {
		this.link = link;
	}

}
