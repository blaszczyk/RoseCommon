package bn.blaszczyk.rosecommon.client;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.internal.StringMap;

import bn.blaszczyk.rosecommon.RoseException;
import bn.blaszczyk.rosecommon.dto.PreferenceDto;

public class ServiceConfigClient {
	
	public static final String CODING_CHARSET = "UTF-8";
	
	private static final Logger LOGGER = Logger.getLogger(ServiceConfigClient.class);

	private static final Gson GSON = new Gson();
	
	private final WebClient webClient;

	public ServiceConfigClient(final String url)
	{
		webClient = WebClient.create(url + "/server/");
	}

	public PreferenceDto getPreferences() throws RoseException
	{
		try
		{
			LOGGER.debug("requesting GET@/server/config");
			webClient.replacePath("config");
			webClient.resetQuery();
			final String encodedResponse = webClient.get(String.class);
			final String response = URLDecoder.decode(encodedResponse, CODING_CHARSET);
			final StringMap<?> stringMap = GSON.fromJson(response, StringMap.class);
			LOGGER.debug("decoded response message:\r\n" + response);
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
			LOGGER.debug("requesting PUT@/server/config");
			webClient.replacePath("config");
			webClient.resetQuery();
			final String request = GSON.toJson(dto);
			LOGGER.debug("decoded request message:\r\n" + request);
			final String encodedRequest = URLEncoder.encode(request, CODING_CHARSET);
			webClient.put(encodedRequest);
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
			webClient.replacePath("status");
			webClient.resetQuery();
			final String encodedResponse = webClient.get(String.class);
			final String response = URLDecoder.decode(encodedResponse, CODING_CHARSET);
			final StringMap<?> status = GSON.fromJson(response, StringMap.class);
			return status.entrySet().stream().
				collect(Collectors.toMap(e -> e.getKey(), e -> String.valueOf(e.getValue())));
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "error on GET@/server/status");
		}
	}

	public void postStopRequest()
	{
		webClient.replacePath("stop");
		webClient.resetQuery();
		webClient.post("");
	}

	public void postRestartRequest()
	{
		webClient.replacePath("restart");
		webClient.resetQuery();
		webClient.post("");
	}
	
	public void close()
	{
		webClient.close();
	}

}
