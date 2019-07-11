/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.exec.network;

import java.util.Objects;

import core.exception.data.ADataException;
import core.exec.OutputExec;
import core.model.network.NetworkModel;

public class PassCLI extends APassphrase {

	public PassCLI(String label, NetworkModel networkModel) {
		super(label, networkModel);
	}

	@Override
	public Boolean init() {
		final OutputExec checkInit = new OutputExec("pass");

		if (Objects.equals(checkInit.getOutput(), "Error: passphrase store is empty. Try \"pass init\".")) {
			System.out.println("\npassphrase store is empty.  Creating you a new one.");
			System.out.println(new OutputExec("pass init " + this.networkModel.getData().getPGP()).getOutput());

			return false;
		}

		return true;
	}

	@Override
	public String getPassphrase() throws ADataException {
		final OutputExec passphraseGetExec = new OutputExec(
				"pass Thornsec/" + this.networkModel.getData().getFQDN(getLabel()) + "/" + this.networkModel.getLabel()
						+ "/" + getLabel());

		String passphrase = passphraseGetExec.getOutput();

		if (Objects.equals(passphrase, "")
				|| Objects.equals(passphrase, "Error: Thornsec/" + this.networkModel.getData().getFQDN(getLabel()) + "/"
						+ this.networkModel.getLabel() + "/" + getLabel() + " is not in the passphrase store.")) {
			System.out.println("\npassphrase for " + getLabel() + " isn't stored.");
			if (this.networkModel.getData().getAutoGenPassphrasess()) {
				System.out.println("\nGenerating you a passphrase for " + getLabel());
				final OutputExec passphraseSetExec = new OutputExec(
						"pass generate Thornsec/" + this.networkModel.getData().getFQDN(getLabel()) + "/"
								+ this.networkModel.getLabel() + "/" + getLabel() + " 31 | tail -n1");
				passphrase = passphraseSetExec.getOutput();

				this.setIsADefaultPassphrase(false);
			} else {
				System.out.println("Add the passphrase to the store by issuing the following command:");
				System.out.println("pass add Thornsec/" + this.networkModel.getData().getFQDN(getLabel()) + "/"
						+ this.networkModel.getLabel() + "/" + getLabel());
				System.out.println("Using your user's default passphrase");

				passphrase = this.networkModel.getData()
						.getUserDefaultPassphrase(this.networkModel.getData().getUser());
				passphrase += " " + getLabel();

				this.setIsADefaultPassphrase(true);
			}
		}

		assert (!passphrase.isEmpty() && !passphrase.isEmpty());

		return passphrase;
	}

	@Override
	protected String generatePassphrase() {
		// TODO Auto-generated method stub
		return null;
	}

}