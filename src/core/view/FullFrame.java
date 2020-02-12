/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Collection;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;

import core.exception.runtime.InvalidDeviceModelException;
import core.exception.runtime.InvalidMachineModelException;
import core.exception.runtime.InvalidServerModelException;
import core.model.machine.configuration.networking.NetworkInterfaceModel;
import core.model.network.NetworkModel;
import core.model.network.ThornsecModel;
import inet.ipaddr.IPAddress;

public class FullFrame {

	public FullFrame(ThornsecModel model) {
		final JTabbedPane jtp = new JTabbedPane();
		for (final String network : model.getNetworkLabels()) {
			jtp.add(network, getNetworkPane(model.getNetwork(network)));
		}

		final JFrame frame = new JFrame("Thornsec");
		frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(jtp);
		frame.pack();
		frame.setVisible(true);
	}

	private JPanel getNewPanel() {
		final GridBagLayout layout = new GridBagLayout();

		final JPanel ripInPepperoniTheIncredibleMrHong = new JPanel(layout);
		ripInPepperoniTheIncredibleMrHong.setBackground(Color.WHITE);

		return ripInPepperoniTheIncredibleMrHong;
	}

	private Component getNetworkPane(NetworkModel model) {
		final JTabbedPane jtp = new JTabbedPane();

		final JTextArea area = new JTextArea();
		area.setEditable(false);
		final TextAreaOutputStream out = new TextAreaOutputStream(area);

		// jtp.add("Network Info", getInfoPanel(model));
		jtp.add("Servers", getServerPanel(model, out));
		jtp.add("Devices", getDevicePanel(model));
		jtp.add("Output", getOutputPanel(area));

		return jtp;
	}

	private JPanel getOutputPanel(JTextArea area) {
		final JPanel panel = getNewPanel();

		final JScrollPane areapane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		final GridBagConstraints g = new GridBagConstraints();

		areapane.setViewportView(area);

		g.gridx = 0;
		g.gridy = 1;
		g.weightx = 1;
		g.weighty = 1;
		g.fill = GridBagConstraints.BOTH;
		panel.add(areapane, g);

		return panel;
	}

	private JSplitPane getDevicePanel(final NetworkModel model) {
		// Right-hand pane
		final JScrollPane detailsPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		final JPanel detailsPanel = getNewPanel();

		detailsPane.setViewportView(detailsPanel);

		final JPanel devicePanel = getNewPanel();
		final JScrollPane devicePane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		final DefaultMutableTreeNode root = new DefaultMutableTreeNode(model.getLabel());

		final DefaultMutableTreeNode usersNode = new DefaultMutableTreeNode("Users");
//		for (ADeviceModel user : model.getUserDevices()) {
//			usersNode.add(new DefaultMutableTreeNode(user.getLabel()));
//		}
		root.add(usersNode);

		final DefaultMutableTreeNode intOnlyNode = new DefaultMutableTreeNode("Internal-Only Devices");
//		for (ADeviceModel intO : model.getInternalOnlyDevices()) {
//			intOnlyNode.add(new DefaultMutableTreeNode(intO.getLabel()));
//		}
		root.add(intOnlyNode);

		final DefaultMutableTreeNode extOnlyNode = new DefaultMutableTreeNode("External-Only Devices");
//		for (ADeviceModel extO : model.getExternalOnlyDevices()) {
//			extOnlyNode.add(new DefaultMutableTreeNode(extO.getLabel()));
//		}
		root.add(extOnlyNode);

		final JTree deviceTree = new JTree(root);
		deviceTree.setCellRenderer(new DeviceIconRenderer(model));
		deviceTree.setRootVisible(false);
		deviceTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		deviceTree.addTreeSelectionListener(e -> {
			final GridBagConstraints g = new GridBagConstraints();

			final String device = e.getPath().getLastPathComponent().toString();

			try {
				if (model.getDeviceModel(device) == null) {
					return;
				}
			} catch (final InvalidDeviceModelException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} // Stop it crashing on labels!

			detailsPanel.removeAll();

			g.fill = GridBagConstraints.VERTICAL;

			// Title
			g.gridx = 0;
			g.gridy = 0;
			g.gridwidth = 4;
			g.ipady = 40;
			detailsPanel.add(new JLabel(device), g);

			// Reset values
			g.gridwidth = 1;
			g.ipady = 10;

			// IP Address
			g.gridy += 1;

			g.gridx = 1;
			g.anchor = GridBagConstraints.LINE_START;
			detailsPanel.add(new JLabel("Subnets:"), g);
			g.gridx = 2;
			g.anchor = GridBagConstraints.LINE_END;
			final JLabel ips = new JLabel();
//				if (model.getDeviceModel(device).getInterfaces().size() > 1) {
//					ips.setText("<html><body><ul>");
//					for (InterfaceData ip : model.getDeviceModel(device).getInterfaces()) {
//						ips.setText(ips.getText() + "<li>" + ip.getAddress().getHostAddress() + "/30</li>");
//					}
//					ips.setText(ips.getText() + "</ul></body></html>");
//				}
//				else {
//					ips.setText(model.getDeviceModel(device).getInterfaces().firstElement().getAddress().getHostAddress() + "/30");
//				}
			detailsPanel.add(ips, g);

			// MACs
			g.gridy += 1;

			g.gridx = 1;
			g.anchor = GridBagConstraints.LINE_START;
			detailsPanel.add(new JLabel("MACs:"), g);
			g.gridx = 2;
			g.anchor = GridBagConstraints.LINE_END;
			final JLabel macs = new JLabel();
//				if (model.getDeviceModel(device).getMacs().size() > 1) {
//					macs.setText("<html><body><ul>");
//					for (String mac : model.getDeviceModel(device).getMacs()) {
//						macs.setText(macs.getText() + "<li>" + mac + "</li>");
//					}
//					macs.setText(macs.getText() + "</body></html>");
//				}
//				else {
//					macs.setText(model.getDeviceModel(device).getMacs().firstElement());
//				}
			detailsPanel.add(macs, g);

			detailsPanel.repaint();
			detailsPanel.validate();
		});

		for (int i = 0; i < deviceTree.getRowCount(); i++) {
			deviceTree.expandRow(i);
		}

		devicePanel.add(deviceTree);
		devicePane.setViewportView(devicePanel);

		final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setLeftComponent(devicePane);
		splitPane.setRightComponent(detailsPane);

		return splitPane;
	}

