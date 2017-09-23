package bn.blaszczyk.rosecommon.controller;

import java.util.ArrayList;
import java.util.List;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Timestamped;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.rosecommon.client.RoseClient;
import bn.blaszczyk.rosecommon.dto.RoseDto;
import bn.blaszczyk.rosecommon.proxy.EntityAccess;
import bn.blaszczyk.rosecommon.proxy.LazyList;
import bn.blaszczyk.rosecommon.proxy.RoseProxy;
import bn.blaszczyk.rosecommon.tools.TypeManager;

final class RestController implements ModelController, EntityAccess
{
	
	private final RoseClient client;
	private EntityAccess access = this;
	private boolean usingLazyList = false;
	
	RestController(final String host, final int port)
	{
		this.client = new RoseClient(String.format("http://%s:%d",host,port));
	}
	
	public void setEntityAccess(final EntityAccess access)
	{
		this.access = access;
	}
	
	public void setUsingLazyList(final boolean usingLazyList)
	{
		this.usingLazyList = usingLazyList;
	}
	
	@Override
	public <T extends Readable> List<T> getEntities(final Class<T> type) throws RoseException
	{
		if(usingLazyList)
		{
			final List<Integer> ids = client.getIds(pathFor(type));
			return new LazyList<T>(type, ids, access);
		}
		else
		{
			final List<RoseDto> dtos = client.getDtos(pathFor(type));
			return createProxys(dtos,type);
		}
	}
	
	@Override
	public <T extends Readable> List<Integer> getIds(final Class<T> type) throws RoseException
	{
		return client.getIds(pathFor(type));
	}
	
	@Override
	public <T extends Readable> int getEntityCount(final Class<T> type) throws RoseException
	{
		return client.getCount(pathFor(type));
	}
	
	@Override
	public <T extends Readable> T getEntityById(final Class<T> type, final int id) throws RoseException
	{
		final RoseDto dto = client.getDto(pathFor(type), id);
		return type.cast(RoseProxy.create(dto, access));
	}
	
	@Override
	public <T extends Readable> List<T> getEntitiesByIds(final Class<T> type, final List<Integer> ids) throws RoseException
	{
		if(usingLazyList)
			return new LazyList<T>(type, ids, access);
		else
		{
			final List<RoseDto> dtos = client.getDtos(pathFor(type), ids);
			return createProxys(dtos,type);
		}
	}
	
	@Override
	public <T extends Readable> T createNew(final Class<T> type) throws RoseException
	{
		final T entity = TypeManager.newInstance(type);
		final RoseDto dto = new RoseDto(entity);
		final RoseDto recievedDto = client.postDto(dto);
		entity.setId(recievedDto.getId());
		if(recievedDto.hasTimestamp() && entity instanceof Timestamped)
			((Timestamped)entity).setTimestamp(recievedDto.getTimestamp());
		return entity;
	}
	
	@Override
	public Writable createCopy(final Writable entity) throws RoseException
	{
		final RoseDto dto = new RoseDto(entity);
		client.postDto(dto);
		return entity;
	}
	
	@Override
	public void update(final Writable... entities) throws RoseException
	{
		for(final Writable entity : entities)
			client.putDto(new RoseDto(entity));
	}
	
	@Override
	public void delete(final Writable entity) throws RoseException
	{
		client.deleteByID(pathFor(entity), entity.getId());
	}

	@Override
	public void close()
	{
		client.close();
	}

	@Override
	public <T extends Readable> T getOne(final Class<T> type, final int id) throws RoseException
	{
		return getEntityById(type, id);
	}

	@Override
	public <T extends Readable> List<T> getMany(final Class<T> type, final List<Integer> ids) throws RoseException
	{
		final List<RoseDto> dtos = client.getDtos(pathFor(type), ids);
		return createProxys(dtos,type);
	}

	private <T extends Readable> List<T> createProxys(final List<RoseDto> dtos, final Class<T> type) throws RoseException
	{
		final List<T> entities = new ArrayList<>(dtos.size());
		for(final RoseDto dto : dtos)
			entities.add(type.cast(RoseProxy.create(dto, access)));
		return entities;
	}

	private <T extends Readable> String pathFor(final Class<T> type)
	{
		return type.getSimpleName().toLowerCase();
	}

	private String pathFor(final Writable entity)
	{
		return entity.getEntityName().toLowerCase();
	}
	
}
