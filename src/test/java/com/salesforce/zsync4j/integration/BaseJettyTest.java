/**
 * Copyright © 2015 salesforce.com, inc. All rights reserved.
 */
package com.salesforce.zsync4j.integration;

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
