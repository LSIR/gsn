# R Script for plotting wireframe data in 3D
require(lattice);

# size of the window
window_size <- length(gsn_data);

# set the coordinate size
x <- seq(-window_size, window_size, len = (window_size * 2));
y <- seq(-window_size, window_size, len = (window_size * 2));
g <- expand.grid(x = x, y = y);

# function to plot the wireframe sin(x)/x
g$z <- sin(gsn_data)/gsn_data;
g$z <- as.real(g$z);

gsn_out_data <- sum(gsn_data) / window_size ;
gsn_out_timestamp <- gsn_timestamps[1];

# configure the graphics device to jpeg capture
graphics.off();
jpeg("plot.jpg",quality=90);
dev.cur();

# plot the wireframe!
plot(wireframe(x ~ z * y, g, drape = TRUE,aspect = c(3,1), colorkey = TRUE));

# calling this function does the plot to jpeg capture
dev.off(dev.cur());

# save the plot as a binary (jpeg) object and assign to gsn_plot variable
gsn_plot <- readBin("plot.jpg","raw",512*512);
unlink("plot.jpg");