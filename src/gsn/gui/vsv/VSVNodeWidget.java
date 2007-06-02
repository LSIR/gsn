/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://www.netbeans.org/cddl.txt.
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package gsn.gui.vsv;

import gsn.Main;
import gsn.VSensorLoader;
import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.InputStream;
import gsn.beans.StreamSource;
import gsn.beans.VSensorConfig;
import gsn.beans.WebInput;
import gsn.gui.forms.VSensorGraphScene;
import gsn.utils.GSNRuntimeException;
import gsn.wrappers.AbstractWrapper;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.KeyValue;
import org.eclipse.mylar.zest.layout.LayoutEntity;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.anchor.Anchor;
import org.netbeans.api.visual.anchor.AnchorFactory;
import org.netbeans.api.visual.border.Border;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.model.StateModel;
import org.netbeans.api.visual.widget.ImageWidget;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.SeparatorWidget;
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.Utilities;

/**
 * This class represents a node widget in the VMD visualization style. It
 * implements the minimize ability. It allows to add pin widgets into the widget
 * using <code>attachPinWidget</code> method.
 * <p>
 * The node widget consists of a header (with an image, a name, secondary name
 * and a glyph set) and the content. The content contains pin widgets. Pin
 * widgets can be organized in pin-categories defined by calling
 * <code>sortPins</code> method. The <code>sortPins</code> method has to be
 * called refresh the order after adding a pin widget.
 * 
 * @author David Kaspar
 */
public class VSVNodeWidget extends Widget implements StateModel.Listener, VSVMinimizeAbility, LayoutEntity {

	static final Image IMAGE_EXPAND = Utilities.loadImage("gsn/gui/resources/vsv-expand.png"); // NOI18N

	static final Image IMAGE_COLLAPSE = Utilities.loadImage("gsn/gui/resources/vsv-collapse.png"); // NOI18N

	static final Image IMAGE_ACTIVE_ADDRESS_BEAN = Utilities.loadImage("gsn/gui/resources/accept.png"); // NOI18N

	static final Color BORDER_CATEGORY_BACKGROUND = new Color(0Xfdc66f);

	static final Border BORDER_MINIMIZE = BorderFactory.createRoundedBorder(2, 2, null, VSVNodeBorder.COLOR_BORDER);

	static final Color COLOR_SELECTED = new Color(0Xffa84c);

	static final Border BORDER = BorderFactory.createOpaqueBorder(2, 8, 2, 8);

	static final Border BORDER_HOVERED = BorderFactory.createLineBorder(2, 8, 2, 8, Color.BLACK);

	private Widget header;

	private ImageWidget minimizeWidget;

	private ImageWidget imageWidget;

	private LabelWidget nameWidget;

	private LabelWidget typeWidget;

	private VSVGlyphSetWidget glyphSetWidget;

	private SeparatorWidget pinsSeparator;

	private HashMap<String, Widget> pinCategoryWidgets = new HashMap<String, Widget>();

	private Font fontPinCategory = getScene().getFont().deriveFont(10.0f);

	private StateModel stateModel = new StateModel(2);

	private Anchor nodeAnchor = new VSVNodeAnchor(this);

	private Object layoutInformation;

	private double layoutX;

	private double layoutY;

	private double layoutWidth;

	private double layoutHeight;

	private boolean animateMinimizing = false;

	private Widget widgetForInputStreams;

