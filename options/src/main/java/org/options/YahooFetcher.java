package org.options;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to fetch options data from Yahoo.
 */
final class YahooFetcher {

  private static final String MAIN_URL = "https://query1.finance.yahoo.com/v7/finance/options/%s?formatted=true"
      + "&lang=en-US&region=US&corsDomain=finance.yahoo.com";
  private static final String EXP_DATE_URL = "https://query1.finance.yahoo.com/v7/finance/options/%s?formatted=true"
      + "&lang=en-US&region=US&corsDomain=finance.yahoo.com&date=%s";
  private static final String CALLS = "calls";
  private static final String PUTS = "puts";

  private static final Logger LOGGER = LogManager.getLogger(YahooFetcher.class);
  private final Path outputDirectory;

  /**
   * Constructor with output dir.
   * @param destPath output dir to store the files.
   */
  YahooFetcher(Path destPath) {
    outputDirectory = destPath;
  }

  /**
   * Fetches data for a given quote.
   * Generates files of the form
   * output_dir/exp_date/day_of_data/quote_calls.csv
   * output_dir/exp_date/day_of_data/quote_puts.csv
   *
   * @param quote stock quote
   */
  void fetchData(final String quote) {
    final String url = String.format(MAIN_URL, quote);
    final StringBuffer buffer = OptionsData.fetchJsonData(url);
    if (buffer != null) {
      final JSONObject rootJson = new JSONObject(new JSONTokener(buffer.toString()));
      final List<Integer> expDates = fetchExpiryDates(rootJson);
      LOGGER.debug("Got expiry dates for {} - {}", quote, expDates);

      for (final Integer expDate : expDates) {
        // Check if data already downloaded - option file exists
        final Path optionFilePath = getFilePath(outputDirectory, CALLS, quote, getMarketDay(rootJson), utcToDate(expDate));
        if (optionFilePath.toFile().exists()) {
          continue;
        }

        final StringBuffer pageData = OptionsData.fetchJsonData(String.format(EXP_DATE_URL, quote, expDate));
        if (pageData != null) {
          final String jsonData = pageData.toString();
          final JSONObject json = new JSONObject(new JSONTokener(jsonData));

          // If data format changes, break out as this needs code changes.
          if (json.getJSONObject("optionChain") == null) {
            LOGGER.warn("Got invalid results from Site. Probably they changed the API output: {} {}", quote, jsonData);
            break;
          }
          // Write Call records
          writeOptionsFile(json, quote, expDate, CALLS);
          // Write Put records
          writeOptionsFile(json, quote, expDate, PUTS);
          LOGGER.debug("Downloaded data for {} for {}", quote, utcToDate(expDate));
        }

        // Be nice and dont overload the server.
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          LOGGER.debug("Thread interrupted !!");
        }
      }
    }
  }

  /**
   * Fetch the expiry dates.
   * @param jsonData output converted to json object
   * @return List of expiry dates
   */
  List<Integer> fetchExpiryDates(final JSONObject jsonData) {
    final List<Integer> integerList = new ArrayList<>();
    final JSONArray expiryDates = jsonData.getJSONObject("optionChain").getJSONArray("result")
        .getJSONObject(0).getJSONArray("expirationDates");
    for (int i = 0; i < expiryDates.length(); i++) {
      integerList.add((Integer)expiryDates.get(i));
    }
    return integerList;
  }

  /**
   * Get day when the results were obtained i.e quote for the day
   * @param jsonData json result object
   * @return date
   */
  String getMarketDay(final JSONObject jsonData) {
    final Integer mktTime = (Integer) jsonData.getJSONObject("optionChain").getJSONArray("result")
      .getJSONObject(0).getJSONObject("quote").get("regularMarketTime");
    return utcToDate(mktTime);
  }

  /**
   * Fetch the option prices.
   * @param optionType call/put
   * @param json  json result object
   * @return data for each of the expiry date
   */
  List<String> fetchOptionPrices(final String optionType, final JSONObject json) {
    final List<String> recordList = new ArrayList<>();
    final JSONArray callObjects = json.getJSONObject("optionChain").getJSONArray("result")
      .getJSONObject(0).getJSONArray("options").getJSONObject(0).getJSONArray(optionType);

    final StringBuilder buffer = new StringBuilder();
    for (int i = 0; i < callObjects.length(); i++) {
      final JSONObject callPriceObj = callObjects.getJSONObject(i);

      // Price,Last,Bid,Ask,Open Int,Volume,IV
      buffer.append(callPriceObj.getJSONObject("strike").get("raw")).append(',');
      buffer.append(callPriceObj.getJSONObject("lastPrice").get("raw")).append(',');
      buffer.append(callPriceObj.getJSONObject("bid").get("raw")).append(',');
      buffer.append(callPriceObj.getJSONObject("ask").get("raw")).append(',');
      buffer.append(callPriceObj.getJSONObject("openInterest").get("raw")).append(',');
      buffer.append(callPriceObj.getJSONObject("volume").get("raw")).append(',');
      buffer.append(callPriceObj.getJSONObject("impliedVolatility").get("raw"));
      recordList.add(buffer.toString());
      buffer.delete(0, buffer.length());
    }
    return recordList;
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
   * @param json result json object
   * @param quote stock quote
   * @param expDate expiry date
   * @param optionType call/put
   */
  void writeOptionsFile(final JSONObject json, final String quote, final Integer expDate, final String optionType) {
    final String currDay = getMarketDay(json);
    final List<String> optionData = fetchOptionPrices(optionType, json);
    final Path optionPath = getFilePath(outputDirectory, optionType, quote, currDay, utcToDate(expDate));
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
}
