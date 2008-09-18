##------------------------------------------------------------------------##
## R Script for calculating Simple Moving Average (SMA)                   ##
## Author: GSN Team                                                       ##
##------------------------------------------------------------------------##

## size of the window
w_len <- length(input_data);

## calculate the average
output_data <- sum(input_data) / w_len ;

## take the last timestamp in window as output_timestamp
output_timestamp <- input_timestamps[1]; ## 1221476697

##time <- input_timestamps;
##seconds <- (time %% 60);
##minutes <- (time %% 60)/60;
##hours <- time/3600;

## do some basic plotting
graphics.off();
jpeg('test.jpg',quality=90); 
dev.cur();
plot(input_data ~ input_timestamps, xlab='Time', ylab='Average', main='Simple Moving Average', type="l");
dev.off(2);

## save the plot as a binary (jpeg) object and assign to output_plot variable
output_plot <- readBin('test.jpg','raw',512*512); 
unlink('test.jpg');
