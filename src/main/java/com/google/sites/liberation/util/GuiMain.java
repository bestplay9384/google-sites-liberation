/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.sites.liberation.util;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.gdata.client.sites.SitesService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.sites.liberation.export.SiteExporter;
import com.google.sites.liberation.export.SiteExporterModule;
import com.google.sites.liberation.imprt.SiteImporter;
import com.google.sites.liberation.imprt.SiteImporterModule;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a GUI for initiating a Sites import or export.
 * 
 * @author bsimon@google.com (Benjamin Simon)
 */
public class GuiMain {

  private static final Logger LOGGER = Logger.getLogger(GuiMain.class.getCanonicalName());

  private List<String> SCOPES = Arrays.asList("https://sites.google.com/feeds");
  private String CLIENT_ID = "448508769873-or0oc515d6soeil0rpb3q9hg4hl99uek.apps.googleusercontent.com";
  private String CLIENT_SECRET = "HZuOlJcNEGFDDcT6n9oWayJI";
  private String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";
  private String REFRESH_TOKEN = "1/zVjvf9yULDNmvxaG6KZbq3sXPsfP0I7diqbbXx7RdkY";
  private Credential credential = null;
  //private String authToken = null;
  //private String authTokenRefresh = null;
  private JFrame optionsFrame;
  private JFrame progressFrame;
  //private JTextField hostField;
  private JTextField domainField;
  private JTextField webspaceField;
  private JTextField tokenField;
  private JTextField fileField;
  private JFileChooser fileChooser;
  private JCheckBox revisionsCheckBox;
  private JTextArea textArea;
  private JProgressBar progressBar;
  private JButton doneButton;

