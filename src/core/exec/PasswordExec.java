package core.exec;

import core.model.NetworkModel;

public class PasswordExec {

	private String server;
	private NetworkModel network;

	public PasswordExec(String server, NetworkModel network) {
		this.server = server;
		this.network = network;
	}

	public Boolean init() {
		OutputExec checkInit = new OutputExec("pass");

		if (checkInit.getOutput().equals("Error: password store is empty. Try \"pass init\".")) {
			System.out.println("\nPassword store is empty.  Creating you a new one.");
			System.out.println(new OutputExec("pass init " + network.getData().getGPG()).getOutput());
		}
		
		return true;
	}
	
	public String getPassword() {
		OutputExec passwordGetExec = new OutputExec("pass Thornsec/" + network.getData().getDomain(server) + "/" + network.getLabel() + "/" + server);
		String password = passwordGetExec.getOutput();
	
		if (password.equals("") || password.equals("Error: Thornsec/" + network.getData().getDomain(server) + "/" + network.getLabel() + "/" + server +" is not in the password store.")) {
			System.out.println("\nPassword for " + server + " isn't stored.");
			if (network.getData().getAutoGenPasswds().equals("true")) {
				System.out.println("\nGenerating you a password for " + server);
				OutputExec passwordSetExec = new OutputExec("pass generate Thornsec/" + network.getData().getDomain(server) + "/" + network.getLabel() + "/" + server + " 31 | tail -n1");
				password = passwordSetExec.getOutput();
			}
			else {
				System.out.println("Add the password to the store by issuing the following command:");
				System.out.println("pass add Thornsec/"  + network.getData().getDomain(server) + "/" + network.getLabel() + "/" + server);
				password = "";
			}
		}
		
		return password;
	}

}