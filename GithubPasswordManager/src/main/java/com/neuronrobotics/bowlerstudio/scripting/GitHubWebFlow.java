package com.neuronrobotics.bowlerstudio.scripting;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

//import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
//import com.google.api.client.auth.oauth2.BearerToken;
//import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
//import com.google.api.client.auth.oauth2.TokenResponse;
//import com.google.api.client.http.GenericUrl;
//import com.google.api.client.http.HttpRequest;
//import com.google.api.client.http.HttpRequestInitializer;
//import com.google.api.client.http.HttpTransport;
//import com.google.api.client.http.javanet.NetHttpTransport;
//import com.google.api.client.json.JsonFactory;
//import com.google.api.client.json.jackson2.JacksonFactory;

/**
 * This Class is to allow for users to request a temporary login token that will
 * be used later to get an authorization token.
 * 
 * REF:
 * https://dzone.com/articles/how-to-implement-java-oauth-20-to-sign-in-with-git-1
 * 
 * 
 * @author hephaestus
 *
 */

public class GitHubWebFlow implements IGitHubLoginManager {
	private static int WEBSERVER_PORT = 3737;
	String[] returnData = null;
	private static Supplier<String> myAPI = () -> {
		return "1edf79fae494c232d4d2";
	};
	private static Supplier<String> mykey = () -> {
		JFrame jframe = new JFrame();
		String answer = JOptionPane.showInputDialog(jframe, "Enter API secret");
		jframe.dispose();
		return answer;
	};
	String state ="";
	@Override
	public String[] prompt(String loginID) {
		if(loginID ==null) {
//			JFrame jframe = new JFrame();
//			loginID = JOptionPane.showInputDialog(jframe, "Github User Name ");
//			jframe.dispose();
			loginID="madhephaestus";
		}
		String id = loginID;
		Server server = new Server(WEBSERVER_PORT);
		int leftLimit = 97; // letter 'a'
	    int rightLimit = 122; // letter 'z'
	    int targetStringLength = 10;
	    Random random = new Random();
	 
	    String generatedString = random.ints(leftLimit, rightLimit + 1)
	      .limit(targetStringLength)
	      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
	      .toString();
	    
	    
		try {
			
			returnData = null;
			ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
			context.addServlet(new ServletHolder(new HttpServlet() {
				@Override
				protected void doGet(HttpServletRequest request, HttpServletResponse response)
						throws ServletException, IOException {
					try {
						final String code = request.getParameter("code");
						response.setStatus(HttpServletResponse.SC_OK);
						// Now perform step 2
						// https://developer.github.com/apps/building-oauth-apps/authorizing-oauth-apps/#2-users-are-redirected-back-to-your-site-by-github
						String doRequest = "https://github.com/login/oauth/access_token?" +
								"client_id=" + getMyAPI().get() + "&"+	
								"client_secret=" + mykey.get() + "&"+	
								"code=" + code + "&"+	
								"redirect_uri=http%3A%2F%2Flocalhost%3A"+WEBSERVER_PORT+"%finished"  ;
						System.out.println(doRequest);
						try {
							if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
								try {
									Desktop.getDesktop().browse(new URI(doRequest));
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
						} catch (URISyntaxException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (Throwable e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} catch (Exception ex) {
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

					} finally {
						response.setContentType("text/html;charset=UTF-8");
						response.getWriter().println("");
						response.getWriter().close();
					}
				}
			}), "/success/*");
			context.addServlet(new ServletHolder(new HttpServlet() {
				@Override
				protected void doGet(HttpServletRequest request, HttpServletResponse response)
						throws ServletException, IOException {
					try {
						final String tok = request.getParameter("access_token");
						response.setStatus(HttpServletResponse.SC_OK);
						returnData=new String[] {id,tok};
					} catch (Exception ex) {
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

					} finally {
						response.setContentType("text/html;charset=UTF-8");
						response.getWriter().println("");
						response.getWriter().close();
					}
				}
			}), "/finished/*");
			server.setHandler(context);
			server.setStopAtShutdown(true);
			try {
				server.start();
				//server.join();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			String doRequest = "https://github.com/login/oauth/authorize?" +
			"client_id=" + getMyAPI().get() + "&"	
			+ "redirect_uri=http%3A%2F%2Flocalhost%3A"+WEBSERVER_PORT+"%2Fsuccess" + "&" +
			"response_type=code" + "&" + 
			"login="+id.replaceAll("@", "%40") + "&" + 
			"allow_signup=true" + "&" + 
			//"state="+generatedString + "&" +
			"scope=";
			List<String> listOfScopes = PasswordManager.listOfScopes;
			for (int i = 0; i < listOfScopes.size(); i++) {
				String scope = listOfScopes.get(i);
				scope = scope.replaceAll(":", "%3A");
				doRequest += scope ;
				if(i!=listOfScopes.size()-1)
					doRequest +=  "%20";
			}
			doRequest = doRequest.trim();
			System.out.println(doRequest);
			// Send request in step 1
			// https://developer.github.com/apps/building-oauth-apps/authorizing-oauth-apps/#1-request-a-users-github-identity
			try {
				if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
					try {
						Desktop.getDesktop().browse(new URI(doRequest));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

     		long start = System.currentTimeMillis();
			// 200 second timeout
			while (System.currentTimeMillis() - start < 200 * 1000 && returnData == null) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break;
				}

			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			server.stop();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return returnData;
	}

	@Override
	public String twoFactorAuthCodePrompt() {
		// TODO Auto-generated method stub
		return null;
	}

	public static Supplier<String> getMyAPI() {
		return myAPI;
	}

	public static void setMyAPI(Supplier<String> myAPI) {
		GitHubWebFlow.myAPI = myAPI;
	}

	public static Supplier<String> getName() {
		return mykey;
	}

	public static void setName(Supplier<String> mykey) {
		GitHubWebFlow.mykey = mykey;
	}

}
