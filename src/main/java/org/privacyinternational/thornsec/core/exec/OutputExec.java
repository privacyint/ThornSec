package org.privacyinternational.thornsec.core.exec;

public class OutputExec {

	private String cmd;

	public OutputExec(String cmd) {
		this.cmd = cmd;
	}

	public String getOutput() {
		String value = "";
		
		try {
			Process proc3 = Runtime.getRuntime().exec(cmd);
			int read = proc3.getInputStream().read();
			while (read != -1) {
				value += (char) read;
				read = proc3.getInputStream().read();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return value;
	}

}
