package gsn.gui.forms;

import gsn.gui.beans.AddressBeanModel;
import gsn.gui.beans.AddressBeanPresentationModel;
import gsn.gui.beans.StreamSourceModel;
import gsn.gui.beans.StreamSourcePresentationModel;
import gsn.gui.util.GUIUtils;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.ArrayListModel;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.forms.builder.ButtonStackBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.ValidationResultModel;
import com.jgoodies.validation.view.ValidationComponentUtils;

public class StreamSourceEditorDialog extends JDialog {
	private boolean canceled;

	private StreamSourceModel streamSourceModel;

	private StreamSourcePresentationModel presentationModel;

	private SelectionInList selectionInList;

	private JTextField aliasTextField;

	private JTextField startTimeTextField;

	private JTextField endTimeTextField;

	private JTextField dbsTextField;

	private JTextField samplingRateTextField;

	private JTextField historySizeTextField;

	private JTextField queryTextField;

	private AddressBeanEditorPanel addressBeanEditorPanel;

	private JList addressingList;

	private JButton addButton;

	private Action addAction;

	private Action removeAction;

	private Action editAction;

	private JButton removeButton;

	private JButton editButton;

	private JComponent editorPanel;
	
	private ArrayListModel oldAddressing;

	public StreamSourceEditorDialog(Frame parent, StreamSourceModel streamSourceModel) {
		super(parent, "Stream Source Editor", true);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.streamSourceModel = streamSourceModel;
		presentationModel = new StreamSourcePresentationModel(streamSourceModel);
		oldAddressing = streamSourceModel.cloneAddressing();
		selectionInList = new SelectionInList((ListModel) streamSourceModel.getAddressing());
		selectionInList.addPropertyChangeListener(SelectionInList.PROPERTYNAME_SELECTION_EMPTY, new SelectionEmptyHandler());
		selectionInList.addPropertyChangeListener(SelectionInList.PROPERTYNAME_SELECTION, new SelectionHandler());
		canceled = false;
	}

	public void open() {
		build();
		canceled = false;
		setVisible(true);
	}

	public void close() {
		dispose();
	}

	public boolean hasBeanCanceled() {
		return canceled;
	}

	private void build() {
		initComponents();
		initComponentAnnotations();
		initEventHandling();
		setContentPane(buildContentPane());
		updateComponentTreeMandatoryAndSeverity(presentationModel.getValidationResultModel().getResult());
		pack();
		// setResizable(false);
		GUIUtils.locateOnOpticalScreenCenter(this);
	}

	private void initComponents() {
		aliasTextField = BasicComponentFactory.createTextField(presentationModel.getBufferedModel(StreamSourceModel.PROPERTY_ALIAS));
		startTimeTextField = BasicComponentFactory.createTextField(presentationModel
				.getBufferedModel(StreamSourceModel.PROPERTY_START_TIME));
		endTimeTextField = BasicComponentFactory.createTextField(presentationModel.getBufferedModel(StreamSourceModel.PROPERTY_END_TIME));
		dbsTextField = BasicComponentFactory.createIntegerField(presentationModel
				.getBufferedModel(StreamSourceModel.PROPERTY_DISCONNECTED_BUFFER_SIZE));
		samplingRateTextField = BasicComponentFactory.createFormattedTextField(presentationModel
				.getBufferedModel(StreamSourceModel.PROPERTY_SAMPLING_RATE), NumberFormat.getNumberInstance());
		historySizeTextField = BasicComponentFactory.createTextField(presentationModel
				.getBufferedModel(StreamSourceModel.PROPERTY_RAW_HISTORY_SIZE));
		queryTextField = BasicComponentFactory.createTextField(presentationModel.getBufferedModel(StreamSourceModel.PROPERTY_SQL_QUERY));

		addressingList = BasicComponentFactory.createList(selectionInList, new AddressBeanListCellRenderer());
		addressBeanEditorPanel = new AddressBeanEditorPanel(null);
		addAction = new AddAction();
		removeAction = new RemoveAction();
		editAction = new EditAction();
		addButton = new JButton(getAddAction());
		removeButton = new JButton(getRemoveAction());
		editButton = new JButton(getEditAction());

		updateActionEnablement();

	}

	private void initComponentAnnotations() {
		ValidationComponentUtils.setMandatory(aliasTextField, true);
		ValidationComponentUtils.setMessageKey(aliasTextField, "StreamSource.Alias");
		ValidationComponentUtils.setMandatory(queryTextField, true);
		ValidationComponentUtils.setMessageKey(queryTextField, "StreamSource.SqlQuery");
	}

	private void initEventHandling() {
		presentationModel.getValidationResultModel().addPropertyChangeListener(ValidationResultModel.PROPERTYNAME_RESULT,
				new ValidationChangeHandler());
	}

	private void updateComponentTreeMandatoryAndSeverity(ValidationResult result) {
		ValidationComponentUtils.updateComponentTreeSeverity(editorPanel, result);
		ValidationComponentUtils.updateComponentTreeMandatoryAndBlankBackground(editorPanel);
	}

	private Action getEditAction() {
		return editAction;
	}

