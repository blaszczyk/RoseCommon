package bn.blaszczyk.rosecommon.controller;

import bn.blaszczyk.rose.RoseException;

public class ControllerBuilder {
	
	public static ControllerBuilder forService()
	{
		return new ControllerBuilder(new RestController());
	}
	
	public static ControllerBuilder forDataBase() throws RoseException
	{
		return new ControllerBuilder(new PersistenceController());
	}
	
	private final  ModelController innerController;
	
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
