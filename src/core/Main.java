package core;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import core.model.ThornsecModel;
import core.view.FullFrame;

public class Main {

	public static void main(String[] args) throws Exception {
		String text = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8)
				.replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)", "");

		ThornsecModel model = new ThornsecModel();
		model.read(text);
		model.init();

		new FullFrame(model);
	}

}
