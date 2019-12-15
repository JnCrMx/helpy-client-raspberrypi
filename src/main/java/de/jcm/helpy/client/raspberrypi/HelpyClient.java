package de.jcm.helpy.client.raspberrypi;

import javax.swing.*;
import java.awt.*;

public class HelpyClient extends JFrame
{
	public HelpyClient()
	{
		super("HelpyBox");
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		JPanel headPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));

		JButton menuButton = new JButton(new ImageIcon(getClass().getResource("/icons/menu.png")));
		menuButton.setBorderPainted(false);
		menuButton.setFocusPainted(false);
		headPanel.add(menuButton);

		panel.add(headPanel, BorderLayout.NORTH);

		setContentPane(panel);
	}

	public static void main(String[] args)
	{
		EventQueue.invokeLater(()->
		{
			try
			{
				UIManager.setLookAndFeel(
						UIManager.getSystemLookAndFeelClassName());
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}

			HelpyClient client = new HelpyClient();
			client.setVisible(true);

			GraphicsEnvironment.getLocalGraphicsEnvironment()
					.getDefaultScreenDevice().setFullScreenWindow(client);
		});
	}
}
