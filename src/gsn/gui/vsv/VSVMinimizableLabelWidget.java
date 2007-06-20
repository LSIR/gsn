package gsn.gui.vsv;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.border.Border;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.model.StateModel;
import org.netbeans.api.visual.widget.ImageWidget;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.Utilities;


public class VSVMinimizableLabelWidget extends Widget implements VSVMinimizeAbility, StateModel.Listener {
	static final Image IMAGE_EXPAND = Utilities.loadImage("gsn/gui/resources/vsv-expand-inner.png");

	static final Image IMAGE_COLLAPSE = Utilities.loadImage("gsn/gui/resources/vsv-collapse-inner.png"); 
	
	private StateModel stateModel = new StateModel(2);
	private ImageWidget minimizeWidget;

	private Widget header;

	private VSVNodeWidget vsvNodeWidget;
	
	private String title;
	
	public VSVMinimizableLabelWidget(VSVNodeWidget vsvNodeWidget, String title, Image image){
		super(vsvNodeWidget.getScene());
		this.vsvNodeWidget = vsvNodeWidget;
		Scene scene = vsvNodeWidget.getScene();
		setLayout(LayoutFactory.createVerticalFlowLayout());
		setBorder(createDetailsBorder());
		setCheckClipping(true);
		setOpaque(false);
		
		header = new Widget(scene);
		header.setBackground(VSVNodeWidget.BORDER_CATEGORY_BACKGROUND);
		header.setOpaque(true);
		header.setLayout(LayoutFactory.createHorizontalFlowLayout(LayoutFactory.SerialAlignment.CENTER, 8));
		addChild(header);
		
		minimizeWidget = new ImageWidget(scene, IMAGE_EXPAND);
		minimizeWidget.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		minimizeWidget.getActions().addAction(new ToggleMinimizedAction());
		header.addChild(minimizeWidget);
		
		if(image != null){
			header.addChild(new ImageWidget(scene, image));
		}
		
		this.title = title;
		LabelWidget titleWidget = new LabelWidget(scene, title);
		titleWidget.setBackground(VSVNodeWidget.BORDER_CATEGORY_BACKGROUND);
		titleWidget.setForeground(Color.GRAY);
		titleWidget.setFont(getScene().getFont().deriveFont(10.0f));
		titleWidget.setAlignment(LabelWidget.Alignment.CENTER);
		titleWidget.setCheckClipping(true);
		header.addChild(titleWidget);
		
		stateModel = new StateModel();
		stateModel.addListener(this);
		notifyStateChanged(ObjectState.createNormal(), ObjectState.createNormal());
	}
	
	public VSVMinimizableLabelWidget(VSVNodeWidget vsvNodeWidget, String title) {
		this(vsvNodeWidget, title, null);
	}
	
	private final class ToggleMinimizedAction extends WidgetAction.Adapter {

		public State mousePressed(Widget widget, WidgetMouseEvent event) {
			if (event.getButton() == MouseEvent.BUTTON1 || event.getButton() == MouseEvent.BUTTON2) {
				stateModel.toggleBooleanState();
				return State.CONSUMED;
			}
			return State.REJECTED;
		}
	}

	private static Border createDetailsBorder() {
		return BorderFactory.createCompositeBorder(BorderFactory.createEmptyBorder(1), BorderFactory.createLineBorder(1,
				VSVNodeBorder.COLOR_BORDER));
	}

	public void collapseWidget() {
		stateModel.setBooleanState(true);
	}

	public void expandWidget() {
		stateModel.setBooleanState(false);
	}

	public void stateChanged() {
		boolean minimized = stateModel.getBooleanState();
		Rectangle rectangle = minimized ? new Rectangle() : null;
		for (Widget widget : getChildren())
			if (widget != header) {
//				getScene().getSceneAnimator().animatePreferredBounds(widget, minimized && isMinimizableWidget(widget) ? rectangle : null);
				widget.setPreferredBounds(minimized && isMinimizableWidget(widget) ? rectangle : null);
				getScene().validate();
				getScene().getSceneAnimator().animateZoomFactor(1);
			}
		minimizeWidget.setImage(minimized ? IMAGE_COLLAPSE : IMAGE_EXPAND);
	}

	private boolean isMinimizableWidget(Widget widget) {
		return true;
	}

	public String getTitle() {
		return title;
	}

}
