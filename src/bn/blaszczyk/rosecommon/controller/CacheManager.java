package bn.blaszczyk.rosecommon.controller;

import java.io.Reader;
import java.io.Writer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.internal.StringMap;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rosecommon.dto.RoseDto;
import bn.blaszczyk.rosecommon.proxy.EntityAccess;
import bn.blaszczyk.rosecommon.proxy.RoseProxy;
import bn.blaszczyk.rosecommon.tools.TypeManager;

public class CacheManager
{
	public static CacheManager forController(final ModelController controller)
	{
		ModelController c = controller;
		while(true)
		{
			if(c instanceof CacheController)
				return new CacheManager((CacheController) c);
			if(c instanceof AbstractControllerDecorator)
				c = ((AbstractControllerDecorator)c).unwrap();
			else
				throw new IllegalStateException("controller does not contain CacheController");
		}
	}

	private static final Logger LOGGER = LogManager.getLogger(CacheManager.class);
	
	private static final Gson GSON = new Gson();
	
	private final EntityAccess access;
	
	private final Cache cache; 
	
	private CacheManager(final CacheController controller)
	{
		this.access = controller;
		this.cache = controller.getCache();
	}
	
	public void write(final Writer writer)
	{
		LOGGER.info("start writing from cache");
		final RoseDto[] dtos = TypeManager.getEntityClasses()
				.stream()
				.flatMap(cache::stream)
				.map(RoseDto::new)
				.toArray(RoseDto[]::new);
		GSON.toJson(dtos, writer);
		LOGGER.info("done writing from cache");
	}
	
	public void read(final Reader reader) throws RoseException
	{
		LOGGER.info("start reading into cache");
		final StringMap<?>[] maps = GSON.fromJson(reader, StringMap[].class);
		for(final StringMap<?> map : maps)
		{
			final RoseDto dto = new RoseDto(map);
			final Readable entity = RoseProxy.create(dto, access);
			cache.put(entity);
		}
		LOGGER.info("done reading into cache");
	}
	
	public void clear()
	{
		cache.clear();
		LOGGER.debug("cache cleared");
	}

}
