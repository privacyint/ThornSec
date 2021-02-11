package org.privacyinternational.thornsec.core.exec;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

class InputExec implements Runnable {

	private InputStreamReader reader;
	private OutputStream writer;

	InputExec(InputStream stream, OutputStream writer) {
		this.writer = writer;
		reader = new InputStreamReader(stream);
	}

	public void run() {
		try {
			int c = reader.read();
			while (c != -1) {
				writer.write(c);
				c = reader.read();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
