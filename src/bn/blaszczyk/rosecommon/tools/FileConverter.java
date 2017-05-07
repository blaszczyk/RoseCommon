package bn.blaszczyk.rosecommon.tools;

import java.io.File;

import bn.blaszczyk.rosecommon.tools.CommonPreference;

import static bn.blaszczyk.rosecommon.tools.Preferences.*;

public class FileConverter {
	
	private final File baseDir;
	private final String baseDirName;
	
	public FileConverter()
	{
		baseDirName = getStringValue(CommonPreference.BASE_DIRECTORY);
		baseDir = new File(baseDirName);
	}
	
	public boolean fileInBaseDir(final File file)
	{
		return file.toPath().startsWith(baseDir.toPath());
	}
	
	public File fromPath(final String path)
	{
		final String cutPath;
		if(path.startsWith("/") || path.startsWith("\\"))
			cutPath = path.substring(1);
		else
			cutPath = path;
		final File file = new File(baseDir,cutPath);
		if(file.isFile())
			return file;
		return new File(path);
	}
	
	public String relativePath(final File file)
	{
		final String fullPath = file.getAbsolutePath();
		if(fileInBaseDir(file))
			return fullPath.substring(baseDirName.length());
		return fullPath;
	}
	
}
