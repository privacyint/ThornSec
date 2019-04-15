package core.exec;

import java.util.Objects;

import core.model.NetworkModel;

public class PasswordExec {

	private String server;
	private NetworkModel model;
	private Boolean isDefaultPass;

	public PasswordExec(String server, NetworkModel model) {
		this.server = server;
		this.model = model;
		this.isDefaultPass = true;
	}

	public Boolean init() {
		OutputExec checkInit = new OutputExec("pass");

		if (Objects.equals(checkInit.getOutput(), "Error: password store is empty. Try \"pass init\".")) {
			System.out.println("\nPassword store is empty.  Creating you a new one.");
			System.out.println(new OutputExec("pass init " + model.getData().getPGP()).getOutput());
		}
		
		return true;
	}
	
	public String getPassword() {
		OutputExec passwordGetExec = new OutputExec("pass Thornsec/" + model.getData().getDomain(server) + "/" + model.getLabel() + "/" + server);
		String password = passwordGetExec.getOutput();
	
		if (Objects.equals(password, "") || Objects.equals(password, "Error: Thornsec/" + model.getData().getDomain(server) + "/" + model.getLabel() + "/" + server +" is not in the password store.")) {
			System.out.println("\nPassword for " + server + " isn't stored.");
			if (model.getData().getAutoGenPasswds()) {
				System.out.println("\nGenerating you a password for " + server);
				OutputExec passwordSetExec = new OutputExec("pass generate Thornsec/" + model.getData().getDomain(server) + "/" + model.getLabel() + "/" + server + " 31 | tail -n1");
				password = passwordSetExec.getOutput();

				isDefaultPass = false;
			}
			else {
				System.out.println("Add the password to the store by issuing the following command:");
				System.out.println("pass add Thornsec/"  + model.getData().getDomain(server) + "/" + model.getLabel() + "/" + server);
				System.out.println("Using your user's default password");
				
				password = model.getData().getUserDefaultPassword(model.getData().getUser());
				password += " " + server;
				
				isDefaultPass = true;
			}
		}
		
		return password;
	}
	
	public Boolean isDefaultPassword() {
		return isDefaultPass;
	}

}