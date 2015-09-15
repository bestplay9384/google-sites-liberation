package com.google.sites.liberation.imprt;

import com.google.gdata.client.sites.SitesService;
import com.google.gdata.data.sites.BaseContentEntry;
import com.google.gdata.util.ServiceException;

import java.io.IOException;
import java.net.URL;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Inserts individual entries to a feed.
 * 
 * @author bsimon@google.com (Benjamin Simon)
 */
final class EntryInserterImpl implements EntryInserter {

  private static final Logger LOGGER = LogManager.getLogger(
      EntryUpdaterImpl.class.getCanonicalName());
  
  @Override
  public BaseContentEntry<?> insertEntry(BaseContentEntry<?> entry, 
      URL feedUrl, SitesService sitesService) {
    try {
      return sitesService.insert(feedUrl, entry);
    } catch (IOException e) {
      LOGGER.warn("Unable to insert entry: " + entry, e);
      return null;
    } catch (ServiceException e) {
      LOGGER.warn("Unable to insert entry: " + entry, e);
      return null;
    }
  }
}
