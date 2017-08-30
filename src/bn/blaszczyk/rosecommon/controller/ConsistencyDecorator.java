package bn.blaszczyk.rosecommon.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Timestamped;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.rosecommon.tools.EntityUtils;
import bn.blaszczyk.rosecommon.tools.TypeManager;

public class ConsistencyDecorator extends AbstractControllerDecorator implements ModelController {
	
	public ConsistencyDecorator(final ModelController controller)
	{
		super(controller);
	}
	
	@Override
	public <T extends Readable> T getEntityById(Class<T> type, int id) throws RoseException
	{
		if(id < 0)
			throw new RoseException("Malicious id: " + id);
		return controller.getEntityById(type, id);
	}
	
	@Override
	public <T extends Readable> List<T> getEntitiesByIds(final Class<T> type, final List<Integer> ids)
			throws RoseException
	{
		if(ids.stream().anyMatch(id -> id < 0))
			throw new RoseException("Malicious id: " + ids);
		return controller.getEntitiesByIds(type, ids);
	}

	@Override
	public void update(final Writable... entities) throws RoseException
	{
		final List<Writable> checkedEntities = Arrays.stream(entities)
				.filter(e -> e != null)
				.collect(Collectors.toList());
		for(final Writable entity : checkedEntities)
		{
			if(entity instanceof Timestamped)
			{
				final Timestamped timestamped = (Timestamped) entity;
				final Timestamped reference = (Timestamped) controller.getEntityById(TypeManager.getClass(entity), entity.getId());
				if(!reference.getTimestamp().equals(timestamped.getTimestamp()))
					throw new RoseException("Entity is out of synchronization: " + EntityUtils.toStringSimple(entity));
			}
		}
		controller.update( checkedEntities.toArray(new Writable[checkedEntities.size()]));
		if(entities.length != checkedEntities.size())
			throw new RoseException("Trying to update null entity.");
	}
	
	@Override
	public void delete(final Writable entity) throws RoseException
	{
		if(entity == null)
			return;
		for(int i = 0; i < entity.getEntityCount(); i++)
		{
			if(entity.getRelationType(i).isSecondMany())
			{
				final Set<? extends Readable> set = new TreeSet<>( entity.getEntityValueMany(i));
				for(Readable subEntity : set)
					if(subEntity != null)
						entity.removeEntity(i, (Writable) subEntity);
			}
			else
				if(entity.getEntityValueOne(i) != null)
					entity.setEntity(i, null);
		}
		super.delete(entity);
	}
	
}
