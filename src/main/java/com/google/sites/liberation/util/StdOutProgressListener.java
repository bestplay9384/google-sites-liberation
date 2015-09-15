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
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Implementation of ProgressListener that uses standard out to report the
 * progress of an operation.
 * 
 * @author bsimon@google.com (Benjamin Simon)
 */
public class StdOutProgressListener implements ProgressListener {

  private static final Logger LOGGER = LogManager.getLogger(StdOutProgressListener.class.getCanonicalName());
  private double progress;
  private String status;
  
  @Override
  public void setProgress(double progress) {
    this.progress = progress;
    String progressString = "Current progress: " + (int)(progress*100) + "%.";
    LOGGER.info(progressString);
    System.out.println(progressString);
  }

  @Override
  public void setStatus(String status) {
    this.status = status;
    System.out.println(status);
    LOGGER.info(status);
  }

  @Override
  public double getProgress() {
    return progress;
  }

  @Override
  public String getStatus() {
    return status;
  }
}
