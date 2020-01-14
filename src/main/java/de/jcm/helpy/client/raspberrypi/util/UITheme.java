package de.jcm.helpy.client.raspberrypi.util;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.base.Objects;

import java.awt.*;
import java.util.Map;

public class UITheme
{
	@JsonIgnore
	public Color foreground;
	@JsonIgnore
	public Color background;

	public String fontFamily;
	public String lookAndFeel;

	@JsonGetter("foreground")
	Map<String, Integer> getForeground()
	{
		return Map.of(
				"red", foreground.getRed(),
				"green", foreground.getGreen(),
				"blue", foreground.getBlue());
	}

	@JsonGetter("background")
	Map<String, Integer> getBackground()
	{
		return Map.of(
				"red", background.getRed(),
				"green", background.getGreen(),
				"blue", background.getBlue());
	}

	@JsonSetter("foreground")
	void setForeground(Map<String, Integer> map)
	{
		foreground=new Color(map.get("red"), map.get("green"), map.get("blue"));
	}

	@JsonSetter("background")
	void setBackground(Map<String, Integer> map)
	{
		background=new Color(map.get("red"), map.get("green"), map.get("blue"));
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		UITheme uiTheme = (UITheme) o;
		return Objects.equal(foreground, uiTheme.foreground) &&
				Objects.equal(background, uiTheme.background) &&
				Objects.equal(fontFamily, uiTheme.fontFamily) &&
				Objects.equal(lookAndFeel, uiTheme.lookAndFeel);
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(foreground, background, fontFamily, lookAndFeel);
	}

	public Font createFont(int size)
	{
		return new Font(fontFamily, Font.PLAIN, size);
	}

	public Font createFont(int style, int size)
	{
		return new Font(fontFamily, style, size);
	}
}
