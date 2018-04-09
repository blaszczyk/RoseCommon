package bn.blaszczyk.rosecommon.client;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.Dto;
import bn.blaszczyk.rose.model.DtoContainer;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rosecommon.dto.DtoContainerRequest;
import bn.blaszczyk.rosecommon.tools.TypeManager;

public class RoseClient implements Closeable
{

	private static final Gson GSON = new Gson();
	
	private final CommonClient client;

	public RoseClient(final String url)
	{
		client = CommonClient.newInstance(url + "/entity");
	}
	
	public DtoContainer getContainer(final DtoContainerRequest request) throws RoseException
	{
		try
		{
			final String response = client.get("", request.getQueries());
			final DtoContainer container = GSON.fromJson(response, TypeManager.getDtoContainerClass());
			return container;
		}
		catch (Exception e)
		{
			throw RoseException.wrap(e, "Error on GET@/entity");
		}
	}
	
	public Dto getDto(final Class<? extends Readable> type, final int id) throws RoseException
	{
		try
		{
			final String response = client.get("/" + type.getSimpleName().toLowerCase() + "/" + id);
			final Dto dto = GSON.fromJson(response, TypeManager.getDtoClass(type));
			return dto;
		}
		catch (Exception e)
		{
			throw RoseException.wrap(e, "Error on GET@/entity/" + type.getSimpleName().toLowerCase() + "/" + id);
		}
	}
	
	public List<Dto> getDtos(final String path, final Class<? extends Readable> type) throws RoseException
	{
		try
		{
			final String response = client.get("/" + path.toLowerCase());
			final Dto[] dtos = GSON.fromJson(response, TypeManager.getDtoArrayClass(type));
			return Arrays.asList(dtos);
		}
		catch (Exception e)
		{
			throw RoseException.wrap(e, "Error on GET@/entity/" + path);
		}
	}

	public List<Dto> getDtos(final Class<? extends Readable> type, final Map<String, String> query)
	{
		final String path = "/" + type.getSimpleName().toLowerCase();
		try
		{
			final Map<String,Object[]> queries = query.entrySet().stream()
					.collect(Collectors.toMap(Entry::getKey, e -> new Object[] {e.getValue()}));
			final String response = client.get(path, queries);
			final Dto[] dtos = GSON.fromJson(response, TypeManager.getDtoArrayClass(type));
			return Arrays.asList(dtos);
		}
		catch (Exception e)
		{
			throw RoseException.wrap(e, "Error on GET@/entity/" + path + "?" + query);
		}
	}

	public List<Integer> getIds(final String typeName) throws RoseException
	{
		final String path = "/" + typeName.toLowerCase() + "/id";
		try
		{
			final String response = client.get(path);
			final String[] ids = GSON.fromJson(response, String[].class);
			return Arrays.stream(ids)
						.map(String::trim)
						.map(Integer::parseInt)
						.collect(Collectors.toList());
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "Error on GET@/entity/" + path);
		}
	}

	public List<Dto> getDtos(final Class<? extends Readable> type, final List<Integer> entityIds) throws RoseException
	{
		if(entityIds.isEmpty())
			return Collections.emptyList();
		try
		{
			final Map<String, Object[]> query = Collections.singletonMap("id", new Object[] {commaSeparated(entityIds)});
			final String response = client.get("/" + type.getSimpleName().toLowerCase(), query);
			final Dto[] dtos = GSON.fromJson(response, TypeManager.getDtoArrayClass(type));
			return Arrays.asList(dtos);
		}
		catch (Exception e)
		{
			throw RoseException.wrap(e, "Error on GET@/entity/" + type.getSimpleName().toLowerCase());
		}
	}

	public List<Dto> getDtos(final Class<? extends Readable> type, final int[] entityIds) throws RoseException
	{
		return getDtos(type, Arrays.stream(entityIds).mapToObj(Integer::new).collect(Collectors.toList()));
	}

	public int getCount(final String typeName) throws RoseException
	{
		final String path = typeName + "/count";
		try
		{
			final String response = client.get(path);
			return Integer.parseInt(response.trim());
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "Error on GET@/entity/" + path);
		}
	}

	public Dto postDto(final Dto dto) throws RoseException
	{
		final String path = pathForType(dto);
		try
		{
			final String request = GSON.toJson(dto);
			final String response = client.post(path, request);
			return GSON.fromJson(response, dto.getClass());
		}
		catch(Exception e)
		{
			throw RoseException.wrap(e, "error on POST@/entity/" + path);
		}
	}

	public void putDto(final Dto dto) throws RoseException
	{
		final String path = pathFor(dto);
		try
		{
			final String request = GSON.toJson(dto);
			client.put(path, request);
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "error on PUT@/entity/" + path);
		}
	}

	public void deleteByID(final String typeName, final int id) throws RoseException
	{
		final String path = typeName + "/" + id;
		client.delete(path);
	}

	@Override
	public void close()
	{
		client.close();
	}
	
	private String pathForType(final Dto dto) throws RoseException
	{
		final Class<?> type = TypeManager.getClass(dto);
		if(type == null)
			throw new RoseException("missing type in " + dto);
		return "/" + type.getSimpleName().toLowerCase();
	}
	
	private String pathFor(final Dto dto) throws RoseException
	{
		final int id = dto.getId();
		if(id < 0)
			throw new RoseException("invalid id " + id);
		return pathForType(dto) + "/" + id;
	}
	
	private static String commaSeparated(final List<?> list)
	{
		boolean first = true;
		final StringBuilder sb = new StringBuilder();
		for(final Object o : list)
		{
			if(first)
				first = false;
			else
				sb.append(",");
			sb.append(o);
		}
		return sb.toString();
	}

}
