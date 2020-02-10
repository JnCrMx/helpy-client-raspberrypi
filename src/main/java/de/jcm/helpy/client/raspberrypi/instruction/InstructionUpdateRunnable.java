package de.jcm.helpy.client.raspberrypi.instruction;

import de.jcm.helpy.CallInteraction;
import de.jcm.helpy.client.raspberrypi.HelpyClient;
import de.jcm.helpy.content.ContentOption;
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

				ContentOption chosen = page.options[interaction.chosenOption];
				if(chosen.message!=null && !chosen.message.isBlank())
				{
					//TODO: test if message already shown
					//instructionPanel.popup(chosen.message);
				}

				if(chosen.target!=null && !chosen.target.isBlank())
				{
					String target = client.contentUtils.getTarget(interaction.contentPath, chosen.target);
					instructionPanel.loadPage(target);
				}
				else
				{
					//TODO: end instruction flow here
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
