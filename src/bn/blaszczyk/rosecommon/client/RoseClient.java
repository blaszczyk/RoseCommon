package bn.blaszczyk.rosecommon.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.internal.StringMap;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rosecommon.dto.RoseDto;

public class RoseClient {

	private static final Gson GSON = new Gson();
	
	private final CommonClient client;

	public RoseClient(final String url)
	{
		client = CommonClient.newInstance(url + "/entity");
	}
	
	public RoseDto getDto(final String typeName, final int id) throws RoseException
	{
		final List<RoseDto> dtos = getDtos(typeName + "/" + id);
		if(dtos.size() != 1)
			throw new RoseException("error on GET@/entity/" + typeName + "/" + id + "; found:" + dtos);
		return dtos.get(0);
	}
	
	public List<RoseDto> getDtos(final String path) throws RoseException
	{
		try
		{
			final List<RoseDto> dtos = new ArrayList<>();
			final String response = client.get("/" + path.toLowerCase());
			final StringMap<?>[] stringMaps = GSON.fromJson(response, StringMap[].class);
			for(StringMap<?> stringMap : stringMaps)
				dtos.add(new RoseDto(stringMap));
			return dtos;
		}
		catch (Exception e)
		{
			throw RoseException.wrap(e, "Error on GET@/entity/" + path);
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

	public List<RoseDto> getDtos(final String typeName, final List<Integer> entityIds) throws RoseException
	{
		if(entityIds.isEmpty())
			return Collections.emptyList();
		return getDtos(typeName + "/" + commaSeparated(entityIds));
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
			throw RoseException.wrap(e, "Errpr on GET@/entity/" + path);
		}
	}

	public RoseDto postDto(final RoseDto dto) throws RoseException
	{
		final String path = pathForType(dto);
		try
		{
			final String request = GSON.toJson(dto);
			final String response = client.post(path, request);
			final StringMap<?> stringMap = GSON.fromJson(response, StringMap.class);
			return new RoseDto(stringMap);
		}
		catch(Exception e)
		{
			throw RoseException.wrap(e, "error on POST@/entity/" + path);
		}
	}

	public void putDto(final RoseDto dto) throws RoseException
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

	public void close()
	{
		client.close();
	}
	
	private String pathForType(final RoseDto dto) throws RoseException
	{
		final Class<?> type = dto.getType();
		if(type == null)
			throw new RoseException("missing type in " + dto);
		return "/" + type.getSimpleName().toLowerCase();
	}
	
	private String pathFor(final RoseDto dto) throws RoseException
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
