package GithubPasswordManager;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import com.neuronrobotics.bowlerstudio.scripting.PasswordManager;

public class loginTest {

	@Test
	public void test() {
		try {
			PasswordManager.logout();
			PasswordManager.login();
		} catch (IOException e) {
			fail();
		}
	}

}
