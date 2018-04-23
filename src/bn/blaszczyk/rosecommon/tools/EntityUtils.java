package bn.blaszczyk.rosecommon.tools;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.Dto;
import bn.blaszczyk.rose.model.DtoLinkType;
import bn.blaszczyk.rose.model.EntityField;
import bn.blaszczyk.rose.model.EntityModel;
import bn.blaszczyk.rose.model.EnumField;
import bn.blaszczyk.rose.model.Field;
import bn.blaszczyk.rose.model.Identifyable;
import bn.blaszczyk.rose.model.PrimitiveField;
import bn.blaszczyk.rose.model.PrimitiveType;
import bn.blaszczyk.rose.model.Readable;

public final class EntityUtils
{
	
	public static Object toEntityValue(final Field field, final Object dtoValue) throws RoseException
	{
		if(dtoValue == null || dtoValue.equals("null"))
			return null;
		try
		{
			if(field instanceof EnumField)
				return dtoValue;
			else if(field instanceof PrimitiveField)
			{
				final PrimitiveField pField = (PrimitiveField) field;
				switch(pField.getType())
				{
				case DATE:
					return new Date((long)dtoValue);
				case NUMERIC:
					final BigDecimal numValue = new BigDecimal((String)dtoValue);
					checkNumeric(numValue,pField);
					return numValue;
				case CHAR:
				case VARCHAR:
					checkString((String)dtoValue,pField);
				case INT:
				case BOOLEAN:
					return dtoValue;
				}
			}
		}
		catch(Exception e)
		{
			throw RoseException.wrap(e,"Error parsing primitive '" + dtoValue + "' for " + field.getName());
		}
		return null;
	}
	
	public static Object toDtoValue(final Field field, final Object value) throws RoseException
	{
		try
		{
			if(field instanceof EnumField)
				return value;
			else if(field instanceof PrimitiveField)
			{
				final PrimitiveField pField = (PrimitiveField) field;
				switch(pField.getType())
				{
				case DATE:
					if(value == null)
						return Long.valueOf(-1);
					return ((Date)value).getTime();
				case NUMERIC:
					final BigDecimal numValue = (BigDecimal)value;
					checkNumeric(numValue,pField);
					return numValue.toPlainString();
				case CHAR:
				case VARCHAR:
					checkString((String)value,pField);
					return value;
				case INT:
					if(value == null)
						return 0;
					return value;
				case BOOLEAN:
					if(value == null)
						return false;
					return value;
				}
			}
		}
		catch(Exception e)
		{
			throw RoseException.wrap(e,"Error parsing primitive '" + value + "' for " + field.getName());
		}
		return null;
	}
	
	public static Dto toDto(final Readable entity, final DtoLinkType oneType, final DtoLinkType manyType) throws RoseException
	{
		if(entity == null)
			return null;
		final EntityModel entityModel = TypeManager.getEntityModel(entity);
		final Dto dto = TypeManager.newDtoInstance(entity.getEntityName());
		dto.setId(entity.getId());
		for(int i = 0; i < entityModel.getFields().size(); i++)
		{
			final Field field = entityModel.getFields().get(i);
			final Object value = entity.getFieldValue(i);
			final String fieldName = field.getName();
			dto.setFieldValue(fieldName, toDtoValue(field, value));
		}
		for(int i = 0; i < entity.getEntityCount(); i++)
			if(entity.getRelationType(i).isSecondMany())
			{
				if(manyType.equals(DtoLinkType.ID))
				{
					final Integer[] ids = entity.getEntityValueMany(i).stream()
						.map(Identifyable::getId)
						.toArray(Integer[]::new);
					dto.setEntityIds(entity.getEntityName(i), ids);
				}
				else if(manyType.equals(DtoLinkType.COUNT))
					dto.setEntityCount(entity.getEntityName(i), entity.getEntityValueMany(i).size());
			}
			else
			{
				final Readable value = entity.getEntityValueOne(i);
				if(oneType.equals(DtoLinkType.ID))
				{
					final int id = value == null ? -1 : value.getId();
					dto.setEntityId(entity.getEntityName(i), id);
				}
				else if(oneType.equals(DtoLinkType.ENTITY) || 
						( oneType.equals(DtoLinkType.ENTITY_CASCADE) && entity.getRelationType(i).isFirstMany() ) )
				{
					final Dto subDto = toDto(value, DtoLinkType.ENTITY_CASCADE, DtoLinkType.NONE);
					dto.setEntity(entity.getEntityName(i), subDto);
				}
			}
		return dto;
	}
	
