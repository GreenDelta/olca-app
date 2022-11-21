/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.openlca.app.tools.graphics.tools;

import org.eclipse.gef.tools.SelectionTool;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Cursor;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.geometry.Point;

import org.eclipse.gef.SharedCursors;
import org.openlca.app.tools.graphics.edit.RootEditPart;


/**
 * A subclass of the SelectionTool that allows panning by holding down the space
 * bar.
 */
public class PanningSelectionTool extends SelectionTool {

	private boolean isSpaceBarDown = false;
	private Point viewLocation;

	/**
	 * The state to indicate that the first button has been pressed on the
	 * background but no drag has been initiated.
	 */
	protected static final int PAN = SelectionTool.MAX_STATE << 1;

	/**
	 * The state to indicate that the space bar has been pressed but no drag has
	 * been initiated.
	 */
	protected static final int SPACE_PAN = PAN << 1;


	/**
	 * The state to indicate that a pan is in progress.
	 */
	protected static final int PAN_IN_PROGRESS = SPACE_PAN << 1;

	/**
	 * Max state
	 */
	protected static final int MAX_STATE = PAN_IN_PROGRESS;

	/**
	 * Returns <code>true</code> if spacebar condition was accepted.
	 *
	 * @param e the key event
	 * @return true if the space bar was the key event.
	 */
	protected boolean acceptSpaceBar(KeyEvent e) {
		return (e.character == ' ' && (e.stateMask & SWT.MODIFIER_MASK) == 0);
	}

	@Override
	protected String getDebugName() {
		return "Panning Tool";//$NON-NLS-1$
	}

	@Override
	protected String getDebugNameForState(int state) {
		if (state == PAN)
			return "Pan Initial";
		else if (state == SPACE_PAN)
			return "Space pan Initial";
		else if (state == PAN_IN_PROGRESS)
			return "Pan In Progress";
		return super.getDebugNameForState(state);
	}

	@Override
	protected Cursor getDefaultCursor() {
		if (isInState(PAN | SPACE_PAN | PAN_IN_PROGRESS))
			return SharedCursors.HAND;
		return super.getDefaultCursor();
	}

	@Override
	protected boolean handleButtonDown(int button) {
		if (button == 1
				&& getCurrentViewer().getControl() instanceof FigureCanvas) {
			// The button is down on the RootEditPart (background).
			if (getTargetEditPart() instanceof RootEditPart
					&& !getCurrentInput().isControlKeyDown()
					&& stateTransition(STATE_INITIAL, PAN)) {
				viewLocation = ((FigureCanvas) getCurrentViewer().getControl())
						.getViewport().getViewLocation();
				return true;
			}

			// The button is down while the space bar is pressed.
			if (stateTransition(SPACE_PAN, PAN_IN_PROGRESS)) {
				viewLocation = ((FigureCanvas) getCurrentViewer().getControl())
						.getViewport().getViewLocation();
				return true;
			}
		}
		return super.handleButtonDown(button);
	}

	@Override
	protected boolean handleButtonUp(int button) {
		if (button == 1 && isSpaceBarDown
				&& stateTransition(PAN_IN_PROGRESS, SPACE_PAN))
			return true;
		else if (button == 1 && stateTransition(PAN_IN_PROGRESS, STATE_INITIAL)) {
			refreshCursor();
			return true;
		} else if (button == 1 && stateTransition(PAN, STATE_INITIAL)) {
			getCurrentViewer().setSelection(new StructuredSelection());
		}

		return super.handleButtonUp(button);
	}

	@Override
	protected boolean handleDrag() {
		if (isInState(PAN_IN_PROGRESS)
				&& getCurrentViewer().getControl() instanceof FigureCanvas canvas) {
			canvas.scrollTo(viewLocation.x - getDragMoveDelta().width,
					viewLocation.y - getDragMoveDelta().height);
			return true;
		} else if (stateTransition(PAN, PAN_IN_PROGRESS)
				&& getCurrentViewer().getControl() instanceof FigureCanvas) {
			refreshCursor();
			return true;
		} else return super.handleDrag();
	}

	@Override
	protected boolean handleFocusLost() {
		if (isInState(SPACE_PAN | PAN_IN_PROGRESS)) {
			setState(STATE_INITIAL);
			refreshCursor();
			return true;
		}
		return super.handleFocusLost();
	}

	@Override
	protected boolean handleKeyDown(KeyEvent e) {
		if (acceptSpaceBar(e)) {
			isSpaceBarDown = true;
			if (stateTransition(STATE_INITIAL, SPACE_PAN))
				refreshCursor();
			return true;
		} else {
			if (stateTransition(SPACE_PAN, STATE_INITIAL)) {
				refreshCursor();
				isSpaceBarDown = false;
				return true;
			} else if (isInState(PAN_IN_PROGRESS))
				isSpaceBarDown = false;
		}

		return super.handleKeyDown(e);
	}

	@Override
	protected boolean handleKeyUp(KeyEvent e) {
		if (acceptSpaceBar(e)) {
			isSpaceBarDown = false;
			if (stateTransition(SPACE_PAN, STATE_INITIAL))
				refreshCursor();
			return true;
		}

		return super.handleKeyUp(e);
	}

}
