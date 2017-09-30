package bn.blaszczyk.rosecommon.controller;

import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.Dto;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rosecommon.proxy.EntityAccess;
import bn.blaszczyk.rosecommon.proxy.RoseProxy;
import bn.blaszczyk.rosecommon.tools.EntityUtils;
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
	
	public void writeTo(final Writer writer) throws RoseException
	{
		LOGGER.debug("start writing from cache");
		final Map<String, String> dtoJsons = new HashMap<>();
		for(final Class<? extends Readable> type : TypeManager.getEntityClasses())
		{
			final Dto[] dtos = cache.stream(type)
					.map(EntityUtils::toDtoSilent)
					.toArray(Dto[]::new);
			dtoJsons.put(type.getSimpleName(), GSON.toJson(dtos));
			LOGGER.debug("writing " + dtos.length + " instances of " + type.getSimpleName());
		}
		GSON.toJson(dtoJsons, writer);
		LOGGER.debug("done writing from cache");
	}
	
	public void readFrom(final Reader reader) throws RoseException
	{
		LOGGER.debug("start reading into cache");
		final Map<?,?> dtoJsons = GSON.fromJson(reader, Map.class);
		for(final Entry<?, ?> entry : dtoJsons.entrySet())
		{
			final Class<? extends Readable> type = TypeManager.getClass(String.valueOf(entry.getKey()));
			final Dto[] dtos = GSON.fromJson(reader, TypeManager.getDtoArrayClass(type));
			for(final Dto dto : dtos)
			{
				final Readable entity = RoseProxy.create(dto, access);
				cache.put(entity);
			}
			LOGGER.debug("reading " + dtos.length + " instances of " + type.getSimpleName() + " into cache");
		}
		LOGGER.debug("done reading into cache");
	}
	
	public void clear()
	{
		cache.clear();
		LOGGER.debug("cache cleared");
	}

}