	/**
	 * Creates a node widget.
	 * 
	 * @param scene
	 *            the scene
	 */
	public VSVNodeWidget(Scene scene) {
		super(scene);

		setOpaque(false);
		setBorder(VSVFactory.createVSVNodeBorder());
		setLayout(LayoutFactory.createVerticalFlowLayout());
		setMinimumSize(new Dimension(128, 8));

		header = new Widget(scene);
		header.setBorder(BORDER);
		header.setBackground(COLOR_SELECTED);
		header.setOpaque(false);
		header.setLayout(LayoutFactory.createHorizontalFlowLayout(LayoutFactory.SerialAlignment.CENTER, 8));
		addChild(header);

		minimizeWidget = new ImageWidget(scene, IMAGE_COLLAPSE);
		minimizeWidget.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		minimizeWidget.setBorder(BORDER_MINIMIZE);
		minimizeWidget.getActions().addAction(new ToggleMinimizedAction());
		header.addChild(minimizeWidget);

		imageWidget = new ImageWidget(scene);
		header.addChild(imageWidget);

		nameWidget = new LabelWidget(scene);
		nameWidget.setFont(scene.getDefaultFont().deriveFont(Font.BOLD));
		header.addChild(nameWidget);

		typeWidget = new LabelWidget(scene);
		typeWidget.setForeground(Color.BLACK);
		header.addChild(typeWidget);

		glyphSetWidget = new VSVGlyphSetWidget(scene);
		header.addChild(glyphSetWidget);

		pinsSeparator = new SeparatorWidget(scene, SeparatorWidget.Orientation.HORIZONTAL);
		pinsSeparator.setForeground(BORDER_CATEGORY_BACKGROUND);
		addChild(pinsSeparator);

		Widget topLayer = new Widget(scene);
		addChild(topLayer);

		stateModel = new StateModel();
		stateModel.addListener(this);

		notifyStateChanged(ObjectState.createNormal(), ObjectState.createNormal());
	}

	/**
	 * Called to check whether a particular widget is minimizable. By default it
	 * returns true. The result have to be the same for whole life-time of the
	 * widget. If not, then the revalidation has to be invoked manually. An
	 * anchor (created by <code>VMDNodeWidget.createPinAnchor</code> is not
	 * affected by this method.
	 * 
	 * @param widget
	 *            the widget
	 * @return true, if the widget is minimizable; false, if the widget is not
	 *         minimizable
	 */
	protected boolean isMinimizableWidget(Widget widget) {
		return true;
	}

	/**
	 * Check the minimized state.
	 * 
	 * @return true, if minimized
	 */
	public boolean isMinimized() {
		return stateModel.getBooleanState();
	}

	/**
	 * Set the minimized state. This method will show/hide child widgets of this
	 * Widget and switches anchors between node and pin widgets.
	 * 
	 * @param minimized
	 *            if true, then the widget is going to be minimized
	 */
	public void setMinimized(boolean minimized) {
		stateModel.setBooleanState(minimized);
	}

	/**
	 * Toggles the minimized state. This method will show/hide child widgets of
	 * this Widget and switches anchors between node and pin widgets.
	 */
	public void toggleMinimized() {
		stateModel.toggleBooleanState();
	}

	/**
	 * Called when a minimized state is changed. This method will show/hide
	 * child widgets of this Widget and switches anchors between node and pin
	 * widgets.
	 */
	public void stateChanged() {
		boolean minimized = stateModel.getBooleanState();
		Rectangle rectangle = minimized ? new Rectangle() : null;
		for (Widget widget : getChildren())
			if (widget != header && widget != pinsSeparator) {
				if (!minimized)
					bringToFront();
				if (animateMinimizing)
					getScene().getSceneAnimator().animatePreferredBounds(widget,
							minimized && isMinimizableWidget(widget) ? rectangle : null);
				else
					widget.setPreferredBounds(minimized && isMinimizableWidget(widget) ? rectangle : null);
			}
		minimizeWidget.setImage(minimized ? IMAGE_EXPAND : IMAGE_COLLAPSE);
	}

	/**
	 * Called to notify about the change of the widget state.
	 * 
	 * @param previousState
	 *            the previous state
	 * @param state
	 *            the new state
	 */
	protected void notifyStateChanged(ObjectState previousState, ObjectState state) {
		if (!previousState.isSelected() && state.isSelected())
			bringToFront();
		// else if (!previousState.isHovered() && state.isHovered())
		// bringToFront();

		header.setOpaque(state.isSelected());
		header.setBorder(state.isFocused() || state.isHovered() ? BORDER_HOVERED : BORDER);
	}

	/**
	 * Sets a node image.
	 * 
	 * @param image
	 *            the image
	 */
	public void setNodeImage(Image image) {
		imageWidget.setImage(image);
		revalidate();
	}

	/**
	 * Returns a node name.
	 * 
	 * @return the node name
	 */
	public String getNodeName() {
		return nameWidget.getLabel();
	}

	/**
	 * Sets a node name.
	 * 
	 * @param nodeName
	 *            the node name
	 */
	public void setNodeName(String nodeName) {
		nameWidget.setLabel(nodeName);
	}

