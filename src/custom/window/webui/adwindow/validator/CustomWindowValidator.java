package custom.window.webui.adwindow.validator;

import org.adempiere.util.Callback;

public interface CustomWindowValidator {
	public void onWindowEvent(CustomWindowValidatorEvent event, Callback<Boolean> callback);
}
