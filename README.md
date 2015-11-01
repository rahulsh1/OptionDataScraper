# Option-Data-Scraper

Scrapes option contract data from Yahoo Finance or Google Finance

##### Prerequisites
1. Python 3.x

##### Configuration

Add/Remove any symbols from stock_list.txt. This file is used to capture option data.

##### To Run
    $ python option_scraper.py
    
##### Output
Folder "option_data" will be created. 
Inside, folders will be created with names of all the available contract expiration dates across all stocks (EX: 6-19-15, 6-26-15). 
Inside each one will be a folder named after the current date (EX: 6-4-15). Inside that will have csv files with the data.
Each stock will have a calls and puts csv.


