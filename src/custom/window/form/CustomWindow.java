package custom.window.form;

import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.CustomForm;
import org.compiere.model.MQuery;
import org.compiere.util.Env;

import custom.window.webui.adwindow.CustomADWindow;


/**
 *  Custom Window
 */
public class CustomWindow extends AbstractCustomWindow {

	private CustomForm form;

    public CustomWindow()
    {
    	form = new CustomForm();
    	//form.setHeight("100%");
    }

    @Override
    public void createCustomWindow(int AD_Window_ID)
    {
    	CustomADWindow adw = new CustomADWindow(Env.getCtx(), AD_Window_ID, null);
    	adw.createPart(form);
    }


	@Override
	public void createCustomWindow(int AD_Window_ID, MQuery query)
	{
		CustomADWindow adw = new CustomADWindow(Env.getCtx(), AD_Window_ID, query);
    	adw.createPart(form);
	}

	@Override
	public ADForm getForm()
	{
		return form;
	}




}
