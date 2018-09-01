package core.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Vector;

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

import core.model.NetworkModel;
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

		Vector<String> userDevices         = new Vector<String>();
		Vector<String> internalOnlyDevices = new Vector<String>();
		Vector<String> externalOnlyDevices = new Vector<String>();

		//Let's just get this out of the way up here rather than repeating over and over
		//This class is difficult enough to follow already!! 
		for (String device : model.getDeviceLabels()) {
			switch (model.getDeviceModel(device).getType()) {
				case "User":
					userDevices.add(device);
					break;
				case "Internal":
					internalOnlyDevices.add(device);
					break;
				case "External":
					externalOnlyDevices.add(device);
					break;
				default:
					//In theory, we should never get here. Theory is a fine thing.
					System.out.println("Encountered an unsupported device type for " + device);
			}
		}

		DefaultMutableTreeNode usersNode = new DefaultMutableTreeNode("Users");
		for (String user : userDevices) {
			usersNode.add(new DefaultMutableTreeNode(user));
		}
		root.add(usersNode);

		DefaultMutableTreeNode intOnlyNode = new DefaultMutableTreeNode("Internal-Only Devices");
		for (String intO : internalOnlyDevices) {
			intOnlyNode.add(new DefaultMutableTreeNode(intO));
		}
		root.add(intOnlyNode);

		DefaultMutableTreeNode extOnlyNode = new DefaultMutableTreeNode("External-Only Devices");
		for (String extO : externalOnlyDevices) {
			extOnlyNode.add(new DefaultMutableTreeNode(extO));
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
				detailsPanel.add(new JLabel(model.getDeviceModel(device).getSubnets()[0] + "/24"), g);

				//MACs
				g.gridy += 1;

				g.gridx = 1;
				g.anchor = GridBagConstraints.LINE_START;
				detailsPanel.add(new JLabel("MACs:"), g);
				g.gridx = 2;
				g.anchor = GridBagConstraints.LINE_END;
				JLabel macs = new JLabel();
				for (String mac : model.getDeviceModel(device).getMacs()) {
					macs.setText(macs.getText() + mac + " ");
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

		for (String router : model.getRouters()) {
			if (!model.getServerModel(router).isMetal()) {
				serverRoot.add(new DefaultMutableTreeNode(router));
			}
		}

		for (String metal : model.getMetals()) {
			DefaultMutableTreeNode metalNode = new DefaultMutableTreeNode(metal);
			for (String service : model.getServerModel(metal).getServices()) {
				metalNode.add(new DefaultMutableTreeNode(service));
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

				String server = e.getPath().getLastPathComponent().toString();

				ServerListener listener = new ServerListener(server, model, out, System.in);

				detailsPanel.removeAll();

				g.fill = GridBagConstraints.VERTICAL;

				//Title
				g.gridx = 0;
				g.gridy = 0;
				g.gridwidth = 4;
				g.ipady = 40;
				detailsPanel.add(new JLabel(server), g);

				//Reset values
				g.gridwidth = 1;
				g.ipady = 10;

				//IP Address
				g.gridy += 1;

				g.gridx = 1;
				g.anchor = GridBagConstraints.LINE_START;
				detailsPanel.add(new JLabel("IP:"), g);
				g.gridx = 2;
				g.anchor = GridBagConstraints.LINE_END;
				detailsPanel.add(new JLabel(model.getServerModel(server).getIP()), g);

				//SSH
				g.gridy += 1;

				g.gridx = 1;
				g.anchor = GridBagConstraints.LINE_START;
				detailsPanel.add(new JLabel("External SSH Port:"), g);
				g.gridx = 2;
				g.anchor = GridBagConstraints.LINE_END;
				detailsPanel.add(new JLabel(model.getData().getAdminPort(server)), g);

				//Profiles
				g.gridy += 1;

				g.gridx = 1;
				g.anchor = GridBagConstraints.LINE_START;
				detailsPanel.add(new JLabel("Profiles:"), g);
				g.gridx = 2;
				g.anchor = GridBagConstraints.LINE_END;
				JLabel profiles = new JLabel();
				for (String profile : model.getServerModel(server).getProfiles()) {
					profiles.setText(profiles.getText() + profile + " ");
				}
				detailsPanel.add(profiles, g);

				//FQDN
				g.gridy += 1;

				g.gridx = 1;
				g.anchor = GridBagConstraints.LINE_START;
				detailsPanel.add(new JLabel("FQDN:"), g);
				g.gridx = 2;
				g.anchor = GridBagConstraints.LINE_END;
				detailsPanel.add(new JLabel(model.getData().getHostname(server) + "." + model.getData().getDomain(server)), g);

				//CNAMEs
				g.gridy += 1;

				g.gridx = 1;
				g.anchor = GridBagConstraints.LINE_START;
				detailsPanel.add(new JLabel("CNAMEs:"), g);
				g.gridx = 2;
				g.anchor = GridBagConstraints.LINE_END;
				JLabel cnames = new JLabel();
				for (String cname : model.getData().getCnames(server)) {
					cnames.setText(cnames.getText() + cname + " ");
				}
				detailsPanel.add(cnames, g);

				//MAC
				g.gridy += 1;

				g.gridx = 1;
				g.anchor = GridBagConstraints.LINE_START;
				detailsPanel.add(new JLabel("MAC Address:"), g);
				g.gridx = 2;
				g.anchor = GridBagConstraints.LINE_END;
				detailsPanel.add(new JLabel(model.getData().getMac(server)), g);

				//Buttons
				g.gridwidth = 2;
				g.gridx = 1;
				g.fill = GridBagConstraints.HORIZONTAL;

				g.gridy += 1;
				JButton buildiso = new JButton("Build ISO");
				buildiso.addActionListener(listener);
				detailsPanel.add(buildiso, g);

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
	private ImageIcon superUserIcon;
	private ImageIcon userIcon;
	private ImageIcon intOnlyIcon;
	private ImageIcon extOnlyIcon;
	private NetworkModel model;

	public DeviceIconRenderer(NetworkModel model) {
		superUserIcon = new ImageIcon(DeviceIconRenderer.class.getResource("images/superuser.png"));
		userIcon      = new ImageIcon(DeviceIconRenderer.class.getResource("images/user.png"));
		intOnlyIcon   = new ImageIcon(DeviceIconRenderer.class.getResource("images/intonly.png"));
		extOnlyIcon   = new ImageIcon(DeviceIconRenderer.class.getResource("images/extonly.jpeg"));

		this.model = model;
	}
	public Component getTreeCellRendererComponent(JTree tree, Object value,boolean sel,boolean expanded,boolean leaf,int row,boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		Object nodeObj = ((DefaultMutableTreeNode)value).getUserObject();

		setIcon(null);

		return this;
	}
}