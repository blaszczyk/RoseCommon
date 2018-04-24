package bn.blaszczyk.rosecommon.dto;

import java.util.ArrayList;
import java.util.List;

import bn.blaszczyk.rose.model.EntityModel;

public class EntityModelDto
{
	private final List<FieldDto> fields;
	private final String name;
	private final String toString;
	
	public EntityModelDto(final EntityModel entityModel)
	{
		name = entityModel.getSimpleClassName();
		fields = new ArrayList<>(entityModel.getFields().size() + entityModel.getEntityFields().size());
		toString = entityModel.getToString();
		entityModel.getFields().forEach(f -> fields.add(new FieldDto(f)));
		entityModel.getEntityFields().forEach(e -> fields.add(new FieldDto(e)));
	}

	public final List<FieldDto> getFields()
	{
		return fields;
	}

	public final String getName()
	{
		return name;
	}

	public final String getToString()
	{
		return toString;
	}
	
	

}
