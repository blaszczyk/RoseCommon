package bn.blaszczyk.rosecommon.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.Dto;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Timestamped;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.rosecommon.client.RoseClient;
import bn.blaszczyk.rosecommon.proxy.EntityAccess;
import bn.blaszczyk.rosecommon.proxy.EntityAccessAdapter;
import bn.blaszczyk.rosecommon.proxy.RoseProxy;
import bn.blaszczyk.rosecommon.tools.EntityUtils;
import bn.blaszczyk.rosecommon.tools.TypeManager;

final class RestController implements ModelController
{
	
	private final RoseClient client;
	private EntityAccess access;
	
	RestController(final String host, final int port)
	{
		this.client = new RoseClient(String.format("http://%s:%d",host,port));
		access = new EntityAccessAdapter(this);
	}
	
	public void setEntityAccess(final EntityAccess access)
	{
		this.access = access;
	}
	
	@Override
	public <T extends Readable> List<T> getEntities(final Class<T> type) throws RoseException
	{
		final List<Dto> dtos = client.getDtos(pathFor(type), type);
		return createProxys(dtos,type);
	}
	
	@Override
	public <T extends Readable> List<T> getEntities(final Class<T> type, final Map<String, String> query) throws RoseException
	{
		final List<Dto> dtos = client.getDtos(type, query);
		return createProxys(dtos, type);
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
		final Dto dto = client.getDto(type, id);
		return type.cast(RoseProxy.create(dto, access));
	}
	
	@Override
	public <T extends Readable> List<T> getEntitiesByIds(final Class<T> type, final List<Integer> ids) throws RoseException
	{
		final List<Dto> dtos = client.getDtos(type, ids);
		return createProxys(dtos,type);
	}
	
	@Override
	public <T extends Writable> T createNew(final Class<T> type) throws RoseException
	{
		final T entity = TypeManager.newInstance(type);
		return createNew(entity);
	}
	
	@Override
	public <T extends Writable> T createNew(final T entity) throws RoseException
	{
		final Dto dto = EntityUtils.toDto(entity);
		final Dto recievedDto = client.postDto(dto);
		entity.setId(recievedDto.getId());
		if(entity instanceof Timestamped && recievedDto instanceof Timestamped)
			((Timestamped)entity).setTimestamp(((Timestamped)recievedDto).getTimestamp());
		return entity;
	}
	
	@Override
	public Writable createCopy(final Writable entity) throws RoseException
	{
		final Dto dto = EntityUtils.toDto(entity);
		client.postDto(dto);
		return entity;
	}
	
	@Override
	public void update(final Writable... entities) throws RoseException
	{
		for(final Writable entity : entities)
			client.putDto(EntityUtils.toDto(entity));
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

	private <T extends Readable> List<T> createProxys(final List<Dto> dtos, final Class<T> type) throws RoseException
	{
		final List<T> entities = new ArrayList<>(dtos.size());
		for(final Dto dto : dtos)
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
