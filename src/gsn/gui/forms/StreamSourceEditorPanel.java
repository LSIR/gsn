package gsn.gui.forms;

import gsn.gui.beans.StreamSourceModel;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListModel;
import javax.swing.table.TableModel;

import com.jgoodies.binding.adapter.AbstractTableAdapter;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.adapter.SingleListSelectionAdapter;
import com.jgoodies.binding.beans.BeanAdapter;
import com.jgoodies.binding.list.ArrayListModel;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.forms.builder.ButtonStackBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class StreamSourceEditorPanel {
	private SelectionInList selectionInList;

	private ArrayListModel streamSourceListModel;

	private JTable table;

	private JTextArea queryTextArea;

	private JButton addButton;

	private Action addAction;

	private Action removeAction;

	private Action editAction;

	private JButton removeButton;

	private JButton editButton;

	public StreamSourceEditorPanel(ArrayListModel streamSourceListModel) {
		this.streamSourceListModel = streamSourceListModel;
		selectionInList = new SelectionInList((ListModel) streamSourceListModel);
		selectionInList.addPropertyChangeListener(SelectionInList.PROPERTYNAME_SELECTION_EMPTY, new SelectionEmptyHandler());
	}

	public void setListModel(ArrayListModel streamSourceListModel) {
		this.streamSourceListModel = streamSourceListModel;
		selectionInList.setList(streamSourceListModel);
		updateActionEnablement();
	}

	public JComponent createPanel() {
		initConponents();

		FormLayout layout = new FormLayout("right:max(pref;50), 4dlu, min(pref;150dlu):g, 7dlu, pref", "pref, 4dlu, min(pref;70dlu):g");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.addLabel("Stream sources", cc.xy(1, 1, "right, top"));
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setPreferredSize(new Dimension(350, 150));
		builder.add(scrollPane, cc.xy(3, 1));
		builder.add(createButtomBar(), cc.xy(5, 1));
		builder.addLabel("Query", cc.xy(1, 3));
		builder.add(new JScrollPane(queryTextArea), cc.xy(3, 3));
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
		table.setModel(new StreamSourceTableModel(selectionInList));
		table.setSelectionModel(new SingleListSelectionAdapter(selectionInList.getSelectionIndexHolder()));

		addAction = new AddAction();
		removeAction = new RemoveAction();
		editAction = new EditAction();
		addButton = new JButton(getAddAction());
		removeButton = new JButton(getRemoveAction());
		editButton = new JButton(getEditAction());

		queryTextArea = BasicComponentFactory.createTextArea(new BeanAdapter(selectionInList, true)
				.getValueModel(StreamSourceModel.PROPERTY_SQL_QUERY));
		queryTextArea.setLineWrap(true);
		queryTextArea.setWrapStyleWord(true);

		queryTextArea.setEditable(false);
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
		getAddAction().setEnabled(streamSourceListModel != null);
	}

	public static class StreamSourceTableModel extends AbstractTableAdapter implements TableModel {

		private static final String[] COLUMNS = { "Alias", "Sampling Rate", "History Size", "Disconnected Buffer Size" };

		public StreamSourceTableModel(SelectionInList selectionInList) {
			super(selectionInList, COLUMNS);
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			StreamSourceModel streamSourceModel = (StreamSourceModel) getRow(rowIndex);
			switch (columnIndex) {
			case 0:
				return streamSourceModel.getAlias();
			case 1:
				return streamSourceModel.getSamplingRate();
				// case 2:
				// return streamSourceModel.getStartTime();
				// case 3:
				// return streamSourceModel.getEndTime();
			case 2:
				return streamSourceModel.getRawHistorySize();
			case 3:
				return streamSourceModel.getDisconnectedBufferSize();
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
			StreamSourceModel streamSourceModel = new StreamSourceModel();
			StreamSourceEditorDialog dialog = new StreamSourceEditorDialog(null, streamSourceModel);
			dialog.open();
			boolean canceled = dialog.hasBeanCanceled();
			if (!canceled) {
				streamSourceListModel.add(streamSourceModel);
			}
		}
	}

	private class EditAction extends AbstractAction {
		public EditAction() {
			super("Edit\u2026");
		}

		public void actionPerformed(ActionEvent e) {
			StreamSourceModel streamSourceModel = (StreamSourceModel) selectionInList.getSelection();
			StreamSourceEditorDialog dialog = new StreamSourceEditorDialog(null, streamSourceModel);
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
			streamSourceListModel.remove(selectionInList.getSelection());
		}
	}

}
