# Test R script

# configure the graphics device to jpeg capture
graphics.off();
jpeg("plot.jpg",quality=90);
dev.cur();

temp123 <- gsn_temp123;
dmem <- gsn_dmem;
light123 <- gsn_light123;
packet_type123 <- gsn_packet_type123;
lon123 <- gsn_lon123;
lat123 <- gsn_lat123;
epoch <- gsn_epoch;

#dataset <- data.frame(gsn_temp123,gsn_dmem,gsn_light123,gsn_packet_type123,lon123,gsn_lat123,gsn_epoch);

# plot the average
plot(gsn_temp123, col="blue", xlab="Time (unix epoch)", ylab="Average", main="Sample Data", pch=1);
points(gsn_light123, col="red", pch=4);
points(gsn_dmem, col="black", pch=1);
points(gsn_lat123, col="green", pch=1);

#plot(dataset,col="blue",pch=4);

# calling this function does the plot to jpeg capture
dev.off(dev.cur());

# save the plot as a binary (jpeg) object and assign to gsn_plot variable
gsn_plot <- readBin("plot.jpg","raw",512*512);
unlink("plot.jpg");
