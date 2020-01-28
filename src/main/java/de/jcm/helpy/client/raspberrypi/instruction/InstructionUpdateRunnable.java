package de.jcm.helpy.client.raspberrypi.instruction;

import de.jcm.helpy.CallInteraction;
import de.jcm.helpy.client.raspberrypi.HelpyClient;
import de.jcm.helpy.content.ContentPage;

import java.io.IOException;

public class InstructionUpdateRunnable implements Runnable
{
	private final HelpyClient client;
	private final InstructionPanel instructionPanel;

	public InstructionUpdateRunnable(HelpyClient client, InstructionPanel instructionPanel)
	{
		this.client = client;
		this.instructionPanel = instructionPanel;
	}

	@Override
	public void run()
	{
		CallInteraction interaction = client.api.calls().getLastCallInteraction(client.currentCall);
		if(interaction!=null)
		{
			try
			{
				ContentPage page = client.contentUtils.readPage(interaction.contentPath);

				assert page != null;
				assert page.language.equals(interaction.language);
				assert page.options.length > interaction.chosenOption;

				String target = client.contentUtils.getTarget(interaction.contentPath,
						page.options[interaction.chosenOption].target);

				instructionPanel.loadPage(target);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
