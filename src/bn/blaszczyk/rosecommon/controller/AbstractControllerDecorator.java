package bn.blaszczyk.rosecommon.controller;

import java.util.List;

import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.rosecommon.RoseException;

public abstract class AbstractControllerDecorator implements ModelController {

	protected final ModelController controller;
	
	public AbstractControllerDecorator(final ModelController controller)
	{
		this.controller = controller;
	}
	
	@Override
	public <T extends Readable> List<T> getEntities(final Class<T> type) throws RoseException
	{
		return controller.getEntities(type);
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
	public <T extends Readable> T createNew(final Class<T> type) throws RoseException
	{
		return controller.createNew(type);
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
