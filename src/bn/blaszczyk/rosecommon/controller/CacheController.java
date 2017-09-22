package bn.blaszczyk.rosecommon.controller;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.logging.log4j.*;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.rosecommon.proxy.EntityAccess;
import bn.blaszczyk.rosecommon.proxy.LazyList;
import bn.blaszczyk.rosecommon.tools.EntityUtils;

public class CacheController extends AbstractControllerDecorator implements ModelController, EntityAccess
{
	
	private static final Logger LOGGER = LogManager.getLogger(CacheController.class);

	private Cache cache = new Cache();

	private final Set<Class<? extends Readable>> fetchedTypes = new HashSet<>() ;

	public CacheController(final ModelController controller)
	{
		super(controller);
	}
	
	Cache getCache()
	{
		return cache;
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
		return cache.stream(type)
			.map(type::cast)
			.collect(Collectors.toList());
	}
	
	@Override
	public <T extends Readable> List<Integer> getIds(final Class<T> type) throws RoseException
	{
		if(fetchedTypes.contains(type))
			return cache.ids(type).collect(Collectors.toList());
		else
			return controller.getIds(type);
	}

	@Override
	public <T extends Readable> int getEntityCount(final Class<T> type) throws RoseException
	{
		if(fetchedTypes.contains(type))
			return cache.count(type);
		else
			return controller.getEntityCount(type);
	}

	@Override
	public <T extends Readable> T getEntityById(final Class<T> type, int id) throws RoseException
	{
		if(!cache.has(type, id))
			cacheOne(controller.getEntityById(type, id));
		return type.cast(cache.get(type,id));
	}
	
	@Override
	public <T extends Readable> List<T> getEntitiesByIds(final Class<T> type, final List<Integer> ids) throws RoseException
	{
		final List<Integer> missingIds = ids.stream()
			.filter(id -> ! cache.has(type, id))
			.collect(Collectors.toList());
		final List<? extends Readable> fetchedEntities = controller.getEntitiesByIds(type, missingIds);
		cacheMany(fetchedEntities, type);
		return ids.stream()
			.map(id -> cache.get(type,id))
			.map(type::cast)
			.collect(Collectors.toList());
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
			assertEqualsCached(entity);
		controller.update(entities);
	}

	@Override
	public void delete(Writable entity) throws RoseException
	{
		assertEqualsCached(entity);
		cache.remove(entity);
		LOGGER.debug("removing entity from cache: " + EntityUtils.toStringSimple(entity));
		controller.delete(entity);
	}

	@Override
	public <T extends Readable> T getOne(final Class<T> type, final int id) throws RoseException
	{
		return getEntityById(type, id);
	}

	@Override
	public <T extends Readable> List<T> getMany(final Class<T> type, final List<Integer> ids) throws RoseException
	{
		return getEntitiesByIds(type, ids);
	}
	
	private void assertEqualsCached(final Readable entity) throws RoseException
	{
		if(!cache.hasExact(entity))
			throw new RoseException("uncached entity: " + EntityUtils.toStringSimple(entity));		
	}

	private Readable cacheOne(final Readable entity)
	{
		if(cache.has(entity))
		{
			LOGGER.error("Trying to add entity with duplicate id: " + EntityUtils.toStringSimple(entity));
			return cache.get(entity.getClass(), entity.getId());
		}
		cache.put(entity);
		return entity;
	}
	
	private void cacheMany(final List<? extends Readable> newEntities, final Class<? extends Readable> type) throws RoseException
	{
		newEntities.stream()
			.filter(e -> !cache.has(e))
			.forEach(cache::put);
	}

}