	public static Dto toDto(final Readable entity) throws RoseException
	{
		return toDto(entity, DtoLinkType.ID, DtoLinkType.ID);
	}
	
	public static String toStringSimple(Identifyable entity)
	{
		return String.format("%s id=%d", entity.getClass().getSimpleName(), entity.getId());
	}
	
	public static String toStringPrimitives(Readable entity)
	{
		if(entity == null)
			return "";
		final StringBuilder builder = new StringBuilder();
		builder.append(entity.getEntityName())
				.append("(").append(entity.getId()).append("){");
		final int fieldCount = entity.getFieldCount();
		for(int i = 0; i < entity.getFieldCount(); i++)
			builder.append(" ").append(entity.getFieldName(i)).append("=").append(entity.getFieldValue(i))
					.append( ( i < fieldCount - 1 ) ? "," : " }");
		return builder.toString();
	}
	
	public static String toStringFull(Readable entity)
	{
		if(entity == null)
			return "";
		final StringBuilder builder = new StringBuilder(toStringPrimitives(entity));
		final int entityCount = entity.getEntityCount();	
		for(int i = 0; i < entityCount; i++)
		{
			builder.append("\r\n\t").append(entity.getEntityName(i)).append("=");
			if(entity.getRelationType(i).isSecondMany())
			{
				builder.append(entity.getEntityClass(i).getSimpleName()).append("(");
				Set<? extends Identifyable> entities = entity.getEntityValueMany(i);
				boolean first = true;
				for(Identifyable id : entities)
				{
					if(first)
						first = false;
					else
						builder.append(",");
					builder.append(id.getId());
				}
				builder.append(")");
			}
			else
				builder.append(toStringPrimitives(entity.getEntityValueOne(i)));
		}
		return builder.toString();
	}
	
	public static boolean equals(Readable i1, Readable i2)
	{
		if(i1 == i2)
			return true;
		if(i1 == null)
			return i2 == null;
		if(i2 == null)
			return false;
		if(! TypeManager.convertType(i1.getClass()).equals(TypeManager.convertType(i2.getClass())))
			return false;
		return i1.getId() == i2.getId();
	}

	private static void checkNumeric(final BigDecimal value, final PrimitiveField field) throws RoseException
	{
		final int precision = field.getLength1();
		final int scale = field.getLength2();
		if(value.scale() > scale)
			throw new RoseException("numeric value " + field.getName() + "=" + value + " has wrong scale; max scale=" + scale);
		if(value.precision() > precision)
			throw new RoseException("numeric value " + field.getName() + "=" + value + " has wrong precision; max precision=" + precision);
	}

	private static void checkString(final String value, final PrimitiveField field) throws RoseException
	{
		if(value == null)
			return;
		final int length = field.getLength1();
		if(value.length() > length)
			throw new RoseException("string value " + field.getName() + "='" + value + "' too long; max_length=" + length);
	}

	public static String toString(final Dto dto)
	{
		final EntityModel entityModel = TypeManager.getEntityModel(dto);
		String toString = entityModel.getToString();
		for(final Field field : entityModel.getFields())
		{
			final String stringValue = toString(field,dto);
			toString = toString.replaceAll("%" + field.getName().toLowerCase(), stringValue);
		}
		for(final EntityField field : entityModel.getEntityFields())
			toString = toString.replaceAll("%" + field.getName(), "");
		return toString.trim();
	}

	private static String toString(final Field field, final Dto dto)
	{
		final Object value = dto.getFieldValue(field.getName());
		if(value == null)
			return "";
		if(field instanceof PrimitiveField && ((PrimitiveField)field).getType().equals(PrimitiveType.DATE))
			return new SimpleDateFormat("dd.MM.yyyy").format(new Date((Long)value));
		return String.valueOf(value);
	}

}
