
package bn.blaszczyk.rosecommon.tools;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.*;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.*;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.parser.RoseParser;

public class TypeManager {
	
	private static final Logger LOGGER = LogManager.getLogger(TypeManager.class);
	
	private final static Map<String, Class<? extends Readable>> entityClasses = new HashMap<>();
	private final static Map<String, Class<? extends Readable>> implClasses = new HashMap<>();
	private final static Map<String, Class<?>> enumClasses = new HashMap<>();
	private final static Map<String,EntityModel> entityModels = new HashMap<>();
	private final static Map<String,EnumModel> enumModels = new HashMap<>();
	
	private TypeManager()
	{
	}
	
	public static void parseRoseFile(final String resource) throws RoseException
	{
		final RoseParser parser = RoseParser.forResources(resource);
		parser.parse();
		for(final EntityModel entity : parser.getEntities())
		{
			entityModels.put(entity.getSimpleClassName(), entity);
			try
			{
				entityClasses.put(entity.getSimpleClassName().toLowerCase(), Class.forName(entity.getClassName()).asSubclass(Readable.class));
				implClasses.put(entity.getSimpleClassName().toLowerCase(), Class.forName(entity.getClassName() + "Impl").asSubclass(Readable.class));
				LOGGER.info( "load entity class " + entity.getClassName());
			}
			catch (ClassNotFoundException e)
			{
				LOGGER.error("unable to load entity class " + entity.getClassName(), e);
				throw new RoseException("error loading classes for " + entity.getSimpleClassName(), e);
			}
		}
		for(EnumModel enumModel : parser.getEnums())
		{
			enumModels.put(enumModel.getSimpleClassName(), enumModel);
			try
			{
				Class<?> enumClass = Class.forName(enumModel.getClassName());
				enumClasses.put(enumModel.getSimpleClassName().toLowerCase(), enumClass );
				LOGGER.info( "load enum class " + enumModel.getClassName());
			}
			catch (ClassNotFoundException e)
			{
				LOGGER.error("unable to load enum class " + enumModel.getClassName(), e);
				throw new RoseException("error loading class for " + enumModel.getSimpleClassName(), e);
			}
		}
	}
	
	public static EntityModel getEntityModel(Class<? extends Readable> type)
	{
		return getEntityModel(convertType(type).getSimpleName());
	}
	
	public static EntityModel getEntityModel(String name)
	{
		return entityModels.get(name);
	}
	
	public static EntityModel getEntityModel( Readable entity )
	{
		if(entity == null)
			return null;
		return getEntityModel( entity.getClass() );
	}
	
	public static EnumModel getEnumModel( Class<?> type )
	{
		return enumModels.get(type.getSimpleName());
	}
	
	public static EnumModel getEnumModel( Enum<?> enumOption )
	{
		if(enumOption == null)
			return null;
		return getEnumModel(enumOption.getClass());
	}
	
	public static Class<? extends Readable> getClass( EntityModel entityModel )
	{
		return entityClasses.get(entityModel.getSimpleClassName().toLowerCase());
	}
	
	public static Class<?> getClass( EnumModel enumModel )
	{
		return enumClasses.get(enumModel.getSimpleClassName().toLowerCase());
	}
	
	public static Collection<Class<? extends Readable>> getEntityClasses()
	{
		return entityClasses.values();
	}

	public static Collection<EntityModel> getEntityModels()
	{
		return entityModels.values();
	}

	public static int getEntityCount()
	{
		return entityModels.size();
	}

	public static Class<? extends Readable> getClass(String entityName)
	{
		return entityClasses.get(entityName.toLowerCase());
	}

	public static Class<? extends Readable> convertType(Class<? extends Readable> type)
	{
		for(Class<? extends Readable> t : entityClasses.values())
			if(t.isAssignableFrom(type))
				return t;
		LOGGER.error("unknown type: " + type.getName());
		return type;
	}

	public static Class<? extends Readable> getClass(Readable entity)
	{
		return convertType(entity.getClass());
	}

	public static <T> T newInstance(final Class<T> type) throws RoseException
	{
		try
		{
			final Class<? extends Readable> implType = implClasses.get(type.getSimpleName().toLowerCase());
			final Readable instance = implType.newInstance();
			return type.cast(instance);
		}
		catch (InstantiationException | IllegalAccessException e)
		{
			throw new RoseException("unable to create new " + type.getName(), e);
		}
	}

	public static <T extends Readable> Class<? extends T> getImplClass(final Class<T> type)
	{
		final Class<?> implType = implClasses.get(type.getSimpleName().toLowerCase());
		return implType.asSubclass(type);
	}
}
