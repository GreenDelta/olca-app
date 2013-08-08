package org.openlca.core.editors;

/**
 * A part of an editor that gets notified about editor events.
 */
public interface IEditorComponent {

	/**
	 * Notification by the editor that the user saved the editor content.
	 */
	void onSaved();

	/**
	 * Notification by the editor that the content changed.
	 */
	void onChange();

}
