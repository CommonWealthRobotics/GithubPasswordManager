package GithubPasswordManager;

import java.util.function.Supplier;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class APIProvider implements Supplier<String>{

	@Override
	public String get() {
		JFrame jframe = new JFrame();
		String answer = JOptionPane.showInputDialog(jframe, "Enter API secret");
		jframe.dispose();
		return answer;
	}

}
