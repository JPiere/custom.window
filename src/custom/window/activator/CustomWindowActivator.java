package custom.window.activator;

import org.compiere.util.CLogger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import custom.window.webui.adwindow.validator.CustomWindowValidatorManager;

/**
 *  Custom Window Activator
 */
public class CustomWindowActivator implements BundleActivator {

	protected final static CLogger logger = CLogger.getCLogger(CustomWindowActivator.class.getName());

	@Override
	public void start(BundleContext context) throws Exception
	{
		CustomWindowValidatorManager validatorMgr = new CustomWindowValidatorManager();
		validatorMgr.start(context);
	}

	@Override
	public void stop(BundleContext context) throws Exception
	{
		CustomWindowValidatorManager.getInstance().stop(context);
	}


}
