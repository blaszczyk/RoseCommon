package bn.blaszczyk.rosecommon.client;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.log4j.Logger;

import bn.blaszczyk.rosecommon.RoseException;
import bn.blaszczyk.rosecommon.tools.CommonPreference;
import bn.blaszczyk.rosecommon.tools.Preferences;

public class CommonClient {
	
	public static final String CODING_CHARSET = "UTF-8";
	
	private static final Logger LOGGER = Logger.getLogger(CommonClient.class);
	
	private final WebClient webClient;
	
	private static CommonClient instance = null;
	
	public static CommonClient newInstance(final String url)
	{
		return new CommonClient(url);
	}
	
	public static CommonClient getInstance()
	{
		if(instance == null)
		{
			final String host = Preferences.getStringValue(CommonPreference.SERVICE_HOST);
			final Integer port = Preferences.getIntegerValue(CommonPreference.SERVICE_PORT);
			instance = new CommonClient(String.format("http://%s:%d", host, port));
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

	private CommonClient(final String url)
	{
		webClient = WebClient.create(url + "/");
	}
	
	public String get(final String path, final Map<String,Object[]> queries) throws RoseException
	{
		try
		{
			LOGGER.debug("requesting GET@/" + path);
			webClient.replacePath("/" + path.toLowerCase());
			webClient.resetQuery();
			for(final Map.Entry<String, Object[]> query : queries.entrySet())
				webClient.query(query.getKey(), query.getValue());
			final String encodedResponse = webClient.get(String.class);
			final String response = URLDecoder.decode(encodedResponse, CODING_CHARSET);
			LOGGER.debug("decoded response message:\r\n" + response);
			return response;
		}
		catch (Exception e)
		{
			throw RoseException.wrap(e, "Error on GET@/" + path);
		}
	}
	
	public String get(final String path) throws RoseException
	{
		return get(path,Collections.emptyMap());
	}
	
	public String post(final String path, final String request, final Map<String,Object[]> queries) throws RoseException
	{
		try
		{
			LOGGER.debug("requesting POST@/" + path);
			webClient.replacePath(path);
			webClient.resetQuery();
			for(final Map.Entry<String, Object[]> query : queries.entrySet())
				webClient.query(query.getKey(), query.getValue());
			LOGGER.debug("decoded request message:\r\n" + request);
			final String encodedRequest = URLEncoder.encode(request, CODING_CHARSET);
			final String encodedResponse = webClient.post(encodedRequest,String.class);
			final String response = URLDecoder.decode(encodedResponse, CODING_CHARSET);
			LOGGER.debug("decoded response message:\r\n" + response);
			return response;
		}
		catch(Exception e)
		{
			throw RoseException.wrap(e, "error on POST@/" + path);
		}
	}
	
	public String post(final String path, final String request) throws RoseException
	{
		return post(path, request, Collections.emptyMap());
	}

	public void put(final String path, final String request, final Map<String,Object[]> queries) throws RoseException
	{
		try
		{
			LOGGER.debug("requesting PUT@/" + path);
			webClient.replacePath(path);
			webClient.resetQuery();
			for(final Map.Entry<String, Object[]> query : queries.entrySet())
				webClient.query(query.getKey(), query.getValue());
			LOGGER.debug("decoded request message:\r\n" + request);
			final String encodedRequest = URLEncoder.encode(request, CODING_CHARSET);
			webClient.put(encodedRequest);
		}
		catch (Exception e)
		{
			throw RoseException.wrap(e, "error on PUT@/" + path);
		}
	}
	
	public void put(final String path, final String request) throws RoseException
	{
		put(path, request, Collections.emptyMap());
	}

	public void delete(final String path, final Map<String,Object[]> queries) throws RoseException
	{
		try
		{
			LOGGER.debug("requesting DELETE@/" + path);
			webClient.replacePath(path);
			webClient.resetQuery();
			for(final Map.Entry<String, Object[]> query : queries.entrySet())
				webClient.query(query.getKey(), query.getValue());
			webClient.delete();
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "error on DELETE@/" + path);
		}
	}
	
	public void delete(final String path) throws RoseException
	{
		delete(path, Collections.emptyMap());
	}

	public void close()
	{
		webClient.close();
	}

}
