package bn.blaszczyk.rosecommon.controller;

import java.util.List;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rosecommon.proxy.EntityAccess;
import bn.blaszczyk.rosecommon.proxy.EntityAccessAdapter;
import bn.blaszczyk.rosecommon.proxy.LazyList;

public class LazyListDecorator extends AbstractControllerDecorator
{
	private final EntityAccess access;

	LazyListDecorator(final ModelController controller)
	{
		super(controller);
		access = new EntityAccessAdapter(controller);
	}

	@Override
	public <T extends Readable> List<T> getEntities(final Class<T> type) throws RoseException
	{
		return new LazyList<>(type, getIds(type), access);
	}

	@Override
	public <T extends Readable> List<T> getEntitiesByIds(final Class<T> type, final List<Integer> ids) throws RoseException
	{
		return new LazyList<>(type, ids, access);
	}

}
