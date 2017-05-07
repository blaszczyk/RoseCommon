package bn.blaszczyk.rosecommon.tools;

import java.math.BigDecimal;

public interface Preference {
	
	public Type getType();
	public String getKey();
	public Object getDefaultValue();
	
	default public boolean needsCaching()
	{
		return false;
	}
	
	default public Preference append(final Object apex)
	{
		return new PreferenceAppender(this, apex);
	}
	
	public static enum Type {
		STRING(String.class),
		BOOLEAN(Boolean.class),
		INT(Integer.class),
		NUMERIC(BigDecimal.class);
		
		private final Class<?> type;
		
		private Type(final Class<?> type)
		{
			this.type = type;
		}
		
		public Class<?> getType()
		{
			return type;
		}
		
	}
	
}
