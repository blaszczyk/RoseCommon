package bn.blaszczyk.rosecommon.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.*;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.EntityModel;
import bn.blaszczyk.rose.model.EntityField;
import bn.blaszczyk.rose.model.Field;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Representable;
import bn.blaszczyk.rose.model.Timestamped;
import bn.blaszczyk.rosecommon.dto.RoseDto;
import bn.blaszczyk.rosecommon.tools.EntityUtils;
import bn.blaszczyk.rosecommon.tools.TypeManager;

public class RoseProxy implements InvocationHandler {
	
	private static final Logger LOGGER = LogManager.getLogger(RoseProxy.class);
	
	private static Boolean fetchLock = false;
	
	public static Representable create(final RoseDto dto, final EntityAccess access) throws RoseException
	{
		final RoseProxy handler = new RoseProxy(dto, access);
		final ClassLoader loader = dto.getType().getClassLoader();
		final Class<?>[] interfaces = new Class<?>[]{dto.getType(),Comparable.class};
		final Representable proxy = (Representable) Proxy.newProxyInstance(loader, interfaces, handler);
		return proxy;
	}

	private final EntityAccess access;

	private final EntityModel entityModel;
	private final Class<? extends Representable> type;
	
	private final Representable entity;
	
	private final boolean[] fetched;
	private boolean fetchedAll = false;
	private final List<List<Integer>> allIds;
	
	private RoseProxy(final RoseDto dto, final EntityAccess access) throws RoseException
	{
		this.access = access;
		entity = (Representable) TypeManager.newInstance(dto.getType());
		entityModel = TypeManager.getEntityModel(entity);
		type = entity.getClass();
		
		fetched = new boolean[entity.getEntityCount()];
		Arrays.fill(fetched, false);
		allIds = new ArrayList<>(entity.getEntityCount());
		
		entity.setId(dto.getId());
		if(dto.hasTimestamp() && entity instanceof Timestamped)
			((Timestamped)entity).setTimestamp(dto.getTimestamp());
		
		for(int i = 0; i < entity.getFieldCount(); i++)
			setPrimitive(i, dto.getFieldValue(entity.getFieldName(i)));
		for(int i = 0; i < entity.getEntityCount(); i++)
			setEntityIds(i, dto);
		checkAllFetched();
	}
	
	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
	{
		if(!fetchedAll)
		{
			final int index = getFetchIndex(method, args);
			if(index >= 0)
				fetchIfRequired(index, (Representable) proxy);
		}
		if(needsRepresentation(method))
			return invokeRepresentorMethod(method, args, proxy);
		return method.invoke(entity, args);
	}

	private void setPrimitive(final int index, final String dtoValue) throws RoseException
	{
		final Field field = entityModel.getFields().get(index);
		final Object value = EntityUtils.getPrimitiveValue(field, dtoValue);
		entity.setField(index, value);
	}
	
	private void setEntityIds(final int index, final RoseDto dto)
	{
		final List<Integer> ids;
		final String fieldName = entityModel.getEntityFields().get(index).getName();
		if(entity.getRelationType(index).isSecondMany())
		{
			ids = dto.getEntityIds(fieldName);
			if(ids.isEmpty())
				fetched[index] = true;
		}
		else
		{
			if(dto.getEntityId(fieldName) < 0)
				fetched[index] = true;
			ids = Collections.singletonList(dto.getEntityId(fieldName));
		}
		allIds.add(index, ids);
	}
	
	private int getFetchIndex(final Method method, final Object[] args)
	{
		final String methodName = method.getName();
		if(! methodName.startsWith("get") || methodName.startsWith("set"))
			return -1;
		if(methodName.equals("getEntityValueOne") || methodName.equals("getEntityValueMany"))
			if(args.length == 1 && args[0] instanceof Integer)
				return (Integer)args[0];
		if(methodName.equals("setEntity"))
			if(args.length > 1  && args[0] instanceof Integer)
				return (Integer)args[0];
		final String choppedName = methodName.substring(3);
		for(int i = 0; i < entity.getEntityCount(); i++)
		{
			final EntityField field = entityModel.getEntityFields().get(i);
			if( choppedName.equals( field.getCapitalName() ) )
				return i;
		}
		return -1;
	}
	
	private void fetchIfRequired(final int index, final Representable proxy)
	{
		if(fetched[index])
			return;
		synchronized (fetchLock)
		{
			if(fetchLock)
				return;
			fetchLock = true;
			fetch(index,proxy);
			fetchLock = false;
		}
		checkAllFetched();
	}

	private void checkAllFetched()
	{
		fetchedAll = true;
		for(final boolean fetched : fetched)
			if(!fetched)
				fetchedAll = false;
	}

	private void fetch(final int index, final Representable proxy)
	{
		final EntityField field = entityModel.getEntityFields().get(index);
		final Class<? extends Readable> type = TypeManager.getClass(field.getEntityModel());
		final List<Integer> ids = allIds.get(index);
		try
		{
			fetched[index] = true;
			if(field.getType().isSecondMany())
				access.getMany(type, ids)
						.forEach( e -> entity.addEntity(index, (Writable) e, proxy));
			else
			{
				final int id = ids.get(0);
				if(id >= 0)
					entity.setEntity(index, (Writable) access.getOne(type, id), proxy);
			}
		}
		catch (RoseException e) 
		{
			LOGGER.error(String.format("Unable to fetch %s %s for %s", type.getSimpleName(),ids.toString(), EntityUtils.toStringSimple(entity) ), e);
			fetched[index]=false;
		}
	}

	private boolean needsRepresentation(final Method method)
	{
		final Class<?>[] parameterTypes = method.getParameterTypes();
		if(parameterTypes.length != 2)
			return false;
		if( !( parameterTypes[0].equals(int.class) && parameterTypes[1].equals(Writable.class) ) )
			return false;
		final String name = method.getName();
		return name.equals("setEntity") || name.equals("addEntity") || name.equals("removeEntity");
	}

	private Object invokeRepresentorMethod(final Method method, final Object[] args, final Object proxy) throws Throwable
	{
		try
		{
			final Method representorMethod = type.getMethod(method.getName(), int.class, Writable.class, Representable.class);
			return representorMethod.invoke(entity, args[0], args[1], proxy);
		}
		catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			LOGGER.error("Error invoking representor method for " + method.getName() + " of " + type.getSimpleName());
			return method.invoke(entity, args);
		}
	}
	
}
