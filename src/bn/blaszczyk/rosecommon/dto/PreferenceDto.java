package bn.blaszczyk.rosecommon.dto;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.internal.StringMap;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rosecommon.tools.Preference;

public class PreferenceDto extends LinkedHashMap<String, String>{

	private static final long serialVersionUID = 6823913867295328671L;
	
	public static final DecimalFormat BIG_DEC_FORMAT = (DecimalFormat) NumberFormat.getNumberInstance();
	static{
		BIG_DEC_FORMAT.setParseBigDecimal(true);
	}
	
	public PreferenceDto(final StringMap<?> stringMap) throws RoseException
	{
		for(final Map.Entry<String, ?> entry : stringMap.entrySet())
		{
			put(entry.getKey(), String.valueOf(entry.getValue()));
		}
	}

	public PreferenceDto(final Map<String, String[]> parameterMap) throws RoseException
	{
		for(final Map.Entry<String, String[]> entry : parameterMap.entrySet())
		{
			checkArrayEntry(entry);
			put(entry.getKey(), entry.getValue()[0]);
		}
	}
	
	public PreferenceDto()
	{
	}
	
	public void put(final Preference preference, final Object value)
	{
		if(!preference.getType().getType().isInstance(value))
			throw new IllegalArgumentException("value for " + preference.getKey() + " is not of type " + preference.getType());
		put(preference.getKey(), String.valueOf(value));
	}
	
	public Object get(final Preference preference)
	{
		switch (preference.getType())
		{
		case STRING:
			return getString(preference);
		case INT:
			return getInt(preference);
		case BOOLEAN:
			return getBoolean(preference);
		case NUMERIC:
			return getNumeric(preference);
		default:
			return null;
		}
	}

	public Integer getInt(final Preference preference)
	{
		if(!preference.getType().equals(Preference.Type.INT))
			throw new IllegalArgumentException("preference " + preference.getKey() + " is not an INT preference.");
		final String value = get(preference.getKey());
		if(value == null)
			return null;
		return Integer.parseInt(value);
	}

	public String getString(final Preference preference)
	{
		if(!preference.getType().equals(Preference.Type.STRING))
			throw new IllegalArgumentException("preference " + preference.getKey() + " is not a STRING preference.");
		return get(preference.getKey());
	}

	public Boolean getBoolean(final Preference preference)
	{
		if(!preference.getType().equals(Preference.Type.BOOLEAN))
			throw new IllegalArgumentException("preference " + preference.getKey() + " is not a BOOLEAN preference.");
		final String value = get(preference.getKey());
		if(value == null)
			return null;
		return Boolean.parseBoolean(value);
	}

	public BigDecimal getNumeric(final Preference preference)
	{
		if(!preference.getType().equals(Preference.Type.NUMERIC))
			throw new IllegalArgumentException("preference " + preference.getKey() + " is not a NUMERIC preference.");
		final String value = get(preference.getKey());
		if(value == null)
			return null;
		try
		{
			return (BigDecimal) BIG_DEC_FORMAT.parse(value);
		}
		catch (ParseException e)
		{
			return null;
		}
	}
	
	public boolean containsPreference( final Preference preference)
	{
		return containsKey(preference.getKey());
	}
	
	private void checkArrayEntry(final Map.Entry<String, String[]> entry) throws RoseException
	{
		final String[] value = entry.getValue();
		if(value.length != 1)
			throw new RoseException("array value " + entry.getKey() + "=" + value + " has no unique element");
	}
	
}
