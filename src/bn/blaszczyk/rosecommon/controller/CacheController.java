package bn.blaszczyk.rosecommon.controller;

import java.util.*;
import java.util.stream.Collectors;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.rosecommon.proxy.LazyList;
import bn.blaszczyk.rosecommon.tools.EntityUtils;

final class CacheController extends AbstractControllerDecorator implements ModelController
{

	private Cache cache = new Cache();

	private final Set<Class<? extends Readable>> fetchedTypes = new HashSet<>() ;

	CacheController(final ModelController controller)
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
		return cache.get(type,id);
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
			.collect(Collectors.toList());
	}

	@Override
	public <T extends Writable> T createNew(final Class<T> type) throws RoseException
	{
		final T entity = controller.createNew(type);
		cacheOne(entity);
		return entity;
	}

	@Override
	public <T extends Writable> T createNew(final T entity) throws RoseException
	{
		controller.createNew(entity);
		cacheOne(entity);
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
	public void update(final Writable... entities) throws RoseException
	{
		for(final Writable entity : entities)
			ensureCached(entity);
		controller.update(entities);
	}

	@Override
	public void delete(final Writable entity) throws RoseException
	{
		ensureCached(entity);
		cache.remove(entity);
		controller.delete(entity);
	}
	
	private void ensureCached(final Readable entity) throws RoseException
	{
		if(!cache.has(entity))
			cache.put(entity);
		if(!cache.hasExact(entity))
			throw new RoseException("uncached entity: " + EntityUtils.toStringSimple(entity));
	}

	private void cacheOne(final Readable entity) throws RoseException
	{
		if(cache.has(entity))
			throw new RoseException("attempting to cache duplicate entity: " + EntityUtils.toStringSimple(entity));
		else
			cache.put(entity);
	}
	
	private void cacheMany(final List<? extends Readable> newEntities, final Class<? extends Readable> type) throws RoseException
	{
		newEntities.stream()
			.filter(e -> !cache.has(e))
			.forEach(cache::put);
	}

}
