package core;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import core.model.network.ThornsecModel;
import core.view.FullFrame;

public class Main {

	/**
	 * The main method.
	 *
	 * @param args [0] - Path to our JSON
	 * @throws Exception Cannot read the JSON file
	 */
	public static void main(String[] args) throws Exception {
		String jsonPath = null;
		
		if (args.length == 0) { 
		    JFileChooser chooser = new JFileChooser();
		    FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON Config File", "json");
		    chooser.setFileFilter(filter);
		    int returnVal = chooser.showOpenDialog(null);
		    
		    if (returnVal == JFileChooser.APPROVE_OPTION) {
		       jsonPath = chooser.getSelectedFile().getPath();
		    }
		}
		else {
			jsonPath = args[0];
		}
		
		String text = new String(Files.readAllBytes(Paths.get(jsonPath)), StandardCharsets.UTF_8)
				.replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)", "");

		ThornsecModel model = new ThornsecModel();
		model.read(text);
		model.init();

		new FullFrame(model);
	}

}
