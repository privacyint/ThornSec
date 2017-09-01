package core.view;

import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

public class TextAreaOutputStream extends OutputStream {

	private JTextArea area;
	
	public TextAreaOutputStream(JTextArea area) {
		this.area = area;
		DefaultCaret caret = (DefaultCaret)area.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
	}

	public void write(int b) throws IOException {
		area.append(String.valueOf((char) b));
	}
	
}