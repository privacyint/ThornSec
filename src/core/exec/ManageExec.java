package core.exec;

import java.io.OutputStream;
import java.net.InetAddress;

import core.model.machine.ServerModel;
import core.model.network.NetworkModel;

public class ManageExec {

	private InetAddress ip;
	
	private Integer port;

	private String user;
	private String cmd;
	private String password;
	
	private OutputStream out;

	public ManageExec(String user, String password, InetAddress inetAddress, Integer port, String cmd, OutputStream out) {
		this.user = user;
		this.ip = inetAddress;
		this.port = port;
		this.cmd = cmd;
		this.out = out;
		this.password = password;
	}

	public ManageExec(ServerModel serverModel, NetworkModel networkModel, String cmd, OutputStream out) {
		// TODO Auto-generated constructor stub
	}

	public ProcessExec manage() {
		try {
			String sshConnect = "";
			sshConnect += "ssh";
			sshConnect += " -o ConnectTimeout=3";
			sshConnect += " -o StrictHostKeyChecking=no";
			sshConnect += " -o UserKnownHostsFile=/dev/null";
			sshConnect += " -p " + this.port;
			sshConnect += " " + this.user + "@" + this.ip;

			String ttyConnect = "";
			ttyConnect += "ssh";
			ttyConnect += " -t -t";
			ttyConnect += " -o ConnectTimeout=3";
			ttyConnect += " -o StrictHostKeyChecking=no";
			ttyConnect += " -o UserKnownHostsFile=/dev/null";
			ttyConnect += " -p " + this.port;
			ttyConnect += " " + this.user + "@" + this.ip;

			String outputScript = "";
			outputScript += " cat > script.sh;";
			outputScript += " chmod +x script.sh;";
			outputScript += " exit;";
			
			ProcessExec exec1 = new ProcessExec(sshConnect + outputScript, out, System.err);
			exec1.writeAllClose(this.cmd.getBytes());
			exec1.waitFor();
			
			String execScript = "";
			execScript += " ./script.sh;";
			//execScript += " rm -rf script.sh;";
			execScript += " exit;";
			
			ProcessExec exec2 = new ProcessExec(ttyConnect + execScript, out, System.err);
			Thread.sleep(500);
			
			String pass = password + "\n";
			exec2.writeAllOpen(pass.getBytes());
			
			return exec2;
		}
		catch (Exception e1) {
			e1.printStackTrace();
		}
		return null;
	}
}
