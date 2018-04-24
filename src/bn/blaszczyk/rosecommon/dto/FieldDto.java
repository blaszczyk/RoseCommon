package bn.blaszczyk.rosecommon.dto;

import bn.blaszczyk.rose.model.EntityField;
import bn.blaszczyk.rose.model.EnumField;
import bn.blaszczyk.rose.model.Field;
import bn.blaszczyk.rose.model.PrimitiveField;
import bn.blaszczyk.rose.model.PrimitiveType;
import bn.blaszczyk.rose.model.RelationType;

public class FieldDto
{

	public enum FieldType
	{
		STRING,
		INT,
		BOOLEAN,
		DATE,
		NUMERIC,
		ENUM,
		ONETOONE,
		ONETOMANY,
		MANYTOONE,
		MANYTOMANY;
	}
	
	private final String name;
	private final String entity;
	private final FieldType fieldType;
	private final String counterName;

	public FieldDto(final Field field)
	{
		if(field == null)
			throw new NullPointerException("No dto for null field!");
		name = field.getName().toLowerCase();
		if(field instanceof PrimitiveField)
		{
			final PrimitiveField pField = (PrimitiveField) field;
			fieldType = convert(pField.getType());
			entity = null;
			counterName = null;
		}
		else if(field instanceof EnumField)
		{
			final EnumField eField = (EnumField) field;
			fieldType = FieldType.ENUM;
			entity = eField.getEnumName();
			counterName = null;
		}
		else if(field instanceof EntityField)
		{
			final EntityField eField = (EntityField) field;
			fieldType = convert(eField.getType());
			entity = eField.getEntityName();
			counterName = eField.getCounterName();
		}
		else
		{
			throw new IllegalArgumentException("Unknown field type:" + field.getClass().getName());
		}
	}
	
	public final String getName()
	{
		return name;
	}

	public final String getEntity()
	{
		return entity;
	}

	public final FieldType getFieldType()
	{
		return fieldType;
	}

	public final String getCounterName()
	{
		return counterName;
	}

	private static FieldType convert(final PrimitiveType type)
	{
		switch (type)
		{
		case BOOLEAN:
			return FieldType.BOOLEAN;
		case CHAR:
		case VARCHAR:
			return FieldType.STRING;
		case DATE:
			return FieldType.DATE;
		case INT:
			return FieldType.INT;
		case NUMERIC:
			return FieldType.NUMERIC;
		}
		return null;
	}
	
	private static FieldType convert(final RelationType type)
	{
		return FieldType.valueOf(type.name());
	}
}