	/**
	 * Sets a node type (secondary name).
	 * 
	 * @param nodeType
	 *            the node type
	 */
	public void setNodeType(String nodeType) {
		typeWidget.setLabel(nodeType != null ? "[" + nodeType + "]" : null);
	}

	/**
	 * Attaches a pin widget to the node widget.
	 * 
	 * @param widget
	 *            the pin widget
	 */
	public void attachPinWidget(Widget widget) {
		widget.setCheckClipping(true);
		addChild(widget);
		if (stateModel.getBooleanState() && isMinimizableWidget(widget))
			widget.setPreferredBounds(new Rectangle());
	}

	/**
	 * Sets node glyphs.
	 * 
	 * @param glyphs
	 *            the list of images
	 */
	public void setGlyphs(List<Image> glyphs) {
		glyphSetWidget.setGlyphs(glyphs);
	}

	/**
	 * Sets all node properties at once.
	 * 
	 * @param image
	 *            the node image
	 * @param nodeName
	 *            the node name
	 * @param nodeType
	 *            the node type (secondary name)
	 * @param glyphs
	 *            the node glyphs
	 */
	public void setNodeProperties(Image image, String nodeName, String nodeType, List<Image> glyphs) {
		setNodeImage(image);
		setNodeName(nodeName);
		setNodeType(nodeType);
		setGlyphs(glyphs);
	}

	/**
	 * Returns a node name widget.
	 * 
	 * @return the node name widget
	 */
	public LabelWidget getNodeNameWidget() {
		return nameWidget;
	}

	/**
	 * Returns a node anchor.
	 * 
	 * @return the node anchor
	 */
	public Anchor getNodeAnchor() {
		return nodeAnchor;
	}

	/**
	 * Creates an extended pin anchor with an ability of reconnecting to the
	 * node anchor when the node is minimized.
	 * 
	 * @param anchor
	 *            the original pin anchor from which the extended anchor is
	 *            created
	 * @return the extended pin anchor
	 */
	public Anchor createAnchorPin(Anchor anchor) {
		return AnchorFactory.createProxyAnchor(stateModel, anchor, nodeAnchor);
	}

	/**
	 * TODO: this method is no longer correct. Returns a list of pin widgets
	 * attached to the node.
	 * 
	 * @return the list of pin widgets
	 */

	/**
	 * Collapses the widget.
	 */
	public void collapseWidget() {
		stateModel.setBooleanState(true);
	}

	/**
	 * Expands the widget.
	 */
	public void expandWidget() {
		stateModel.setBooleanState(false);
	}

