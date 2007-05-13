package gsn.gui.forms;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import gsn.gui.beans.VSensorConfigModel;
import gsn.gui.beans.WebInputModel;

import gsn.gui.util.GUIUtil;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.border.EmptyBorder;

import org.apache.commons.collections.KeyValue;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.forms.builder.ButtonStackBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class VSensorWebInpuPanel {

	private PresentationModel presentationModel;

	private SelectionInList selectionInList;

	private JTextField passwordTextField;

	private JList commandList;

	private JButton addButton;

	private Action addAction;

	private Action removeAction;

	private Action editAction;

	private JButton removeButton;

	private JButton editButton;

	private VSensorConfigModel vSensorConfigModel;
	
	private DataFieldEditorPanel dataFieldEditorPanel;

	public VSensorWebInpuPanel(PresentationModel presentationModel) {
		this.presentationModel = presentationModel;
		vSensorConfigModel = (VSensorConfigModel) presentationModel.getBean();
		selectionInList = new SelectionInList((ListModel) vSensorConfigModel.getWebinput());
		selectionInList.addPropertyChangeListener(SelectionInList.PROPERTYNAME_SELECTION_EMPTY, new SelectionEmptyHandler());
		selectionInList.addPropertyChangeListener(SelectionInList.PROPERTYNAME_SELECTION, new SelectionHandler());
	}

	public JComponent createPanel() {
		initComponents();
		
		FormLayout layout = new FormLayout("right:pref, 4dlu, max(pref;150dlu), pref:g", "pref, 8dlu, pref, 5dlu, pref, 5dlu, pref");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.addLabel("Password", cc.xy(1, 1));
		builder.add(passwordTextField, cc.xy(3, 1));
		builder.addSeparator("Commands", cc.xyw(1, 3, 4));
		builder.add(createListPanel(), cc.xyw(1, 5, 4));
		builder.add(dataFieldEditorPanel.createPanel(), cc.xyw(1, 7, 4));
		return builder.getPanel();
	}

	private JComponent createListPanel() {
		FormLayout layout = new FormLayout("pref, pref", "pref");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.add(new JScrollPane(commandList), cc.xy(1, 1));
		builder.add(createButtomBar(), cc.xy(2, 1));
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
		passwordTextField = BasicComponentFactory.createTextField(presentationModel
				.getModel(VSensorConfigModel.PROPERTY_WEB_PARAMETER_PASSWORD));
		commandList = BasicComponentFactory.createList(selectionInList);
		dataFieldEditorPanel = new DataFieldEditorPanel(null);
		addAction = new AddAction();
		removeAction = new RemoveAction();
		editAction = new EditAction();
		addButton = new JButton(getAddAction());
		removeButton = new JButton(getRemoveAction());
		editButton = new JButton(getEditAction());
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

	private class SelectionEmptyHandler implements PropertyChangeListener {
		public void propertyChange(PropertyChangeEvent evt) {
			updateActionEnablement();
		}
	}
	
	private class SelectionHandler implements PropertyChangeListener {
		public void propertyChange(PropertyChangeEvent evt) {
			if(selectionInList.hasSelection())
				dataFieldEditorPanel.setListModel(((WebInputModel)selectionInList.getSelection()).getParameters());
			else
				dataFieldEditorPanel.setListModel(null);
		}
	}

	private class AddAction extends AbstractAction {
		public AddAction() {
			super("Add\u2026");
		}

		public void actionPerformed(ActionEvent e) {
			WebInputModel webInputModel = new WebInputModel();
			WebINputEditorDialog dialog = new WebINputEditorDialog(null, webInputModel);
			dialog.open();
			boolean canceled = dialog.hasBeanCanceled();
			if (!canceled) {
				vSensorConfigModel.addWebInputModel(webInputModel);
			}
		}
	}

	private class EditAction extends AbstractAction {
		public EditAction() {
			super("Edit\u2026");
		}

		public void actionPerformed(ActionEvent e) {
			WebInputModel webInputModel = (WebInputModel) selectionInList.getSelection();
			WebINputEditorDialog dialog = new WebINputEditorDialog(null, webInputModel);
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
			vSensorConfigModel.getWebinput().remove(selectionInList.getSelection());
		}
	}

	private class WebINputEditorDialog extends JDialog {
		private boolean canceled;

		private JTextField commandNameTextField;
		
		private PresentationModel presentationModel;


		public WebINputEditorDialog(Frame parent, WebInputModel webInputModel) {
			super(parent, "Web Input Editor", true);
			presentationModel = new PresentationModel(webInputModel);
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
			pack();
			setResizable(false);
			GUIUtil.locateOnOpticalScreenCenter(this);
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
			commandNameTextField = BasicComponentFactory.createTextField(presentationModel.getModel(WebInputModel.PROPERTY_NAME));
			FormLayout layout = new FormLayout("right:pref, 4dlu, pref:g", "pref");
			PanelBuilder builder = new PanelBuilder(layout);
			CellConstraints cc = new CellConstraints();
			builder.addLabel("Command name", cc.xy(1, 1));
			builder.add(commandNameTextField, cc.xy(3, 1));
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
