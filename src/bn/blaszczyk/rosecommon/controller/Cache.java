package bn.blaszczyk.rosecommon.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rosecommon.tools.TypeManager;

public class Cache
{
	private final Map<Class<? extends Readable>,Map<Integer,Readable>> entities = new HashMap<>();
	
	public Cache()
	{
		for(final Class<? extends Readable> type : TypeManager.getEntityClasses())
			entities.put(TypeManager.convertType(type), new TreeMap<>());
	}
	
	public <T extends Readable> int count(final Class<T> type)
	{
		return entities(type).size();
	}
	
	public <T extends Readable> Stream<T> stream(final Class<T> type)
	{
		return entities(type).values().stream();
	}

	public <T extends Readable> Stream<Integer> ids(final Class<T> type)
	{
		return entities(type).keySet().stream();
	}
	
	public <T extends Readable> boolean has(final Class<T> type, final Integer id)
	{
		return entities(type).containsKey(id);
	}

	public <T extends Readable> boolean has(final T entity)
	{
		if(entity == null)
			return false;
		return has(entity.getClass(), entity.getId());
	}
	
	public <T extends Readable> boolean hasExact(final T entity)
	{
		if(entity == null)
			return false;
		return entity == get(entity.getClass(), entity.getId());
	}
	
	public <T extends Readable> T get(final Class<T> type, final Integer id)
	{
		return entities(type).get(id);
	}
	
	<T extends Readable> void put(final T entity)
	{
		if(entity == null)
			return;
		entities.get(TypeManager.getClass(entity)).put(entity.getId(), entity);
	}
	
	void remove(final Class<? extends Readable> type, final Integer id)
	{
		entities(type).remove(id);
	}
	
	public void remove(final Readable entity)
	{
		remove(entity.getClass(), entity.getId());
	}

	public void clear()
	{
		entities.values().forEach(Map::clear);
	}

	@SuppressWarnings("unchecked")
	private <T extends Readable> Map<Integer, T> entities(final Class<T> type)
	{
		return (Map<Integer, T>) entities.get(TypeManager.convertType(type));
	}
	
}
