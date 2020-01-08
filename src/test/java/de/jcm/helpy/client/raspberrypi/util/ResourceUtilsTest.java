package de.jcm.helpy.client.raspberrypi.util;

import de.jcm.helpy.client.raspberrypi.HelpyClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ResourceUtilsTest
{
	@ParameterizedTest
	@ValueSource(strings = {"/lang/de-DE.lang",
			"/model/de/lm.binary", "/model/de/output_graph.pb", "/model/de/trie"})
	void testResourceUtils(String name) throws IOException
	{
		File extracted = ResourceUtils.extractResource(name);

		String expectedSuffix = name.lastIndexOf('.')>name.lastIndexOf('/')?
			name.substring(name.lastIndexOf('.')):"";

		Assertions.assertNotNull(extracted);

		String ePath = extracted.getAbsolutePath();
		String suffix = ePath.lastIndexOf('.')>ePath.lastIndexOf('/')?
				ePath.substring(ePath.lastIndexOf('.')):"";

		// Is there is no suffix to the file, we don't care.
		if(!expectedSuffix.isBlank())
			Assertions.assertEquals(expectedSuffix, suffix);

		InputStream in1 = HelpyClient.class.getResourceAsStream(name);
		InputStream in2 = new FileInputStream(extracted);

		int r1, r2;
		// We will do a binary or instead of a logical here, so both methods get executed.
		while( (r1 = in1.read())!=-1 | (r2 = in2.read())!=-1)
		{
			Assertions.assertEquals(r1, r2);
		}

		in1.close();
		in2.close();
	}
}
