package bn.blaszczyk.rosecommon.controller;

import java.util.ArrayList;
import java.util.List;

import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Timestamped;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.rosecommon.RoseException;
import bn.blaszczyk.rosecommon.client.RoseClient;
import bn.blaszczyk.rosecommon.dto.RoseDto;
import bn.blaszczyk.rosecommon.proxy.EntityAccess;
import bn.blaszczyk.rosecommon.proxy.LazyList;
import bn.blaszczyk.rosecommon.proxy.RoseProxy;
import bn.blaszczyk.rosecommon.tools.TypeManager;

public class RestController implements ModelController, EntityAccess {
	
	private final RoseClient client;
	private EntityAccess access = this;
	private boolean usingLazyList = false;
	
	public RestController(final RoseClient client)
	{
		this.client = client;
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
	public List<? extends Readable> getEntities(final Class<? extends Readable> type) throws RoseException
	{
		if(usingLazyList)
		{
			final List<Integer> ids = client.getIds(type.getSimpleName().toLowerCase());
			return new LazyList(type, ids, access);
		}
		else
		{
			final List<RoseDto> dtos = client.getDtos(type.getSimpleName());
			return createProxys(dtos);
		}
	}
	
	@Override
	public List<Integer> getIds(final Class<? extends Readable> type) throws RoseException
	{
		return client.getIds(type.getSimpleName().toLowerCase());
	}
	
	@Override
	public int getEntityCount(final Class<? extends Readable> type) throws RoseException
	{
		return client.getCount(type.getSimpleName().toLowerCase());
	}
	
	@Override
	public Readable getEntityById(final Class<? extends Readable> type, final int id) throws RoseException
	{
		return getOne(type, id);
	}
	
	@Override
	public List<? extends Readable> getEntitiesByIds(final Class<? extends Readable> type, final List<Integer> ids) throws RoseException
	{
		if(usingLazyList)
			return new LazyList(type, ids, access);
		else
			return getMany(type, ids);
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
		client.deleteByID(entity.getEntityName().toLowerCase(), entity.getId());
	}

	@Override
	public void close()
	{
		client.close();
	}

	@Override
	public Writable getOne(final Class<? extends Readable> type, final int id) throws RoseException
	{
		final RoseDto dto = client.getDto(type.getSimpleName().toLowerCase(), id);
		return RoseProxy.create(dto, access);
	}

	@Override
	public List<Writable> getMany(Class<? extends Readable> type, final List<Integer> ids) throws RoseException
	{
		final List<RoseDto> dtos = client.getDtos(type.getSimpleName().toLowerCase(), ids);
		return createProxys(dtos);
	}

	private List<Writable> createProxys(final List<RoseDto> dtos) throws RoseException
	{
		final List<Writable> entities = new ArrayList<>(dtos.size());
		for(final RoseDto dto : dtos)
			entities.add(RoseProxy.create(dto, access));
		return entities;
	}
	
}
