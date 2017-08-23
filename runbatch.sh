#!/bin/csh

# A sample script for running Qsim in a batch mode. The cmd line params
# specify the config file and the output directory

time java -cp lib/qsim.jar -Druns=10 -DT=1000000 qsim.Qsim config/config-99pc-trivial.dat ../out/run1

