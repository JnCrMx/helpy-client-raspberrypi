package de.jcm.helpy.client.raspberrypi.util;

import de.jcm.helpy.client.raspberrypi.HelpyClient;
import de.jcm.helpy.distribution.VersionInfo;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Updater
{
	private final HelpyClient client;
	public final File downloadDirectory;

	private final boolean contentUpdate;
	private final String contentBranch;
	private final File contentZip;
	public final File contentDirectory;

	public Updater(HelpyClient client)
	{
		this.client = client;

		downloadDirectory = new File(client.dataDirectory, "download");
		downloadDirectory.mkdir();

		contentUpdate = client.config.getBoolean("distribution.content.update", true);

		contentZip = new File(downloadDirectory, "content.zip");

		contentDirectory = new File(client.dataDirectory, "content");
		contentDirectory.mkdir();

		contentBranch = client.config.getString("distribution.content.branch", "latest");
	}

	public void update()
	{
		try
		{
			updateContent();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void updateContent() throws IOException
	{
		if(contentUpdate)
		{
			downloadContent();
			extractContent();
		}
		else
		{
			System.err.println("The client is configured to not update its content distribution!");
			System.err.println("This is likely cause by \"distribution.content.update\" being set to \"false\" in " +
					"configuration.");
			System.err.println("Please be aware that this might be dangerous!");
		}
	}

	private void downloadContent() throws IOException
	{
		if(contentZip.exists())
		{
			VersionInfo info = client.api.distributions().content().branch(contentBranch);

			try
			{
				String hash = DigestUtils.sha256Hex(new FileInputStream(contentZip));
				if (info.hash.equals(hash))
				{
					return;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		// We will return; before if we don't need to download
		System.out.println("Downloading content distribution...");
		client.api.distributions().content().downloadBranch(contentBranch, contentZip);
	}

	private void extractContent() throws IOException
	{
		if(!contentZip.exists())
		{
			throw new FileNotFoundException(contentZip.getAbsolutePath());
		}

		CRC32 crc32 = new CRC32();

		ZipFile zip = new ZipFile(contentZip, ZipFile.OPEN_READ);
		Enumeration<? extends ZipEntry> entries = zip.entries();
		while(entries.hasMoreElements())
		{
			ZipEntry entry = entries.nextElement();
			File file = new File(contentDirectory, entry.getName());

			if(entry.isDirectory())
			{
				if(!file.exists())
				{
					file.mkdir();
				}
				continue;
			}
			if(file.exists() &&
					// I have no idea how to checksum symlinks, so let's just skip those and always extract symlinks ^^
					entry.getMethod()!=ZipEntry.STORED)
			{
				InputStream in = new CheckedInputStream(new FileInputStream(file), crc32);
				in.readAllBytes();
				in.close();

				long localCrc = crc32.getValue();
				crc32.reset();
				long distCrc = entry.getCrc();

				if(localCrc == distCrc)
				{
					continue;
				}
			}

			// We will continue; if the file doesn't need updating
			System.out.println("Extracting \""+entry.getName()+"\" from content distribution to \""+
					file.getAbsolutePath()+"\"...");
			try
			{
				// We assume that a stored file is a symlink
				if(entry.getMethod() == ZipEntry.STORED)
				{
					InputStream in = zip.getInputStream(entry);
					String target = new String(in.readAllBytes());
					in.close();

					/*
					 We need to delete the file if it exists in order to avoid a
					 java.nio.file.FileAlreadyExistsException thrown by Files.createSymbolicLink
					 */
					Files.deleteIfExists(file.toPath());

					Files.createSymbolicLink(file.toPath(), new File(file.getParent(), target).toPath());
				}
				else
				{
					InputStream in = zip.getInputStream(entry);
					OutputStream out = new FileOutputStream(file);

					IOUtils.copy(in, out);

					in.close();
					out.close();
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
