<?php
include('common.php');
include('yahoo_fetcher.php');

if ($argc < 2) {
	echo("php get_options_data.php [Stock file] [Output Directory]\n");
	exit(0);
}

$stock_file	= $argv[1];
$output_dir	= $argv[2];

function getQuotes() {
  global $output_dir, $stock_file;

  if ($file = fopen($stock_file, "r")) {
    while(!feof($file)) {
      $line = fgets($file);
      // fetch option data for each quote
      debug("Fetch data for $line");
      fetchData(trim($line), $output_dir);
    }
    fclose($file);
  }
}

getQuotes();

?>