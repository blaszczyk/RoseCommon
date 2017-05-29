package bn.blaszczyk.rosecommon.client;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.log4j.Logger;

import bn.blaszczyk.rosecommon.RoseException;
import bn.blaszczyk.rosecommon.tools.CommonPreference;
import bn.blaszczyk.rosecommon.tools.FileConverter;
import bn.blaszczyk.rosecommon.tools.Preferences;

public class FileClient {

	private static final Logger LOGGER = Logger.getLogger(FileClient.class);
	
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
			webClient.replacePath("/" + path.toLowerCase());
			webClient.resetQuery();
			webClient.query("exists","void");
			final String response = webClient.get(String.class);
			LOGGER.debug("response message:\r\n" + response);
			return Boolean.parseBoolean(response);
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
			webClient.replacePath("/" + path.toLowerCase());
			webClient.resetQuery();
			final InputStream inputStream = webClient.get(InputStream.class);
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
			webClient.replacePath("/" + path.toLowerCase());
			webClient.resetQuery();
			final byte[] bytes = Files.readAllBytes(file.toPath());
			final String request = new String(bytes);
			webClient.put(request);
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
	
	public String fullPathFor(final String path) throws RoseException
	{
		return webClient.getBaseURI().toString() + encodePath(path);
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
