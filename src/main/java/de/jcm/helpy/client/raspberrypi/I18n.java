package de.jcm.helpy.client.raspberrypi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Scanner;

public class I18n
{
	private static Logger logger = LoggerFactory.getLogger(I18n.class);

	private static Locale currentLanguage;
	private static HashMap<Locale, HashMap<String, String>> languages = new HashMap<>();

	public static String getLanguageEntry(String key)
	{
		if(languages.containsKey(currentLanguage))
		{
			HashMap<String, String> words = languages.get(currentLanguage);
			if(words.containsKey(key))
			{
				return words.get(key);
			}
		}
		return key;
	}

	private static void loadLanguage(Locale lang)
	{
		String filename = "/lang/"+lang.toLanguageTag()+".lang";

		try
		{
			Scanner scanner = new Scanner(HelpyClient.class.getResourceAsStream(filename));

			HashMap<String, String> words = new HashMap<>();
			while(scanner.hasNextLine())
			{
				String line = scanner.nextLine();

				if(line.contains("="))
				{
					String key = line.split("=")[0];
					String value = line.split("=")[1];

					words.put(key, value);
				}
			}
			scanner.close();

			logger.debug("Loaded language: "+lang);
			languages.put(lang, words);
		}
		catch (Exception e)
		{
			logger.error("Cannot load language: "+lang, e);
		}
	}

	public static HashMap<Locale, HashMap<String, String>> getLanguages()
	{
		return languages;
	}

	public static void setLocale(Locale language)
	{
		currentLanguage = language;

		if(!languages.containsKey(language))
		{
			loadLanguage(language);
		}
	}

	public static Locale getLocale()
	{
		return currentLanguage;
	}

	public static Locale getLanguage()
	{
		return Locale.forLanguageTag(I18n.getLocale().getLanguage());
	}

	public static String translate(String key, Object...args)
	{
		String translation = getLanguageEntry(key);

		int keyStart;
		while((keyStart = translation.indexOf("@{"))!=-1)
		{
			int keyEnd = translation.indexOf('}', keyStart);

			String newKey = translation.substring(keyStart+2, keyEnd);

			String before = translation.substring(0, keyStart);
			String after = translation.substring(keyEnd+1);

			translation = before + translate(newKey, args) + after;
		}

		return String.format(translation, args);
	}

	public static String translate(String key)
	{
		return translate(key, new Object[0]);
	}
}
