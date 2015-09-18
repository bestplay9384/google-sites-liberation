/*
 * Copyright (C) 2015 Free Construction Sp. z.o.
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

import com.google.gdata.client.sites.SitesService;
import com.google.gdata.data.sites.SiteEntry;
import com.google.gdata.data.sites.SiteFeed;
import com.google.gdata.util.ServiceException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.sites.liberation.export.SiteExporter;
import com.google.sites.liberation.export.SiteExporterModule;
import com.google.sites.liberation.imprt.SiteImporter;
import com.google.sites.liberation.imprt.SiteImporterModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {
    private static final Logger LOGGER = LogManager.getLogger(Main.class);
    private String loggerError = null;
    private static Auth auth = new Auth();

    private String host = "sites.google.com";

    @Option(name="-t", usage="generated auth token")
    private String token = null;

    @Option(name="-d", usage="domain of site")
    private String domain = null;

    @Option(name="-w", usage="webspace of site")
    private String webspace = null;

    @Option(name="-p", usage="directory/path in which to export")
    private String path = null;

    @Option(name="-o", usage="choose option - export/import")
    private String option = "export";

    @Option(name="-r", usage="import revisions")
    private boolean revisions = false;

    @Option(name="-e", usage="Load Webspace list from external file")
    private String external = null;

    private String newlyCreatedPath = null;

    private void doMain(String[] args) {

        if(Lock.notRunning("lock.lock")) {
            CmdLineParser parser = new CmdLineParser(this);
            try {
                parser.parseArgument(args);
                auth.loadConfig(token);

                if (webspace == null && external == null) {
                    loggerError = "Webspace of site not specified!";
                    LOGGER.error(loggerError);
                    throw new CmdLineException(parser, loggerError);
                }

                if(webspace != null && external != null) {
                    LOGGER.warn("List of webspaces that are used comes from external file! These specified with -w are now being ignored!");
                }

                if(domain == null) {
                    loggerError = "Domain is not specified!";
                    LOGGER.error(loggerError);
                    throw new CmdLineException(parser, loggerError);
                }

                if(path == null) {
                    loggerError = "Directory is not specified!";
                    LOGGER.error(loggerError);
                    throw new CmdLineException(parser, loggerError);
                } else {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); Date date = new Date();
                    newlyCreatedPath = dateFormat.format(date);
                    path = path + System.getProperty("file.separator") + newlyCreatedPath + System.getProperty("file.separator");
                }

                LOGGER.info("Selected Mode: " + option.toUpperCase());
                System.out.println("\nSelected Mode: " + option.toUpperCase());

                SitesService sitesService = new SitesService("sites-liberation-5");
                sitesService.setOAuth2Credentials(auth.credential);
                List<String> webspaceList = prepareWebspaceList(host, domain, webspace, sitesService, external);

                if(webspaceList.size() == 0) {
                    LOGGER.info("Your account does not have permissions to view webspaces from selected domain!");
                    System.out.println("Your account does not have permissions to view webspaces from selected domain!");
                    System.exit(1);
                }

                for(String parseWebspace: webspaceList) {
                    if (option.equals("export")) {
                        Injector injector = Guice.createInjector(new SiteExporterModule());
                        SiteExporter siteExporter = injector.getInstance(SiteExporter.class);
                        siteExporter.exportSite(host, domain, parseWebspace, revisions, sitesService, Paths.get(path + parseWebspace).toFile(), new StdOutProgressListener());
                    } else {
                        Injector injector = Guice.createInjector(new SiteImporterModule());
                        SiteImporter siteImporter = injector.getInstance(SiteImporter.class);
                        siteImporter.importSite(host, domain, parseWebspace, revisions, sitesService, Paths.get(path + parseWebspace).toFile(), new StdOutProgressListener());
                    }
                }

            } catch (CmdLineException e) {
                parser.printUsage(System.err);
                return;
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }  catch (Exception ex) {
                ex.printStackTrace();
            }


        } else {
            LOGGER.debug("Application is already working! FIX: delete lock.lock file with location: " + Lock.lockFilePath());
            System.out.println("Application is already working. See the log file to see instructions how to fix problem.");
            System.exit(1);
        }

    }

    /**
     * Prepares webspace list to use when ALL from account are needed.
     */
    private List<String> prepareWebspaceList(String host, String domain, String webspace, SitesService sitesService, String external) {
        List<String> ret = new ArrayList<String>();

        if(external != null) {
            Path webspaceListFile = Paths.get(external);
            if(webspaceListFile.toFile().exists()) {
                Properties properties = new Properties();
                String webspaceHeap = null;
                try {
                    InputStream res =  new FileInputStream(webspaceListFile.toFile());
                    properties.load(res);
                    webspaceHeap = properties.getProperty("webspaces");
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                ret = Arrays.asList(webspaceHeap.split(";"));
            } else {
                throw new IllegalStateException("External file with this path does not exist!");
            }
        } else if(webspace.equals("ALL")) {
            URL urlToSee = UrlUtils.getUserWebspacesURL(host, domain);

            SiteFeed siteFeed = null;
            try {
                siteFeed = sitesService.getFeed(urlToSee, SiteFeed.class);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ServiceException e) {
                e.printStackTrace();
            }

            assert siteFeed != null;
            for (SiteEntry entry : siteFeed.getEntries()) {
                System.out.println(entry.getId() + " - " + entry.getSiteName().getValue() + " = " + entry.getSiteName());
                ret.add(entry.getSiteName().getValue());
            }

        } else {
            ret = Arrays.asList(webspace.split(","));
        }
        return ret;
    }

    /**
     * Loads main content of appliaction.
     */
    public static void main(String[] args) {
        new Main().doMain(args);
    }
}

