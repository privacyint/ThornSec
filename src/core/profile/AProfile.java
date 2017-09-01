package core.profile;

import core.iface.IProfile;

public abstract class AProfile implements IProfile {

	protected String name;

	protected AProfile(String name) {
		this.name = name;
	}

	public String getLabel() {
		return name;
	}

}
