package bn.blaszczyk.rosecommon.tools;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.*;

import bn.blaszczyk.rose.model.Entity;
import bn.blaszczyk.rose.model.Readable;

public class Preferences {
	
	private static final Logger LOGGER = LogManager.getLogger(Preferences.class);
	
	private static final Map<Preference, Object> PREFS_CACHE = new HashMap<>();
	
	private final static DecimalFormat DECIMAL_FORMAT =  (DecimalFormat) NumberFormat.getNumberInstance();
	static {
		DECIMAL_FORMAT.setParseBigDecimal(true);
	}

	private static java.util.prefs.Preferences preferences;
	
	private Preferences()
	{
	}

	public static void setMainClass(final Class<?> type)
	{
		preferences = java.util.prefs.Preferences.userNodeForPackage(type);
	}
	
	public static void cacheArguments(final String[] args, final Preference[]... preferences)
	{
		final List<Preference> prefs = Arrays
				.stream(preferences)
				.flatMap(Arrays::stream)
				.filter(Preference::needsCaching)
				.collect(Collectors.toList());
		boolean hasPreference = false;
		Preference preference = null;
		for(final String arg : args)
		{
			if(hasPreference)
			{
				final Object value = parseValue(arg, preference);
				PREFS_CACHE.put(preference, value);
				hasPreference = false;
			}
			else if(arg.startsWith("-"))
			{
				final String key = arg.substring(1);
				final Optional<Preference> optPreference = prefs
						.stream()
						.filter(p -> key.equals(p.getKey()))
						.findFirst();
				if(optPreference.isPresent())
				{
					hasPreference = true;
					preference = optPreference.get();
				}
			}
			else
				hasPreference = false;
		}
	}
	
	private static Object parseValue(final String value, final Preference preference)
	{
		switch (preference.getType())
		{
		case BOOLEAN:
			return Boolean.parseBoolean(value);
		case INT:
			return Integer.parseInt(value);
		case NUMERIC:
			return parseBigDecimal(value, (BigDecimal) preference.getDefaultValue());
		case STRING:
			return value;
		}
		return preference.getDefaultValue();
	}

	public static void clearCache()
	{
		PREFS_CACHE.clear();
	}
	
	public static Object getValue(final Preference preference)
	{
		switch (preference.getType())
		{
		case STRING:
			return getStringValue(preference);
		case INT:
			return getIntegerValue(preference);
		case BOOLEAN:
			return getBooleanValue(preference);
		case NUMERIC:
			return getBigDecimalValue(preference);
		default:
			return null;
		}
	}
	
	public static void putValue(final Preference preference, final Object value)
	{
		switch (preference.getType())
		{
		case STRING:
			putStringValue(preference,(String)value);
			break;
		case INT:
			putIntegerValue(preference, (Integer)value);
			break;
		case BOOLEAN:
			putBooleanValue(preference, (Boolean)value);
			break;
		case NUMERIC:
			putBigDecimalValue(preference, (BigDecimal)value);
			break;
		}
	}

	public static String getStringValue( final Preference preference )
	{
		if(preference.needsCaching() &&  PREFS_CACHE.containsKey(preference))
			return (String) PREFS_CACHE.get(preference);
		final String value = preferences.get(preference.getKey(), (String) preference.getDefaultValue() );
		logGet(preference, value);
		if(preference.needsCaching())
			PREFS_CACHE.put(preference, value);
		return value;
	}
	
	public static void putStringValue( final Preference preference, final String value)
	{
		preferences.put(preference.getKey(), value);
	}

	public static boolean getBooleanValue( final Preference preference )
	{
		if(preference.needsCaching() &&  PREFS_CACHE.containsKey(preference))
			return (Boolean) PREFS_CACHE.get(preference);
		final boolean value = preferences.getBoolean(preference.getKey(), (Boolean) preference.getDefaultValue() );
		logGet(preference, value);
		if(preference.needsCaching())
			PREFS_CACHE.put(preference, value);
		return value;
	}
	
	public static void putBooleanValue( final Preference preference, final boolean value)
	{
		logPut(preference, value);
		preferences.putBoolean(preference.getKey(), value);
	}

	public static int getIntegerValue( final Preference preference )
	{
		if(preference.needsCaching() &&  PREFS_CACHE.containsKey(preference))
			return (Integer) PREFS_CACHE.get(preference);
		final int value = preferences.getInt(preference.getKey(), (Integer) preference.getDefaultValue());
		logGet(preference, value);
		if(preference.needsCaching())
			PREFS_CACHE.put(preference, value);
		return value;
	}
	
	public static void putIntegerValue( final Preference preference, final int value)
	{
		logPut(preference, value);
		preferences.putInt(preference.getKey(), value);
	}

	public static BigDecimal getBigDecimalValue( final Preference preference )
	{
		if(preference.needsCaching() &&  PREFS_CACHE.containsKey(preference))
			return (BigDecimal) PREFS_CACHE.get(preference);
		final BigDecimal defaultValue = (BigDecimal) preference.getDefaultValue();
		final String stringValue = preferences.get(preference.getKey(), DECIMAL_FORMAT.format(defaultValue) );
		final BigDecimal value = parseBigDecimal(stringValue, defaultValue);
		logGet(preference, value);
		if(preference.needsCaching())
			PREFS_CACHE.put(preference, value);
		return value;
	}

