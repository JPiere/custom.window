package custom.window.webui.adwindow.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.adempiere.util.Callback;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import custom.window.webui.adwindow.CustomADWindow;

public class CustomWindowValidatorManager implements BundleActivator, ServiceTrackerCustomizer<CustomWindowValidator, CustomWindowValidator> {

	private static CustomWindowValidatorManager instance = null;

	private BundleContext context;
	private Map<String, List<CustomWindowValidator>> validatorMap = new HashMap<String, List<CustomWindowValidator>>();
	private List<CustomWindowValidator> globalValidators = new ArrayList<CustomWindowValidator>();

	private ServiceTracker<CustomWindowValidator, CustomWindowValidator> serviceTracker;

	@Override
	public CustomWindowValidator addingService(
			ServiceReference<CustomWindowValidator> reference) {
		CustomWindowValidator service = context.getService(reference);

		Object obj = reference.getProperty("AD_Window_UU");

		if (obj instanceof String) {
			String uuid = (String) reference.getProperty("AD_Window_UU");
			if (uuid == null || "*".equals(uuid)) {
				globalValidators.add(service);
				return service;
			}
			addService(service, uuid);
		}
		else if (obj instanceof String []) {
			String[] uuids = (String []) reference.getProperty("AD_Window_UU");
			for (String uuid : uuids)
				addService(service, uuid);
		}
		return service;
	}

	void addService(CustomWindowValidator service, String uuid) {
		List<CustomWindowValidator> list = validatorMap.get(uuid);
		if (list == null) {
			list = new ArrayList<CustomWindowValidator>();
			validatorMap.put(uuid, list);
		}
		list.add(service);
	}

	@Override
	public void modifiedService(ServiceReference<CustomWindowValidator> reference,
			CustomWindowValidator service) {
	}

	@Override
	public void removedService(ServiceReference<CustomWindowValidator> reference,
			CustomWindowValidator service) {

		Object obj = reference.getProperty("AD_Window_UU");

		if (obj instanceof String) {
			String uuid = (String) reference.getProperty("AD_Window_UU");
			if (uuid == null || "*".equals(uuid)) {
				globalValidators.remove(service);
			}
			else
				removeService(service, uuid);
		}
		else if (obj instanceof String []) {
			String[] uuids = (String []) reference.getProperty("AD_Window_UU");
			for (String uuid : uuids)
				removeService(service, uuid);
		}
	}

	void removeService(CustomWindowValidator service, String uuid) {
		List<CustomWindowValidator> list = validatorMap.get(uuid);
		if (list != null) {
			list.remove(service);
		}
	}

	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		serviceTracker = new ServiceTracker<CustomWindowValidator, CustomWindowValidator>(context, CustomWindowValidator.class.getName(), this);
		serviceTracker.open();

		instance = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		serviceTracker.close();
		this.context = null;
		instance = null;
	}

	public static CustomWindowValidatorManager getInstance() {
		return instance;
	}

	public void fireWindowValidatorEvent(CustomWindowValidatorEvent event, Callback<Boolean> callback) {
		CustomADWindow window = event.getWindow();
		String uuid = window.getAD_Window_UU();
		List<CustomWindowValidator> list = validatorMap.get(uuid);
		int listSize = list != null ? list.size() : 0;
		CustomWindowValidator[] validators = new CustomWindowValidator[listSize+globalValidators.size()];
		int index = -1;
		if (listSize > 0) {
			for(CustomWindowValidator validator : list) {
				index++;
				validators[index] = validator;
			}
		}
		for(CustomWindowValidator validator : globalValidators) {
			index++;
			validators[index] = validator;
		}
		ChainCallback chain = new ChainCallback(event, validators, callback);
		chain.start();
	}

	private static class ChainCallback implements Callback<Boolean> {

		private Callback<Boolean> callback;
		private CustomWindowValidator[] validators;
		private CustomWindowValidatorEvent event;
		private int index = -1;

		public ChainCallback(CustomWindowValidatorEvent event, CustomWindowValidator[] validators, Callback<Boolean> callback) {
			this.event = event;
			this.validators = validators;
			this.callback = callback;
		}

		public void start() {
			index = 0;
			if (index < validators.length)
				validators[index].onWindowEvent(event, this);
			else if (callback != null)
				callback.onCallback(true);
		}

		@Override
		public void onCallback(Boolean result) {
			if (result) {
				if (index < validators.length-1) {
					index++;
					validators[index].onWindowEvent(event, this);
				} else if (callback != null){
					callback.onCallback(result);
				}
			} else if (callback != null){
				callback.onCallback(result);
			}
		}

	}
}
