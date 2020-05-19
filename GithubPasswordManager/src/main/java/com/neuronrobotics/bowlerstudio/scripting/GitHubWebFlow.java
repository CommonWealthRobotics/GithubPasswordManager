package com.neuronrobotics.bowlerstudio.scripting;

import java.awt.Desktop;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import GithubPasswordManager.APIProvider;

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
	private static Supplier<String> myname =() -> {
		JFrame jframe = new JFrame();
		String answer = JOptionPane.showInputDialog(jframe, "Enter API secret");
		jframe.dispose();
		return answer;
	};
	String state ="";
	@SuppressWarnings("serial")
	@Override
	public String[] prompt(String loginID) {
		if(loginID ==null) {
			JFrame jframe = new JFrame();
			loginID = JOptionPane.showInputDialog(jframe, "Github User Name ");
			jframe.dispose();
			//loginID="madhephaestus";
		}
		String id = loginID;
		Server server = new Server(WEBSERVER_PORT);
		
		try {
			
			returnData = null;
			ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
			context.addServlet(new ServletHolder(new HttpServlet() {
				/**
				 * 
				 */
				private static final long serialVersionUID = 8089806363114431858L;

				@Override
				protected void doGet(HttpServletRequest request, HttpServletResponse response)
						throws ServletException, IOException {
					try {
						final String code = request.getParameter("code");
						final String tok = request.getParameter("access_token");
						if (tok != null) {
							response.setStatus(HttpServletResponse.SC_OK);
							returnData = new String[] { id, tok };
						}
						if(code !=null) {
							response.setStatus(HttpServletResponse.SC_OK);
							runStep2(id, code);
							
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
	
			server.setHandler(context);
			server.setStopAtShutdown(true);
			try {
				server.start();
				//server.join();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			doStepOne(id);

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
	private void doStepOne(String id) {
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
		// User interaction is needed to approve the authorization
		// Open this URL in a desktop browser
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
	}
	private void runStep2(String id, final String code) {
		// Now perform step 2
		// https://developer.github.com/apps/building-oauth-apps/authorizing-oauth-apps/#2-users-are-redirected-back-to-your-site-by-github
		/*
		 * Create the POST request
		 */
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost("https://github.com/login/oauth/access_token");
		// Request parameters and other properties.
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("client_id",getMyAPI().get()));
		params.add(new BasicNameValuePair("client_secret", mykey.get()));
		params.add(new BasicNameValuePair("code",code));
		try {
		    httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
		    // writing error to Log
		    e.printStackTrace();
		}
		/*
		 * Execute the HTTP Request
		 */
		try {
		    HttpResponse response2 = httpClient.execute(httpPost);
		    HttpEntity respEntity = response2.getEntity();

		    if (respEntity != null) {
		        // EntityUtils to get the response content
		        String[] content =  EntityUtils.toString(respEntity).split("&");
		        if(content!=null && content.length>0) {
		        	String [] keys = content[0].split("=");
		        	if(keys!=null && keys.length>1) {
		        		String string = keys[1];
						//System.out.println("Key = "+string);
		        		returnData= new String[] {id,string};
		        	}
		        }
		        
		    }
		} catch (ClientProtocolException e) {
		    // writing exception to log
		    e.printStackTrace();
		} catch (IOException e) {
		    // writing exception to log
		    e.printStackTrace();
		}
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
		return myname;
	}

	public static void setName(Supplier<String> mykey) {
		GitHubWebFlow.myname = mykey;
	}

}
