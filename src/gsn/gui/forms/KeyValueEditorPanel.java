package gsn.gui.forms;

import gsn.gui.util.GUIUtils;
import gsn.utils.KeyValueImp;

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

import org.apache.commons.collections.KeyValue;

import com.jgoodies.binding.adapter.AbstractTableAdapter;
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
import com.jgoodies.validation.Validator;
import com.jgoodies.validation.util.ValidationUtils;
import com.jgoodies.validation.view.ValidationComponentUtils;

public class KeyValueEditorPanel {

	private SelectionInList selectionInList;

	private ArrayListModel keyValueList;

	private JTable table;

	private JButton addButton;

	private Action addAction;

	private Action removeAction;

	private Action editAction;

	private JButton removeButton;

	private JButton editButton;

	public KeyValueEditorPanel(ArrayListModel keyValueList) {
		this.keyValueList = keyValueList;
		selectionInList = new SelectionInList((ListModel) this.keyValueList);
		selectionInList.addPropertyChangeListener(SelectionInList.PROPERTYNAME_SELECTION_EMPTY, new SelectionEmptyHandler());
	}

	public void setListModel(ArrayListModel keyValueList) {
		this.keyValueList = keyValueList;
		selectionInList.setList(keyValueList);
		updateActionEnablement();
	}

	public JComponent createPanel() {
		initComponents();

		FormLayout layout = new FormLayout("pref:g, 7dlu, pref", "pref");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setPreferredSize(new Dimension(200, 200));
		builder.add(scrollPane, cc.xy(1, 1));
		builder.add(createButtomBar(), cc.xy(3, 1));
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

	private void initComponents() {
		table = new JTable();
		table.setModel(new KeyValueTableModel(selectionInList));
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
		getAddAction().setEnabled(keyValueList != null);
	}

	public static class KeyValueTableModel extends AbstractTableAdapter implements TableModel {

		private static final String[] COLUMNS = { "Key", "Value" };

		public KeyValueTableModel(SelectionInList selectionInList) {
			super(selectionInList, COLUMNS);
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			KeyValue keyValue = (KeyValue) getRow(rowIndex);
			switch (columnIndex) {
			case 0:
				return keyValue.getKey();
			case 1:
				return keyValue.getValue();
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
			KeyValueImp keyValueImp = new KeyValueImp("", "");
			KeyValueEditorDialog dialog = new KeyValueEditorDialog(null, keyValueImp);
			dialog.open();
			boolean canceled = dialog.hasBeanCanceled();
			if (!canceled) {
				keyValueList.add(keyValueImp);
			}
		}
	}

	private class EditAction extends AbstractAction {
		public EditAction() {
			super("Edit\u2026");
		}

		public void actionPerformed(ActionEvent e) {
			KeyValueImp keyValueImp = (KeyValueImp) selectionInList.getSelection();
			KeyValueEditorDialog dialog = new KeyValueEditorDialog(null, keyValueImp);
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
			keyValueList.remove(selectionInList.getSelection());
		}
	}

	private class KeyValueEditorDialog extends JDialog {
		private boolean canceled;

		private KeyValue keyValue;

		private JTextField keyTextField;

		private JTextField valueTextField;

		public KeyValueEditorDialog(Frame parent, KeyValue keyValue) {
			super(parent, "Key-Value Editor", true);
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			this.keyValue = keyValue;
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
			setContentPane(buildContentPane());
			initComponentAnnotations();
			pack();
			setResizable(false);
			GUIUtils.locateOnOpticalScreenCenter(this);
		}

		private JComponent buildContentPane() {
			FormLayout layout = new FormLayout("pref", "pref, 6dlu, pref");
			PanelBuilder builder = new PanelBuilder(layout);
			builder.getPanel().setBorder(new EmptyBorder(18, 12, 12, 12));
			CellConstraints cc = new CellConstraints();
			builder.add(buildEditorPanel(), cc.xy(1, 1));
			builder.add(buildButtonBar(), cc.xy(1, 3));
			return builder.getPanel();
		}

		private JComponent buildEditorPanel() {
			keyTextField = new JTextField(String.valueOf(keyValue.getKey()));
			valueTextField = new JTextField(String.valueOf(keyValue.getValue()));
			FormLayout layout = new FormLayout("right:pref, 4dlu, pref:g", "pref, 3dlu, pref");
			PanelBuilder builder = new PanelBuilder(layout);
			CellConstraints cc = new CellConstraints();
			builder.addLabel("Key", cc.xy(1, 1));
			builder.add(keyTextField, cc.xy(3, 1));
			builder.addLabel("Value", cc.xy(1, 3));
			builder.add(valueTextField, cc.xy(3, 3));
			return builder.getPanel();
		}

		private void initComponentAnnotations() {
			ValidationComponentUtils.setMandatory(keyTextField, true);
			ValidationComponentUtils.setMessageKey(keyTextField, "KeyValue.key");
			ValidationComponentUtils.setMandatory(valueTextField, true);
			ValidationComponentUtils.setMessageKey(valueTextField, "KeyValue.value");
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
				canceled = false;
				Validator validator = new KeyValueValidator();
				ValidationResult validationResult = validator.validate();
				if (validationResult.hasErrors()) {
					GUIUtils.showValidationMessage(e, "Please fix the following errors:", validationResult);
				} else {
					((KeyValueImp) keyValue).setKey(keyTextField.getText());
					((KeyValueImp) keyValue).setValue(valueTextField.getText());
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
				close();
			}
		}
		
		private final class KeyValueValidator implements Validator{
			public ValidationResult validate() {
				ValidationResult result = new ValidationResult();
				if(ValidationUtils.isBlank(keyTextField.getText()))
					result.addError("Key is empty");
				if(ValidationUtils.isBlank(valueTextField.getText())){
					result.addError("Value is empty");
				}
				return result;
			}
			
		}
	}

}
