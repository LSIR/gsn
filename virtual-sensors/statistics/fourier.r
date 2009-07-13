# fourier.r - Example R script to compute the fast fourier transform
#
# To use R scripts from GSN these are the following conventions:
# 
# 1) All R commands need to be terminated with semicolon ';'. Adding the semicolon
#    indicates that evaluation of the R variable or statement is done in the background
#    not inline.
# 
# 2) Variable (attributes) from the virtual sensor passed to the R script
#    need to be mapped or evaluated in the script. See the script below for
#    examples.
#
# 3) To be able to display R plots in GSN, they need to be exported as jpeg
#    images. To do this, you have to configure the graphics device to 
#    capture plots as jpeg files. See example below. 
#
# 4) Consider that R is quite slow for plotting large datasets, we only tested
#    this implementation up to 20,000 items.
#


# Get the lenght of the data window
size <- length(gsn_temp);

# Create the matrix=row*col
col <- size / 10; # was 10 for 100
row <- col;

# Apply the FFT on the input data
res <- fft(gsn_temp);
z <- as.real(res);
out <- z;
dim(z) <- c(row,col);

# VARIABLE/VECTOR MAPPING
temp <- as.numeric(out);
epoch <- gsn_epoch;

# CONFIGURE THE GRAPHICS DEVICE TO CAPTURE PLOTS AS JPEG IMAGES
graphics.off();
jpeg("plot.jpg",quality=90);
dev.cur();

# PLOT THE DATA
#par(bg = "white",mar=c(0.5,0.5,2.5,0.5));
#persp(z);
persp(z,col="yellow", shade=0.40, box=TRUE, scale=TRUE,expand=0.5);
title(main = "Fast Fourier Transform");

# calling this function does the plot to jpeg capture
dev.off(dev.cur());

# save the plot as a binary (jpeg) object and assign to gsn_plot variable
gsn_plot <- readBin("plot.jpg","raw",512*512);
unlink("plot.jpg");

