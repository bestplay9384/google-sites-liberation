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

package com.google.sites.liberation.export;

import com.google.gdata.client.sites.SitesService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.sites.liberation.imprt.SiteImporter;
import com.google.sites.liberation.imprt.SiteImporterModule;
import com.google.sites.liberation.util.Auth;
import com.google.sites.liberation.util.StdOutProgressListener;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Processes command line arguments for exporting a site and then
 * calls SiteExporter accordingly.
 * 
 * @author bsimon@google.com (Benjamin Simon)
 */
public class Main {
  //private static final Logger LOGGER = Logger.getLogger(Main.class.getCanonicalName());

  private static Auth auth = new Auth();

  private boolean exportRevisions = false;
  private String host = "sites.google.com";

  @Option(name="-t", usage="generated auth token")
  private String token = null;

  @Option(name="-d", usage="domain of site")
  private String domain = null;
  
  @Option(name="-w", usage="webspace of site")
  private String webspace = null;

  @Option(name="-p", usage="directory/path in which to export")
  private String path = null;

  @Option(name="-opt", usage="choose option - export/import (default export)")
  private String option = "export";

  private void doMain(String[] args) {
    CmdLineParser parser = new CmdLineParser(this);
    //System.out.println( new File(".").getAbsolutePath());

    try {
      parser.parseArgument(args);
      auth.loadConfig(token);


      if (webspace == null)
        throw new CmdLineException("Webspace of site not specified!");

      if(domain == null)
        throw new CmdLineException("Domain is not specified!");

      if(path == null) {
        throw new CmdLineException("Directory is not specified!");
      }
      else {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss"); Date date = new Date();
        path = path + "\\" + dateFormat.format(date);
      }

      System.out.println("Selected Mode: " + option.toUpperCase());

      SitesService sitesService = new SitesService("sites-liberation-5");
      sitesService.setOAuth2Credentials(auth.credential);

      if(option.equals("export")) {
          Injector injector = Guice.createInjector(new SiteExporterModule());
          SiteExporter siteExporter = injector.getInstance(SiteExporter.class);
          siteExporter.exportSite(host, domain, webspace, exportRevisions, sitesService, new File(path), new StdOutProgressListener());
      }

      if(option.equals("import")) {
          Injector injector = Guice.createInjector(new SiteImporterModule());
          SiteImporter siteImporter = injector.getInstance(SiteImporter.class);
          siteImporter.importSite(host, domain, webspace, exportRevisions, sitesService, new File(path), new StdOutProgressListener());
      }

    } catch (CmdLineException e) {
      parser.printUsage(System.err);
      return;
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
  
  /**
   * Exports a Site.
   */
  public static void main(String[] args) {
    new Main().doMain(args);
  }
}
