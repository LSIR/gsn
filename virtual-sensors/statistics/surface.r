# get the lenght of the data window
size <- length(gsn_data);

# create the matrix=row*col
col <- size / 10;
row <- col;

# apply the FFT on the input data
res <- fft(gsn_data);
z <- as.real(res);
out <- z;
dim(z) <- c(row,col);

# save the result and include timestamp
gsn_out_data <- out ;
gsn_out_timestamp <- gsn_timestamps[1];

# configure the graphics device to jpeg capture
graphics.off();
jpeg("plot.jpg",quality=90);
dev.cur();

# plot the surface!
par(bg = "white",mar=c(0.5,0.5,2.5,0.5));
persp(z,col="yellow", shade=0.40, box=TRUE, scale=TRUE,expand=0.5);
title(main = "Fast Fourier Transform");

# calling this function does the plot to jpeg capture
dev.off(dev.cur());

# save the plot as a binary (jpeg) object and assign to output_plot variable
gsn_plot <- readBin("plot.jpg","raw",512*512);
unlink("plot.jpg");

