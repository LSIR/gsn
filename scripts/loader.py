import csv, sys

reader = csv.reader(open("table2.csv", "rb"))
file_timed = open('table2_timed.csv', 'w')
file_wind_direction = open('table2_wind_direction.csv', 'w')
file_wind_speed = open('table2_wind_speed.csv', 'w')
file_rain_meter = open('table2_rain_meter.csv', 'w')
file_air_humid = open('table2_air_humid.csv', 'w')
file_air_temp = open('table2_air_temp.csv', 'w')
for row in reader:
    timed = row[1]
    wind_direction = row[2]
    wind_speed = row[3]
    rain_meter = row[4]
    air_humid = row[5]
    air_temp = row[6]
    file_timed.write(timed+"\n")
    file_wind_direction.write(wind_direction+"\n")
    file_wind_speed.write(wind_speed+"\n")
    file_rain_meter.write(rain_meter+"\n")
    file_air_humid.write(air_humid+"\n")
    file_air_temp.write(air_temp+"\n")
    print len(row)
    #print time, wind_direction 

