package bn.blaszczyk.rosecommon.client;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bn.blaszczyk.rosecommon.RoseException;
import bn.blaszczyk.rosecommon.tools.CommonPreference;
import bn.blaszczyk.rosecommon.tools.FileConverter;
import bn.blaszczyk.rosecommon.tools.Preferences;

public class FileClient {

	private static final Logger LOGGER = LogManager.getLogger(FileClient.class);
	
	private static FileClient instance = null;

	public static FileClient getInstance()
	{
		if(instance == null)
		{
			final String host = Preferences.getStringValue(CommonPreference.SERVICE_HOST);
			final Integer port = Preferences.getIntegerValue(CommonPreference.SERVICE_PORT);
			instance = new FileClient(String.format("http://%s:%d", host, port));
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
	
	public static FileClient newInstance(final String url)
	{
		return new FileClient(url);
	}
	
	private final WebClient webClient;

	private FileClient(final String url)
	{
		webClient = WebClient.create(url + "/file/");
	}
	
	public boolean exists(final String pathArg) throws RoseException
	{
		try
		{
			final String path = encodePath(pathArg);
			LOGGER.debug("requesting GET@/" + path + "?exists");
			webClient.replacePath("/" + path);
			webClient.resetQuery();
			webClient.query("exists","void");
			final Response response = webClient.get();
			final String responseString = response.readEntity(String.class);
			if(response.getStatus() >= 300)
				throw new RoseException(responseString);
			LOGGER.debug("response message:\r\n" + responseString);
			return Boolean.parseBoolean(responseString);
		}
		catch(final Exception e)
		{
			throw RoseException.wrap(e, "Error checking file existence of " + pathArg);
		}
	}
	
	public void download(final String pathArg, final File file) throws RoseException
	{
		try
		{
			final String path = encodePath(pathArg);
			LOGGER.debug("requesting GET@/" + path);
			webClient.replacePath("/" + path);
			webClient.resetQuery();
			final Response response = webClient.get();
			if(response.getStatus() >= 300)
				throw new RoseException(response.readEntity(String.class));
			final InputStream inputStream = response.readEntity(InputStream.class);
			if(!file.getParentFile().exists())
				file.getParentFile().mkdirs();
			Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		catch(final Exception e)
		{
			throw RoseException.wrap(e, "Error checking file existence of " + pathArg);
		}
	}
	
	public void download(final String path) throws RoseException
	{
		download(path, new FileConverter().fromPath(path));
	}
	
	public void upload(final String pathArg, final File file) throws RoseException
	{
		try
		{
			final String path = encodePath(pathArg);
			LOGGER.debug("requesting GET@/" + path);
			webClient.replacePath("/" + path);
			webClient.resetQuery();
			final Response response = webClient.put(file);
			if(response.getStatus() >= 300)
				throw new RoseException(response.readEntity(String.class));
		}
		catch(final Exception e)
		{
			throw RoseException.wrap(e, "Error checking file existence of " + pathArg);
		}
	}
	
	public void close()
	{
		webClient.close();
	}
	
	public URL urlFor(final String path) throws RoseException
	{
		try
		{
			return new URL(webClient.getBaseURI().toString() + encodePath(path));
		}
		catch (MalformedURLException e)
		{
			throw RoseException.wrap(e, "Error creating URL for " + path);
		}
	}


	private static String encodePath(final String pathArg) throws RoseException
	{
		try
		{
			final String path;
			if(pathArg.startsWith("/") || pathArg.startsWith("\\"))
				path = pathArg.substring(1);
			else
				path = pathArg;
			return path.replaceAll("\\\\", "/");
		}
		catch (Exception e)
		{
			throw new RoseException("error in path:" + pathArg, e);
		}
	}
	
}
