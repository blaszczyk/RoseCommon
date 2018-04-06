package bn.blaszczyk.rosecommon.controller;

import java.io.Reader;
import java.io.Writer;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.Dto;
import bn.blaszczyk.rose.model.DtoContainer;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rosecommon.proxy.EntityAccess;
import bn.blaszczyk.rosecommon.proxy.EntityAccessAdapter;
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
		this.access = new EntityAccessAdapter(controller);
		this.cache = controller.getCache();
	}
	
	public void writeTo(final Writer writer) throws RoseException
	{
		LOGGER.debug("start writing from cache");
		final DtoContainer container = TypeManager.newDtoContainer();
		for(final Class<? extends Readable> type : TypeManager.getEntityClasses())
		{
			cache.stream(type)
					.map(EntityUtils::toDtoSilent)
					.forEach(container::put);
			LOGGER.debug("writing " + cache.count(type) + " instances of " + type.getSimpleName());
		}
		GSON.toJson(container, writer);
		LOGGER.debug("done writing from cache");
	}
	
	public void readFrom(final Reader reader) throws RoseException
	{
		LOGGER.debug("start reading into cache");
		final DtoContainer container = GSON.fromJson(reader, TypeManager.getDtoContainerClass());
		for(final Class<? extends Readable> type : TypeManager.getEntityClasses())
		{
			final Collection<? extends Dto> dtos = container.getAll(type.getSimpleName().toLowerCase());
			for(final Dto dto : dtos)
			{
				final Readable entity = RoseProxy.create(dto, access);
				cache.put(entity);
			}
			LOGGER.debug("reading " + dtos.size() + " instances of " + type.getSimpleName() + " into cache");
		}
		LOGGER.debug("done reading into cache");
	}
	
	public void clear()
	{
		cache.clear();
		LOGGER.debug("cache cleared");
	}

}
