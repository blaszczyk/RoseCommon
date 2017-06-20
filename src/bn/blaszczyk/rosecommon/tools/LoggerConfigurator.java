package bn.blaszczyk.rosecommon.tools;

//import java.io.File;
//
//import org.apache.log4j.Appender;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//import org.apache.log4j.RollingFileAppender;
//
//import static bn.blaszczyk.rosecommon.tools.Preferences.*;

public class LoggerConfigurator {
	
	public static void configureLogger(final Preference baseDirectoryPreference, final Preference logLevelPreference)
	{
//		final String baseDirectory = getStringValue(baseDirectoryPreference);
//		final String loglevelName = getStringValue(logLevelPreference);
//		final Level loglevel = Level.toLevel(loglevelName);
//		final Logger logger = Logger.getRootLogger();
//		logger.setLevel(loglevel);
//		final Appender appender = logger.getAppender("rolling-file");
//		if(appender instanceof RollingFileAppender)
//		{
//			final RollingFileAppender rfAppender = (RollingFileAppender) appender;
//			final String fullLoggerPath = baseDirectory + "/" + rfAppender.getFile();
//			File file = new File(fullLoggerPath);
//			if(!file.getParentFile().exists())
//				file.getParentFile().mkdirs();
//			rfAppender.setFile(fullLoggerPath);
//			logger.info("log file: " + fullLoggerPath);
//		}
	}

	
	private LoggerConfigurator()
	{
	}
	
}
