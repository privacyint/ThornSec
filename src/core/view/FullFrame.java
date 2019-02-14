package core.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.net.InetAddress;

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
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;

import core.data.InterfaceData;

import core.model.DeviceModel;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.model.ThornsecModel;

public class FullFrame {

	public FullFrame(ThornsecModel model) {
		JTabbedPane jtp = new JTabbedPane();
		for (String network : model.getNetworkLabels()) {
			jtp.add(network, getNetworkPane(model.getNetworkModel(network)));
		}

		JFrame frame = new JFrame("Thornsec");
		frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(jtp);
		frame.pack();
		frame.setVisible(true);
	}

	private JPanel getNewPanel() {
		GridBagLayout layout = new GridBagLayout();

		JPanel ripInPepperoniTheIncredibleMrHong = new JPanel(layout);
		ripInPepperoniTheIncredibleMrHong.setBackground(Color.WHITE);

		return ripInPepperoniTheIncredibleMrHong;
	}

	private Component getNetworkPane(NetworkModel model) {
		JTabbedPane jtp = new JTabbedPane();

		JTextArea area = new JTextArea();
		area.setEditable(false);
		TextAreaOutputStream out = new TextAreaOutputStream(area);
		
		//jtp.add("Network Info", getInfoPanel(model));
		jtp.add("Servers", getServerPanel(model, out));
		jtp.add("Devices", getDevicePanel(model));
		jtp.add("Output", getOutputPanel(area));
		
		return jtp;
	}
	
