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
import java.util.Random;

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
  private final Random random;

  /**
   * Constructor with output dir.
   * @param destPath output dir to store the files.
   */
  YahooFetcher(Path destPath) {
    outputDirectory = destPath;
    random = new Random();
  }

  /**
   * Fetches data for a given quote.
   * Generates files of the form
   * output_dir/exp_date/day_of_data/quote_calls.csv
   * output_dir/exp_date/day_of_data/quote_puts.csv
   *
   * @param quote stock quote
   */
  public void fetchData(final String quote) {
    final String url = String.format(MAIN_URL, quote);
    final StringBuffer buffer = OptionsData.fetchJsonData(url);
    if (buffer != null) {
      final JSONObject rootJson = new JSONObject(new JSONTokener(buffer.toString()));
      final List<Integer> expDates = fetchExpiryDates(rootJson);
      LOGGER.debug("Got expiry dates for {} - {}", quote, expDates);

      for (final Integer expDate : expDates) {
        // Check if data already downloaded - option file exists
        final String expDateStr = OptionsData.utcToDate(expDate);
        final String currDay = getMarketDay(rootJson);

        final Path optionFilePath = OptionsData.getFilePath(outputDirectory, CALLS, quote, currDay, expDateStr);
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

          // Fetch the Call data
          final List<String> callData = fetchOptionPrices(json, CALLS);
          final Path callPath = OptionsData.getFilePath(outputDirectory, CALLS, quote, currDay, expDateStr);
          OptionsData.writeOptionsFile(callData, callPath);

          // Fetch the Put data
          final List<String> putData = fetchOptionPrices(json, PUTS);
          final Path putPath = OptionsData.getFilePath(outputDirectory, PUTS, quote, currDay, expDateStr);
          OptionsData.writeOptionsFile(putData, putPath);

          LOGGER.debug("Downloaded data for {} for {}", quote, OptionsData.utcToDate(expDate));
        }

        // Be nice and dont overload the server.
        try {
          Thread.sleep(random.nextInt(3000) + 2000);
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
    return OptionsData.utcToDate(mktTime);
  }

  /**
   * Fetch the option prices.
   * @param json  json result object
   * @param optionType call/put
   * @return data for each of the expiry date
   */
  List<String> fetchOptionPrices(final JSONObject json, final String optionType) {
    final List<String> recordList = new ArrayList<>();
    final JSONArray optionObjects = json.getJSONObject("optionChain").getJSONArray("result")
      .getJSONObject(0).getJSONArray("options").getJSONObject(0).getJSONArray(optionType);

    final StringBuilder buffer = new StringBuilder();
    for (int i = 0; i < optionObjects.length(); i++) {
      final JSONObject optionPriceObj = optionObjects.getJSONObject(i);

      // Price,Last,Bid,Ask,Open Int,Volume,IV
      buffer.append(optionPriceObj.getJSONObject("strike").get("raw")).append(',');
      buffer.append(optionPriceObj.getJSONObject("lastPrice").get("raw")).append(',');
      buffer.append(optionPriceObj.getJSONObject("bid").get("raw")).append(',');
      buffer.append(optionPriceObj.getJSONObject("ask").get("raw")).append(',');
      buffer.append(optionPriceObj.getJSONObject("openInterest").get("raw")).append(',');
      buffer.append(optionPriceObj.getJSONObject("volume").get("raw")).append(',');
      buffer.append(optionPriceObj.getJSONObject("impliedVolatility").get("raw"));
      recordList.add(buffer.toString());
      buffer.delete(0, buffer.length());
    }
    return recordList;
  }

}
