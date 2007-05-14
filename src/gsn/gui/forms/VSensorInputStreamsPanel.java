package gsn.gui.forms;

import gsn.gui.beans.InputStreamModel;
import gsn.gui.beans.VSensorConfigModel;
import gsn.gui.util.GUIUtil;

import java.awt.Component;
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
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableModel;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.AbstractTableAdapter;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.adapter.SingleListSelectionAdapter;
import com.jgoodies.binding.beans.BeanAdapter;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.forms.builder.ButtonStackBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class VSensorInputStreamsPanel {
	private PresentationModel presentationModel;
	private SelectionInList selectionInList;
	private VSensorConfigModel vSensorConfigModel;
	
	private JTable table;
	private JTextField queryTextField;
	private JButton addButton;

	private Action addAction;

	private Action removeAction;

	private Action editAction;

	private JButton removeButton;

	private JButton editButton;
	
	private StreamSourceEditorPanel streamSourceEditorPanel;

	public VSensorInputStreamsPanel(PresentationModel presentationModel) {
		this.presentationModel = presentationModel;
		vSensorConfigModel = (VSensorConfigModel) presentationModel.getBean();
		selectionInList = new SelectionInList((ListModel) vSensorConfigModel.getInputStreams());
		selectionInList.addPropertyChangeListener(SelectionInList.PROPERTYNAME_SELECTION_EMPTY, new SelectionEmptyHandler());
		selectionInList.addPropertyChangeListener(SelectionInList.PROPERTYNAME_SELECTION, new SelectionHandler());
	}

	public Component createPanel() {
		initComponents();
		
		FormLayout layout = new FormLayout("right:max(pref;60), 4dlu, max(pref;150dlu):g", "pref, 8dlu, pref");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.add(createTablePanel(), cc.xyw(1, 1, 3));
		builder.add(streamSourceEditorPanel.createPanel(), cc.xyw(1, 3, 3));
		return builder.getPanel();
	}
	
	private JComponent createTablePanel() {
		FormLayout layout = new FormLayout("right:max(pref;60), 4dlu, pref:g, 7dlu, pref", "pref, 4dlu, pref");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.addLabel("Input streams", cc.xy(1, 1, "right, top"));
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setPreferredSize(new Dimension(350, 200));
		builder.add(scrollPane, cc.xy(3, 1));
		builder.addLabel("Query", cc.xy(1, 3));
		builder.add(queryTextField, cc.xy(3, 3));
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
	
	private void initComponents() {
		table = new JTable();
		table.setModel(new InputStreamTableModel(selectionInList));
		table.setSelectionModel(new SingleListSelectionAdapter(selectionInList.getSelectionIndexHolder()));
		
		addAction = new AddAction();
		removeAction = new RemoveAction();
		editAction = new EditAction();
		addButton = new JButton(getAddAction());
		removeButton = new JButton(getRemoveAction());
		editButton = new JButton(getEditAction());
		
		queryTextField = BasicComponentFactory.createTextField(new BeanAdapter(selectionInList, true).getValueModel(InputStreamModel.PROPERTY_QUERY));
		queryTextField.setEditable(false);
		
		streamSourceEditorPanel = new StreamSourceEditorPanel(null);
		
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
	}
	
	public static class InputStreamTableModel extends AbstractTableAdapter implements TableModel {

		private static final String[] COLUMNS = { "Name", "Count", "Rate" };

		public InputStreamTableModel(SelectionInList selectionInList) {
			super(selectionInList, COLUMNS);
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			InputStreamModel inputStreamModel = (InputStreamModel) getRow(rowIndex);
			switch (columnIndex) {
			case 0:
				return inputStreamModel.getInputStreamName();
			case 1:
				return inputStreamModel.getCount();
			case 2:
				return inputStreamModel.getRate();
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
	
	private class SelectionHandler implements PropertyChangeListener {
		public void propertyChange(PropertyChangeEvent evt) {
			if(selectionInList.hasSelection())
				streamSourceEditorPanel.setListModel(((InputStreamModel)selectionInList.getSelection()).getSources());
			else
				streamSourceEditorPanel.setListModel(null);
		}
	}

	private class AddAction extends AbstractAction {
		public AddAction() {
			super("Add\u2026");
		}

		public void actionPerformed(ActionEvent e) {
			InputStreamModel inputStreamModel = new InputStreamModel();
			InputStreamEditorDialog dialog = new InputStreamEditorDialog(null, inputStreamModel);
			dialog.open();
			boolean canceled = dialog.hasBeanCanceled();
			if (!canceled) {
				vSensorConfigModel.addInputStreamModel(inputStreamModel);
			}
		}
	}

	private class EditAction extends AbstractAction {
		public EditAction() {
			super("Edit\u2026");
		}

		public void actionPerformed(ActionEvent e) {
			InputStreamModel inputStreamModel = (InputStreamModel) selectionInList.getSelection();
			InputStreamEditorDialog dialog = new InputStreamEditorDialog(null, inputStreamModel);
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
			vSensorConfigModel.getInputStreams().remove(selectionInList.getSelection());
		}
	}
	
	private class InputStreamEditorDialog extends JDialog {
		private boolean canceled;

		private InputStreamModel inputStreamModel;
		private PresentationModel presentationModel;

		private JTextField nameTextField;
		private JTextField countTextField;
		private JTextField rateTextField;
		private JTextField queryTextField;

		public InputStreamEditorDialog(Frame parent, InputStreamModel dataFieldModel) {
			super(parent, "Input Stream Editor", true);
			this.inputStreamModel = dataFieldModel;
			presentationModel = new PresentationModel(dataFieldModel);
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
			setContentPane(buildContentPane());
			pack();
//			setResizable(false);
			GUIUtil.locateOnOpticalScreenCenter(this);
		}
		
		private void initComponents(){
			nameTextField = BasicComponentFactory.createTextField(presentationModel.getModel(InputStreamModel.PROPERTY_INPUT_STREAM_NAME));
			countTextField = BasicComponentFactory.createLongField(presentationModel.getModel(InputStreamModel.PROPERTY_COUNT));
			rateTextField = BasicComponentFactory.createIntegerField(presentationModel.getModel(InputStreamModel.PROPERTY_RATE));
			queryTextField = BasicComponentFactory.createTextField(presentationModel.getModel(InputStreamModel.PROPERTY_QUERY));
			
		}

		private JComponent buildContentPane() {
			FormLayout layout = new FormLayout("pref:g", "pref, 6dlu, pref");
			PanelBuilder builder = new PanelBuilder(layout);
			builder.getPanel().setBorder(new EmptyBorder(18, 12, 12, 12));
			CellConstraints cc = new CellConstraints();
			builder.add(buildEditorPanel(), cc.xy(1, 1));
			builder.add(buildButtonBar(), cc.xy(1, 3));
			return builder.getPanel();
		}

		private JComponent buildEditorPanel() {
			FormLayout layout = new FormLayout("right:pref, 4dlu, pref:g", "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");
			PanelBuilder builder = new PanelBuilder(layout);
			CellConstraints cc = new CellConstraints();
			builder.addLabel("Name", cc.xy(1, 1));
			builder.add(nameTextField, cc.xy(3, 1));
			builder.addLabel("Count", cc.xy(1, 3));
			builder.add(countTextField, cc.xy(3, 3));
			builder.addLabel("Rate", cc.xy(1, 5));
			builder.add(rateTextField, cc.xy(3, 5));
			builder.addLabel("Query", cc.xy(1, 7));
			builder.add(queryTextField, cc.xy(3, 7));
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
				canceled = false;
				presentationModel.setBean(null);
				close();
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
	}
	

}
