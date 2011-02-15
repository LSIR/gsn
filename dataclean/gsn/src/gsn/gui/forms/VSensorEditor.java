package gsn.gui.forms;

import gsn.beans.VSensorConfig;
import gsn.gui.beans.VSensorConfigModel;
import gsn.gui.beans.VSensorConfigPresentationModel;
import gsn.gui.util.GUIUtils;
import gsn.gui.util.VSensorConfigUtil;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.jibx.runtime.JiBXException;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.validation.util.ValidationUtils;

public class VSensorEditor {
	private VSensorConfigPresentationModel presentationModel;

	private JDialog dialog;

	private boolean canceled;

	// Initial virtual sensor config
	private VSensorConfig oldVSensorConfig;

	public VSensorEditor(VSensorConfig vSensorConfig) {
		presentationModel = new VSensorConfigPresentationModel(new VSensorConfigModel(vSensorConfig));
		oldVSensorConfig = vSensorConfig;
		canceled = false;
		dialog = new JDialog((Frame) null, true);
		dialog.setIconImage(VSensorVisualizerPanel.VS_EDIT_ICON.getImage());
		dialog.setTitle("Virtual Sensor Editor");
		dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	}

	public VSensorConfig getVSensorConfig() {
		return ((VSensorConfigModel) presentationModel.getBean()).getVSensorConfig();
	}

	public void open() {
		build();
		canceled = false;
		dialog.setVisible(true);
	}

	public void close() {
		dialog.dispose();
	}

	public boolean hasBeenCanceled() {
		return canceled;
	}

	private void build() {
		dialog.setContentPane(buildContentPane());
		dialog.pack();
		GUIUtils.locateOnOpticalScreenCenter(dialog);
	}

	public void showDialog() {
		dialog.setVisible(true);
	}

	private JComponent buildContentPane() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(createTabbedPane(), BorderLayout.CENTER);
		panel.add(buildButtonBar(), BorderLayout.SOUTH);
		return panel;
	}

	private JComponent createTabbedPane() {
		JTabbedPane tabbedPane = new JTabbedPane();
		// tabbedPane.putClientProperty("jgoodies.noContentBorder",
		// Boolean.TRUE);
		tabbedPane.addTab("General", new VSensorGeneralPanel(presentationModel).createPanel());
		tabbedPane.addTab("Processing Class", new VSensorProcessingClassPanel(presentationModel).createPanel());
		tabbedPane.addTab("Input Streams", new VSensorInputStreamsPanel(presentationModel).createPanel());
		return tabbedPane;
	}

	private JComponent buildButtonBar() {
		JPanel bar = ButtonBarFactory.buildOKCancelBar(new JButton(new SaveAction()), new JButton(new CancelAction()));
		bar.setBorder(Borders.DLU7_BORDER);
		return bar;
	}

	private final class SaveAction extends AbstractAction {

		private SaveAction() {
			super("Save");
		}

		public void actionPerformed(ActionEvent e) {
			presentationModel.updateValidationResult();
			if (!presentationModel.getValidationResultModel().hasErrors()) {
				// This is a new VSensor Config
				if (oldVSensorConfig.getFileName() == null) {
					String fileName = JOptionPane.showInputDialog("Please specify the file name");
					if (!ValidationUtils.isBlank(fileName)) {
						int index = fileName.lastIndexOf(".xml");
						if (index == -1)
							fileName = fileName + ".xml";
						saveVSensorConfig(fileName, true);
					} else {
						// TODO: this message should not be displayed when
						// dialog is canceled
						GUIUtils.showErrorMessage("Invalid file name");
					}
				} else {
					saveVSensorConfig(oldVSensorConfig.getFileName(), false);
					// TODO : show proper message
				}
			} else {
				GUIUtils.showValidationMessage(e, "Some errors exist, please correct them:", presentationModel.getValidationResultModel()
						.getResult());
			}
		}

		private void saveVSensorConfig(String fileName, boolean newFile) {
			File file;
			if (newFile)
				file = new File(GUIConfig.VSENSOR_DISABLED_DIR_PATH, fileName);
			else
				file = new File(fileName);
			if (!file.exists()
					|| JOptionPane.showConfirmDialog(dialog, "<html>The file &lt;" + file.getAbsolutePath()
							+ "&gt; already exists.<br>Do you want to overwrite it?</html>", "Confirmation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				try {
					presentationModel.triggerCommit();
					VSensorConfigUtil.saveVSensorConfig(getVSensorConfig(), file);
					canceled = false;
					close();
				} catch (FileNotFoundException e1) {
					GUIUtils.showErrorMessage("Can not write file");
					e1.printStackTrace();
				} catch (JiBXException e1) {
					GUIUtils.showErrorMessage("Error in virtual sensor definition, saving the virtual sensor was aborted");
					e1.printStackTrace();
				}
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

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel("com.jgoodies.looks.plastic.PlasticXPLookAndFeel");
		} catch (Exception e) {
			// Likely PlasticXP is not in the class path; ignore.
		}
		VSensorConfig sensorConfig = new VSensorConfig();
		sensorConfig.setName("name");
		new VSensorEditor(sensorConfig).showDialog();
	}

}
