package bn.blaszczyk.rosecommon.controller;

import java.util.List;

import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.rosecommon.RoseException;

public class SynchronizingDecorator implements ModelController {

	protected final ModelController controller;
	
	public SynchronizingDecorator(final ModelController controller)
	{
		this.controller = controller;
	}
	
	@Override
	public <T extends Readable> List<T> getEntities(final Class<T> type) throws RoseException
	{
		synchronized (controller)
		{
			return controller.getEntities(type);
		}
	}
	
	@Override
	public List<Integer> getIds(final Class<? extends Readable> type) throws RoseException
	{
		synchronized (controller)
		{
			return controller.getIds(type);
		}
	}
	
	@Override
	public int getEntityCount(final Class<? extends Readable> type) throws RoseException
	{
		synchronized (controller)
		{
			return controller.getEntityCount(type);
		}
	}
	
	@Override
	public <T extends Readable> T getEntityById(final Class<T> type, final int id) throws RoseException
	{
		synchronized (controller)
		{
			return controller.getEntityById(type, id);
		}
	}
	
	@Override
	public <T extends Readable> List<T> getEntitiesByIds(final Class<T> type, final List<Integer> ids) throws RoseException
	{
		synchronized (controller)
		{
			return controller.getEntitiesByIds(type, ids);
		}
	}
	
	@Override
	public <T extends Readable> T createNew(final Class<T> type) throws RoseException
	{
		synchronized (controller)
		{
			return controller.createNew(type);
		}
	}
	
	@Override
	public Writable createCopy(final Writable entity) throws RoseException
	{
		synchronized (controller)
		{
			return controller.createCopy(entity);
		}
	}
	
	@Override
	public void update(final Writable... entities) throws RoseException
	{
		synchronized (controller)
		{
			controller.update(entities);
		}
	}
	
	@Override
	public void delete(final Writable entity) throws RoseException
	{
		synchronized (controller)
		{
			controller.delete(entity);
		}
	}
	
	@Override
	public void close()
	{
		synchronized (controller)
		{
			controller.close();
		}
	}
	
}
