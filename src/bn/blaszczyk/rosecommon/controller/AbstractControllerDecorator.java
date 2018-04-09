package bn.blaszczyk.rosecommon.controller;

import java.util.List;
import java.util.Map;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Writable;

abstract class AbstractControllerDecorator implements ModelController
{

	final ModelController controller;
	
	AbstractControllerDecorator(final ModelController controller)
	{
		this.controller = controller;
	}
	
	ModelController unwrap()
	{
		return controller;
	}
	
	@Override
	public <T extends Readable> List<T> getEntities(final Class<T> type) throws RoseException
	{
		return controller.getEntities(type);
	}
	
	@Override
	public <T extends Readable> List<T> getEntities(final Class<T> type, final Map<String, String> query) throws RoseException
	{
		return controller.getEntities(type, query);
	}
	
	@Override
	public <T extends Readable> List<Integer> getIds(final Class<T> type) throws RoseException
	{
		return controller.getIds(type);
	}
	
	@Override
	public <T extends Readable> int getEntityCount(final Class<T> type) throws RoseException
	{
		return controller.getEntityCount(type);
	}
	
	@Override
	public <T extends Readable> T getEntityById(final Class<T> type, final int id) throws RoseException
	{
		return controller.getEntityById(type, id);
	}
	
	@Override
	public <T extends Readable> List<T> getEntitiesByIds(final Class<T> type, final List<Integer> ids) throws RoseException
	{
		return controller.getEntitiesByIds(type, ids);
	}
	
	@Override
	public <T extends Writable> T createNew(final Class<T> type) throws RoseException
	{
		return controller.createNew(type);
	}
	
	@Override
	public <T extends Writable> T createNew(final T entity) throws RoseException
	{
		return controller.createNew(entity);
	}
	
	@Override
	public Writable createCopy(final Writable entity) throws RoseException
	{
		return controller.createCopy(entity);
	}
	
	@Override
	public void update(final Writable... entities) throws RoseException
	{
		controller.update(entities);
	}
	
	@Override
	public void delete(final Writable entity) throws RoseException
	{
		controller.delete(entity);
	}
	
	@Override
	public void close() throws RoseException
	{
		controller.close();
	}
	
}
