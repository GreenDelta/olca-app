package org.openlca.app.editors.graphical.requests;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.Request;
import org.eclipse.gef.requests.DropRequest;
import org.openlca.core.model.descriptors.RootDescriptor;

import java.util.ArrayList;
import java.util.List;

public class GraphRequest extends Request implements DropRequest {

	private List<RootDescriptor> descriptors;
	private Point mouseLocation;
	private Dimension size;

	public GraphRequest(Object type) {
		setType(type);
	}

	@Override
	public Point getLocation() {
		return mouseLocation;
	}

	/**
	 * Sets the location of the mouse pointer.
	 *
	 * @param p
	 *            The location of the mouse pointer.
	 */
	public void setLocation(Point p) {
		mouseLocation = p;
	}

	/**
	 * Returns a List containing the descriptors of this GraphRequest.
	 *
	 * @return A List containing the descriptors of this GraphRequest.
	 */
	public List<RootDescriptor> getDescriptors() {
		return descriptors;
	}

	/**
	 * Sets the RootDescriptor of this Request to the given List.
	 *
	 * @param list
	 *            The List of RootDescriptor.
	 */
	public void setDescriptors(List<RootDescriptor> list) {
		descriptors = list;
	}

	/**
	 * A helper method to set the given descriptors as the only descriptor.
	 *
	 * @param descriptor
	 *            The EditPart making the request.
	 */
	public void setDescriptors(RootDescriptor descriptor) {
		descriptors = new ArrayList<>();
		descriptors.add(descriptor);
	}

	/**
	 * Returns the size of the object to be created.
	 *
	 * @return the size
	 */
	public Dimension getSize() {
		return size;
	}

	/**
	 * Sets the size of the new object.
	 *
	 * @param size
	 *            the size
	 */
	public void setSize(Dimension size) {
		this.size = size;
	}


}
