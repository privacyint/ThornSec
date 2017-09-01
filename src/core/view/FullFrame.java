package core.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import core.model.NetworkModel;
import core.model.ThornsecModel;

public class FullFrame {

	public FullFrame(ThornsecModel model) {
		JTabbedPane jtp = new JTabbedPane();
		String[] nets = model.getNetworkLabels();
		for (int i = 0; i < nets.length; i++) {
			jtp.add(nets[i], getNetworkPane(model.getNetworkModel(nets[i])));
		}

		JFrame frame = new JFrame("Thornsec");
		frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(jtp);
		frame.pack();
		frame.setVisible(true);
	}

	public Component getNetworkPane(NetworkModel model) {
		JTextArea area = new JTextArea();
		area.setEditable(false);
		TextAreaOutputStream out = new TextAreaOutputStream(area);

		JSplitPane jsp1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		jsp1.setLeftComponent(getServerPanel(model, out));
		jsp1.setRightComponent(getDevicePanel(model));
		jsp1.setDividerLocation(0.5);
		JSplitPane jsp2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		jsp2.setTopComponent(jsp1);
		jsp2.setBottomComponent(getConsolePanel(area));
		jsp2.setDividerLocation(0.5);

		return jsp2;
	}

	private JPanel getConsolePanel(JTextArea area) {
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints g = new GridBagConstraints();

		JPanel panel = new JPanel(layout);
		panel.setBackground(Color.WHITE);

		JScrollPane areapane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		areapane.setViewportView(area);

		g.gridx = 0;
		g.gridy = 1;
		g.weightx = 1;
		g.weighty = 1;
		g.fill = GridBagConstraints.BOTH;
		panel.add(areapane, g);

		g.gridx = 0;
		g.gridy = 2;
		g.weightx = 0;
		g.weighty = 0;
		g.fill = GridBagConstraints.HORIZONTAL;
		panel.add(new JTextField(), g);

		return panel;
	}

	private JScrollPane getServerPanel(NetworkModel model, TextAreaOutputStream out) {
		JScrollPane serverPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		GridBagLayout layout = new GridBagLayout();
		JPanel panel = new JPanel(layout);
		panel.setLayout(layout);
		panel.setBackground(Color.WHITE);

		GridBagConstraints g = new GridBagConstraints();

		String[] servers = model.getServerLabels();
		for (int i = 0; i < servers.length; i++) {
			ServerListener listener = new ServerListener(servers[i], model, out, System.in);
			g.gridx = 0;
			g.gridy = i;
			g.weightx = 1;
			g.weighty = 0;
			g.fill = GridBagConstraints.HORIZONTAL;

			DefaultMutableTreeNode root = new DefaultMutableTreeNode(servers[i] + " (" + model.getServerModel(servers[i]).getUnitCount() + " units)");
			
			String[] types = model.getServerModel(servers[i]).getTypes();
			for (int j = 0; j < types.length; j++) {
				root.add(new DefaultMutableTreeNode(types[j]));
			}
			String[] profiles = model.getServerModel(servers[i]).getProfiles();
			for (int j = 0; j < profiles.length; j++) {
				root.add(new DefaultMutableTreeNode(profiles[j]));
			}
			JTree tree = new JTree(root);
			tree.setCellRenderer(new DefaultTreeCellRenderer());
			panel.add(tree, g);

			g.gridx = 1;
			g.gridy = i;
			g.weightx = 0;
			g.weighty = 0;
			g.fill = GridBagConstraints.NONE;
			JButton buildiso = new JButton("Build ISO");
			buildiso.addActionListener(listener);
			panel.add(buildiso, g);

			g.gridx = 2;
			g.gridy = i;
			g.weightx = 0;
			g.weighty = 0;
			g.fill = GridBagConstraints.NONE;
			JButton audit = new JButton("Audit");
			audit.addActionListener(listener);
			panel.add(audit, g);

			g.gridx = 3;
			g.gridy = i;
			g.weightx = 0;
			g.weighty = 0;
			g.fill = GridBagConstraints.NONE;
			JButton dryrun = new JButton("Dry Run");
			dryrun.addActionListener(listener);
			panel.add(dryrun, g);

			g.gridx = 4;
			g.gridy = i;
			g.weightx = 0;
			g.weighty = 0;
			g.fill = GridBagConstraints.NONE;
			JButton config = new JButton("Config");
			config.addActionListener(listener);
			panel.add(config, g);

		}
		serverPane.setViewportView(panel);
		return serverPane;
	}

	private JScrollPane getDevicePanel(NetworkModel model) {
		JScrollPane devicePane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints g = new GridBagConstraints();
		JPanel panel = new JPanel(layout);
		panel.setBorder(new TitledBorder("devices"));
		panel.setBackground(Color.WHITE);

		String[] devices = model.getDeviceLabels();
		for (int i = 0; i < devices.length; i++) {
			g.gridx = 0;
			g.gridy = i;
			g.weightx = 1;
			g.weighty = 0;
			g.fill = GridBagConstraints.HORIZONTAL;

			JLabel label = new JLabel(devices[i] + " (" + model.getDeviceModel(devices[i]).getType() + ")");
			panel.add(label, g);
		}
		devicePane.setViewportView(panel);
		return devicePane;
	}
	
}
