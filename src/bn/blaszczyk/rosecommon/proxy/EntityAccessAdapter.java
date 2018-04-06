package bn.blaszczyk.rosecommon.proxy;

import java.util.List;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rosecommon.controller.ModelController;

public class EntityAccessAdapter implements EntityAccess
{
	private final ModelController controller;

	public EntityAccessAdapter(final ModelController controller)
	{
		this.controller = controller;
	}

	@Override
	public <T extends Readable> T getOne(final Class<T> type, final int id) throws RoseException
	{
		return controller.getEntityById(type, id);
	}

	@Override
	public <T extends Readable> List<T> getMany(final Class<T> type, final List<Integer> ids) throws RoseException
	{
		return controller.getEntitiesByIds(type, ids);
	}

}
