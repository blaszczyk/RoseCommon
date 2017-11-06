package bn.blaszczyk.rosecommon.dto;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import bn.blaszczyk.rose.model.Dto;
import bn.blaszczyk.rose.model.EntityField;
import bn.blaszczyk.rose.model.EntityModel;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rosecommon.tools.TypeManager;

public class DtoContainerRequest
{
	private final Map<String, Set<Integer>> allIds;
	
	public DtoContainerRequest()
	{
		allIds = new HashMap<>(TypeManager.getEntityCount());
		for(final Class<? extends Readable> type : TypeManager.getEntityClasses())
			allIds.put(type.getSimpleName().toLowerCase(), new TreeSet<>());
	}
	
	public void request(final String type, final int id)
	{
		allIds.get(type.toLowerCase()).add(id);
	}
	
	public void requestOwners(final Dto dto)
	{
		final EntityModel entityModel = TypeManager.getEntityModel(dto);
		for(final EntityField entityField : entityModel.getEntityFields())
			if(!entityField.getType().isSecondMany())
				request(entityField.getEntityName(), dto.getEntityId(entityField.getName()));
	}

	public Map<String, Object[]> getQueries()
	{
		final Map<String, Object[]> queries = new HashMap<>();
		for(Map.Entry<String, Set<Integer>> ids : allIds.entrySet())
			if(!ids.getValue().isEmpty())
			{
				final String idsString = ids.getValue()
						.stream()
						.map(String::valueOf)
						.collect(Collectors.joining(","));
				queries.put(ids.getKey(), new Object[] {idsString});
			}
		return queries;
	}

}
