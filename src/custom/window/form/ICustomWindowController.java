package custom.window.form;

import org.compiere.model.MQuery;


/**
 *  Interface Custom Window Controller
 */
public interface ICustomWindowController
{
	public void createCustomWindow(int AD_Window_ID);

	public void createCustomWindow(int AD_Window_ID,MQuery query);
}