	public static void putBigDecimalValue( final Preference preference, final BigDecimal value)
	{
		logPut(preference, value);
		preferences.put(preference.getKey(), DECIMAL_FORMAT.format(value));
	}
	
	public static String getStringEntityValue(Class<? extends Readable> type, final Preference preference )
	{
		final String value = getEntityNode(type).get(preference.getKey(), (String) preference.getDefaultValue());
		logGet(type.getName() + "." + preference, value);
		return value;
	}

	public static String getStringEntityValue(Readable entity, final Preference preference )
	{
		return getStringEntityValue(entity.getClass(), preference );
	}
	
	public static String getStringEntityValue(Entity entity, final Preference preference )
	{
		return getStringEntityValue(TypeManager.getClass(entity), preference );
	}
	
	public static void putStringEntityValue(Class<? extends Readable> type, final Preference preference, final String value)
	{
		logPut(type.getName() + "." + preference, value);
		getEntityNode(type).put(preference.getKey(), value);
	}
	
	public static void putStringEntityValue(Readable entity, final Preference preference, final String value)
	{
		putStringEntityValue(entity.getClass(), preference, value);
	}
	
	public static void putStringEntityValue(Entity entity, final Preference preference, final String value)
	{
		putStringEntityValue(TypeManager.getClass(entity), preference, value);
	}
	
	public static boolean getBooleanEntityValue(Class<? extends Readable> type, final Preference preference )
	{
		final boolean value = getEntityNode(type).getBoolean(preference.getKey(), (Boolean) preference.getDefaultValue());
		logGet(type.getName() + "." + preference, value);
		return value;
	}
	
	public static boolean getBooleanEntityValue(Readable entity, final Preference preference )
	{
		return getBooleanEntityValue(entity.getClass(), preference );
	}
	
	public static boolean getBooleanEntityValue(Entity entity, final Preference preference )
	{
		return getBooleanEntityValue(TypeManager.getClass(entity), preference );
	}
	
	public static void putBooleanEntityValue(Class<? extends Readable> type, final Preference preference, final boolean value)
	{
		logPut(type.getName() + "." + preference, value);
		getEntityNode(type).putBoolean(preference.getKey(), value);
	}
	
	public static void putBooleanEntityValue(Readable entity, final Preference preference, final boolean value)
	{
		putBooleanEntityValue(entity.getClass(), preference, value);
	}
	
	public static void putBooleanEntityValue(Entity entity, final Preference preference, final boolean value)
	{
		putBooleanEntityValue(TypeManager.getClass(entity), preference, value);
	}
	
	public static int getIntegerEntityValue(Class<? extends Readable> type, final Preference preference )
	{
		final int value = getEntityNode(type).getInt(preference.getKey(), (Integer) preference.getDefaultValue());
		logGet(type.getName() + "." + preference, value);
		return value;
	}
	
	public static int getIntegerEntityValue(Readable entity, final Preference preference )
	{
		return getIntegerEntityValue(entity.getClass(), preference);
	}
	
	public static int getIntegerEntityValue(Entity entity, final Preference preference )
	{
		return getIntegerEntityValue(TypeManager.getClass(entity), preference);
	}
	
	public static void putIntegerEntityValue(Class<? extends Readable> type, final Preference preference, final int value)
	{
		logPut(type.getName() + "." + preference, value);
		getEntityNode(type).putInt(preference.getKey(), value);
	}
	
	public static void putIntegerEntityValue(Readable entity, final Preference preference, final int value)
	{
		putIntegerEntityValue(entity.getClass(), preference, value);
	}
	
	public static void putIntegerEntityValue(Entity entity, final Preference preference, final int value)
	{
		putIntegerEntityValue(TypeManager.getClass(entity), preference, value);
	}

	private static BigDecimal parseBigDecimal(final String stringValue, final BigDecimal defaultValue)
	{
		try
		{
			final BigDecimal value = (BigDecimal) DECIMAL_FORMAT.parse( stringValue );
			return value;
		}
		catch (ParseException e)
		{
			LOGGER.error("Unable to parse BigDecimal from \"" + stringValue , e);
			return defaultValue;
		}
	}
	
	private static void logGet( final Preference preference, final Object value)
	{
		logGet(preference.getKey(), value);
	}
	
	private static void logPut( final Preference preference, final Object value)
	{
		logPut(preference.getKey(), value);
	}

	private static void logGet( final String key, final Object value)
	{
		LOGGER.debug("loading preference " + key + "=" + value);
	}
	
	private static void logPut( final String key, final Object value)
	{
		LOGGER.debug("writing preference " + key + "=" + value);
	}
	
	private static java.util.prefs.Preferences getEntityNode(Class<? extends Readable> type)
	{
		return preferences.node(TypeManager.convertType(type).getSimpleName().toLowerCase());
	}
	
}
