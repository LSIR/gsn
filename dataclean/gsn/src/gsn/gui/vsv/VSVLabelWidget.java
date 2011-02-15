package gsn.gui.vsv;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

public class VSVLabelWidget extends Widget {

	public static final int DEFAULT_TEXT_SIZE_PER_LINE = 50;

	private boolean wrapLabel;

	private int textSizePerLine;

	private String label;

	public VSVLabelWidget(Scene scene, String label) {
		this(scene, label, false);
	}

	public VSVLabelWidget(Scene scene, String label, boolean wrapLabel) {
		this(scene, label, wrapLabel, DEFAULT_TEXT_SIZE_PER_LINE);
	}

	public VSVLabelWidget(Scene scene, String label, boolean wrapLabel, int textSizePerLine) {
		super(scene);
		this.label = label;
		this.wrapLabel = wrapLabel;
		this.textSizePerLine = textSizePerLine > 0 ? textSizePerLine : DEFAULT_TEXT_SIZE_PER_LINE;
		setOpaque(false);
		setCheckClipping(true);
		setLayout(LayoutFactory.createVerticalFlowLayout(LayoutFactory.SerialAlignment.JUSTIFY, 2));
		createLabelWidgets();
	}

	private void createLabelWidgets() {
		if (label == null)
			return;
		if (wrapLabel == true) {
			String wrappedLabel = WordUtils.wrap(label, textSizePerLine);
			StrTokenizer tokenizer = new StrTokenizer(wrappedLabel, SystemUtils.LINE_SEPARATOR);
			while(tokenizer.hasNext()){
				addChild(new LabelWidget(getScene(), tokenizer.nextToken()));
			}
//			int textLength = label.length();
//			int wrapEndIndex = textLength - 1;
//			int wrapStartIndex = 0;
//			
//			if (textLength > textSizePerLine)
//				wrapEndIndex = wrapStartIndex + textSizePerLine;
//			while (wrapEndIndex < textLength) {
//				if (Character.isWhitespace(label.charAt(wrapEndIndex))) {
//					addChild(new LabelWidget(getScene(), label.substring(wrapStartIndex, wrapEndIndex)));
//					wrapStartIndex = wrapEndIndex + 1;
//				}
//			}
//
		} else {
			LabelWidget labelWidget = new LabelWidget(getScene(), label);
			addChild(labelWidget);
		}
	}
}
