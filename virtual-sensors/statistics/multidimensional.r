# Test R script

# configure the graphics device to jpeg capture
graphics.off();
jpeg("plot.jpg",quality=90);
dev.cur();

temp <- gsn_temp;
light <- gsn_light;
packet <- gsn_packet;
epoch <- gsn_epoch;

# plot the average
plot(epoch, temp, xlab="Time (unix epoch)", ylab="Temperature (average)", main="Sample Data", type="l");

# calling this function does the plot to jpeg capture
dev.off(dev.cur());

# save the plot as a binary (jpeg) object and assign to gsn_plot variable
gsn_plot <- readBin("plot.jpg","raw",512*512);
unlink("plot.jpg");
