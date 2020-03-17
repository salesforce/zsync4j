/**
 * Copyright (c) 2015, Salesforce.com, Inc. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 * 
 * Neither the name of Salesforce.com nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.zsync.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.junit.After;
import org.junit.Before;

/**
 * Base class for integration tests that run against an embedded Jetty server.
 *
 * @author bstclair
 */
public class BaseJettyTest {

  private static final String WEB_ROOT = "/jetty";

  private Server server;
  private int port;
  private Path tempDirectory;

  @Before
  public void createTempDirectory() throws Exception {
    this.tempDirectory = Files.createTempDirectory(this.getClass().getName());
    this.tempDirectory.toFile().deleteOnExit();
  }

  @Before
  public void startJetty() throws Exception {
    this.server = new Server(0);
    ServletContextHandler servletContextHandler = new ServletContextHandler();
    servletContextHandler.setContextPath("/");
    servletContextHandler.setBaseResource(Resource.newClassPathResource(WEB_ROOT));
    ServletHolder defaultServletHolder = new ServletHolder("default", DefaultServlet.class);
    defaultServletHolder.setInitParameter("acceptRanges", "true");
    servletContextHandler.addServlet(defaultServletHolder, "/");
    this.server.setHandler(servletContextHandler);
    this.server.start();
    this.port = ((ServerConnector) this.server.getConnectors()[0]).getLocalPort();
  }

  @After
  public void stopJetty() throws Exception {
    this.server.stop();
  }

  protected String getUrlBase() {
    return "http://localhost:" + this.port + "/";
  }

  protected String makeUrl(String path) {
    String url = this.getUrlBase();
    if (path.startsWith("/")) {
      url += path.substring(1);
    } else {
      url += path;
    }
    return url;
  }

  protected Path getWebRoot() {
    return Paths.get(WEB_ROOT);
  }

  protected Path getTempDirectory() {
    return this.tempDirectory;
  }

  protected Path createTempFile(String suffix) throws IOException {
    Path tempFile = Files.createTempFile(this.getClass().getName(), suffix);
    tempFile.toFile().deleteOnExit();
    return tempFile;
  }
}
