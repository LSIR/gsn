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

import org.netbeans.api.visual.anchor.Anchor;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

import java.awt.*;
import java.util.List;

/**
 * This class represents a pin widget in the VMD visualization style.
 * The pin widget consists of a name and a glyph set.
 *
 * @author David Kaspar
 */
public class VSVPinWidget extends Widget {

    private LabelWidget nameWidget;
    private VSVGlyphSetWidget glyphsWidget;
    private VSVNodeAnchor anchor;

    /**
     * Creates a pin widget.
     * @param scene the scene
     */
    public VSVPinWidget (Scene scene) {
        super (scene);

        setBorder (VSVNodeWidget.BORDER);
//        setBackground (VSVNodeWidget.COLOR_SELECTED);
        setOpaque (false);
        setLayout (LayoutFactory.createVerticalFlowLayout());
        
        Widget innerWidget = new Widget(scene);
        innerWidget.setLayout (LayoutFactory.createHorizontalFlowLayout (LayoutFactory.SerialAlignment.CENTER, 8));
        innerWidget.addChild (nameWidget = new LabelWidget (scene));
        innerWidget.addChild (glyphsWidget = new VSVGlyphSetWidget (scene));
        nameWidget.setFont(scene.getFont().deriveFont(Font.BOLD + Font.ITALIC));
        nameWidget.setCheckClipping(true);
        setCheckClipping(true);
        addChild(innerWidget);

        notifyStateChanged (ObjectState.createNormal (), ObjectState.createNormal ());
    }

    /**
     * Called to notify about the change of the widget state.
     * @param previousState the previous state
     * @param state the new state
     */
    protected void notifyStateChanged (ObjectState previousState, ObjectState state) {
        setOpaque (state.isSelected ());
        setBorder (state.isFocused () || state.isHovered () ? VSVNodeWidget.BORDER_HOVERED : VSVNodeWidget.BORDER);
//        LookFeel lookFeel = getScene ().getLookFeel ();
//        setBorder (BorderFactory.createCompositeBorder (BorderFactory.createEmptyBorder (8, 2), lookFeel.getMiniBorder (state)));
//        setForeground (lookFeel.getForeground (state));
    }

    /**
     * Returns a pin name widget.
     * @return the pin name widget
     */
    public Widget getPinNameWidget () {
        return nameWidget;
    }

    /**
     * Sets a pin name.
     * @param name the pin name
     */
    public void setPinName (String name) {
        nameWidget.setLabel (name);
    }

    /**
     * Returns a pin name.
     * @return the pin name
     */
    public String getPinName () {
        return nameWidget.getLabel();
    }

    /**
     * Sets pin glyphs.
     * @param glyphs the list of images
     */
    public void setGlyphs (List<Image> glyphs) {
        glyphsWidget.setGlyphs (glyphs);
    }

    /**
     * Sets all pin properties at once.
     * @param name the pin name
     * @param glyphs the pin glyphs
     */
    public void setProperties (String name, List<Image> glyphs) {
        setPinName (name);
        glyphsWidget.setGlyphs (glyphs);
    }

    /**
     * Creates a horizontally oriented anchor similar to VMDNodeWidget.createAnchorPin
     * @return the anchor
     */
    public Anchor createAnchor () {
        if (anchor == null)
            anchor = new VSVNodeAnchor (this, false);
        return anchor;
    }

}