	private JSplitPane getServerPanel(final NetworkModel model, final TextAreaOutputStream out) {
		// Right-hand pane
		final JScrollPane detailsPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		final JPanel detailsPanel = getNewPanel();

		detailsPane.setViewportView(detailsPanel);

		// Left-hand pane
		final JScrollPane serverPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		final JPanel serverPanel = getNewPanel();
		final DefaultMutableTreeNode serverRoot = new DefaultMutableTreeNode(model.getLabel());

		for (final String server : model.getServers().keySet()) {
			serverRoot.add(new DefaultMutableTreeNode(server));
//		for (ServerModel router : model.getRouterServers()) {
//			if (!router.isMetal()) {
//				serverRoot.add(new DefaultMutableTreeNode(router.getLabel()));
//			}
//		}
//
//		for (ServerModel metal : model.getMetalServers()) {
//			DefaultMutableTreeNode metalNode = new DefaultMutableTreeNode(metal.getLabel());
//			for (ServerModel service : metal.getServices()) {
//				metalNode.add(new DefaultMutableTreeNode(service.getLabel()));
//			}
//			serverRoot.add(metalNode);
//		}
//
//		for (ServerModel dedi : model.getDediServers()) {
//			DefaultMutableTreeNode dediNode = new DefaultMutableTreeNode(dedi.getLabel());
//			serverRoot.add(dediNode);
//		}
		}

		final JTree serverTree = new JTree(serverRoot);
		serverTree.setCellRenderer(new CustomServerIconRenderer(model));
		serverTree.setRootVisible(false);
		serverTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		serverTree.addTreeSelectionListener(e -> {
			final GridBagConstraints g = new GridBagConstraints();

			final String serverLabel = e.getPath().getLastPathComponent().toString();
			try {
				model.getServerModel(serverLabel);
			} catch (final InvalidServerModelException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			final ServerListener listener = new ServerListener(serverLabel, model, out, System.in);

			detailsPanel.removeAll();

			g.fill = GridBagConstraints.VERTICAL;

			// Title
			g.gridx = 0;
			g.gridy = 0;
			g.gridwidth = 4;
			g.ipady = 40;
			detailsPanel.add(new JLabel(serverLabel), g);

			// Reset values
			g.gridwidth = 1;
			g.ipady = 10;

			// IP Address
			g.gridy += 1;

			g.gridx = 1;
			g.anchor = GridBagConstraints.LINE_START;
			detailsPanel.add(new JLabel("IPs:"), g);
			g.gridx = 2;
			g.anchor = GridBagConstraints.LINE_END;

			final JLabel addresses = new JLabel();

			addresses.setText(addresses.getText() + "<html><body><ul>");
			try {
				final Collection<NetworkInterfaceModel> ifaces = model.getNetworkInterfaces(serverLabel);
				if (ifaces.size() > 0) {
					for (final NetworkInterfaceModel iface : ifaces) {
						final Collection<IPAddress> ipAddresses = iface.getAddresses();
						if (ipAddresses == null) {
							continue;
						}

						for (final IPAddress address : ipAddresses) {
							addresses.setText(addresses.getText() + "<li>" + address.toCompressedString() + "</li>");
						}
					}
				}
			} catch (final InvalidMachineModelException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			addresses.setText(addresses.getText() + "</ul></body></html>");

			detailsPanel.add(addresses, g);

			// SSH
			g.gridy += 1;

			g.gridx = 1;
			g.anchor = GridBagConstraints.LINE_START;
			detailsPanel.add(new JLabel("External SSH Port:"), g);
			g.gridx = 2;
			g.anchor = GridBagConstraints.LINE_END;
			detailsPanel.add(new JLabel(model.getData().getAdminPort(serverLabel) + ""), g);

			// Profiles
//				if (server.getProfiles().length > 0) {
//					g.gridy += 1;
//
//					g.gridx = 1;
//					g.anchor = GridBagConstraints.LINE_START;
//					detailsPanel.add(new JLabel("Profiles:"), g);
//					g.gridx = 2;
//					g.anchor = GridBagConstraints.LINE_END;
//					JLabel profiles = new JLabel();
//
//					if (server.getProfiles().length > 1) {
//						profiles.setText("<html><body><ul>");
//						for (String profile : model.getServerModel(serverLabel).getProfiles()) {
//							profiles.setText(profiles.getText() + "<li>" + profile + "</li> ");
//						}
//						profiles.setText(profiles.getText() + "</ul></body></html>");
//					}
//					else {
//						profiles.setText(server.getProfiles()[0]);
//					}
//
//					detailsPanel.add(profiles, g);
//				}

			// FQDN
			g.gridy += 1;

			g.gridx = 1;
			g.anchor = GridBagConstraints.LINE_START;
			detailsPanel.add(new JLabel("FQDN:"), g);
			g.gridx = 2;
			g.anchor = GridBagConstraints.LINE_END;
//				detailsPanel.add(new JLabel(model.getData().getHostname(serverLabel) + "." + model.getData().getDomain(serverLabel)), g);

//				//CNAMEs
//				if (model.getData().getCnames(serverLabel).length > 0) {
//					g.gridy += 1;
//
//					g.gridx = 1;
//					g.anchor = GridBagConstraints.LINE_START;
//					detailsPanel.add(new JLabel("CNAMEs:"), g);
//					g.gridx = 2;
//					g.anchor = GridBagConstraints.LINE_END;
//					JLabel cnames = new JLabel();
//					if (model.getData().getCnames(serverLabel).length > 1) {
//						cnames.setText("<html><body><ul>");
//						for (String cname : model.getData().getCnames(serverLabel)) {
//							cnames.setText(cnames.getText() + "<li>" + cname + "</li>");
//						}
//						cnames.setText(cnames.getText() + "</li></body></html>");
//					}
//					else {
//						cnames.setText(model.getData().getCnames(serverLabel)[0]);
//					}
//
//					detailsPanel.add(cnames, g);
//				}

//				//MAC
//				if (!server.getMacs().isEmpty()) {
//					g.gridy += 1;
//
//					g.gridx = 1;
//					g.anchor = GridBagConstraints.LINE_START;
//					detailsPanel.add(new JLabel("MAC Addresses:"), g);
//					g.gridx = 2;
//					g.anchor = GridBagConstraints.LINE_END;
//					JLabel macs = new JLabel();
//
//					if (server.getMacs().size() == 1) {
//						macs.setText(server.getMacs().firstElement());
//					}
//					else {
//						macs.setText("<html><body><ul>");
//						for (String mac : server.getMacs()) {
//							if (macs.getText().contains(mac)) { continue; }
//							macs.setText(macs.getText() + "<li>" + mac + "</li>");
//						}
//						macs.setText(macs.getText() + "</ul></body></html>");
//					}
//
//					detailsPanel.add(macs, g);
//				}

			// Buttons
			g.gridwidth = 2;
			g.gridx = 1;
			g.fill = GridBagConstraints.HORIZONTAL;

//				if (!server.isService() ) {
			g.gridy += 1;
			final JButton buildiso = new JButton("Build ISO");
			buildiso.addActionListener(listener);
			detailsPanel.add(buildiso, g);
//				}
//
//				if (!server.isDedi()) {
			g.gridy += 1;
			final JButton audit = new JButton("Audit");
			audit.addActionListener(listener);
			detailsPanel.add(audit, g);

			g.gridy += 1;
			final JButton dryrun = new JButton("Dry Run");
			dryrun.addActionListener(listener);
			detailsPanel.add(dryrun, g);

			g.gridy += 1;
			final JButton config = new JButton("Config");
			config.addActionListener(listener);
			detailsPanel.add(config, g);
//				}

			detailsPanel.repaint();
			detailsPanel.validate();
		});

		for (int i = 0; i < serverTree.getRowCount(); i++) {
			serverTree.expandRow(i);
		}

		serverPanel.add(serverTree);

		// GridBagConstraints g = new GridBagConstraints();
		// g.fill = GridBagConstraints.VERTICAL;
		// JButton buildiso = new JButton("Audit All");
		// buildiso.addActionListener(new ServerListener(null, model, out, System.in));
		// serverPanel.add(buildiso, g);

		serverPane.setViewportView(serverPanel);

		final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setLeftComponent(serverPane);
		splitPane.setRightComponent(detailsPane);

		return splitPane;
	}
}

class CustomServerIconRenderer extends DefaultTreeCellRenderer {
	private static final long serialVersionUID = 51204425271690803L;
	private ImageIcon routerIcon;
	private ImageIcon metalIcon;
	private ImageIcon serviceIcon;

	public CustomServerIconRenderer(NetworkModel model) {
		this.routerIcon = null;
		this.metalIcon = null;
		this.serviceIcon = null;

		try {
			this.routerIcon = new ImageIcon(CustomServerIconRenderer.class.getResource("images/router.png"));
			this.metalIcon = new ImageIcon(CustomServerIconRenderer.class.getResource("images/metal.jpeg"));
			this.serviceIcon = new ImageIcon(CustomServerIconRenderer.class.getResource("images/service.jpeg"));
		} catch (final Exception e) {
		}
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		final Object nodeObj = ((DefaultMutableTreeNode) value).getUserObject();

		nodeObj.toString();

//		if (row > 0) {
//			if (model.getServerModel(node).isRouter()) {
//				setIcon(routerIcon);
//			}
//			else if (model.getServerModel(node).isMetal() || model.getServerModel(node).isDedi()) {
//				setIcon(metalIcon);
//			}
//			else if (model.getServerModel(node).isService()) {
//				setIcon(serviceIcon);
//			}
//		}
//		else {
		setIcon(null);
//		}

		return this;
	}
}

class DeviceIconRenderer extends DefaultTreeCellRenderer {
	private static final long serialVersionUID = 513044252716923403L;
	private ImageIcon userIcon;
	private ImageIcon intOnlyIcon;
	private ImageIcon extOnlyIcon;

	public DeviceIconRenderer(NetworkModel model) {
		this.userIcon = null;
		this.intOnlyIcon = null;
		this.extOnlyIcon = null;

		try {
			this.userIcon = new ImageIcon(DeviceIconRenderer.class.getResource("images/user.png"));
			this.intOnlyIcon = new ImageIcon(DeviceIconRenderer.class.getResource("images/intonly.png"));
			this.extOnlyIcon = new ImageIcon(DeviceIconRenderer.class.getResource("images/extonly.jpeg"));
		} catch (final Exception e) {
		} finally {
		}
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		final Object nodeObj = ((DefaultMutableTreeNode) value).getUserObject();

		nodeObj.toString();

//		if (row > 0 && !node.equals("Users") && !node.equals("Internal-Only Devices") && !node.equals("External-Only Devices")) {
//			switch (model.getDeviceModel(node).getType()) {
//				case "Internal":
//					setIcon(intOnlyIcon);
//					break;
//				case "External":
//					setIcon(extOnlyIcon);
//					break;
//				case "User":
//					setIcon(userIcon);
//					break;
//				default:
//					setIcon(null);
//			}
//		}
//		else {
		setIcon(null);
//		}

		return this;
	}
}
