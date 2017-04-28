
package bn.blaszczyk.rosecommon.controller;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.rosecommon.RoseException;
import bn.blaszczyk.rosecommon.proxy.EntityAccess;
import bn.blaszczyk.rosecommon.tools.EntityUtils;
import bn.blaszczyk.rosecommon.tools.TypeManager;

public class CacheController extends AbstractControllerDecorator implements ModelController, EntityAccess {
	
	private static final Logger LOGGER = Logger.getLogger(CacheController.class);

	private final Map<Class<? extends Readable>,Map<Integer,Readable>> allEntities = new HashMap<>();
	
	private final Set<Class<? extends Readable>> fetchedTypes = new HashSet<>() ;

	public CacheController(final ModelController controller)
	{
		super(controller);
		for(Class<? extends Readable> type : TypeManager.getEntityClasses())
			allEntities.put(type, new TreeMap<>());
	}
	
	@Override
	public List<Readable> getEntities(final Class<? extends Readable> type) throws RoseException
	{
		if(!fetchedTypes.contains(type))
			synchronize(type);
		final Collection<Readable> entities = allEntities.get(type).values();
		return Collections.unmodifiableList(new ArrayList<Readable>(entities));
	}
	
	@Override
	public List<Integer> getIds(Class<? extends Readable> type) throws RoseException
	{
		if(!fetchedTypes.contains(type))
			return super.getIds(type);
		return allEntities.get(type)
						.values()
						.stream()
						.map(Readable::getId)
						.collect(Collectors.toList());
	}

	@Override
	public int getEntityCount(final Class<? extends Readable> type) throws RoseException
	{
		return allEntities.get(type).size();
	}

	@Override
	public Readable getEntityById(Class<? extends Readable> type, int id) throws RoseException
	{
		if(!hasEntityId(type, id))
			return addEntity(controller.getEntityById(type, id));
		return allEntities.get(type).get(id);
	}
	
	@Override
	public List<? extends Readable> getEntitiesByIds(Class<? extends Readable> type, List<Integer> ids)
			throws RoseException
	{
		return getMany(type, ids);
	}

	@Override
	public <T extends Readable> T createNew(final Class<T> type) throws RoseException
	{
		final T entity = controller.createNew(type);
		addEntity(entity);
		LOGGER.info("buffering new entity: " + EntityUtils.toStringSimple(entity));
		return entity;
	}

	@Override
	public Writable createCopy(final Writable entity) throws RoseException
	{
		final Writable copy = controller.createCopy(entity);
		addEntity(copy);
		return copy;
	}

	@Override
	public void delete(Writable entity) throws RoseException
	{
		allEntities.get(TypeManager.convertType(entity.getClass())).remove(entity);
		LOGGER.debug("entity removed from buffer: " + EntityUtils.toStringSimple(entity));
	}
	
	public void synchronize(final Class<? extends Readable> type) throws RoseException
	{
		final Map<Integer,Readable> entities = allEntities.get(type);
		entities.clear();
		controller.getEntities(type)
			.stream()
			.forEach( e -> entities.put(e.getId(), e));
		fetchedTypes.add(type);
	}

	public void clearEntities(final Class<?> type)
	{
		allEntities.get(type).clear();
	}
	
	private boolean hasEntityId(final Class<? extends Readable> type, final Integer id)
	{
		return allEntities.get(type).containsKey(id);
	}

	private Readable addEntity(final Readable entity)
	{
		final Map<Integer,Readable> entities = allEntities.get(TypeManager.getClass(entity));
		final Integer id = entity.getId();
		if(entities.containsKey(id))
		{
			LOGGER.error("Trying to add entity with duplicate id: " + EntityUtils.toStringSimple(entity));
			return entities.get(id);
		}
		entities.put(id, entity);
		return entity;
	}

	@Override
	public Writable getOne(final Class<? extends Readable> type, final int id) throws RoseException
	{
		return (Writable) getEntityById(type, id);
	}

	@Override
	public List<? extends Writable> getMany(final Class<? extends Readable> type, final List<Integer> ids) throws RoseException
	{
		final List<Writable> entities = new ArrayList<>(ids.size());
		final List<Integer> missingIds = new ArrayList<>();
		for(final Integer id : ids)
			if(hasEntityId(type, id))
				entities.add((Writable) allEntities.get(type).get(id));
			else
				missingIds.add(id);
		super.getEntitiesByIds(type, missingIds).forEach( e -> entities.add((Writable)e));
		return entities;
	}

}
