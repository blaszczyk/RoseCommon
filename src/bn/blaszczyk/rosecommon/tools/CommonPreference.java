package bn.blaszczyk.rosecommon.tools;

import static bn.blaszczyk.rosecommon.tools.Preference.Type.*;

public enum CommonPreference implements Preference {
	
	SERVICE_HOST(STRING,"servicehost","localhost",true),
	SERVICE_PORT(INT,"serviceport",4053,true),

	DB_HOST(STRING,"dbhost","localhost",true),
	DB_PORT(STRING,"dbport", "3306",true),
	DB_NAME(STRING,"dbname","myschema",true),
	DB_USER(STRING,"dbuser","root",true),
	DB_PASSWORD(STRING,"dbpassword","",true),
	
	BASE_DIRECTORY(STRING,"basefolder","C:/temp"),
	LOG_LEVEL(STRING,"loglevel","INFO"),
	FETCH_ON_START(BOOLEAN,"fetchonstart",true,true),
	FETCH_TIMESPAN(INT,"fetchtimespan",Integer.MAX_VALUE);
	
	private final Type type;
	private final String key;
	private final Object defaultValue;
	private final boolean needsCaching;

	private CommonPreference(final Type type, final String key, final Object defaultValue, final boolean needsCaching)
	{
		if(defaultValue != null && !type.getType().isInstance(defaultValue))
			throw new IllegalArgumentException("preference " + key + "of type " + type + " has false default value class: " + defaultValue.getClass());
		this.type = type;
		this.key = key;
		this.defaultValue = defaultValue;
		this.needsCaching = needsCaching;
	}

	private CommonPreference(final Type type, final String key, final Object defaultValue)
	{
		this(type, key, defaultValue, false);
	}

	@Override
	public Type getType()
	{
		return type;
	}

	@Override
	public String getKey()
	{
		return key;
	}

	@Override
	public Object getDefaultValue()
	{
		return defaultValue;
	}
	
	@Override
	public boolean needsCaching()
	{
		return needsCaching;
	}
	
}
