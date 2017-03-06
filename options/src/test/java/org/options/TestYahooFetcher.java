package org.options;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test class for Yahoo fetcher.
 */
public class TestYahooFetcher {

  @Test
  public void testExpiryDates() {
    String json = loadJsonFile("src/test/resources/json/main.json");
    YahooFetcher yahooFetcher = new YahooFetcher(null);
    final List<Integer> expDates = yahooFetcher.fetchExpiryDates(toJsonObject(json));

    List<Integer> expectedDates = Arrays.asList(
      1485475200,
      1486080000,
      1486684800,
      1487289600,
      1487894400,
      1488499200,
      1489708800,
      1492732800,
      1497571200,
      1500595200,
      1508457600,
      1510876800,
      1516320000,
      1547769600);

    assertEquals("List not match", expectedDates, expDates);
  }

  @Test
  public void testCallPrices() {
    String json = loadJsonFile("src/test/resources/json/1485475200.json");
    YahooFetcher yahooFetcher = new YahooFetcher(null);
    JSONObject jsonObj = toJsonObject(json);
    final List<String> callPrices = yahooFetcher.fetchOptionPrices(jsonObj, "calls");

    assertEquals("List not match", "75.0,44.92,43.2,46.75,0,2,1.6875015625", callPrices.get(0));
    assertEquals("List not match", "135.0,0.01,0.0,0.0,0,30,0.2500075", callPrices.get(callPrices.size()-1));

    assertEquals("Mkt Time not match", "2017-01-24", yahooFetcher.getMarketDay(jsonObj));
  }

  @Test
  public void testPutPrices() {
    String json = loadJsonFile("src/test/resources/json/1485475200.json");
    YahooFetcher yahooFetcher = new YahooFetcher(null);
    JSONObject jsonObj = toJsonObject(json);
    final List<String> putPrices = yahooFetcher.fetchOptionPrices(jsonObj, "puts");

    assertEquals("List not match", "98.5,0.01,0.0,0.01,491,2,0.7656273437500001", putPrices.get(0));
    assertEquals("List not match", "133.0,13.4,11.3,14.6,1,1,1.302249582519531", putPrices.get(putPrices.size()-1));
  }

  @Test
  public void testUtcDates() {
    assertEquals("Dates mismatch", "2017-02-10", OptionsData.utcToDate(1486684800));
    assertEquals("Dates mismatch", "2017-01-27", OptionsData.utcToDate(1485475200));
    assertEquals("Dates mismatch", "2019-01-18", OptionsData.utcToDate(1547769600));
  }

  @Test
  public void testFilePath() {
    Path base = Paths.get("/test/options");
    final Path p = OptionsData.getFilePath(base, "calls", "AAPL", "2017-02-10", "2017-02-27");
    assertEquals("Path mismatch", Paths.get(base.toString(), "2017-02-27/2017-02-10/AAPL_calls.csv"), p);
    final Path p1 = OptionsData.getFilePath(base, "puts", "AAPL", "2017-02-10", "2017-02-27");
    assertEquals("Path mismatch", Paths.get(base.toString(), "2017-02-27/2017-02-10/AAPL_puts.csv"), p1);
  }

  @Test
  public void testOptionWrite() {
    String json = loadJsonFile("src/test/resources/json/1485475200.json");
    final Path p = OptionsData.getFilePath(Paths.get("data"), "calls", "AAPL", "2017-02-10", "2017-02-27");
    JSONObject jsonObj = toJsonObject(json);
//    OptionsData.writeOptionsFile(jsonObj, "AAPL", 1485475200, "calls");
    // TODO
  }

  private String loadJsonFile(String file) {
    try {
      byte[] data = Files.readAllBytes(Paths.get(file));
      return new String(data, Charset.defaultCharset());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private JSONObject toJsonObject(String data) {
    return new JSONObject(new JSONTokener(data));
  }
}