	/**
	 * Returns a header widget.
	 * 
	 * @return the header widget
	 */
	public Widget getHeader() {
		return header;
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

	public double getHeightInLayout() {
		if (getBounds() == null)
			return layoutHeight * 2;
		if (isMinimized())
			return getBounds().getHeight() * 2;
		return getBounds().getHeight();
	}

	public Object getLayoutInformation() {
		return layoutInformation;
	}

	public double getWidthInLayout() {
		if (getBounds() == null)
			return layoutWidth;
		return getBounds().getWidth();
	}

	public double getXInLayout() {
		return layoutX;
	}

	public double getYInLayout() {
		return layoutY;
	}

	public boolean hasPreferredLocation() {
		return false;
	}

	public void setLayoutInformation(Object layoutInformation) {
		this.layoutInformation = layoutInformation;
	}

	public void setLocationInLayout(double x, double y) {
		this.layoutX = x;
		this.layoutY = y;
		setPreferredLocation(new Point((int) x, (int) y));
		// System.out.println(getNodeName() + " :: setLocationInLayout(x, y)" +
		// x + " , " + y);
	}

	public void setSizeInLayout(double width, double height) {
		if (getBounds() == null) {
			layoutWidth = width;
			layoutHeight = height;
		}
	}

	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * 
	 */
	public void addDetails() {
		Widget detailsWidget = createVSensorDetailWidget((VSensorGraphScene) getScene());
		addChild(detailsWidget);
	}

	private Widget createVSensorDetailWidget(VSensorGraphScene scene) {
		VSensorConfig vSensorConfig = (VSensorConfig) scene.findObject(this);
		Widget topWidget = createTitledDetailWidget(scene, "Details");
		topWidget.setToolTipText(vSensorConfig.getDescription());
		topWidget.setLayout(LayoutFactory.createVerticalFlowLayout());
		topWidget.setCheckClipping(true);
		topWidget.setOpaque(false);

		// adding processing class
		Widget processingClassWidget = addVSensorProcessingClass(scene, vSensorConfig);
		topWidget.addChild(processingClassWidget);

		KeyValue[] addressing = vSensorConfig.getAddressing();
		// adding addressing
		if (addressing.length > 0) {
			Widget addressingWidget = createTitledDetailWidget(scene, "Addressing");
			for (int i = 0; i < addressing.length; i++) {
				addressingWidget.addChild(createTitleValueWidget(scene, String.valueOf(addressing[i].getKey()), String
						.valueOf(addressing[i].getValue())));
			}
			topWidget.addChild(addressingWidget);
		}

		topWidget.addChild(createTitleValueWidget(scene, "Priority : ", String.valueOf(vSensorConfig.getPriority())));
		if (vSensorConfig.getGeneralPassword() != null)
			topWidget.addChild(createTitleValueWidget(scene, "Password : ", vSensorConfig.getGeneralPassword()));
		topWidget.addChild(createTitleValueWidget(scene, "Pool size : ", String.valueOf(vSensorConfig.getLifeCyclePoolSize())));
		topWidget.addChild(createTitleValueWidget(scene, "Storage size : ", vSensorConfig.getStorageHistorySize()));
		int outputStreamRate = vSensorConfig.getOutputStreamRate();
		topWidget.addChild(createTitleValueWidget(scene, "Maximum allowed rate : ", outputStreamRate == 0 ? "Unlimited" : String
				.valueOf(outputStreamRate)));
		if (stateModel.getBooleanState())
			topWidget.setPreferredBounds(new Rectangle());
		return topWidget;
	}

	private Widget addVSensorProcessingClass(VSensorGraphScene scene, VSensorConfig vSensorConfig) {
		Widget processingClassWidget = createTitledDetailWidget(scene, "Processing class");
		processingClassWidget.addChild(createTitleValueWidget(scene, "Name : ", vSensorConfig.getProcessingClass()));
		TreeMap<String, String> mainClassInitialParams = vSensorConfig.getMainClassInitialParams();
		if (mainClassInitialParams.size() > 0) {
			Widget initParamsWidget = createTitledDetailWidget(scene, "Init params");
			for (Map.Entry<String, String> entry : mainClassInitialParams.entrySet()) {
				initParamsWidget.addChild(createTitleValueWidget(scene, entry.getKey(), entry.getValue()));
			}
			processingClassWidget.addChild(initParamsWidget);
		}
		DataField[] outputStructure = vSensorConfig.getOutputStructure();
		if (outputStructure.length > 0) {
			Widget outputStructureWidget = createTitledDetailWidget(scene, "Output structure");
			for (int i = 0; i < outputStructure.length; i++) {
				String description = outputStructure[i].getDescription();
				StringBuilder stringBuilder = new StringBuilder();
				if (description != null && description.length() > 0) {
					stringBuilder.append(" [");
					stringBuilder.append(description);
					stringBuilder.append("] ");
				}
				outputStructureWidget.addChild(createTitleValueWidget(scene, outputStructure[i].getName(), outputStructure[i].getType()
						+ stringBuilder.toString()));
			}
			processingClassWidget.addChild(outputStructureWidget);
		}
		WebInput[] webinput = vSensorConfig.getWebinput();
		if (webinput != null && webinput.length > 0) {
			Widget webInputWidgets = createTitledDetailWidget(scene, "Web input");
			if (vSensorConfig.getWebParameterPassword() != null)
				webInputWidgets.addChild(createTitleValueWidget(scene, "Password : ", vSensorConfig.getWebParameterPassword()));
			for (int i = 0; i < webinput.length; i++) {
				Widget webInputWidget = createTitledDetailWidget(scene, webinput[i].getName());
				DataField[] parameters = webinput[i].getParameters();
				for (int j = 0; j < parameters.length; j++) {
					StringBuilder stringBuilder = new StringBuilder();
					String description = parameters[j].getDescription();
					if (description != null && description.length() > 0) {
						stringBuilder.append(" [");
						stringBuilder.append(description);
						stringBuilder.append("] ");
					}
					webInputWidget.addChild(createTitleValueWidget(scene, parameters[j].getName(), parameters[j].getType()
							+ stringBuilder.toString()));
				}
				webInputWidgets.addChild(webInputWidget);
			}
			processingClassWidget.addChild(webInputWidgets);
		}
		return processingClassWidget;
	}

	private Widget createTitledDetailWidget(VSensorGraphScene scene, String title) {
		return new VSVMinimizableLabelWidget(this, title);
	}

	private Widget createImagedTitledDetailWidget(VSensorGraphScene scene, String title, Image image) {
		return new VSVMinimizableLabelWidget(this, title, image);
	}

	private Border createDetailsBorder() {
		return BorderFactory.createCompositeBorder(BorderFactory.createEmptyBorder(1), BorderFactory.createLineBorder(1,
				VSVNodeBorder.COLOR_BORDER));
	}

	private Widget createTitleValueWidget(VSensorGraphScene scene, String title, String value) {
		Widget nameWidget = new Widget(scene);
		nameWidget.setBorder(BORDER);
		nameWidget.setBackground(VSVNodeWidget.COLOR_SELECTED);
		nameWidget.setOpaque(false);
		nameWidget.setLayout(LayoutFactory.createHorizontalFlowLayout(LayoutFactory.SerialAlignment.CENTER, 8));
		LabelWidget titleWidget = new LabelWidget(scene, title);
		Font derivedFont = scene.getFont().deriveFont(10.0f);
		titleWidget.setFont(derivedFont);

		VSVLabelWidget valueWidget = new VSVLabelWidget(scene, value + " ", true);
		valueWidget.setFont(derivedFont);

		nameWidget.addChild(titleWidget);
		nameWidget.addChild(valueWidget);
		return nameWidget;
	}

	public void addPins(VSensorGraphScene scene, HashMap<InputStream, List<Widget>> inputStreamPins) {
		widgetForInputStreams = null;
		for (Map.Entry<InputStream, List<Widget>> entry : inputStreamPins.entrySet()) {
			InputStream inputStream = entry.getKey();
			List<Widget> widgets = entry.getValue();
			if (widgetForInputStreams == null)
				widgetForInputStreams = new VSVMinimizableLabelWidget(this, "Input \nStreams");
			VSVMinimizableLabelWidget inputStreamWidget = new VSVMinimizableLabelWidget(this, inputStream.getInputStreamName());
			for (Widget widget : widgets) {
				VSVPinWidget pinWidget = (VSVPinWidget) widget;
				removeChild(pinWidget);

				StreamSource streamSource = inputStream.getSource(pinWidget.getPinName());
				pinWidget.addChild(createTitleValueWidget(scene, "Window size : ", streamSource.getStorageSize()));
				pinWidget.addChild(createTitleValueWidget(scene, "Sampling rate : ", String.valueOf(streamSource.getSamplingRate())));
				pinWidget.addChild(createTitleValueWidget(scene, "Disconnect buffer size : ", String.valueOf(streamSource
						.getDisconnectedBufferSize())));
				pinWidget.addChild(createTitleValueWidget(scene, "Start time : ", String.valueOf(streamSource.getStartTime())));
				pinWidget.addChild(createTitleValueWidget(scene, "End time : ", String.valueOf(streamSource.getEndTime())));
				pinWidget.addChild(createTitleValueWidget(scene, "Query : ", "'" + streamSource.getSqlQuery() + "'"));
				AddressBean[] addressing = streamSource.getAddressing();
				if (addressing.length > 0) {
					for (int i = 0; i < addressing.length; i++) {
						Widget addressingWidget;
						
						AddressBean activeAddressBean = streamSource.getActiveAddressBean();
						if (activeAddressBean != null && activeAddressBean.equals(addressing[i]))
							addressingWidget = createImagedTitledDetailWidget(scene, "Address", IMAGE_ACTIVE_ADDRESS_BEAN);
						else
							addressingWidget = createTitledDetailWidget(scene, "Address");
						addressingWidget.addChild(createTitleValueWidget(scene, "Wrapper", addressing[i].getWrapper()));
						KeyValue[] predicates = addressing[i].getPredicates();
						for (int j = 0; j < predicates.length; j++) {
							addressingWidget.addChild(createTitleValueWidget(scene, predicates[j].getKey().toString(), predicates[j]
									.getValue().toString()));
						}
						pinWidget.addChild(addressingWidget);
					}
				}
				inputStreamWidget.addChild(pinWidget);
			}
			SeparatorWidget separatorWidget = new SeparatorWidget(scene, SeparatorWidget.Orientation.HORIZONTAL);
			separatorWidget.setForeground(BORDER_CATEGORY_BACKGROUND);
			inputStreamWidget.addChild(separatorWidget);
			inputStreamWidget.addChild(createTitleValueWidget(scene, "Rate : ", String.valueOf(inputStream.getRate())));
			long count = inputStream.getCount();
			inputStreamWidget.addChild(createTitleValueWidget(scene, "Count : ", count == Long.MAX_VALUE ? "Max Value" : String
					.valueOf(count)));
			inputStreamWidget.addChild(createTitleValueWidget(scene, "Query : ", "'" + inputStream.getQuery() + "'"));
			widgetForInputStreams.addChild(inputStreamWidget);
			// addChild(inputStreamWidget);
			// if(isMinimized())
			// inputStreamWidget.collapseWidget();
			// else
			// inputStreamWidget.expandWidget();
		}
		if (widgetForInputStreams != null)
			addChild(widgetForInputStreams);
	}

	/**
	 * Returns whether animate minimization or not
	 * 
	 * @return
	 */
	public boolean isAnimateMinimizing() {
		return animateMinimizing;
	}

	/**
	 * Sets whether animate minimization or not
	 * 
	 * @param animateMinimizing
	 */
	public void setAnimateMinimizing(boolean animateMinimizing) {
		this.animateMinimizing = animateMinimizing;
	}

	public void setWidgetIcon(Image statusImage) {
		setNodeImage(statusImage);
	}

	public void objectUpdated() {
		if (widgetForInputStreams == null)
			return;
		List<Widget> inputStreamWidgetList = widgetForInputStreams.getChildren();
		for (Widget inputStreamWidget : inputStreamWidgetList) {
//			 We must exclude label and minimize/maximize image widgets
			if (inputStreamWidget instanceof VSVMinimizableLabelWidget) {
				List<Widget> streamSourceWidgetList = inputStreamWidget.getChildren();
				for (Widget streamSourceWidget : streamSourceWidgetList) {
					// We must exclude label and minimize/maximize image widgets
					if (streamSourceWidget instanceof VSVPinWidget) {
						VSVPinWidget pinWidget = (VSVPinWidget) streamSourceWidget;
						List<Widget> streamSourceDetailWidgetList = pinWidget.getChildren();
						VSensorGraphScene graphScene = (VSensorGraphScene) getScene();
						VSensorConfig config = (VSensorConfig) graphScene.findObject(this);

						List<Widget> childrenToRemove = new ArrayList<Widget>();
						for (int i = 6; i < streamSourceDetailWidgetList.size(); i++) {
							childrenToRemove.add(streamSourceDetailWidgetList.get(i));
						}
						// Removes addressing widgets
						pinWidget.removeChildren(childrenToRemove);
						
						InputStream inputStream = config.getInputStream(((VSVMinimizableLabelWidget)inputStreamWidget).getTitle());
						StreamSource streamSource = inputStream.getSource(pinWidget.getPinName());
						AddressBean[] addressing = streamSource.getAddressing();
						if (addressing.length > 0) {
							for (int i = 0; i < addressing.length; i++) {
								Widget addressingWidget;
								
								AddressBean activeAddressBean = streamSource.getActiveAddressBean();
								if (activeAddressBean != null && activeAddressBean.equals(addressing[i]))
									addressingWidget = createImagedTitledDetailWidget(graphScene, "Address", IMAGE_ACTIVE_ADDRESS_BEAN);
								else
									addressingWidget = createTitledDetailWidget(graphScene, "Address");
								addressingWidget.addChild(createTitleValueWidget(graphScene, "Wrapper", addressing[i].getWrapper()));
								KeyValue[] predicates = addressing[i].getPredicates();
								for (int j = 0; j < predicates.length; j++) {
									addressingWidget.addChild(createTitleValueWidget(graphScene, predicates[j].getKey().toString(), predicates[j]
											.getValue().toString()));
								}
								pinWidget.addChild(addressingWidget);
							}
						}
					}
				}
			}
		}

	}

}