package de.jcm.helpy.client.raspberrypi.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jcm.helpy.client.raspberrypi.HelpyClient;
import de.jcm.helpy.client.raspberrypi.I18n;
import de.jcm.helpy.content.ContentPage;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class ContentUtils
{
	private final ObjectMapper mapper = new ObjectMapper();
	private File contentDirectory;

	/*
	The InstructionUpdateRunnable needs to read the page in order to calculate the next one.
	This is repeated constantly. Therefore better cache the read pages to save time and file accesses.
	 */
	private HashMap<String, ContentPage> pageCache = new HashMap<>();

	public ContentUtils(HelpyClient client)
	{
		this.contentDirectory = new File(client.updater.contentDirectory, I18n.getLocale().toLanguageTag());
	}

	public ContentPage readPage(String path) throws IOException
	{
		if(pageCache.containsKey(path))
		{
			return pageCache.get(path);
		}
		else
		{
			File file = new File(contentDirectory, path + ".json");

			// somehow our ObjectMapper doesn't use UTF-8 by default, so we use an UTF-8 Reader
			Reader reader = new FileReader(file, StandardCharsets.UTF_8);

			ContentPage page = mapper.readValue(reader, ContentPage.class);
			reader.close();

			pageCache.put(path, page);

			return page;
		}
	}

	public String fileToPath(File file)
	{
		return contentDirectory.toPath().relativize(file.toPath()).toString().replace(".json", "");
	}

	public String getTarget(String oldPath, String target)
	{
		int lastDelimiter = oldPath.lastIndexOf('/');
		return (lastDelimiter>0?oldPath.substring(0, lastDelimiter):"")+target;
	}
}