	private JPanel getOutputPanel(JTextArea area) {
		JPanel panel = getNewPanel();

		JScrollPane areapane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		GridBagConstraints g = new GridBagConstraints();

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
		JScrollPane  detailsPane  = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		final JPanel detailsPanel = getNewPanel();

		detailsPane.setViewportView(detailsPanel);

		JPanel devicePanel = getNewPanel();
		JScrollPane   devicePane  = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		DefaultMutableTreeNode root  = new DefaultMutableTreeNode(model.getLabel());

		DefaultMutableTreeNode usersNode = new DefaultMutableTreeNode("Users");
		for (DeviceModel user : model.getUserDevices()) {
			usersNode.add(new DefaultMutableTreeNode(user.getLabel()));
		}
		root.add(usersNode);

		DefaultMutableTreeNode intOnlyNode = new DefaultMutableTreeNode("Internal-Only Devices");
		for (DeviceModel intO : model.getInternalOnlyDevices()) {
			intOnlyNode.add(new DefaultMutableTreeNode(intO.getLabel()));
		}
		root.add(intOnlyNode);

		DefaultMutableTreeNode extOnlyNode = new DefaultMutableTreeNode("External-Only Devices");
		for (DeviceModel extO : model.getExternalOnlyDevices()) {
			extOnlyNode.add(new DefaultMutableTreeNode(extO.getLabel()));
		}
		root.add(extOnlyNode);

		final JTree deviceTree = new JTree(root);
		deviceTree.setCellRenderer(new DeviceIconRenderer(model));
		deviceTree.setRootVisible(false);
		deviceTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		deviceTree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				GridBagConstraints g = new GridBagConstraints();

				String device = e.getPath().getLastPathComponent().toString();

				if (model.getDeviceModel(device) == null) { return; } //Stop it crashing on labels!
				
				detailsPanel.removeAll();

				g.fill = GridBagConstraints.VERTICAL;

				//Title
				g.gridx = 0;
				g.gridy = 0;
				g.gridwidth = 4;
				g.ipady = 40;
				detailsPanel.add(new JLabel(device), g);

				//Reset values
				g.gridwidth = 1;
				g.ipady = 10;

				//IP Address
				g.gridy += 1;

				g.gridx = 1;
				g.anchor = GridBagConstraints.LINE_START;
				detailsPanel.add(new JLabel("Subnets:"), g);
				g.gridx = 2;
				g.anchor = GridBagConstraints.LINE_END;
				JLabel ips = new JLabel();
				if (model.getDeviceModel(device).getInterfaces().size() > 1) {
					ips.setText("<html><body><ul>");
					for (InterfaceData ip : model.getDeviceModel(device).getInterfaces()) {
						ips.setText(ips.getText() + "<li>" + ip.getAddress().getHostAddress() + "/30</li>");
					}
					ips.setText(ips.getText() + "</ul></body></html>");
				}
				else {
					ips.setText(model.getDeviceModel(device).getInterfaces().firstElement().getAddress().getHostAddress() + "/30");
				}
				detailsPanel.add(ips, g);

				//MACs
				g.gridy += 1;

				g.gridx = 1;
				g.anchor = GridBagConstraints.LINE_START;
				detailsPanel.add(new JLabel("MACs:"), g);
				g.gridx = 2;
				g.anchor = GridBagConstraints.LINE_END;
				JLabel macs = new JLabel();
				if (model.getDeviceModel(device).getMacs().size() > 1) {
					macs.setText("<html><body><ul>");
					for (String mac : model.getDeviceModel(device).getMacs()) {
						macs.setText(macs.getText() + "<li>" + mac + "</li>");
					}
					macs.setText(macs.getText() + "</body></html>");
				}
				else {
					macs.setText(model.getDeviceModel(device).getMacs().firstElement());
				}
				detailsPanel.add(macs, g);

				detailsPanel.repaint();
				detailsPanel.validate();
			}
		});

		for (int i = 0; i < deviceTree.getRowCount(); i++) {
			deviceTree.expandRow(i);
		}

		devicePanel.add(deviceTree);
		devicePane.setViewportView(devicePanel);

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setLeftComponent(devicePane);
		splitPane.setRightComponent(detailsPane);

		return splitPane;
	}

	private JSplitPane getServerPanel(final NetworkModel model, final TextAreaOutputStream out) {
		// Right-hand pane
		JScrollPane  detailsPane  = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		final JPanel detailsPanel = getNewPanel();

		detailsPane.setViewportView(detailsPanel);

		// Left-hand pane
		JScrollPane            serverPane  = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		JPanel                 serverPanel = getNewPanel();
		DefaultMutableTreeNode serverRoot  = new DefaultMutableTreeNode(model.getLabel());

		for (ServerModel router : model.getRouterServers()) {
			if (!router.isMetal()) {
				serverRoot.add(new DefaultMutableTreeNode(router.getLabel()));
			}
		}

		for (ServerModel metal : model.getMetalServers()) {
			DefaultMutableTreeNode metalNode = new DefaultMutableTreeNode(metal.getLabel());
			for (ServerModel service : metal.getServices()) {
				metalNode.add(new DefaultMutableTreeNode(service.getLabel()));
			}
			serverRoot.add(metalNode);
		}

		final JTree serverTree = new JTree(serverRoot);
		serverTree.setCellRenderer(new CustomServerIconRenderer(model));
		serverTree.setRootVisible(false);
		serverTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		serverTree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				GridBagConstraints g = new GridBagConstraints();

				String serverLabel = e.getPath().getLastPathComponent().toString();
				ServerModel server = model.getServerModel(serverLabel);

				ServerListener listener = new ServerListener(serverLabel, model, out, System.in);

				detailsPanel.removeAll();

				g.fill = GridBagConstraints.VERTICAL;

				//Title
				g.gridx = 0;
				g.gridy = 0;
				g.gridwidth = 4;
				g.ipady = 40;
				detailsPanel.add(new JLabel(serverLabel), g);

				//Reset values
				g.gridwidth = 1;
				g.ipady = 10;

				//IP Address
				g.gridy += 1;

				g.gridx = 1;
				g.anchor = GridBagConstraints.LINE_START;
				detailsPanel.add(new JLabel("IPs:"), g);
				g.gridx = 2;
				g.anchor = GridBagConstraints.LINE_END;
				
				JLabel addresses = new JLabel();
				
				if (server.getAddresses().size() > 1) {
					addresses.setText("<html><body><ul>");
					for (InetAddress address : model.getServerModel(serverLabel).getAddresses()) {
						if (address == null) { continue; }
						addresses.setText(addresses.getText() + "<li>" + address.getHostAddress() + "</li>");
					}
					addresses.setText(addresses.getText() + "</ul></body></html>");
				}
				else {
					addresses.setText(server.getAddresses().firstElement().getHostAddress());
				}
				detailsPanel.add(addresses, g);
				
				//SSH
				g.gridy += 1;

				g.gridx = 1;
				g.anchor = GridBagConstraints.LINE_START;
				detailsPanel.add(new JLabel("External SSH Port:"), g);
				g.gridx = 2;
				g.anchor = GridBagConstraints.LINE_END;
				detailsPanel.add(new JLabel(model.getData().getAdminPort(serverLabel) + ""), g);

				//Profiles
				if (server.getProfiles().length > 0) {
					g.gridy += 1;
	
					g.gridx = 1;
					g.anchor = GridBagConstraints.LINE_START;
					detailsPanel.add(new JLabel("Profiles:"), g);
					g.gridx = 2;
					g.anchor = GridBagConstraints.LINE_END;
					JLabel profiles = new JLabel();
					
					if (server.getProfiles().length > 1) {
						profiles.setText("<html><body><ul>");
						for (String profile : model.getServerModel(serverLabel).getProfiles()) {
							profiles.setText(profiles.getText() + "<li>" + profile + "</li> ");
						}
						profiles.setText(profiles.getText() + "</ul></body></html>");
					}
					else {
						profiles.setText(server.getProfiles()[0]);
					}
					
					detailsPanel.add(profiles, g);
				}
				
				//FQDN
				g.gridy += 1;

				g.gridx = 1;
				g.anchor = GridBagConstraints.LINE_START;
				detailsPanel.add(new JLabel("FQDN:"), g);
				g.gridx = 2;
				g.anchor = GridBagConstraints.LINE_END;
				detailsPanel.add(new JLabel(model.getData().getHostname(serverLabel) + "." + model.getData().getDomain(serverLabel)), g);

				//CNAMEs
				if (model.getData().getCnames(serverLabel).length > 0) {
					g.gridy += 1;
	
					g.gridx = 1;
					g.anchor = GridBagConstraints.LINE_START;
					detailsPanel.add(new JLabel("CNAMEs:"), g);
					g.gridx = 2;
					g.anchor = GridBagConstraints.LINE_END;
					JLabel cnames = new JLabel();
					if (model.getData().getCnames(serverLabel).length > 1) {
						cnames.setText("<html><body><ul>");
						for (String cname : model.getData().getCnames(serverLabel)) {
							cnames.setText(cnames.getText() + "<li>" + cname + "</li>");
						}
						cnames.setText(cnames.getText() + "</li></body></html>");
					}
					else {
						cnames.setText(model.getData().getCnames(serverLabel)[0]);
					}
				
					detailsPanel.add(cnames, g);
				}
				
				//MAC
				if (!server.getMacs().isEmpty()) {
					g.gridy += 1;
	
					g.gridx = 1;
					g.anchor = GridBagConstraints.LINE_START;
					detailsPanel.add(new JLabel("MAC Addresses:"), g);
					g.gridx = 2;
					g.anchor = GridBagConstraints.LINE_END;
					JLabel macs = new JLabel();

					if (server.getMacs().size() == 1) {
						macs.setText(server.getMacs().firstElement());
					}
					else {
						macs.setText("<html><body><ul>");
						for (String mac : server.getMacs()) {
							macs.setText(macs.getText() + "<li>" + mac + "</li>");
						}
						macs.setText(macs.getText() + "</ul></body></html>");
					}

					detailsPanel.add(macs, g);
				}

				//Buttons
				g.gridwidth = 2;
				g.gridx = 1;
				g.fill = GridBagConstraints.HORIZONTAL;

				if (!server.isService() ) {
					g.gridy += 1;
					JButton buildiso = new JButton("Build ISO");
					buildiso.addActionListener(listener);
					detailsPanel.add(buildiso, g);
				}
				
				g.gridy += 1;
				JButton audit = new JButton("Audit");
				audit.addActionListener(listener);
				detailsPanel.add(audit, g);

				g.gridy += 1;
				JButton dryrun = new JButton("Dry Run");
				dryrun.addActionListener(listener);
				detailsPanel.add(dryrun, g);

				g.gridy += 1;
				JButton config = new JButton("Config");
				config.addActionListener(listener);
				detailsPanel.add(config, g);

				detailsPanel.repaint();
				detailsPanel.validate();
			}
		});

		for (int i = 0; i < serverTree.getRowCount(); i++) {
			serverTree.expandRow(i);
		}

		serverPanel.add(serverTree);

		//GridBagConstraints g = new GridBagConstraints();
		//g.fill = GridBagConstraints.VERTICAL;
		//JButton buildiso = new JButton("Audit All");
		//buildiso.addActionListener(new ServerListener(null, model, out, System.in));
		//serverPanel.add(buildiso, g);

		serverPane.setViewportView(serverPanel);

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
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
	private NetworkModel model;

	public CustomServerIconRenderer(NetworkModel model) {
		routerIcon  = new ImageIcon(CustomServerIconRenderer.class.getResource("images/router.png"));
		metalIcon   = new ImageIcon(CustomServerIconRenderer.class.getResource("images/metal.jpeg"));
		serviceIcon = new ImageIcon(CustomServerIconRenderer.class.getResource("images/service.jpeg"));

		this.model = model;
	}
	public Component getTreeCellRendererComponent(JTree tree, Object value,boolean sel,boolean expanded,boolean leaf,int row,boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		Object nodeObj = ((DefaultMutableTreeNode)value).getUserObject();

		String node = nodeObj.toString();
		
		if (row > 0) {
			if (model.getServerModel(node).isRouter()) {
				setIcon(routerIcon);
			}
			else if (model.getServerModel(node).isMetal()) {
				setIcon(metalIcon);
			}
			else if (model.getServerModel(node).isService()) {
				setIcon(serviceIcon);
			}
		}
		else {
			setIcon(null);
		}

		return this;
	}
}

