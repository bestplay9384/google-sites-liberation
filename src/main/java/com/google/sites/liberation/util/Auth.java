package com.google.sites.liberation.util;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author bestplay9@me.com
 */
public class Auth {
    private static final Logger LOGGER = LogManager.getLogger(Auth.class);
    private String loggerError = null;
    private List<String> SCOPES = Arrays.asList("https://sites.google.com/feeds");
    private static String CLIENT_ID = null;
    private static String CLIENT_SECRET = null;
    private static String REDIRECT_URI = null;
    private String REFRESH_TOKEN = null;
    public Credential credential = null;

    private URL TOKEN_PATH = getClass().getClassLoader().getResource("token.properties");
    private URL CONFIG_PATH = getClass().getClassLoader().getResource("config.properties");

    /**
     * Method loads all important variables from config file and token file.
     * It also generates missing files by token given by user - refresh session
     */
    public void loadConfig(String auth_token) throws Exception {
        File f1 = null;
        try {
            f1 = new File(CONFIG_PATH.toURI());
        } catch (Exception e) {
            loggerError = "Configuration file does not exist! (config.properties)";
            LOGGER.warn(loggerError);
            System.out.println(loggerError);
            System.exit(1);
        }

        if(f1.exists() && !f1.isDirectory()) {
            Properties properties = new Properties();
            try {
                InputStream res =  new FileInputStream(f1);
                properties.load(res);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            REDIRECT_URI = properties.getProperty("redirect_url");
            CLIENT_ID = properties.getProperty("client_id");
            CLIENT_SECRET = properties.getProperty("client_secret");
        }

        File f = null;
        try {
            f = new File(TOKEN_PATH.toURI());
        } catch (Exception e) {
            if(auth_token != null) {
                refreshSession(auth_token);
            } else {
                LOGGER.warn("No token file found. Authorize Google App first (url: http://tiny.cc/fcauth), then rerun app with option: -t <TOKEN>");
                System.out.println("http://tiny.cc/fcauth\n");
                System.out.println("alternatively: " + generateAuthUrl() + "\n");
                System.out.println("Open browser and paste url given above, let google application access your Google Sites and copy generated token.");
                System.out.println("Then reopen app with command: APP -t <PASTED_TOKEN>\n");
                System.exit(1);
            }
        }

        if(f.exists() && !f.isDirectory()) {
            Properties token_properties = new Properties();
            try {
                InputStream res =  new FileInputStream(f);
                token_properties.load(res);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            REFRESH_TOKEN = token_properties.getProperty("refresh_token");
            credential = refreshAccessToken();
        }

    }

    /**
     * Method gets directory path of working jar-with-depencencies.
     */
    @Deprecated
    private String getPathOfJar() {
        File f = new File(System.getProperty("java.class.path"));
        File dir = f.getAbsoluteFile().getParentFile();
        return dir.toString();
    }

    /**
     * Method refreshes session of user that have alredy given permissions to google App.
     */
    public void refreshSession(String accessToken) throws IOException {
        credential = getCredentials(accessToken);
        REFRESH_TOKEN = credential.getRefreshToken();
        saveRefreshToken();
    }

    public String getClassPath() throws UnsupportedEncodingException {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        URL[] urls = ((URLClassLoader)cl).getURLs();
        return URLDecoder.decode(urls[1].getFile().substring(1), "UTF-8");
    }

    /**
     * Method saves new refresh token to external file - token config file.
     */
    public void saveRefreshToken() {
        try {
            Properties props = new Properties();
            props.setProperty("refresh_token", REFRESH_TOKEN);
            File f = new File(getClassPath() + "token.properties");
            OutputStream out = new FileOutputStream(f);
            props.store(out, "File contains sensitive token which automatically log in user.");
            LOGGER.info("Token file has been successfully saved! Rerun app without -t parameter.");
            System.out.println("Token file has been successfully saved! Now try to use App normally! (without -t parameter)");
            System.exit(1);
        }
        catch (Exception e ) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Method opens browser on google App API authentication website.
     * Allows user to give privileges to Google App.
     * ONLY FOR GUI!
     */
    public void openBrowserAndGetToken() {
        try {
            java.awt.Desktop.getDesktop().browse(new URI(generateAuthUrl()));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method generates google api authorization url
     */
    public String generateAuthUrl() {
        LOGGER.debug("Google API App Authorization URL was generated!");
        return new GoogleAuthorizationCodeRequestUrl(CLIENT_ID, REDIRECT_URI, SCOPES).setAccessType("offline").setApprovalPrompt("force").build();
    }

    /**
     * Get new credentials for access Token given by user
     */
    public Credential getCredentials(String accessToken) throws IOException {
        HttpTransport transport = new NetHttpTransport();
        JacksonFactory jsonFactory = new JacksonFactory();
        GoogleTokenResponse response = new GoogleAuthorizationCodeTokenRequest(transport, jsonFactory, CLIENT_ID, CLIENT_SECRET, accessToken, REDIRECT_URI).execute();
        LOGGER.debug("New authorization data has been downloaded - accessToken: " + response.getAccessToken() + ", refreshToken: " + response.getRefreshToken());
        return new GoogleCredential.Builder()
                .setClientSecrets(CLIENT_ID, CLIENT_SECRET)
                .setJsonFactory(jsonFactory).setTransport(transport).build()
                .setAccessToken(response.getAccessToken())
                .setRefreshToken(response.getRefreshToken());
    }

    /**
     * Method refreshes access token by given REFRESH_TOKEN
     */
    public Credential refreshAccessToken() throws IOException {
        HttpTransport transport = new NetHttpTransport();
        JacksonFactory jsonFactory = new JacksonFactory();
        TokenResponse response = new TokenResponse();

        try {
            response = new GoogleRefreshTokenRequest(transport, jsonFactory, REFRESH_TOKEN, CLIENT_ID, CLIENT_SECRET).execute();
            LOGGER.debug("New accessToken (" + response.getAccessToken() + ") has been downloaded (by refreshToken in token file) with Google API Authorization!");
        } catch (TokenResponseException e) {
            LOGGER.warn("Downloaded accessToken is empty! Please check provided token (propriety of -t parameter)");
            if (e.getDetails() != null) {
                System.err.println("Error: " + e.getDetails().getError());
                if (e.getDetails().getErrorDescription() != null) {
                    System.err.println(e.getDetails().getErrorDescription());
                }
                if (e.getDetails().getErrorUri() != null) {
                    System.err.println(e.getDetails().getErrorUri());
                }
            } else {
                System.err.println(e.getMessage());
            }
        }

        return new GoogleCredential.Builder()
                .setClientSecrets(CLIENT_ID, CLIENT_SECRET)
                .setJsonFactory(jsonFactory).setTransport(transport).build()
                .setAccessToken(response.getAccessToken());

    }
}
