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

import org.eclipse.mylar.zest.layout.LayoutEntity;
import org.eclipse.mylar.zest.layout.LayoutRelationship;
import org.netbeans.api.visual.anchor.AnchorShape;
import org.netbeans.api.visual.anchor.PointShape;
import org.netbeans.api.visual.anchor.PointShapeFactory;
import org.netbeans.api.visual.graph.GraphPinScene;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.router.Router;
import org.netbeans.api.visual.widget.ConnectionWidget;
import org.netbeans.api.visual.widget.Scene;
import org.openide.util.Utilities;

import java.awt.*;

/**
 * This class represents a connection widget in the VMD visualization style. Can
 * be combined with any other widget.
 * 
 * @author David Kaspar
 */
public class VSVConnectionWidget extends ConnectionWidget implements LayoutRelationship {

	private static final PointShape POINT_SHAPE_IMAGE = PointShapeFactory.createImagePointShape(Utilities
			.loadImage("gsn/gui/resources/vsv-pin.png")); // NOI18N

	private static final Color COLOR_NORMAL = VSVNodeBorder.COLOR_BORDER;

	private static final Color COLOR_HOVERED = Color.BLACK;

	private static final Color COLOR_HIGHLIGHTED = new Color(49, 106, 197);

	private GraphPinScene<String, String, String> graphPinScene;

	private String edgeID;

	private Object layoutInformation;

	/**
	 * Creates a connection widget.
	 * 
	 * @param scene
	 *            the scene
	 * @param router
	 * @param edgeID
	 */
	public VSVConnectionWidget(Scene scene, Router router, String edgeID) {
		super(scene);
		this.edgeID = edgeID;
		setRouter(router);
		setStroke(new BasicStroke(1.5f));
		setSourceAnchorShape(AnchorShape.NONE);
		setTargetAnchorShape(AnchorShape.TRIANGLE_FILLED);
		setPaintControlPoints(true);
		setState(ObjectState.createNormal());
		graphPinScene = (GraphPinScene<String, String, String>) getScene();
	}

	/**
	 * Implements the widget-state specific look of the widget.
	 * 
	 * @param previousState
	 *            the previous state
	 * @param state
	 *            the new state
	 */
	public void notifyStateChanged(ObjectState previousState, ObjectState state) {
		if (state.isHovered())
			setForeground(COLOR_HOVERED);
		else if (state.isSelected())
			setForeground(VSVNodeWidget.COLOR_SELECTED);
		else if (state.isHighlighted())
			setForeground(COLOR_HIGHLIGHTED);
		else if (state.isFocused())
			setForeground(COLOR_HOVERED);
		else
			setForeground(COLOR_NORMAL);

		if (state.isSelected()) {
			setControlPointShape(PointShape.SQUARE_FILLED_SMALL);
			setEndPointShape(PointShape.SQUARE_FILLED_BIG);
		} else {
			setControlPointShape(PointShape.NONE);
			setEndPointShape(POINT_SHAPE_IMAGE);
		}
	}

	public LayoutEntity getDestinationInLayout() {
		return (LayoutEntity) ((VSVPinWidget) graphPinScene.findWidget(graphPinScene.getEdgeTarget(edgeID))).getParentWidget()
				.getParentWidget().getParentWidget();
	}

	public Object getLayoutInformation() {
		return layoutInformation;
	}

	public LayoutEntity getSourceInLayout() {
		String targetPinID = graphPinScene.getEdgeSource(edgeID);
		return (VSVNodeWidget) graphPinScene.findWidget(targetPinID.substring(0, targetPinID.indexOf(VSVGraphScene.PIN_ID_DEFAULT_SUFFIX)));
	}

	public double getWeightInLayout() {
		return 1;
	}

	public boolean isBidirectionalInLayout() {
		return false;
	}

	public void setLayoutInformation(Object layoutInformation) {
		this.layoutInformation = layoutInformation;
	}

	public void setWeightInLayout(double weight) {
		// Do nothing
	}

}
