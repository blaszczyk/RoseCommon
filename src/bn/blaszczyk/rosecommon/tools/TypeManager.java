
package bn.blaszczyk.rosecommon.tools;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.*;

import bn.blaszczyk.rose.MetaData;
import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.*;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.parser.RoseParser;

public class TypeManager {
	
	private static final Logger LOGGER = LogManager.getLogger(TypeManager.class);
	
	private final static Map<String, Class<? extends Readable>> entityClasses = new HashMap<>();
	private final static Map<String, Class<? extends Readable>> implClasses = new HashMap<>();
	private final static Map<String, Class<?>> enumClasses = new HashMap<>();
	private final static Map<String, Class<? extends Dto>> dtoClasses = new HashMap<>();
	private final static Map<String, Class<? extends Dto[]>> dtoArrayClasses = new HashMap<>();
	private final static Map<String,EntityModel> entityModels = new HashMap<>();
	private final static Map<String,EnumModel> enumModels = new HashMap<>();

	private static Class<? extends DtoContainer> dtoContainerClass;
	
	private TypeManager()
	{
	}
	
	public static void parseRoseFile(final String resource) throws RoseException
	{
		final RoseParser parser = RoseParser.forResources(resource);
		parser.parse();
		final MetaData metadata = parser.getMetadata();
		for(final EntityModel entityModel : parser.getEntities())
		{
			entityModels.put(entityModel.getSimpleClassName().toLowerCase(), entityModel);
			try
			{
				final String key = entityModel.getSimpleClassName().toLowerCase();
				entityClasses.put(key, Class.forName(entityModel.getClassName()).asSubclass(Readable.class));
				implClasses.put(key, Class.forName(entityModel.getClassName() + "Impl").asSubclass(Readable.class));
				dtoClasses.put(key, Class.forName(getDtoName(entityModel, metadata)).asSubclass(Dto.class));
				dtoArrayClasses.put(key, Class.forName("[L" + getDtoName(entityModel, metadata) + ";").asSubclass(Dto[].class));
				LOGGER.info( "register entity class " + entityModel.getClassName());
			}
			catch (ClassNotFoundException e)
			{
				LOGGER.error("unable to register entity class " + entityModel.getClassName(), e);
				throw new RoseException("error registering classes for " + entityModel.getSimpleClassName(), e);
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
		try
		{
			dtoContainerClass = Class.forName(metadata.getDtopackage() + "." + metadata.getDtocontainername()).asSubclass(DtoContainer.class);
			LOGGER.info( "load dto container class " + metadata.getDtocontainername());
		}
		catch (ClassNotFoundException e)
		{
			LOGGER.error("unable to load dto container class " + metadata.getDtocontainername(), e);
			throw new RoseException("error loading dto container class " + metadata.getDtocontainername(), e);
		}
	}

	public static EntityModel getEntityModel(Class<? extends Readable> type)
	{
		return getEntityModel(keyFor(type));
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

	public static Class<?> getClass(EnumModel enumType)
	{
		return enumClasses.get(enumType.getObjectName());
		
	}
	
	public static Class<? extends Readable> getClass( EntityModel entityModel )
	{
		return entityClasses.get(entityModel.getSimpleClassName().toLowerCase());
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

	public static <T extends Readable> T newInstance(final Class<T> type) throws RoseException
	{
		try
		{
			final Class<? extends Readable> implType = implClasses.get(keyFor(type));
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
		final Class<?> implType = implClasses.get(keyFor(type));
		return implType.asSubclass(type);
	}

	public static Readable newInstance(final Dto dto) throws RoseException
	{
		final Class<? extends Readable> type = getClass(dto);
		return newInstance(type);
	}

	public static Class<? extends Readable> getClass(final Dto dto)
	{
		final String dtoTypeName = dto.getClass().getSimpleName();
		final String typeName = dtoTypeName.substring(0, dtoTypeName.length() - 3);
		final Class<? extends Readable> type = getClass(typeName);
		return type;
	}

	public static Class<? extends Dto> getDtoClass(final String type)
	{
		return dtoClasses.get(type.toLowerCase());
	}
	
	public static Dto newDtoInstance(final String type) throws RoseException
	{
		try
		{
			return getDtoClass(type).newInstance();
		}
		catch (InstantiationException | IllegalAccessException e)
		{
			throw new RoseException("error instatioating dto of type " + type, e);
		}
	}

	public static Class<? extends Dto> getDtoClass(final Class<? extends Readable> type)
	{
		return dtoClasses.get(keyFor(type));
	}

	public static Class<? extends Dto> getDtoClass(final Readable entity)
	{
		return getDtoClass(entity.getClass());
	}
	
	public static Class<? extends Dto[]> getDtoArrayClass(final Class<? extends Readable> type)
	{
		return dtoArrayClasses.get(keyFor(type));
	}

	private static String keyFor(final Class<? extends Readable> type)
	{
		return convertType(type).getSimpleName().toLowerCase();
	}
		
	private static String getDtoName(final EntityModel entity, final MetaData metadata)
	{
		return metadata.getDtopackage() + "." + entity.getSimpleClassName() + "Dto";
	}

	public static EntityModel getEntityModel(final Dto dto)
	{
		return getEntityModel(getClass(dto));
	}

	public static DtoContainer newDtoContainer() throws RoseException
	{
		try
		{
			return dtoContainerClass.newInstance();
		}
		catch (InstantiationException | IllegalAccessException e)
		{
			throw new RoseException("error instatiating new dto container", e);
		}
	}

	public static Class<? extends DtoContainer> getDtoContainerClass()
	{
		return dtoContainerClass;
	}
}
