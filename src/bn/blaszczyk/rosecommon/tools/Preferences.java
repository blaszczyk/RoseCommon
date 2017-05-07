package bn.blaszczyk.rosecommon.tools;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

import org.apache.log4j.Logger;

import bn.blaszczyk.rose.model.Entity;
import bn.blaszczyk.rose.model.Readable;

public class Preferences {
	
	private final static Logger LOGGER = Logger.getLogger(Preferences.class);
	
	private final static DecimalFormat DECIMAL_FORMAT =  (DecimalFormat) NumberFormat.getNumberInstance();
	static {
		DECIMAL_FORMAT.setParseBigDecimal(true);
	}

	private static java.util.prefs.Preferences preferences;
	
	private Preferences()
	{
	}

	public static void setMainClass(Class<?> type)
	{
		preferences = java.util.prefs.Preferences.userNodeForPackage(type);
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

	public static String getStringValue( final Preference key )
	{
		String value = preferences.get(key.getKey(), (String) key.getDefaultValue() );
		logGet(key, value);
		return value;
	}
	
	public static void putStringValue( final Preference key, final String value)
	{
		preferences.put(key.getKey(), value);
	}

	public static boolean getBooleanValue( final Preference key )
	{
		boolean value = preferences.getBoolean(key.getKey(), (Boolean) key.getDefaultValue() );
		logGet(key, value);
		return value;
	}
	
	public static void putBooleanValue( final Preference key, final boolean value)
	{
		logPut(key, value);
		preferences.putBoolean(key.getKey(), value);
	}

	public static int getIntegerValue( final Preference key )
	{
		int value = preferences.getInt(key.getKey(), (Integer) key.getDefaultValue());
		logGet(key, value);
		return value;
	}
	
	public static void putIntegerValue( final Preference key, final int value)
	{
		logPut(key, value);
		preferences.putInt(key.getKey(), value);
	}

	public static BigDecimal getBigDecimalValue( final Preference key )
	{
		final BigDecimal defaultValue = (BigDecimal) key.getDefaultValue();
		String stringValue = preferences.get(key.getKey(), DECIMAL_FORMAT.format(defaultValue) );
		try
		{
			BigDecimal value = (BigDecimal) DECIMAL_FORMAT.parse( stringValue );
			logGet(key, value);
			return value;
		}
		catch (ParseException e)
		{
			LOGGER.error("Unable to parse BigDecimal from \"" + stringValue , e);
			return defaultValue;
		}
	}
	
	public static void putBigDecimalValue( final Preference key, final BigDecimal value)
	{
		logPut(key, value);
		preferences.put(key.getKey(), DECIMAL_FORMAT.format(value));
	}
	
	public static String getStringEntityValue(Class<? extends Readable> type, final Preference key )
	{
		String value = getEntityNode(type).get(key.getKey(), (String) key.getDefaultValue());
		logGet(type.getName() + "." + key, value);
		return value;
	}

	public static String getStringEntityValue(Readable entity, final Preference key )
	{
		return getStringEntityValue(entity.getClass(), key );
	}
	
	public static String getStringEntityValue(Entity entity, final Preference key )
	{
		return getStringEntityValue(TypeManager.getClass(entity), key );
	}
	
	public static void putStringEntityValue(Class<? extends Readable> type, final Preference key, final String value)
	{
		logPut(type.getName() + "." + key, value);
		getEntityNode(type).put(key.getKey(), value);
	}
	
	public static void putStringEntityValue(Readable entity, final Preference key, final String value)
	{
		putStringEntityValue(entity.getClass(), key, value);
	}
	
	public static void putStringEntityValue(Entity entity, final Preference key, final String value)
	{
		putStringEntityValue(TypeManager.getClass(entity), key, value);
	}
	
	public static boolean getBooleanEntityValue(Class<? extends Readable> type, final Preference key )
	{
		boolean value = getEntityNode(type).getBoolean(key.getKey(), (Boolean) key.getDefaultValue());
		logGet(type.getName() + "." + key, value);
		return value;
	}
	
	public static boolean getBooleanEntityValue(Readable entity, final Preference key )
	{
		return getBooleanEntityValue(entity.getClass(), key );
	}
	
	public static boolean getBooleanEntityValue(Entity entity, final Preference key )
	{
		return getBooleanEntityValue(TypeManager.getClass(entity), key );
	}
	
	public static void putBooleanEntityValue(Class<? extends Readable> type, final Preference key, final boolean value)
	{
		logPut(type.getName() + "." + key, value);
		getEntityNode(type).putBoolean(key.getKey(), value);
	}
	
	public static void putBooleanEntityValue(Readable entity, final Preference key, final boolean value)
	{
		putBooleanEntityValue(entity.getClass(), key, value);
	}
	
	public static void putBooleanEntityValue(Entity entity, final Preference key, final boolean value)
	{
		putBooleanEntityValue(TypeManager.getClass(entity), key, value);
	}
	
	public static int getIntegerEntityValue(Class<? extends Readable> type, final Preference key )
	{
		int value = getEntityNode(type).getInt(key.getKey(), (Integer) key.getDefaultValue());
		logGet(type.getName() + "." + key, value);
		return value;
	}
	
	public static int getIntegerEntityValue(Readable entity, final Preference key )
	{
		return getIntegerEntityValue(entity.getClass(), key);
	}
	
	public static int getIntegerEntityValue(Entity entity, final Preference key )
	{
		return getIntegerEntityValue(TypeManager.getClass(entity), key);
	}
	
	public static void putIntegerEntityValue(Class<? extends Readable> type, final Preference key, final int value)
	{
		logPut(type.getName() + "." + key, value);
		getEntityNode(type).putInt(key.getKey(), value);
	}
	
	public static void putIntegerEntityValue(Readable entity, final Preference key, final int value)
	{
		putIntegerEntityValue(entity.getClass(), key, value);
	}
	
	public static void putIntegerEntityValue(Entity entity, final Preference key, final int value)
	{
		putIntegerEntityValue(TypeManager.getClass(entity), key, value);
	}

	private static void logGet( final Preference key, final Object value)
	{
		logGet(key.getKey(), value);
	}
	
	private static void logPut( final Preference key, final Object value)
	{
		logPut(key.getKey(), value);
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
