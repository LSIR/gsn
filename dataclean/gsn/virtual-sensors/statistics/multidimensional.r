# multidimensional.r - Example R script to plot multidimensional data
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

# VARIABLE/VECTOR MAPPING
# variables with prefix 'gsn_' are input variables (from GSN to Rserve) and variables without
# the prefix, represent the output of the R script (from Rserve to GSN).
# It is important that they match the virtual sensor's output structure, otherwise
# Rserve and GSN will return errors/exceptions. If the variables are not used, 
# they still need to be declared in the script. See the example below.

temp <- gsn_temp;
light <- gsn_light;
packet <- gsn_packet;
epoch <- gsn_epoch;

# make a data frame of all the variables
dataset <- data.frame(temp,light,packet,epoch);

# CONFIGURE THE GRAPHICS DEVICE TO CAPTURE PLOTS AS JPEG IMAGES
graphics.off();
jpeg("plot.jpg",quality=90);
dev.cur();

# PLOT THE DATA
plot(dataset, col="blue", main="Multidimensional Plot");

# calling this function does the plot to jpeg capture
dev.off(dev.cur());

# save the plot as a binary (jpeg) object and assign to gsn_plot variable
gsn_plot <- readBin("plot.jpg","raw",512*512);
unlink("plot.jpg");
