package org.options;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main class to fetch the data.
 */
public final class OptionsData {

  private static final Logger LOGGER = LogManager.getLogger(OptionsData.class);

  /**
   * Fetch Json from a given URL
   * @param requestUrl input URL
   * @return json data if 200 OK
   */
  static StringBuffer fetchJsonData(String requestUrl) {
    StringBuffer buffer = null;
    try {
      final URL url = new URL(requestUrl);
      final HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
      httpConn.setRequestMethod("GET");
      httpConn.setRequestProperty("Accept", "application/json");

      if (httpConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
        final BufferedReader br = new BufferedReader(new InputStreamReader(httpConn.getInputStream(), Charset.defaultCharset()));
        String output;
        final StringWriter writer = new StringWriter();
        while ((output = br.readLine()) != null) {
          writer.write(output);
        }
        writer.close();
        httpConn.disconnect();
        buffer = writer.getBuffer();
      } else {
        LOGGER.warn("Error: Got response code as {}", httpConn.getResponseCode());
      }
    } catch (IOException e) {
      LOGGER.warn("Failed to get data from URL:{} {}", requestUrl, e.getMessage());
      LOGGER.warn(e);
    }
    return buffer;
  }

  public static void main(String[] args) throws IOException {
    if (args.length == 2) {
      System.out.println("Started");
      final YahooFetcher fetcher = new YahooFetcher(Paths.get(args[1]));
      final List<String> quotes = Files.readAllLines(Paths.get(args[0]));
      for (final String quote : quotes) {
        System.out.println(quote + "...");
        LOGGER.debug("--> Retrieving data for {}", quote);
        fetcher.fetchData(quote.trim());
        LOGGER.debug("--> Done Retrieving data for {}", quote);
      }
      System.out.println("Done");
    } else {
      System.out.println("java OptionsData <quote_file> <output_dir>");
    }
  }
}
