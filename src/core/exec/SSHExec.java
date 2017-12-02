package core.exec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.util.concurrent.Semaphore;

public class SSHExec {
	private Semaphore outputSem;
	private String output;
	private Semaphore errorSem;
	private String error;
	private Process p;
	private OutputStream outputStream;

	private class InputWriter extends Thread {
		private String input;

		public InputWriter(String input) {
			this.input = input;
		}

		public void run() {
			OutputStream write = p.getOutputStream();
			try {
				write.write(input.getBytes());
				write.flush();
				write.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private class OutputReader extends Thread {
		public OutputReader() {
			try {
				outputSem = new Semaphore(1);
				outputSem.acquire();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			try {
				StringBuffer readBuffer = new StringBuffer();
				BufferedReader isr = new BufferedReader(new InputStreamReader(p.getInputStream()));
				
				//String buff = new String();
				//int c;
				//while ((c = isr.read()) != -1) {
				//	readBuffer.append(String.valueOf((char) c));
				//	System.out.print(String.valueOf((char) c));
				//	outputStream.write(c);
				//	outputStream.flush();
				//}
				String buff = "";
				while ((buff = isr.readLine()) != null) {
					readBuffer.append(buff);
				}
				int c;
				for (int i = 0; i < readBuffer.length(); ++i) {
					outputStream.write(readBuffer.charAt(i));
					outputStream.flush();
				}
				
				output = readBuffer.toString();
				outputSem.release();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private class ErrorReader extends Thread {
		public ErrorReader() {
			try {
				errorSem = new Semaphore(1);
				errorSem.acquire();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			try {
				StringBuffer readBuffer = new StringBuffer();
				BufferedReader isr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				
				String buff = new String();
				while ((buff = isr.readLine()) != null) {
					readBuffer.append(buff);
				}
				
				error = readBuffer.toString();
				errorSem.release();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			if (error.length() > 0)
				System.out.println(error);
		}
	}

	public SSHExec(String[] command, String input) {
		try {
			ProcessBuilder pb = new ProcessBuilder(command);
			p = pb.start();
			
			new InputWriter(input).start();
			new OutputReader().start();
			new ErrorReader().start();
			p.waitFor();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public SSHExec(String[] command, String input, OutputStream output) {
		try {
			this.outputStream = output;
			
			ProcessBuilder pb = new ProcessBuilder(command);
			p = pb.start();
			
			new InputWriter(input).start();
			new OutputReader().start();
			new ErrorReader().start();
			p.waitFor();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public SSHExec(String[] command) {
		try {
			ProcessBuilder pb = new ProcessBuilder(command);
			p = pb.start();
			
			new OutputReader().start();
			new ErrorReader().start();
			p.waitFor();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public String getOutput() {
		try {
			outputSem.acquire();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	
		String value = output;
		outputSem.release();
		return value;
	}

	public String getError() {
		try {
			errorSem.acquire();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	
		String value = error;
		errorSem.release();
		return value;
	}
	
	public void waitFor() {
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}