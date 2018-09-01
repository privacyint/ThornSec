package core.exec;

import java.io.OutputStream;

public class ProcessExec {

	private Process proc;

	public ProcessExec(String cmd, OutputStream out, OutputStream err) {
		try {
			proc = Runtime.getRuntime().exec(cmd);
			InputExec procin = new InputExec(proc.getInputStream(), out);
			Thread inthread = new Thread(procin);
			inthread.start();
			InputExec procerr = new InputExec(proc.getErrorStream(), err);
			Thread errthread = new Thread(procerr);
			errthread.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void waitFor() {
		try {
			proc.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void writeAllOpen(byte[] bytes) {
		try {
			proc.getOutputStream().write(bytes);
			proc.getOutputStream().flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void writeAllClose(byte[] bytes) {
		try {
			proc.getOutputStream().write(bytes);
			proc.getOutputStream().flush();
			proc.getOutputStream().close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
