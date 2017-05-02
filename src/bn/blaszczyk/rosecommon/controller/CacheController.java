package bn.blaszczyk.rosecommon.controller;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.rosecommon.RoseException;
import bn.blaszczyk.rosecommon.proxy.EntityAccess;
import bn.blaszczyk.rosecommon.proxy.LazyList;
import bn.blaszczyk.rosecommon.tools.EntityUtils;
import bn.blaszczyk.rosecommon.tools.TypeManager;

public class CacheController extends AbstractControllerDecorator implements ModelController, EntityAccess {
	
	private static final Logger LOGGER = Logger.getLogger(CacheController.class);

	private final Map<Class<? extends Readable>,Map<Integer,Readable>> allEntities = new HashMap<>();
	
	private final Set<Class<? extends Readable>> fetchedTypes = new HashSet<>() ;

	public CacheController(final ModelController controller)
	{
		super(controller);
		for(final Class<? extends Readable> type : TypeManager.getEntityClasses())
			allEntities.put(type, new TreeMap<>());
	}
	
	@Override
	public <T extends Readable> List<T> getEntities(final Class<T> type) throws RoseException
	{
		if(!fetchedTypes.contains(type))
		{
			final List<T> fetchedEntities = controller.getEntities(type);
			if(fetchedEntities instanceof LazyList)
				return fetchedEntities;
			cacheMany(fetchedEntities, type);
			fetchedTypes.add(type);
		}
		return allEntities.get(type)
				.values()
				.stream()
				.map(type::cast)
				.collect(Collectors.toList());
	}
	
	@Override
	public List<Integer> getIds(Class<? extends Readable> type) throws RoseException
	{
		if(!fetchedTypes.contains(type))
			return controller.getIds(type);
		return allEntities.get(type)
						.values()
						.stream()
						.map(Readable::getId)
						.collect(Collectors.toList());
	}

	@Override
	public int getEntityCount(final Class<? extends Readable> type) throws RoseException
	{
		if(fetchedTypes.contains(type))
			return allEntities.get(type).size();
		else
			return controller.getEntityCount(type);
	}

	@Override
	public <T extends Readable> T getEntityById(Class<T> type, int id) throws RoseException
	{
		if(!hasEntity(type, id))
			cacheOne(controller.getEntityById(type, id));
		return type.cast(allEntities.get(type).get(id));
	}
	
	@Override
	public <T extends Readable> List<T> getEntitiesByIds(Class<T> type, List<Integer> ids)
			throws RoseException
	{
		return getMany(type, ids);
	}

	@Override
	public <T extends Readable> T createNew(final Class<T> type) throws RoseException
	{
		final T entity = controller.createNew(type);
		cacheOne(entity);
		LOGGER.debug("caching new entity: " + EntityUtils.toStringSimple(entity));
		return entity;
	}

	@Override
	public Writable createCopy(final Writable entity) throws RoseException
	{
		final Writable copy = controller.createCopy(entity);
		cacheOne(copy);
		return copy;
	}
	
	@Override
	public void update(Writable... entities) throws RoseException
	{
		for(final Writable entity : entities)
			if(!equalsCached(entity))
				throw new RoseException("Updating uncached entity: " + EntityUtils.toStringSimple(entity));
		controller.update(entities);
	}

	@Override
	public void delete(Writable entity) throws RoseException
	{
		if(!equalsCached(entity))
			throw new RoseException("Deleting uncached entity: " + EntityUtils.toStringSimple(entity));
		allEntities.get(TypeManager.convertType(entity.getClass())).remove(entity.getId());
		LOGGER.debug("removing entity from cache: " + EntityUtils.toStringSimple(entity));
		controller.delete(entity);
	}
	
	private void cacheMany(final List<? extends Readable> newEntities, final Class<? extends Readable> type) throws RoseException
	{
		final Map<Integer,Readable> entities = allEntities.get(type);
		newEntities.stream()
			.filter(e -> !entities.containsKey(e.getId()))
			.forEach( e -> entities.put(e.getId(), e));
	}
	
	private boolean equalsCached(final Readable entity)
	{
		final Readable cachedEntity = allEntities.get(TypeManager.getClass(entity)).get(entity.getId());
		return cachedEntity == entity;
	}
	
	private boolean hasEntity(final Class<? extends Readable> type, final Integer id)
	{
		return allEntities.get(type).containsKey(id);
	}

	private Readable cacheOne(final Readable entity)
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
	public <T extends Readable> T getOne(final Class<T> type, final int id) throws RoseException
	{
		return getEntityById(type, id);
	}

	@Override
	public <T extends Readable> List<T> getMany(final Class<T> type, final List<Integer> ids) throws RoseException
	{
		final Map<Integer,Readable> entities = allEntities.get(type);
		final List<Integer> missingIds = ids.stream()
											.filter(id -> ! entities.containsKey(id))
											.collect(Collectors.toList());
		final List<? extends Readable> fetchedEntities = controller.getEntitiesByIds(type, missingIds);
		cacheMany(fetchedEntities, type);
		return ids.stream()
					.map(id -> entities.get(id))
					.map(type::cast)
					.collect(Collectors.toList());
	}

}
