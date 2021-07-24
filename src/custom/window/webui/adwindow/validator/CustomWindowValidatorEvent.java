package custom.window.webui.adwindow.validator;

import custom.window.webui.adwindow.CustomADWindow;

public class CustomWindowValidatorEvent {
	private CustomADWindow window;
	private String name;

	public CustomWindowValidatorEvent(CustomADWindow window, String name) {
		this.window = window;
		this.name = name;
	}

	public CustomADWindow getWindow() {
		return this.window;
	}

	public String getName() {
		return this.name;
	}
}
