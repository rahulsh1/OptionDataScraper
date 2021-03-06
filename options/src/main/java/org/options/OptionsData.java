package org.options;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

  /**
   * Get path to the destination file
   * @param basePath root dir
   * @param optionType call/put
   * @param quote quote
   * @param currentDay day when record was fetched
   * @param expiryDate exp date of option
   * @return path to option file
   */
  static Path getFilePath(Path basePath, String optionType, String quote, String currentDay, String expiryDate) {
    final String deltaPath =
      String.format("%s%s%s%s%s_%s.csv", expiryDate, File.separator, currentDay, File.separator, quote, optionType);
    return basePath.resolve(deltaPath);
  }

  /**
   * Convert UTC to yyyy-mm-dd form.
   * @param utc UTC time
   * @return day in form yyyy-mm-dd
   */
  static String utcToDate(int utc) {
    return LocalDateTime.ofEpochSecond(utc, 0, ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE);
  }

  /**
   * Write to options file.
   *
   * @param optionData result data
   * @param optionPath path to write to
   */
  static void writeOptionsFile(List<String> optionData, Path optionPath) {
    try {
      final Path parent = optionPath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.write(optionPath, optionData, Charset.defaultCharset(), StandardOpenOption.CREATE);
    } catch (IOException e) {
      e.printStackTrace();
    }
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
