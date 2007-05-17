package gsn.gui.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.jgoodies.validation.ValidationResult;

public class GUIUtils {

	public static void locateOnOpticalScreenCenter(Component component) {
		Dimension paneSize = component.getSize();
		Dimension screenSize = component.getToolkit().getScreenSize();
		component.setLocation((screenSize.width - paneSize.width) / 2, (int) ((screenSize.height - paneSize.height) * 0.45));
	}

	public static void showValidationMessage(ActionEvent e, String headerText, ValidationResult validationResult) {
		if (validationResult.isEmpty())
			throw new IllegalArgumentException("The validation result must not be empty.");

		Object eventSource = e.getSource();
		Component parent = null;
		if (eventSource instanceof Component) {
			parent = SwingUtilities.windowForComponent((Component) eventSource);
		}
		boolean error = validationResult.hasErrors();
		String messageText = headerText + "\n\n" + validationResult.getMessagesText() + "\n\n";
		String titleText = "Validation " + (error ? "Error" : "Warning");
		int messageType = error ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE;
		JOptionPane.showMessageDialog(parent, messageText, titleText, messageType);
	}

	public static boolean isBlank(String str) {
		int length;
		if (str == null || (length = str.trim().length()) == 0)
			return true;
		for (int i = length - 1; i >= 0; i--) {
			System.out.println("character : " + i + " " + (int) str.charAt(i));
			if (!Character.isWhitespace(str.charAt(i)))
				return false;
		}
		return false;
	}

	public static void showErrorMessage(String message) {
		JOptionPane.showMessageDialog(null, "<html><center>" + message, "Error", JOptionPane.ERROR_MESSAGE);
	}

	public static void showMessage(String message, String title) {
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE);
	}
}
