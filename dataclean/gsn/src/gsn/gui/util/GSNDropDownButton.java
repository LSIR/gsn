package gsn.gui.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.border.Border;
import org.openide.util.Utilities;

public class GSNDropDownButton extends Box {
	private JPopupMenu popupMenu;

	private JButton dropDownButton;

	private JButton mainButton;

	private Action activeAction;

	private ActionListener menuItemActionListener;

	public GSNDropDownButton() {
		super(BoxLayout.X_AXIS);

		ButtonsMouseListener buttonsMouseListener = new ButtonsMouseListener();

		popupMenu = new JPopupMenu();
		mainButton = new JButton("popup menu empty");
		// mainButton.setBorder(new RightChoppedBorder(mainButton.getBorder(),
		// 2));
//		mainButton.setContentAreaFilled(false);
//		mainButton.setFocusPainted(false);
//		mainButton.addMouseListener(buttonsMouseListener);

		dropDownButton = new JButton(new SmallDownArrow());
		dropDownButton.setMinimumSize(new Dimension(11, (int) mainButton.getMinimumSize().getHeight()));
		dropDownButton.setPreferredSize(new Dimension(11, (int) mainButton.getPreferredSize().getHeight()));
		dropDownButton.setMaximumSize(new Dimension(11, (int) mainButton.getMaximumSize().getHeight()));
//		dropDownButton.setFocusPainted(false);
//		dropDownButton.setContentAreaFilled(false);
//		dropDownButton.addMouseListener(buttonsMouseListener);
		add(mainButton);
		add(dropDownButton);

		dropDownButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				popupMenu.show(mainButton, 0, mainButton.getHeight());
			}
		});

		menuItemActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object source = e.getSource();
				if (source instanceof JMenuItem) {
					JMenuItem menuItem = (JMenuItem) source;
					mainButton.setAction(menuItem.getAction());
					mainButton.setText("");
				}
			}
		};
	}

	public JMenuItem addMenuItem(JMenuItem menuItem) {
		menuItem.addActionListener(menuItemActionListener);
		if (mainButton.getAction() == null) {
			mainButton.setAction(menuItem.getAction());
			mainButton.setText("");
			dropDownButton.setMinimumSize(new Dimension(11, (int) mainButton.getMinimumSize().getHeight()));
			dropDownButton.setPreferredSize(new Dimension(11, (int) mainButton.getPreferredSize().getHeight()));
			dropDownButton.setMaximumSize(new Dimension(11, (int) mainButton.getMaximumSize().getHeight()));
		}
		return popupMenu.add(menuItem);
	}
	

	@Override
	public void setToolTipText(String text) {
		super.setToolTipText(text);
		mainButton.setToolTipText(text);
		dropDownButton.setToolTipText(text);
	}

	@SuppressWarnings("serial")
	public static void main(String[] args) {
		System.out.println(new java.util.Date(System.currentTimeMillis()));
		System.out.println(new java.util.Date(System.currentTimeMillis() * 2));
		final JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GSNDropDownButton ddb = new GSNDropDownButton();
		JMenuItem menuItem1 = new JMenuItem(new AbstractAction("Spring", new ImageIcon(Utilities
				.loadImage("gsn/gui/resources/icon_spring_layout.gif"))) {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(frame, getValue(NAME));
			}
		});
		ddb.addMenuItem(menuItem1);
		JMenuItem menuItem2 = new JMenuItem(new AbstractAction("Radial", new ImageIcon(Utilities
				.loadImage("gsn/gui/resources/icon_radial_layout.gif"))) {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(frame, getValue(NAME));
			}
		});
		ddb.addMenuItem(menuItem2);

		JMenuItem menuItem3 = new JMenuItem(new AbstractAction("Grid", new ImageIcon(Utilities
				.loadImage("gsn/gui/resources/icon_grid_layout.gif"))) {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(frame, getValue(NAME));
			}
		});
		ddb.addMenuItem(menuItem3);

		JMenuItem menuItem4 = new JMenuItem(new AbstractAction("Vertical Tree", new ImageIcon(Utilities
				.loadImage("gsn/gui/resources/icon_tree_layout.gif"))) {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(frame, getValue(NAME));
			}
		});
		ddb.addMenuItem(menuItem4);

		JMenuItem menuItem5 = new JMenuItem(new AbstractAction("Horizontal Tree", new ImageIcon(Utilities
				.loadImage("gsn/gui/resources/icon_tree_layout_horizontal.gif"))) {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(frame, getValue(NAME));
			}
		});
		ddb.addMenuItem(menuItem5);

		frame.add(ddb);
		frame.pack();
		frame.setVisible(true);
	}

	private static class SmallDownArrow implements Icon {

		Color arrowColor = Color.black;

		public void paintIcon(Component c, Graphics g, int x, int y) {
			g.setColor(arrowColor);
			g.drawLine(x, y, x + 4, y);
			g.drawLine(x + 1, y + 1, x + 3, y + 1);
			g.drawLine(x + 2, y + 2, x + 2, y + 2);
		}

		public int getIconWidth() {
			return 6;
		}

		public int getIconHeight() {
			return 4;
		}

	}

	/**
	 * An adapter that wraps a border object, and chops some number of pixels
	 * off the right hand side of the border.
	 */
	private class RightChoppedBorder implements Border {
		private Border b;

		private int w;

		public RightChoppedBorder(Border b, int width) {
			this.b = b;
			this.w = width;
		}

		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			Shape clipping = g.getClip();
			g.setClip(x, y, width, height);
			b.paintBorder(c, g, x, y, width + w, height);
			g.setClip(clipping);
		}

		public Insets getBorderInsets(Component c) {
			Insets i = b.getBorderInsets(c);
			return new Insets(i.top, i.left, i.bottom, i.right - w);
		}

		public boolean isBorderOpaque() {
			return b.isBorderOpaque();
		}
	}

	private class ButtonsMouseListener extends MouseAdapter {
		public ButtonsMouseListener() {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			mainButton.setContentAreaFilled(true);
			dropDownButton.setContentAreaFilled(true);
		}

		@Override
		public void mouseExited(MouseEvent e) {
			mainButton.setContentAreaFilled(false);
			dropDownButton.setContentAreaFilled(false);
		}

	}

}
