package bn.blaszczyk.rosecommon.client;

import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.internal.StringMap;

import bn.blaszczyk.rosecommon.RoseException;
import bn.blaszczyk.rosecommon.dto.PreferenceDto;
import bn.blaszczyk.rosecommon.tools.CommonPreference;
import bn.blaszczyk.rosecommon.tools.Preferences;

public class ServiceConfigClient {

	private static final Gson GSON = new Gson();
	
	private static ServiceConfigClient instance = null;

	public static ServiceConfigClient getInstance()
	{
		if(instance == null)
		{
			final String host = Preferences.getStringValue(CommonPreference.SERVICE_HOST);
			final Integer port = Preferences.getIntegerValue(CommonPreference.SERVICE_PORT);
			instance = new ServiceConfigClient(String.format("http://%s:%d", host, port));
		}
		return instance;
	}
	
	public static void closeInstance()
	{
		if(instance != null)
		{
			instance.close();
			instance = null;
		}
	}
	
	public static ServiceConfigClient newInstance(final String url)
	{
		return new ServiceConfigClient(url);
	}
	
	private final CommonClient client;

	private ServiceConfigClient(final String url)
	{
		client = CommonClient.newInstance(url + "/server");
	}

	public PreferenceDto getPreferences() throws RoseException
	{
		try
		{
			final String response = client.get("config");
			final StringMap<?> stringMap = GSON.fromJson(response, StringMap.class);
			return new PreferenceDto(stringMap);
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "Error on GET@/server/config");
		}
	}

	public void putPreferences(final PreferenceDto dto) throws RoseException
	{
		try
		{
			final String request = GSON.toJson(dto);
			client.put("config", request);
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "error on PUT@/server/config");
		}
	}
	
	public Map<String,String> getServerStatus() throws RoseException
	{
		try
		{
			final String response = client.get("status");
			final StringMap<?> status = GSON.fromJson(response, StringMap.class);
			return status.entrySet().stream().
				collect(Collectors.toMap(e -> e.getKey(), e -> String.valueOf(e.getValue())));
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "error on GET@/server/status");
		}
	}

	public void postStopRequest() throws RoseException
	{
		client.post("stop", "");
	}

	public void postRestartRequest() throws RoseException
	{
		client.post("restart", "");
	}
	
	public void close()
	{
		client.close();
	}

}