	private Action getRemoveAction() {
		return removeAction;
	}

	private Action getAddAction() {
		return addAction;
	}

	public void updateActionEnablement() {
		boolean hasSelection = selectionInList.hasSelection();
		getEditAction().setEnabled(hasSelection);
		getRemoveAction().setEnabled(hasSelection);
	}

	public class AddressBeanListCellRenderer extends DefaultListCellRenderer {

		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof AddressBeanModel) {
				label.setText(((AddressBeanModel) value).getWrapper());
			}
			return this;
		}

	}

	private class SelectionEmptyHandler implements PropertyChangeListener {
		public void propertyChange(PropertyChangeEvent evt) {
			updateActionEnablement();
		}
	}

	private class SelectionHandler implements PropertyChangeListener {
		public void propertyChange(PropertyChangeEvent evt) {
			if (selectionInList.hasSelection())
				addressBeanEditorPanel.setModel((AddressBeanModel) selectionInList.getSelection());
			else
				addressBeanEditorPanel.setModel(null);
		}
	}

	private class AddAction extends AbstractAction {
		public AddAction() {
			super("Add\u2026");
		}

		public void actionPerformed(ActionEvent e) {
			AddressBeanModel addressBeanModel = new AddressBeanModel();
			AddressBeanEditorDialog dialog = new AddressBeanEditorDialog(StreamSourceEditorDialog.this, addressBeanModel);
			dialog.open();
			boolean canceled = dialog.hasBeanCanceled();
			if (!canceled) {
				streamSourceModel.addAddressBeanModel(addressBeanModel);
			}
		}
	}

	private class EditAction extends AbstractAction {
		public EditAction() {
			super("Edit\u2026");
		}

		public void actionPerformed(ActionEvent e) {
			AddressBeanModel addressBeanModel = (AddressBeanModel) selectionInList.getSelection();
			AddressBeanEditorDialog dialog = new AddressBeanEditorDialog(StreamSourceEditorDialog.this, addressBeanModel);
			dialog.open();
			boolean canceled = dialog.hasBeanCanceled();
			if (!canceled) {
				selectionInList.fireSelectedContentsChanged();
			}
		}
	}

	private class RemoveAction extends AbstractAction {
		public RemoveAction() {
			super("Remove");
		}

		public void actionPerformed(ActionEvent e) {
			streamSourceModel.getAddressing().remove(selectionInList.getSelection());
		}
	}

	private final class ValidationChangeHandler implements PropertyChangeListener {

		public void propertyChange(PropertyChangeEvent evt) {
			updateComponentTreeMandatoryAndSeverity((ValidationResult) evt.getNewValue());
		}
	}

	private JComponent buildContentPane() {
		FormLayout layout = new FormLayout("pref:g", "pref, 6dlu, pref, 6dlu, pref, 6dlu, pref");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.getPanel().setBorder(new EmptyBorder(18, 12, 12, 12));
		CellConstraints cc = new CellConstraints();
		editorPanel = buildEditorPanel();
		builder.add(editorPanel, cc.xy(1, 1));
		builder.add(buildAddressingBeanPanel(), cc.xy(1, 3));
		builder.add(buildDialogButtonBar(), cc.xy(1, 7));
		builder.add(addressBeanEditorPanel.createPanel(), cc.xy(1, 5));
		return builder.getPanel();
	}

	private JComponent buildAddressingBeanPanel() {
		FormLayout layout = new FormLayout("right:max(pref;40), 4dlu, pref:g, 7dlu, pref", "pref, 3dlu, pref");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.addSeparator("Addressing", cc.xyw(1, 1, 5));
		builder.addLabel("Wrappers", cc.xy(1, 3, "right, top"));
		JScrollPane scrollPane = new JScrollPane(addressingList);
		scrollPane.setPreferredSize(new Dimension(200, 200));
		builder.add(scrollPane, cc.xy(3, 3));
		builder.add(createButtomBar(), cc.xy(5, 3));
		return builder.getPanel();
	}

	private JComponent createButtomBar() {
		ButtonStackBuilder builder = new ButtonStackBuilder();
		builder.addGridded(addButton);
		builder.addRelatedGap();
		builder.addGridded(editButton);
		builder.addRelatedGap();
		builder.addGridded(removeButton);
		return builder.getPanel();
	}

	private JComponent buildEditorPanel() {
		FormLayout layout = new FormLayout("right:pref, 4dlu, pref:g",
				"pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, , 3dlu, pref");
		PanelBuilder builder = new PanelBuilder(layout);
		CellConstraints cc = new CellConstraints();
		builder.addLabel("Alias", cc.xy(1, 1));
		builder.add(aliasTextField, cc.xy(3, 1));
		builder.addLabel("Sampling Rate", cc.xy(1, 3));
		builder.add(samplingRateTextField, cc.xy(3, 3));
		builder.addLabel("Start Time", cc.xy(1, 5));
		builder.add(startTimeTextField, cc.xy(3, 5));
		builder.addLabel("End Time", cc.xy(1, 7));
		builder.add(endTimeTextField, cc.xy(3, 7));
		builder.addLabel("History Size", cc.xy(1, 9));
		builder.add(historySizeTextField, cc.xy(3, 9));
		builder.addLabel("Disconnected Buffer Size", cc.xy(1, 11));
		builder.add(dbsTextField, cc.xy(3, 11));
		builder.addLabel("Query", cc.xy(1, 13));
		builder.add(queryTextField, cc.xy(3, 13));
		return builder.getPanel();
	}

	private JComponent buildDialogButtonBar() {
		JPanel bar = ButtonBarFactory.buildOKCancelBar(new JButton(new OKAction()), new JButton(new CancelAction()));
		bar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
		return bar;
	}

	private final class OKAction extends AbstractAction {

		private OKAction() {
			super("OK");
		}

		public void actionPerformed(ActionEvent e) {
			if (!presentationModel.getValidationResultModel().hasErrors()) {
				canceled = false;
				presentationModel.triggerCommit();
				close();
			}
		}
	}

	private final class CancelAction extends AbstractAction {

		private CancelAction() {
			super("Cancel");
		}

		public void actionPerformed(ActionEvent e) {
			canceled = true;
			((StreamSourceModel)presentationModel.getBean()).setAddressing(oldAddressing);
			presentationModel.triggerFlush();
			close();
		}
	}

	private class AddressBeanEditorDialog extends JDialog {
		private boolean canceled;

		private JTextField wrapperNameTextField;

		private AddressBeanPresentationModel presentationModel;

		private JComponent editorPanel;

		public AddressBeanEditorDialog(Dialog parent, AddressBeanModel addressBeanModel) {
			super(parent, "Addressing Editor", true);
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			presentationModel = new AddressBeanPresentationModel(addressBeanModel);
			canceled = false;
		}

		public void open() {
			build();
			canceled = false;
			setVisible(true);
		}

		public void close() {
			dispose();
		}

		public boolean hasBeanCanceled() {
			return canceled;
		}

		private void build() {
			initComponents();
			initComponentAnnotations();
			initEventHandling();
			setContentPane(buildContentPane());
			updateComponentTreeMandatoryAndSeverity(presentationModel.getValidationResultModel().getResult());
			pack();
			setResizable(false);
			GUIUtils.locateOnOpticalScreenCenter(this);
		}

		private void initComponents() {
			wrapperNameTextField = BasicComponentFactory.createTextField(presentationModel
					.getBufferedModel(AddressBeanModel.PROPERTY_WRAPPER));
		}

		private JComponent buildContentPane() {
			FormLayout layout = new FormLayout("pref", "pref, 6dlu, pref");
			PanelBuilder builder = new PanelBuilder(layout);
			builder.getPanel().setBorder(new EmptyBorder(18, 12, 12, 12));
			CellConstraints cc = new CellConstraints();
			editorPanel = buildEditorPanel();
			builder.add(editorPanel, cc.xy(1, 1));
			builder.add(buildButtonBar(), cc.xy(1, 3));
			return builder.getPanel();
		}

		private JComponent buildEditorPanel() {
			FormLayout layout = new FormLayout("right:pref, 4dlu, pref:g", "pref");
			PanelBuilder builder = new PanelBuilder(layout);
			CellConstraints cc = new CellConstraints();
			builder.addLabel("Wrapper name", cc.xy(1, 1));
			builder.add(wrapperNameTextField, cc.xy(3, 1));
			return builder.getPanel();
		}

		private void initComponentAnnotations() {
			ValidationComponentUtils.setMandatory(wrapperNameTextField, true);
			ValidationComponentUtils.setMessageKey(wrapperNameTextField, "AddressBean.Wrapper");
		}

		private void initEventHandling() {
			presentationModel.getValidationResultModel().addPropertyChangeListener(ValidationResultModel.PROPERTYNAME_RESULT,
					new ValidationChangeHandler());
		}

		private void updateComponentTreeMandatoryAndSeverity(ValidationResult result) {
			ValidationComponentUtils.updateComponentTreeSeverity(editorPanel, result);
			ValidationComponentUtils.updateComponentTreeMandatoryAndBlankBackground(editorPanel);
		}

		private JComponent buildButtonBar() {
			JPanel bar = ButtonBarFactory.buildOKCancelBar(new JButton(new OKAction()), new JButton(new CancelAction()));
			bar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
			return bar;
		}

		private final class ValidationChangeHandler implements PropertyChangeListener {

			public void propertyChange(PropertyChangeEvent evt) {
				updateComponentTreeMandatoryAndSeverity((ValidationResult) evt.getNewValue());
			}
		}

		private final class OKAction extends AbstractAction {

			private OKAction() {
				super("OK");
			}

			public void actionPerformed(ActionEvent e) {
				if (!presentationModel.getValidationResultModel().hasErrors()) {
					canceled = false;
					presentationModel.triggerCommit();
					close();
				}
			}
		}

		private final class CancelAction extends AbstractAction {

			private CancelAction() {
				super("Cancel");
			}

			public void actionPerformed(ActionEvent e) {
				canceled = true;
				presentationModel.triggerFlush();
				close();
			}
		}
	}
}