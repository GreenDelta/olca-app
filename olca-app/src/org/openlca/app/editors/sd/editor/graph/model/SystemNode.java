package org.openlca.app.editors.sd.editor.graph.model;

import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.sd.model.Rect;
import org.openlca.sd.model.SystemBinding;

import java.util.Objects;

public class SystemNode implements NotifySupport {

	private final SystemBinding binding;
	private final Rectangle bounds = new Rectangle();
	private final Notifier notifier = new Notifier();

	public SystemNode(SystemBinding binding, Rectangle bounds) {
		this.binding = Objects.requireNonNull(binding);
		if (bounds != null) {
			this.bounds.setBounds(bounds);
		}
	}

	@Override
	public Notifier notifier() {
		return notifier;
	}

	public SystemBinding binding() {
		return binding;
	}

	public Rectangle bounds() {
		return bounds;
	}

	public void moveTo(Rectangle rect) {
		if (rect == null)
			return;
		bounds.setBounds(rect);
		binding.setView(new Rect(rect.x, rect.y, rect.width, rect.height));
		notifier.fire();
	}

	public String name() {
		var system = binding.system();
		return system != null && system.name() != null
			? system.name()
			: "Product system";
	}
}