class DeviceIconRenderer extends DefaultTreeCellRenderer {
	private static final long serialVersionUID = 513044252716923403L;
	private ImageIcon userIcon;
	private ImageIcon intOnlyIcon;
	private ImageIcon extOnlyIcon;
	private NetworkModel model;

	public DeviceIconRenderer(NetworkModel model) {
		userIcon      = new ImageIcon(DeviceIconRenderer.class.getResource("images/user.png"));
		intOnlyIcon   = new ImageIcon(DeviceIconRenderer.class.getResource("images/intonly.png"));
		extOnlyIcon   = new ImageIcon(DeviceIconRenderer.class.getResource("images/extonly.jpeg"));

		this.model = model;
	}
	public Component getTreeCellRendererComponent(JTree tree, Object value,boolean sel,boolean expanded,boolean leaf,int row,boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		Object nodeObj = ((DefaultMutableTreeNode)value).getUserObject();

		String node = nodeObj.toString();
		
		if (row > 0 && !node.equals("Users") && !node.equals("Internal-Only Devices") && !node.equals("External-Only Devices")) {
			switch (model.getDeviceModel(node).getType()) {
				case "Internal":
					setIcon(intOnlyIcon);
					break;
				case "External":
					setIcon(extOnlyIcon);
					break;
				case "User":
					setIcon(userIcon);
					break;
				default:
					setIcon(null);
			}
		}
		else {
			setIcon(null);
		}
		
		return this;
	}
}
