package bn.blaszczyk.rosecommon.controller;

import java.util.List;
import java.util.Map;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Writable;

final class SynchronizingDecorator extends AbstractControllerDecorator implements ModelController
{
	
	SynchronizingDecorator(final ModelController controller)
	{
		super(controller);
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
	public <T extends Readable> List<T> getEntities(final Class<T> type, final Map<String, String> query) throws RoseException
	{
		synchronized (controller)
		{
			return controller.getEntities(type,query);
		}
	}
	
	@Override
	public <T extends Readable> List<Integer> getIds(final Class<T> type) throws RoseException
	{
		synchronized (controller)
		{
			return controller.getIds(type);
		}
	}
	
	@Override
	public <T extends Readable> int getEntityCount(final Class<T> type) throws RoseException
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
	public <T extends Writable> T createNew(final Class<T> type) throws RoseException
	{
		synchronized (controller)
		{
			return controller.createNew(type);
		}
	}
	
	@Override
	public <T extends Writable> T createNew(final T entity) throws RoseException
	{
		synchronized (controller)
		{
			return controller.createNew(entity);
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
	public void close() throws RoseException
	{
		synchronized (controller)
		{
			controller.close();
		}
	}
	
}
