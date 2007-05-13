package gsn.gui.vsv;

import java.awt.Color;
import java.awt.Paint;

import org.netbeans.api.visual.border.Border;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.laf.LookFeel;
import org.netbeans.api.visual.model.ObjectState;

/**
 * @author Mehdi Riahi
 * 
 */
public class VSVLookFeal extends LookFeel {
	private static final Color COLOR_SELECTED = new Color(0xeaa524);

	private static final Color COLOR_HIGHLIGHTED = COLOR_SELECTED.darker();

	private static final Color COLOR_HOVERED = COLOR_SELECTED.brighter();

	private static final int MARGIN = 3;

	private static final int ARC = 10;

	private static final int MINI_THICKNESS = 1;

	private static final Border BORDER_NORMAL = BorderFactory.createEmptyBorder(MARGIN, MARGIN);

	private static final Border BORDER_HOVERED = BorderFactory.createRoundedBorder(ARC, ARC, MARGIN, MARGIN, COLOR_HOVERED, COLOR_HOVERED
			.darker());

	private static final Border BORDER_SELECTED = BorderFactory.createRoundedBorder(ARC, ARC, MARGIN, MARGIN, COLOR_SELECTED,
			COLOR_SELECTED.darker());

	private static final Border MINI_BORDER_NORMAL = BorderFactory.createEmptyBorder(MINI_THICKNESS);

	private static final Border MINI_BORDER_HOVERED = BorderFactory.createRoundedBorder(MINI_THICKNESS, MINI_THICKNESS, MINI_THICKNESS,
			MINI_THICKNESS, COLOR_HOVERED, COLOR_HOVERED.darker());

	private static final Border MINI_BORDER_SELECTED = BorderFactory.createRoundedBorder(MINI_THICKNESS, MINI_THICKNESS, MINI_THICKNESS,
			MINI_THICKNESS, COLOR_SELECTED, COLOR_SELECTED.darker());

	@Override
	public Paint getBackground() {
		return Color.WHITE;
	}

	@Override
	public Paint getBackground(ObjectState state) {
		if (state.isHovered())
			return COLOR_HOVERED;
		if (state.isSelected())
			return COLOR_SELECTED;
		if (state.isHighlighted() || state.isFocused())
			return COLOR_HIGHLIGHTED;
		return Color.WHITE;
	}

	@Override
	public Border getBorder(ObjectState state) {
		if (state.isHovered())
			return BORDER_HOVERED;
		if (state.isSelected())
			return BORDER_SELECTED;
		if (state.isFocused())
			return BORDER_HOVERED;
		return BORDER_NORMAL;
	}

	@Override
	public Color getForeground() {
		return Color.BLACK;
	}

	@Override
	public Color getForeground(ObjectState state) {
		return state.isSelected () ? Color.WHITE : Color.BLACK;
	}

	@Override
	public Color getLineColor(ObjectState state) {
		if (state.isHovered())
			return COLOR_HOVERED;
		if (state.isSelected())
			return COLOR_SELECTED;
		if (state.isHighlighted() || state.isFocused())
			return COLOR_HIGHLIGHTED;
		return Color.BLACK;
	}

	@Override
	public int getMargin() {
		return MARGIN;
	}

	@Override
	public Border getMiniBorder(ObjectState state) {
		if (state.isHovered())
			return MINI_BORDER_HOVERED;
		if (state.isSelected())
			return MINI_BORDER_SELECTED;
		if (state.isFocused())
			return MINI_BORDER_HOVERED;
		return MINI_BORDER_NORMAL;
	}

	@Override
	public boolean getOpaque(ObjectState state) {
		return state.isHovered() || state.isSelected();
	}

}
