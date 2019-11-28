package com.neuronrobotics.bowlerstudio.scripting;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadKeyTemplates;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kohsuke.github.GHAuthorization;
import org.kohsuke.github.GitHub;

import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.JsonKeysetWriter;
import com.google.crypto.tink.config.TinkConfig;

public class PasswordManager {
	private static IGitHubLoginManager loginManager = new IGitHubLoginManager() {

		@Override
		public String[] prompt(String username) {
			// new RuntimeException("Login required").printStackTrace();

			if (username != null) {
				if (username.equals(""))
					username = null;
			}
			String[] creds = new String[] { "", "" };
			System.out.println("#Github Login Prompt#");
			System.out.println("For anynomous mode hit enter twice");
			System.out.print("Github Username: " + (username != null ? "(" + username + ")" : ""));
			// create a scanner so we can read the command-line input
			BufferedReader buf = new BufferedReader(new InputStreamReader(System.in));

			do {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {

					creds[0] = buf.readLine();

				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
				if (creds[0].equals("") && (username == null)) {
					System.out.println("No username, using anynomous login");
					return null;
				}

			} while (creds[0] == null);

			// System.out.print("Github Password: ");
			try {
				Console cons;
				char[] passwd;
				if ((cons = System.console()) != null
						&& (passwd = cons.readPassword("[%s]", "GitHub Password:")) != null) {
					creds[1] = new String(passwd);
					java.util.Arrays.fill(passwd, ' ');
				}
				// creds[1] = buf.readLine();
				if (creds[1].equals("")) {
					System.out.println("GitHub Password Cleartext:");
					creds[1] = buf.readLine();
					if (creds[1].equals("")) {
						System.out.println("No password, using anynomous login");
					}
				}
			} catch (Exception e) {
				return null;
			}
			return creds;
		}

		@Override
		public String twoFactorAuthCodePrompt() {
			System.out.print("Github 2 factor temp key: ");
			// create a scanner so we can read the command-line input
			BufferedReader buf = new BufferedReader(new InputStreamReader(System.in));
			// TODO Auto-generated method stub
			try {
				return buf.readLine().trim();
			} catch (IOException e) {
				return null;
			}
		}
	};
	static {

		checkInternet();
		try {
			TinkConfig.register();
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void checkInternet() {
		try {
			final URL url = new URL("http://github.com");
			final URLConnection conn = url.openConnection();
			conn.connect();
			conn.getInputStream();
			hasnetwork = true;
		} catch (Exception e) {
			// we assuming we have no access to the server and run off of the
			// chached gists.
			hasnetwork = false;
		}
	}
//	private static File tokenfile = null;
//	private static File usernamefile = null;
//	private static File passfile = null;
//	private static File keyfile = null;
	private static File workspace = null;
	private static String loginID = null;
	private static String pw = null;
	private static CredentialsProvider cp;// = new
	private static GitHub github;
	private static boolean hasnetwork;
	private static boolean isLoggedIn = false;

	public static String getPassword() {
		return pw;
	}

	public static GitHub getGithub() {
		return github;
	}

	public static void setGithub(GitHub g) {
		github = g;
	}

	public static String getUsername() {
		return getLoginID();
	}

	public static synchronized void login() throws IOException {
		checkInternet();
		if (!hasnetwork)
			return;
		boolean b = !hasStoredCredentials();
		boolean c = !isLoggedIn;
		boolean c2 = c && b;
		if (c2) {
			String[] creds = loginManager.prompt(PasswordManager.getUsername());
			setLoginID(creds[0]);
			pw = creds[1];

		} else {
			try {
				loadLoginData(getWorkspace());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		try {
			waitForLogin();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * The GistID we are waiting to see
	 */
	public static void waitForLogin() throws Exception {

		if (!hasnetwork)
			return;
		if (loggedIn())
			return;
		if (getLoginID() != null && pw != null) {

			performLogin(getLoginID(), pw);

			if (getGithub() == null) {
				System.out.println("\nERROR: Wrong Password!\n");
				login();
			}

		}
	}

	private static void performLogin(String u, String p) throws Exception {

		github = null;
		GitHub gh = null;
		String token=null;
		try {
			gh = GitHub.connectUsingPassword(u, p);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if(getTokenfile().exists()) {
			byte[] passEncrypt = Files.readAllBytes(Paths.get(getTokenfile().toURI()));
			// 2. Get the primitive.
			Aead aead = getKey().getPrimitive(Aead.class);
			// ... or to decrypt a ciphertext.
			try {
				byte[] decrypted = aead.decrypt(passEncrypt, null);
				token = new String(decrypted).trim();
			} catch (GeneralSecurityException ex) {
				ex.printStackTrace();
				try {
					logout();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return;
			}
		}else {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
			String timestamp = dateFormat.format(new Date());
			List<String> asList = Arrays.asList("repo", "gist", "write:packages", "read:packages", "delete:packages",
					"user", "delete_repo");
			String string = "BowlerStudio-" + timestamp;
			GHAuthorization t=gh.createToken(asList, string, "",()->{
				return loginManager.twoFactorAuthCodePrompt();
			});
			token=t.getToken();
		}
		
		try {
			gh = GitHub.connectUsingPassword(u, token);
			if (gh.getRateLimit().getRemaining() < 2) {
				System.err.println("##Github Is Rate Limiting You## Disabling autoupdate");
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		setGithub(gh);
		setCredentialProvider(new UsernamePasswordCredentialsProvider(u, token));
		isLoggedIn = true;
		try {
			writeData(u, token);
			writeToken(u, token);
			System.out.println("\n\nSuccess Login " + u + "\n\n");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static boolean loggedIn() {
		return isLoggedIn;
	}

	public static boolean hasStoredCredentials() {

		if (getUsernamefile() != null && getPassfile() != null) {
			return getUsernamefile().exists() && getPassfile().exists();
		}
		return false;
	}
	public static boolean hasStoredToken() {
		return getUsernamefile().exists() && getTokenfile().exists();
	}
	public static void logout() throws IOException {

		setGithub(null);
		isLoggedIn = false;
		if (getPassfile() != null)
			if (getPassfile().exists())
				getPassfile().delete();
		if (getTokenfile() != null)
			if (getTokenfile().exists())
				getTokenfile().delete();
		pw = null;
		cp = null;
	}

	public static GitHub setupAnyonmous() throws IOException {
		System.err.println("Using anynomous login, autoupdate disabled");

		logout();
		setGithub(GitHub.connectAnonymously());

		return getGithub();
	}

	public static IGitHubLoginManager getLoginManager() {
		return loginManager;
	}

	public static void setLoginManager(IGitHubLoginManager lm) {
		loginManager = lm;
	}

	public static void loadLoginData(File ws) throws Exception {
		setWorkspace(ws);

		if (!getUsernamefile().exists()) {
			// setUsernamefile(null);
		} else {
			List linesu = Files.readAllLines(Paths.get(getUsernamefile().toURI()), StandardCharsets.UTF_8);
			setLoginID((String) linesu.get(0));
		}
		KeysetHandle keysetHandle = getKey();
//		if (!getPassfile().exists())
//			setPassfile(null);
		if (hasStoredCredentials()) {

			byte[] passEncrypt = Files.readAllBytes(Paths.get(getPassfile().toURI()));
			// 2. Get the primitive.
			Aead aead = keysetHandle.getPrimitive(Aead.class);
			// ... or to decrypt a ciphertext.
			try {
				byte[] decrypted = aead.decrypt(passEncrypt, null);
				String cleartext = new String(decrypted).trim();
				performLogin(getLoginID(), cleartext);
			} catch (GeneralSecurityException ex) {
				ex.printStackTrace();
				logout();
			}
		}
	}
	

	private static KeysetHandle getKey() throws IOException {
		KeysetHandle keysetHandle = null;
		File keyfile = new File(getWorkspace().getAbsoluteFile() + "/loadData.json");
		String keysetFilename = keyfile.getAbsolutePath();
		if (!keyfile.exists()) {
			// Generate the key material...
			// System.err.println("Creating keyfile ");
			try {
				keysetHandle = KeysetHandle.generateNew(AeadKeyTemplates.AES128_GCM);
				// and write it to a file.

				CleartextKeysetHandle.write(keysetHandle, JsonKeysetWriter.withFile(new File(keysetFilename)));
			} catch (GeneralSecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// System.err.println("Loading keyfile ");
			try {
				keysetHandle = CleartextKeysetHandle.read(JsonKeysetReader.withFile(new File(keysetFilename)));
			} catch (GeneralSecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return keysetHandle;
	}

	private static void writeData(String user, String passcleartext) throws Exception {
		setLoginID(user);
		pw = passcleartext;
		if (!getUsernamefile().exists())
			getUsernamefile().createNewFile();
		Files.write(Paths.get(getUsernamefile().toURI()), user.getBytes());
		KeysetHandle keysetHandle = getKey();
		if (!getPassfile().exists())
			getPassfile().createNewFile();
		// 2. Get the primitive.
		Aead aead = keysetHandle.getPrimitive(Aead.class);
		byte[] ciphertext = aead.encrypt(passcleartext.getBytes(), null);
		Files.write(Paths.get(getPassfile().toURI()), ciphertext);

	}
	private static void writeToken(String user, String passcleartext) throws Exception {
		setLoginID(user);
		pw = passcleartext;
		if (!getUsernamefile().exists())
			getUsernamefile().createNewFile();
		Files.write(Paths.get(getUsernamefile().toURI()), user.getBytes());
		KeysetHandle keysetHandle = getKey();
		if (!getTokenfile().exists())
			getTokenfile().createNewFile();
		// 2. Get the primitive.
		Aead aead = keysetHandle.getPrimitive(Aead.class);
		byte[] ciphertext = aead.encrypt(passcleartext.getBytes(), null);
		Files.write(Paths.get(getTokenfile().toURI()), ciphertext);

	}
	public static CredentialsProvider getCredentialProvider() {
		return cp;
	}

	private static void setCredentialProvider(CredentialsProvider cp) {
		PasswordManager.cp = cp;
	}

	public static boolean hasNetwork() {
		return hasnetwork;
	}

	public static String getLoginID() {
		return loginID;
	}

	private static void setLoginID(String loginID) {
		// new RuntimeException(loginID).printStackTrace();
		PasswordManager.loginID = loginID;
	}

	public static File getWorkspace() {
		if (workspace == null)
			workspace = new File(System.getProperty("user.home") + "/bowler-workspace/");
		return workspace;
	}

	public static void setWorkspace(File workspace) {
		PasswordManager.workspace = workspace;
	}

	public static File getUsernamefile() {
		return new File(getWorkspace().getAbsoluteFile() + "/username.json");
	}

	public static File getPassfile() {

		return new File(getWorkspace().getAbsoluteFile() + "/timestamp.json");
	}
	
	public static File getTokenfile() {

		return new File(getWorkspace().getAbsoluteFile() + "/token.json");
	}
}
