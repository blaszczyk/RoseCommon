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
	
	public int count(final Class<? extends Readable> type)
	{
		return entities.get(TypeManager.convertType(type)).size();
	}
	
	public Stream<Readable> stream(final Class<? extends Readable> type)
	{
		return entities.get(TypeManager.convertType(type)).values().stream();
	}
	
	public boolean has(final Class<? extends Readable> type, final Integer id)
	{
		return entities.get(TypeManager.convertType(type)).containsKey(id);
	}

	public boolean has(final Readable entity)
	{
		return has(entity.getClass(), entity.getId());
	}
	
	public boolean hasExact(final Readable entity)
	{
		return entity == get(entity.getClass(), entity.getId());
	}
	
	public Readable get(final Class<? extends Readable> type, final Integer id)
	{
		return entities.get(type).get(id);
	}
	
	public void put(final Readable entity)
	{
		entities.get(TypeManager.getClass(entity)).put(entity.getId(), entity);
	}
	
	public void remove(final Class<? extends Readable> type, final Integer id)
	{
		entities.get(TypeManager.convertType(type)).remove(id);
	}
	
	public void remove(final Readable entity)
	{
		remove(entity.getClass(), entity.getId());
	}

	public void clear()
	{
		entities.values().forEach(Map::clear);
	}
	
}