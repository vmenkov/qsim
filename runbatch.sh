#!/bin/csh

# A sample script for running Qsim in a batch mode. The cmd line params
# specify the config file and the output directory

time java -cp lib/qsim.jar -Druns=10 -DT=1000000 qsim.Qsim config/config-99pc-trivial.dat ../out/run1

# Later, you can run something like this (to produce more manageable sample files)
#---------------
# cd ../out/run1
# foreach x ( 0 1 2 3 4 5 6 7 8 9 )
# 	grep '000 ' queue-00${x}.dat > tmp-$x.dat
# end
#-----------------
# And then feed the output files into gnuplot, with
#-------------------
# plot 'tmp-0.dat' with lines, 'tmp-1.dat' with lines, 'tmp-2.dat' with lines, 'tmp-3.dat' with lines, 'tmp-4.dat' with lines, 'tmp-5.dat' with lines, 'tmp-6.dat' with lines, 'tmp-7.dat' with lines, 'tmp-8.dat' with lines, 'tmp-9.dat' with lines
#-------------------
