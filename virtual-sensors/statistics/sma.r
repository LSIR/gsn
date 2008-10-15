# R Script for calculating Simple Moving Average (SMA)

# size of the window
size <- length(gsn_data);

# calculate the average
gsn_out_data <- sum(gsn_data) / size ;

# take the last timestamp in window as gsn_out_timestamp
gsn_out_timestamp <- gsn_timestamps[1];

# configure the graphics device to jpeg capture
graphics.off();
jpeg("plot.jpg",quality=90);
dev.cur();

# plot the average
plot(gsn_data ~ gsn_timestamps, xlab="Time (unix epoch)", ylab="Average", main="Simple Moving Average", type="l");

# calling this function does the plot to jpeg capture
dev.off(dev.cur());

# save the plot as a binary (jpeg) object and assign to gsn_plot variable
gsn_plot <- readBin("plot.jpg","raw",512*512);
unlink("plot.jpg");