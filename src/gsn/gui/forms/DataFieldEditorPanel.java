package gsn.gui.forms;

import gsn.gui.beans.DataFieldModel;
import gsn.gui.beans.DataFieldPresentationModel;
import gsn.gui.util.GUIUtils;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableModel;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.AbstractTableAdapter;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.adapter.SingleListSelectionAdapter;
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

public class DataFieldEditorPanel {
	private SelectionInList selectionInList;

	private ArrayListModel dataFieldListModel;

	private JTable table;

	private JButton addButton;

	private Action addAction;

	private Action removeAction;

	private Action editAction;

	private JButton removeButton;

	private JButton editButton;

	public DataFieldEditorPanel(ArrayListModel dataFieldListModel) {
		this.dataFieldListModel = dataFieldListModel;
		selectionInList = new SelectionInList((ListModel) dataFieldListModel);
		selectionInList.addPropertyChangeListener(SelectionInList.PROPERTYNAME_SELECTION_EMPTY, new SelectionEmptyHandler());
	}

	public void setListModel(ArrayListModel dataFieldListModel) {
		this.dataFieldListModel = dataFieldListModel;
		selectionInList.setList(dataFieldListModel);
		updateActionEnablement();
	}

	public JComponent createPanel() {
		initConponents();

		FormLayout layout = new FormLayout("right:max(pref;60), 4dlu, pref:g, 7dlu, pref", "pref");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.addLabel("Fields", cc.xy(1, 1, "right, top"));
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setPreferredSize(new Dimension(200, 200));
		builder.add(scrollPane, cc.xy(3, 1));
		builder.add(createButtomBar(), cc.xy(5, 1));
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

	private void initConponents() {
		table = new JTable();
		table.setModel(new DataFieldTableModel(selectionInList));
		table.setSelectionModel(new SingleListSelectionAdapter(selectionInList.getSelectionIndexHolder()));
		addAction = new AddAction();
		removeAction = new RemoveAction();
		editAction = new EditAction();
		addButton = new JButton(getAddAction());
		removeButton = new JButton(getRemoveAction());
		editButton = new JButton(getEditAction());

		updateActionEnablement();
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
		getAddAction().setEnabled(dataFieldListModel != null);
	}

	public static class DataFieldTableModel extends AbstractTableAdapter implements TableModel {

		private static final String[] COLUMNS = { "Name", "Type", "Description" };

		public DataFieldTableModel(SelectionInList selectionInList) {
			super(selectionInList, COLUMNS);
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			DataFieldModel dataFieldModel = (DataFieldModel) getRow(rowIndex);
			switch (columnIndex) {
			case 0:
				return dataFieldModel.getName();
			case 1:
				return dataFieldModel.getType();
			case 2:
				return dataFieldModel.getDescription();
			default:
				throw new IllegalStateException("Unknown column");
			}
		}
	}

	private class SelectionEmptyHandler implements PropertyChangeListener {
		public void propertyChange(PropertyChangeEvent evt) {
			updateActionEnablement();
		}
	}

	private class AddAction extends AbstractAction {
		public AddAction() {
			super("Add\u2026");
		}

		public void actionPerformed(ActionEvent e) {
			DataFieldModel dataFieldModel = new DataFieldModel();
			DataFieldEditorDialog dialog = new DataFieldEditorDialog(null, dataFieldModel);
			dialog.open();
			boolean canceled = dialog.hasBeanCanceled();
			if (!canceled) {
				dataFieldListModel.add(dataFieldModel);
			}
		}
	}

	private class EditAction extends AbstractAction {
		public EditAction() {
			super("Edit\u2026");
		}

		public void actionPerformed(ActionEvent e) {
			DataFieldModel dataFieldModel = (DataFieldModel) selectionInList.getSelection();
			DataFieldEditorDialog dialog = new DataFieldEditorDialog(null, dataFieldModel);
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
			dataFieldListModel.remove(selectionInList.getSelection());
		}
	}

	private class DataFieldEditorDialog extends JDialog {
		private boolean canceled;

		private DataFieldModel dataFieldModel;

		private DataFieldPresentationModel presentationModel;

		private JTextField nameTextField;

		private JTextField typeTextField;

		private JTextField descriptionTextField;

		private JComponent editorPanel;

		public DataFieldEditorDialog(Frame parent, DataFieldModel dataFieldModel) {
			super(parent, "Data Field Editor", true);
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			this.dataFieldModel = dataFieldModel;
			presentationModel = new DataFieldPresentationModel(dataFieldModel);
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
			pack();
			// setResizable(false);
			GUIUtils.locateOnOpticalScreenCenter(this);
		}

		private void initComponents() {
			nameTextField = BasicComponentFactory.createTextField(presentationModel.getBufferedModel(DataFieldModel.PROPERTY_NAME));
			typeTextField = BasicComponentFactory.createTextField(presentationModel.getBufferedModel(DataFieldModel.PROPERTY_TYPE));
			descriptionTextField = BasicComponentFactory.createTextField(presentationModel.getBufferedModel(DataFieldModel.PROPERTY_DESCRIPTION));

		}

		private void initComponentAnnotations() {
			ValidationComponentUtils.setMandatory(nameTextField, true);
			ValidationComponentUtils.setMessageKey(nameTextField, "DataField.Name");
			ValidationComponentUtils.setMandatory(typeTextField, true);
			ValidationComponentUtils.setMessageKey(typeTextField, "DataField.Type");
		}

		private void initEventHandling() {
			presentationModel.getValidationResultModel().addPropertyChangeListener(ValidationResultModel.PROPERTYNAME_RESULT,
					new ValidationChangeHandler());
		}

		private void updateComponentTreeMandatoryAndSeverity(ValidationResult result) {
			ValidationComponentUtils.updateComponentTreeSeverity(editorPanel, result);
			ValidationComponentUtils.updateComponentTreeMandatoryAndBlankBackground(editorPanel);
		}

		private JComponent buildContentPane() {
			FormLayout layout = new FormLayout("pref:g", "pref, 6dlu, pref");
			PanelBuilder builder = new PanelBuilder(layout);
			builder.getPanel().setBorder(new EmptyBorder(18, 12, 12, 12));
			CellConstraints cc = new CellConstraints();
			editorPanel = buildEditorPanel();
			builder.add(editorPanel, cc.xy(1, 1));
			builder.add(buildButtonBar(), cc.xy(1, 3));

			updateComponentTreeMandatoryAndSeverity(presentationModel.getValidationResultModel().getResult());
			return builder.getPanel();
		}

		private JComponent buildEditorPanel() {
			FormLayout layout = new FormLayout("right:pref, 4dlu, pref:g", "pref, 3dlu, pref, 3dlu, pref");
			PanelBuilder builder = new PanelBuilder(layout);
			CellConstraints cc = new CellConstraints();
			builder.addLabel("Name", cc.xy(1, 1));
			builder.add(nameTextField, cc.xy(3, 1));
			builder.addLabel("Type", cc.xy(1, 3));
			builder.add(typeTextField, cc.xy(3, 3));
			builder.addLabel("Description", cc.xy(1, 5));
			builder.add(descriptionTextField, cc.xy(3, 5));
			return builder.getPanel();
		}

		private JComponent buildButtonBar() {
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
					presentationModel.triggerCommit();
					canceled = false;
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

		private final class ValidationChangeHandler implements PropertyChangeListener {

			public void propertyChange(PropertyChangeEvent evt) {
				updateComponentTreeMandatoryAndSeverity((ValidationResult) evt.getNewValue());
			}
		}

	}
}
