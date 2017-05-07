package bn.blaszczyk.rosecommon.tools;

import static bn.blaszczyk.rosecommon.tools.Preference.Type.*;

public enum CommonPreference implements Preference {
	
	SERVICE_HOST(STRING,"servicehost","localhost"),
	SERVICE_PORT(STRING,"serviceport","4053"),

	DB_HOST(STRING,"dbhost","localhost"),
	DB_PORT(STRING,"dbport", "3306"),
	DB_NAME(STRING,"dbname","myschema"),
	DB_USER(STRING,"dbuser","root"),
	DB_PASSWORD(STRING,"dbpassword",""),
	
	BASE_DIRECTORY(STRING,"basefolder","C:/temp"),
	LOG_LEVEL(STRING,"loglevel","INFO"),
	FETCH_ON_START(BOOLEAN,"fetchonstart",true),
	FETCH_TIMESPAN(INT,"fetchtimespan",Integer.MAX_VALUE);
	
	private final Type type;
	private final String key;
	private final Object defaultValue;
	
	private CommonPreference(final Type type, final String key, final Object defaultValue)
	{
		if(defaultValue != null && !type.getType().isInstance(defaultValue))
			throw new IllegalArgumentException("preference " + key + "of type " + type + " has false default value class: " + defaultValue.getClass());
		this.type = type;
		this.key = key;
		this.defaultValue = defaultValue;
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
	
}
