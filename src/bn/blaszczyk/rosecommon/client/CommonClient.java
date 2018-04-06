package bn.blaszczyk.rosecommon.client;

import java.util.Collections;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.logging.log4j.*;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rosecommon.tools.CommonPreference;
import bn.blaszczyk.rosecommon.tools.Preferences;

public class CommonClient {
	
	private static final Logger LOGGER = LogManager.getLogger(CommonClient.class);
	
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
			setPath(path);
			for(final Map.Entry<String, Object[]> query : queries.entrySet())
				webClient.query(query.getKey(), query.getValue());
			final Response response = webClient.get();
			final int status = response.getStatus();
			final String responseString = response.readEntity(String.class);
			if(status >= 300)
				throw new RoseException(responseString);
			LOGGER.debug("response message:\r\n" + responseString);
			return responseString;
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
			setPath(path);
			for(final Map.Entry<String, Object[]> query : queries.entrySet())
				webClient.query(query.getKey(), query.getValue());
			LOGGER.debug("request message:\r\n" + request);
			final Response response = webClient.post(request);
			final int status = response.getStatus();
			final String responseString = response.readEntity(String.class);
			if(status >= 300)
				throw new RoseException(responseString);
			if(responseString == null)
				return "";
			LOGGER.debug("response message:\r\n" + responseString);
			return responseString;
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
			setPath(path);
			for(final Map.Entry<String, Object[]> query : queries.entrySet())
				webClient.query(query.getKey(), query.getValue());
			LOGGER.debug("request message:\r\n" + request);
			final Response response = webClient.put(request);
			final int status = response.getStatus();
			final String encodedResponse = response.readEntity(String.class);
			if(status >= 300)
				throw new RoseException(encodedResponse);
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
			setPath(path);
			for(final Map.Entry<String, Object[]> query : queries.entrySet())
				webClient.query(query.getKey(), query.getValue());
			final Response response = webClient.delete();
			final int status = response.getStatus();
			final String encodedResponse = response.readEntity(String.class);
			if(status >= 300)
				throw new RoseException(encodedResponse);
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


	private void setPath(final String path)
	{
		final String absolutePath = path.startsWith("/") ? path : "/" + path;
		webClient.replacePath(absolutePath);
		webClient.resetQuery();
		webClient.encoding("UTF-8");
		webClient.acceptEncoding("UTF-8");
	}
}
