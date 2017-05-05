package bn.blaszczyk.rosecommon.tools;

public class PreferenceAppender implements Preference {
	
	private final Preference preference;
	private final Object apex;
	
	public PreferenceAppender(final Preference preference, final Object apex)
	{
		this.preference = preference;
		this.apex = apex;
	}
	
	@Override
	public Type getType()
	{
		return preference.getType();
	}
	
	@Override
	public String getKey()
	{
		return preference.getKey() + apex;
	}
	
	@Override
	public Object getDefaultValue()
	{
		return preference.getDefaultValue();
	}
	
}
