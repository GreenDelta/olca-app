package org.openlca.app.editors.sd.editor.graph.model;

import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.sd.model.Rect;
import org.openlca.sd.model.SystemBinding;

import java.util.Objects;

public class SystemNode implements NotifySupport {

	private final SystemBinding binding;
	private final Rectangle bounds;
	private final Notifier notifier = new Notifier();

	public SystemNode(SystemBinding binding) {
		this.binding = Objects.requireNonNull(binding);
		var v = binding.view();
		bounds = v != null
			? new Rectangle(v.x(), v.y(), v.width(), v.height())
			: new Rectangle(100, 100, 80, 40);
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
