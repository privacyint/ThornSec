package core.exec;

import java.io.OutputStream;

import core.model.NetworkModel;

public class ManageExec {

	private String server;
	private NetworkModel network;
	private String pass;
	private String cmd;
	private OutputStream out;
	private String sshIP;
	private String sshPort;

	public ManageExec(String server, NetworkModel network, String pass, String cmd, OutputStream out) {
		this.server = server;
		this.network = network;
		this.pass = pass;
		this.cmd = cmd;
		this.out = out;

		if (network.getData().getConnection(this.server).equals("direct")) {
			if (network.getServerModel(server).isRouter()) {
				this.sshIP = network.getServerModel(server).getGateway();
			}
			else {
				this.sshIP = network.getServerModel(server).getIP();
			}
			
			this.sshPort = network.getData().getAdminPort(this.server);
		}
		else if (network.getData().getConnection(this.server).equals("tunnelled")) {
			this.sshIP = "127.0.0.1";
			
			this.sshPort = "65432";
		}

	}

	public SSHExec runScriptNonBlock() {
		String[] tunnel;
		String[] scriptOutput;
		String[] scriptExecute;
		
		tunnel = new String[] {
			"ssh",
			"-f",
			"-o ControlMaster=no",
			"-o ExitOnForwardFailure=yes",
			"-o ConnectTimeout=10",
			"-L " + sshPort + ":" + network.getServerModel(server).getIP() + ":" + network.getData().getAdminPort(server),
			"-p " + network.getData().getAdminPort(network.getRouters().firstElement()),
			network.getData().getUser() + "@" + network.getData().getIP(),
			"sleep 10" //Make it self close!
		};
							
		//Connect & spit out our script
		scriptOutput = new String[] {
				"ssh",
				"-o ConnectTimeout=3",
				"-p " + sshPort,
				network.getData().getUser() + "@" + sshIP,
				"cat > script.sh;",
				"chmod +x script.sh;"		
		};
		
		//Run it!
		scriptExecute = new String[] {
				"ssh",
				"-t", //Give me a tty
				"-t", //No... 4rlysrs!
				"-o ConnectTimeout=3",
				"-p " + sshPort,
				network.getData().getUser() + "@" + sshIP,
				"./script.sh;",
				"while [ -f ~/script.pid ]; do sleep 2; done;",
				"rm script.sh;"					
		};
		
		if (network.getData().getConnection(this.server).equals("tunnelled")) {
			SSHExec tunnelExec = new SSHExec(tunnel);
			System.out.println(tunnelExec.getOutput());
			System.out.println(tunnelExec.getError());
			tunnelExec.waitFor();
		}
		
		SSHExec scriptOutputExec = new SSHExec(scriptOutput, cmd);
		System.out.println(scriptOutputExec.getOutput());
		System.out.println(scriptOutputExec.getError());

		SSHExec scriptExecuteExec = new SSHExec(scriptExecute, pass, out);	
		System.out.println(scriptOutputExec.getOutput());
		System.out.println(scriptOutputExec.getError());
		
		return scriptExecuteExec;
	}

	public void runScriptBlock() {
		SSHExec exec = runScriptNonBlock();
		exec.waitFor();
	}

}