# options data 

Fetches options data from Yahoo

[![Build Status](https://travis-ci.org/rahulsh1/OptionDataScraper.svg?branch=master)](https://travis-ci.org/rahulsh1/OptionDataScraper)

### Pre-requisites
- JDK 1.8
- Maven 3.x

### Build
Download all sources and build with maven. Maven will download the correct dependencies.

    $ git clone https://github.com/rahulsh1/OptionDataScraper.git
    $ cd options
    $ mvn install

## Stock List
Add stock quotes to a file separated by new line.

    $ cat stock_list.txt
    GOOG
    AAPL
    CSCO
    
### Run

    $ java -jar ./target/optionsdata-1.0-with-dependencies.jar $PWD/../stock_list.txt $PWD/data

The results are stored in the `data` directory.

### Output
  
     $ tree data
     ├── 2017-02-24
     │   ├── 2017-02-17
     │   │   ├── AAPL_calls.csv
     │   │   ├── AAPL_puts.csv
     │   │   ├── CSCO_calls.csv
     │   │   ├── CSCO_puts.csv
     │   │   ├── GOOG_calls.csv
     │   └── 2017-02-22
     │       ├── CSCO_calls.csv
     │       ├── CSCO_puts.csv
     │       ├── GOOG_calls.csv
     │       ├── GOOG_puts.csv
     ├── 2017-03-03
     │   ├── 2017-02-17
     │   │   ├── AAPL_calls.csv
     │   │   ├── AAPL_puts.csv
     │   │   ├── CSCO_calls.csv
     │   │   ├── CSCO_puts.csv
     │   │   ├── GOOG_calls.csv
     │   │   ├── GOOG_puts.csv
     │   └── 2017-02-22
     │       ├── CSCO_calls.csv
     │       ├── CSCO_puts.csv
     │       ├── GOOG_calls.csv
     │       ├── GOOG_puts.csv
     │       
     │       

## License

MIT
