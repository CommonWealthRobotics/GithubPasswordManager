package com.neuronrobotics.bowlerstudio.scripting;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

public interface IURLOpen {
	public default void open(URI toOpe) {
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
			try {
				Desktop.getDesktop().browse(toOpe);
			} catch (Exception e) {
				// Auto-generated catch block
				e.printStackTrace();
			}
	}
}
