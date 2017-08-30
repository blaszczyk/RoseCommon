
package bn.blaszczyk.rosecommon.tools;

import java.io.InputStream;
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
	private final static Map<String,Entity> entites = new HashMap<>();
	private final static Map<String,EnumType> enums = new HashMap<>();
	
	private TypeManager()
	{
	}
	
	public static void parseRoseFile(final InputStream stream) throws RoseException
	{
		
		final RoseParser parser = new RoseParser(stream);
		parser.parse();
		for(final Entity e : parser.getEntities())
		{
			entites.put(e.getSimpleClassName(), e);
			try
			{
				entityClasses.put(e.getSimpleClassName().toLowerCase(), Class.forName(e.getClassName()).asSubclass(Readable.class));
				implClasses.put(e.getSimpleClassName().toLowerCase(), Class.forName(e.getClassName() + "Impl").asSubclass(Readable.class));
				LOGGER.info( "load entity class " + e.getClassName());
			}
			catch (ClassNotFoundException e1)
			{
				LOGGER.error("unable to load entity class " + e.getClassName(), e1);
			}
		}
		for(EnumType e : parser.getEnums())
		{
			enums.put(e.getSimpleClassName(), e);
			try
			{
				Class<?> enumClass = Class.forName(e.getClassName());
				enumClasses.put(e.getSimpleClassName().toLowerCase(), enumClass );
				LOGGER.info( "load enum class " + e.getClassName());
			}
			catch (ClassNotFoundException e1)
			{
				LOGGER.error("unable to load enum class " + e.getClassName(), e1);
			}
		}
	}
	
	public static Entity getEntity(Class<? extends Readable> type)
	{
		return getEntity(convertType(type).getSimpleName());
	}
	
	public static Entity getEntity(String name)
	{
		return entites.get(name);
	}
	
	public static Entity getEntity( Readable entity )
	{
		if(entity == null)
			return null;
		return getEntity( entity.getClass() );
	}
	
	public static EnumType getEnum( Class<?> type )
	{
		return enums.get(type.getSimpleName());
	}
	
	public static EnumType getEnum( Enum<?> enumOption )
	{
		if(enumOption == null)
			return null;
		return getEnum(enumOption.getClass());
	}
	
	public static Class<? extends Readable> getClass( Entity entity )
	{
		return entityClasses.get(entity.getSimpleClassName().toLowerCase());
	}
	
	public static Class<?> getClass( EnumType enumType )
	{
		return enumClasses.get(enumType.getSimpleClassName().toLowerCase());
	}
	
	public static Collection<Class<? extends Readable>> getEntityClasses()
	{
		return entityClasses.values();
	}

	public static Collection<Entity> getEntites()
	{
		return entites.values();
	}

	public static int getEntityCount()
	{
		return entites.size();
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
