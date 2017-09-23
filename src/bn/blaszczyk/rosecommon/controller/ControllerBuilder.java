package bn.blaszczyk.rosecommon.controller;

import static bn.blaszczyk.rosecommon.tools.CommonPreference.*;
import static bn.blaszczyk.rosecommon.tools.Preferences.*;

import java.util.HashMap;
import java.util.Map;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rosecommon.tools.CommonPreference;
import bn.blaszczyk.rosecommon.tools.Preferences;

public class ControllerBuilder
{
	
	public static ControllerBuilder forService()
	{
		final String host = Preferences.getStringValue(CommonPreference.SERVICE_HOST);
		final int port = Preferences.getIntegerValue(CommonPreference.SERVICE_PORT);
		return forService(host, port);
	}
	
	public static ControllerBuilder forService(final String host, final int port)
	{
		return new ControllerBuilder(new RestController(host, port));
	}
	
	public static ControllerBuilder forDataBase() throws RoseException
	{
		final Map<String, String> properties = new HashMap<>();
		if(!isDefault(DB_HOST) && !isDefault(DB_PORT) && !isDefault(DB_NAME))
			properties.put(PersistenceController.KEY_URL, String.format("jdbc:mysql://%s:%s/%s",getStringValue(DB_HOST),getStringValue(DB_PORT), getStringValue(DB_NAME)));
		if(!isDefault(DB_USER))
			properties.put(PersistenceController.KEY_USER, getStringValue(DB_USER));
		if(!isDefault(DB_PASSWORD))
			properties.put(PersistenceController.KEY_PW, getStringValue(DB_PASSWORD));
		return forDataBase(properties);
	}
	
	public static ControllerBuilder forDataBase(final Map<String,String> properties) throws RoseException
	{
		return new ControllerBuilder(new PersistenceController(properties));
	}
	
	private final ModelController innerController;
	
	private CacheController cacheController;
	
	private ModelController controller;
	
	private ControllerBuilder(final ModelController controller)
	{
		this.innerController = controller;
		this.controller = controller;
	}
	
	public ControllerBuilder withCache()
	{
		cacheController = new CacheController(controller);
		if(innerController instanceof RestController)
			((RestController)innerController).setEntityAccess(cacheController);
		controller = cacheController;
		return this;
	}
	
	public ControllerBuilder withSynchronizer()
	{
		controller = new SynchronizingDecorator(controller);
		return this;
	}
	
	public ControllerBuilder withConsistencyCheck()
	{
		controller = new ConsistencyDecorator(controller);
		return this;
	}
	
	public CacheController getCacheController()
	{
		return cacheController;
	}
	
	public ModelController build()
	{
		return controller;
	}
	
}
