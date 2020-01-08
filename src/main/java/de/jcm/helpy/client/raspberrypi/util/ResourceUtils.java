package de.jcm.helpy.client.raspberrypi.util;

import de.jcm.helpy.client.raspberrypi.HelpyClient;
import org.apache.commons.io.IOUtils;

import java.io.*;

public class ResourceUtils
{
	public static File extractResource(String name) throws IOException
	{
		InputStream in = HelpyClient.class.getResourceAsStream(name);

		if(in==null)
			throw new FileNotFoundException(name);

		String prefix = (name.lastIndexOf('.')>name.lastIndexOf('/')?
				name.substring(name.lastIndexOf('/')+1, name.lastIndexOf('.')):
				name.substring(name.lastIndexOf('/')+1))+".";
		String suffix = name.lastIndexOf('.')>name.lastIndexOf('/')?
				name.substring(name.lastIndexOf('.')):"";

		File file = File.createTempFile(prefix, suffix);
		if(!file.isFile() || !file.canWrite())
			throw new FileNotFoundException(file.getAbsolutePath());

		FileOutputStream out = new FileOutputStream(file);

		IOUtils.copy(in, out);

		in.close();
		out.close();

		return file;
	}
}
