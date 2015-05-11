package com.salesforce.zsync4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import com.salesforce.zsync4j.internal.ControlFile;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

public class Zsync implements Callable<Path> {

  public static final String VERSION = "0.6.2";

  private final String url;
  private final Path seed;
  private final Path outputFile;
  private final OkHttpClient httpClient;

  public Zsync(String url, Path seed, Path outputFile, OkHttpClient httpClient) {
    super();
    this.url = url;
    this.seed = seed;
    this.outputFile = outputFile;
    this.httpClient = httpClient;
  }

  @Override
  public Path call() throws IOException {

    // 1. download zsync file
    // TODO -k cache zsync file locally + conditional get
    final Response response = httpClient.newCall(new Request.Builder().url(url).build()).execute();
    if (!response.isSuccessful())
      throw new IOException("http error code received: " + response.code());

    final ControlFile controlFile;
    try (InputStream in = response.body().byteStream()) {
      controlFile = ControlFile.read(in);
    }

    

    return outputFile;
  }
}