  private GuiMain() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ClassNotFoundException e) {
      LOGGER.log(Level.WARNING, "Unable to set look and feel.", e);
    } catch (InstantiationException e) {
      LOGGER.log(Level.WARNING, "Unable to set look and feel.", e);
    } catch (IllegalAccessException e) {
      LOGGER.log(Level.WARNING, "Unable to set look and feel.", e);
    } catch (UnsupportedLookAndFeelException e) {
      LOGGER.log(Level.WARNING, "Unable to set look and feel.", e);
    }
    initOptionsFrame();
    initProgressFrame();
  }

  private void initOptionsFrame() {
    optionsFrame = new JFrame("Sites Import/Export");
    JPanel mainPanel = new JPanel();
    GridLayout layout = new GridLayout(0, 2);
    mainPanel.setLayout(layout);
    //mainPanel.add(new JLabel(" Host: "));
    //hostField = new JTextField("sites.google.com");
    //mainPanel.add(hostField);
    mainPanel.add(new JLabel(" Domain: "));
    domainField = new JTextField();
    mainPanel.add(domainField);
    mainPanel.add(new JLabel(" Webspace: "));
    webspaceField = new JTextField();
    mainPanel.add(webspaceField);
    //mainPanel.add(new JLabel(" Import/Export Revisions: "));
    revisionsCheckBox = new JCheckBox();
    //mainPanel.add(revisionsCheckBox);
    fileField = new JTextField();
    fileField.setEditable(false);
    fileChooser = new JFileChooser();
    JButton directoryButton = new JButton("Choose Target Directory");
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

    directoryButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int result = fileChooser.showOpenDialog(optionsFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
          DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss"); Date date = new Date();
          fileChooser.setSelectedFile(new File(fileChooser.getSelectedFile().getPath() + "\\" + dateFormat.format(date)));
          fileField.setText(fileChooser.getSelectedFile().getPath());
        }
      }
    });

    mainPanel.add(directoryButton);
    mainPanel.add(fileField);

    JButton openBrowserButton = new JButton("Get a token from browser");
    openBrowserButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        openBrowserAndGetToken();
      }
    });
    //mainPanel.add(openBrowserButton);
    tokenField = new JTextField();
    //mainPanel.add(tokenField);

    mainPanel.add(new JPanel());
    mainPanel.add(new JPanel());
    JButton importButton = new JButton("Import to Sites");
    importButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (checkArguments()) {
          startAction(false);
        }
      }
    });
    mainPanel.add(importButton);
    JButton exportButton = new JButton("Export from Sites");
    exportButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (checkArguments()) {
          startAction(true);
        }
      }
    });
    mainPanel.add(exportButton);
    mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
    optionsFrame.getContentPane().add(mainPanel);
    optionsFrame.pack();
    optionsFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    optionsFrame.setVisible(true);
  }

  private void initProgressFrame() {
    progressFrame = new JFrame("Progress");
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BorderLayout());
    progressBar = new JProgressBar();
    progressBar.setMinimum(0);
    progressBar.setMaximum(100);
    progressBar.setPreferredSize(new Dimension(500, 25));
    JPanel progressPanel = new JPanel();
    progressPanel.add(progressBar);
    progressPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
    mainPanel.add(progressPanel, BorderLayout.NORTH);
    textArea = new JTextArea();
    textArea.setRows(20);
    textArea.setEditable(false);
    JScrollPane scrollPane = new JScrollPane(textArea);
    scrollPane.setBorder(new EmptyBorder(10, 10, 10, 10));
    mainPanel.add(scrollPane, BorderLayout.CENTER);
    doneButton = new JButton("Done");
    doneButton.setPreferredSize(new Dimension(495, 25));
    doneButton.setEnabled(false);
    doneButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doneButton.setEnabled(false);
        progressFrame.setVisible(false);
        optionsFrame.setVisible(true);
      }
    });
    JPanel donePanel = new JPanel();
    donePanel.setLayout(new BorderLayout());
    donePanel.add(doneButton, BorderLayout.CENTER);
    donePanel.setBorder(new EmptyBorder(0, 10, 10, 10));
    mainPanel.add(donePanel, BorderLayout.SOUTH);
    progressFrame.getContentPane().add(mainPanel);
    progressFrame.pack();
    progressFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }

  private String generateAuthUrl() {
    GoogleAuthorizationCodeRequestUrl x = new GoogleAuthorizationCodeRequestUrl(CLIENT_ID, REDIRECT_URI, SCOPES).setAccessType("offline").setApprovalPrompt("force");
    return x.build();
  }

  private void openBrowserAndGetToken() {
    try {
      java.awt.Desktop.getDesktop().browse(new URI(generateAuthUrl()));
    } catch (URISyntaxException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Retrieve OAuth 2.0 credentials.
   * 
   * @return OAuth 2.0 Credential instance.
   * @throws IOException
   */
  /*private Credential getCredentials() throws IOException {
    String code = tokenField.getText();
    HttpTransport transport = new NetHttpTransport();
    JacksonFactory jsonFactory = new JacksonFactory();
    GoogleTokenResponse response = new GoogleAuthorizationCodeTokenRequest(transport, jsonFactory, CLIENT_ID, CLIENT_SECRET, code, REDIRECT_URI).execute();

    return new GoogleCredential.Builder()
        .setClientSecrets(CLIENT_ID, CLIENT_SECRET)
        .setJsonFactory(jsonFactory).setTransport(transport).build()
        .setAccessToken(response.getAccessToken())
        .setRefreshToken(response.getRefreshToken());
  }*/

  private Credential refreshAccessToken() throws IOException {
      HttpTransport transport = new NetHttpTransport();
      JacksonFactory jsonFactory = new JacksonFactory();
      TokenResponse response = new TokenResponse();

    try {
      response = new GoogleRefreshTokenRequest(transport, jsonFactory, REFRESH_TOKEN, CLIENT_ID, CLIENT_SECRET).execute();
    } catch (TokenResponseException e) {
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

  private void startAction(boolean export) {
    optionsFrame.setVisible(false);
    progressBar.setValue(0);
    progressBar.setIndeterminate(true);
    textArea.setText("");
    progressFrame.setVisible(true);
    new Thread(new ImportExportRunnable(export)).start();
  }

  private boolean checkArguments() {
    /*if (hostField.getText().equals("")) {
      error("Please provide a host name.");
      return false;
    }*/
    if (webspaceField.getText().equals("")) {
      error("Please provide a webspace (site name).");
      return false;
    }
    /*if (tokenField.getText().equals("")) {
      error("Please provide a token.");
      return false;
    }*/
    if (fileChooser.getSelectedFile() == null) {
      error("Please provide a target directory.");
      return false;
    }
    try {
      if (credential == null) {
        //credential = getCredentials();
        credential = refreshAccessToken();
      }


      /*String accessToken = credential.getAccessToken();
      System.out.println("Access token before: " + accessToken);
      credential.refreshToken();
      accessToken = credential.getAccessToken();
      System.out.println("Access token after: " + accessToken);*/


    } catch (TokenResponseException e) {
      error("The token is invalid!");
      return false;
    } catch (IOException e) {
      error(e.toString());
      return false;
    }
    return true;
  }

  private void error(String message) {
    JOptionPane.showMessageDialog(optionsFrame, message, "Error",
        JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Launches a new GuiMain, allowing a user to graphically initiate a Sites
   * import or export.
   */
  public static void main(String[] args) {
    new GuiMain();
  }

  private class ImportExportRunnable implements Runnable {

    private boolean export;

    ImportExportRunnable(boolean export) {
      this.export = export;
    }

    @Override
    public void run() {
      //String host = hostField.getText();
      String host = "sites.google.com";
      String domain = (domainField.getText().equals("")) ? null : domainField.getText();
      String webspace = webspaceField.getText();
      boolean revisions = revisionsCheckBox.isSelected();
      File directory = fileChooser.getSelectedFile();
      String applicationName = "sites-liberation-5";
      SitesService sitesService = new SitesService(applicationName);
      sitesService.setOAuth2Credentials(credential);
      if (export) {
        Injector injector = Guice.createInjector(new SiteExporterModule());
        SiteExporter siteExporter = injector.getInstance(SiteExporter.class);
        siteExporter.exportSite(host, domain, webspace, revisions,
            sitesService, directory, new GuiProgressListener(progressBar, textArea));
      } else {
        Injector injector = Guice.createInjector(new SiteImporterModule());
        SiteImporter siteImporter = injector.getInstance(SiteImporter.class);
        siteImporter.importSite(host, domain, webspace, revisions,
                sitesService, directory, new GuiProgressListener(progressBar, textArea));
      }
      doneButton.setEnabled(true);
    }

  }
}
