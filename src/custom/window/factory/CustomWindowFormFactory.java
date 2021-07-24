package custom.window.factory;

import java.util.logging.Level;

import org.adempiere.webui.factory.IFormFactory;
import org.adempiere.webui.panel.ADForm;
import org.compiere.util.CLogger;

import custom.window.form.AbstractCustomWindow;

/**
 *  Custom Window form Factory
 */
public class CustomWindowFormFactory implements IFormFactory {

	private static final CLogger log = CLogger.getCLogger(CustomWindowFormFactory.class);

	public CustomWindowFormFactory()
	{
		;
	}


	@Override
	public ADForm newFormInstance(String formName)
	{

		Object form = null;
		if(formName.startsWith("custom.window.AD_Window_ID=")){

			int AD_Window_ID = Integer.valueOf(formName.substring("custom.window.AD_Window_ID=".length())).intValue();

			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			Class<?> clazz = null;
			if (loader != null) {
	    		try
	    		{
	        		clazz = loader.loadClass("custom.window.form.CustomWindow");
	    		}
	    		catch (Exception e)
	    		{
	    			if (log.isLoggable(Level.INFO))
	    				log.log(Level.INFO, e.getLocalizedMessage(), e);
	    		}
			}
			if (clazz == null) {
				loader = this.getClass().getClassLoader();
				try
	    		{
	    			//	Create instance w/o parameters
	        		clazz = loader.loadClass("custom.window.form.CustomWindow");
	    		}
	    		catch (Exception e)
	    		{
	    			if (log.isLoggable(Level.INFO))
	    				log.log(Level.INFO, e.getLocalizedMessage(), e);
	    		}
			}

			if (clazz != null) {
				try
	    		{
	    			form = clazz.getDeclaredConstructor().newInstance();
	    		}
	    		catch (Exception e)
	    		{
	    			if (log.isLoggable(Level.WARNING))
	    				log.log(Level.WARNING, e.getLocalizedMessage(), e);
	    		}
			}

			if (form != null) {
				if (form instanceof AbstractCustomWindow ) {
					AbstractCustomWindow  controller = (AbstractCustomWindow) form;
					controller.createCustomWindow(AD_Window_ID);
					ADForm adForm = controller.getForm();
					adForm.setICustomForm(controller);
					return adForm;
				}
			}
		}

		return null;
	}


}
