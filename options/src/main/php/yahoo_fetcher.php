<?php
/**
* Yahoo fetcher.
*/

function fetchData($quote, $outputDir) {
  global $yhoo_main_url, $yhoo_exp_date_url;
  $url = sprintf($yhoo_main_url, $quote);
  $json = fetchJson($url);

  $expDateArr = fetchExpiryDates($json);
  $currDay = getMarketDay($json);

  foreach($expDateArr as $expDate) {
    $tmpPath = getFilePath($outputDir, "calls", $quote, $currDay, utcToDate($expDate));
    if (file_exists($tmpPath)) {
      debug("Data already downloaded for $expDate");
      continue;
    }

    $expDateUrl = sprintf($yhoo_exp_date_url, $quote, $expDate);
    debug("Got date as $expDate ");
    $expDateJson = fetchJson($expDateUrl);

    // Fetch the Call data
    $callData = fetchOptionPrices($expDateJson, "calls");
    $callPath = getFilePath($outputDir, "calls", $quote, $currDay, utcToDate($expDate));
    writeOptionsFile($callPath, $callData);

    // Fetch the Put data
    $putData = fetchOptionPrices($expDateJson, "puts");
    $putPath = getFilePath($outputDir, "puts", $quote, $currDay, utcToDate($expDate));
    writeOptionsFile($putPath, $putData);

    //sleep for 5 seconds
    sleep(5);
  }
}

/**
 * Fetch the expiry dates.
 * @param jsonData output converted to json object
 * @return List of expiry dates
 */
function fetchExpiryDates($json) {
  $expDates = array();
  foreach($json['optionChain']['result'][0]['expirationDates'] as $item) {
    array_push($expDates,  $item);
  }
  return $expDates;
}

/**
 * Get day when the results were obtained i.e quote for the day
 * @param jsonData json result object
 * @return date
 */
function getMarketDay($json) {
  $mktTime = $json['optionChain']['result'][0]['quote']['regularMarketTime'];
  return utcToDate($mktTime);
}

/**
 * Fetch the option prices.
 *
 * @param optionType call/put
 * @param json  json result object
 * @return data for each of the expiry date
 */
function fetchOptionPrices($json, $optionType) {
  $data = "";
  $optionObjects = $json['optionChain']['result'][0]['options'][0][$optionType];

  foreach($optionObjects as $option) {
    // Price,Last,Bid,Ask,Open Int,Volume,IV
    $data .= $option['strike']['raw'] . ",";
    $data .= $option['lastPrice']['raw'] . ",";
    $data .= $option['bid']['raw'] . ",";
    $data .= $option['ask']['raw'] . ",";
    $data .= $option['openInterest']['raw'] . ",";
    $data .= $option['volume']['raw'] . ",";
    $data .= $option['impliedVolatility']['raw'] . "\n";
  }
  return $data;
}
