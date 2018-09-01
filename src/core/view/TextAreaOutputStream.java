package core.view;

import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JTextArea;

public class TextAreaOutputStream extends OutputStream {

	private JTextArea area;

	public TextAreaOutputStream(JTextArea area) {
		this.area = area;
	}

	public void write(int b) throws IOException {
		area.append(String.valueOf((char) b));
	}

}
