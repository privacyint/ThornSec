/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
package core.exec.network;

import java.util.Objects;

import core.exec.OutputExec;

import core.model.machine.ServerModel;
import core.model.network.NetworkModel;

import core.exception.data.ADataException;

public class PassCLI extends APassphrase {

	public PassCLI(ServerModel server, NetworkModel networkModel) {
		super(server, networkModel);
	}

	@Override
	public Boolean init() {
		OutputExec checkInit = new OutputExec("pass");

		if (Objects.equals(checkInit.getOutput(), "Error: passphrase store is empty. Try \"pass init\".")) {
			System.out.println("\npassphrase store is empty.  Creating you a new one.");
			System.out.println(new OutputExec("pass init " + this.getNetworkModel().getData().getPGP()).getOutput());
			
			return false;
		}
		
		return true;
	}

	@Override
	public String getPassphrase()
	throws ADataException {
			OutputExec passphraseGetExec = new OutputExec("pass Thornsec/" + this.getNetworkModel().getData().getFQDN(this.getServer().getLabel()) + "/" + this.getNetworkModel().getLabel() + "/" + getServer().getLabel());
			
			String passphrase = passphraseGetExec.getOutput();
		
			if (Objects.equals(passphrase, "") || Objects.equals(passphrase, "Error: Thornsec/" + this.getNetworkModel().getData().getFQDN(this.getServer().getLabel()) + "/" + this.getNetworkModel().getLabel() + "/" + this.getServer().getLabel() +" is not in the passphrase store.")) {
				System.out.println("\npassphrase for " + this.getServer().getLabel() + " isn't stored.");
				if (this.getNetworkModel().getData().getAutoGenPassphrasess()) {
					System.out.println("\nGenerating you a passphrase for " + this.getServer().getLabel());
					OutputExec passphraseSetExec = new OutputExec("pass generate Thornsec/" + this.getNetworkModel().getData().getFQDN(this.getServer().getLabel()) + "/" + this.getNetworkModel().getLabel() + "/" + this.getServer().getLabel() + " 31 | tail -n1");
					passphrase = passphraseSetExec.getOutput();

					this.setIsADefaultPassphrase(false);
				}
				else {
					System.out.println("Add the passphrase to the store by issuing the following command:");
					System.out.println("pass add Thornsec/"  + this.getNetworkModel().getData().getFQDN(this.getServer().getLabel()) + "/" + this.getNetworkModel().getLabel() + "/" + this.getServer().getLabel());
					System.out.println("Using your user's default passphrase");
					
					passphrase = this.getNetworkModel().getData().getUserDefaultPassphrase(this.getNetworkModel().getData().getUser());
					passphrase += " " + this.getServer().getLabel();
					
					this.setIsADefaultPassphrase(true);
				}
			}
			
			assert ( !passphrase.isEmpty() && !passphrase.isEmpty() );
			
			return passphrase;
	}

	@Override
	protected String generatePassphrase() {
		// TODO Auto-generated method stub
		return null;
	}

}