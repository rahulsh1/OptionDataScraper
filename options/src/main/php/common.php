<?php

$yhoo_main_url =  "https://query1.finance.yahoo.com/v7/finance/options/%s?formatted=true&lang=en-US&region=US&corsDomain=finance.yahoo.com";
$yhoo_exp_date_url = "https://query1.finance.yahoo.com/v7/finance/options/%s?formatted=true&lang=en-US&region=US&corsDomain=finance.yahoo.com&date=%s";

$DEBUG=1;

function debug($msg)
{
  global $DEBUG;
  if ($DEBUG == 1)
  echo $msg. "\n";
}

function fetchJson($url) {
  $html = file_get_contents($url);
  if (!isset($html)) {
    $json = "Error fetching data from URL: $url\n";
  } else {
    $json = json_decode($html, true);
  }
  return $json;
}

/**
 * Convert UTC to yyyy-mm-dd form.
 * @param utc UTC time
 * @return day in form yyyy-mm-dd
 */
function utcToDate($utc) {
  return gmdate("Y-m-d", $utc);
}

function writeOptionsFile($fileName, $contents) {
  $pos = strrpos($fileName, '/');
  $dir = substr($fileName, 0, $pos);
  if (!file_exists($dir) && !is_dir($dir)) {
    mkdir($dir, 0777, true);
  }
  file_put_contents($fileName, $contents);
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
function getFilePath($basePath, $optionType, $quote, $currentDay, $expiryDate) {
  $deltaPath = sprintf("%s/%s/%s_%s.csv", $expiryDate, $currentDay, $quote, $optionType);
  return $basePath . "/" . $deltaPath;
}

?>