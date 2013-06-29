************************************************************************
file with basedata            : mm27_.bas
initial value random generator: 1443287569
************************************************************************
projects                      :  1
jobs (incl. supersource/sink ):  12
horizon                       :  81
RESOURCES
  - renewable                 :  2   R
  - nonrenewable              :  2   N
  - doubly constrained        :  0   D
************************************************************************
PROJECT INFORMATION:
pronr.  #jobs rel.date duedate tardcost  MPM-Time
    1     10      0       15        5       15
************************************************************************
PRECEDENCE RELATIONS:
jobnr.    #modes  #successors   successors
   1        1          3           2   3   4
   2        3          1           8
   3        3          2           9  10
   4        3          3           5   6   9
   5        3          1           7
   6        3          2           8  11
   7        3          2           8  11
   8        3          1          10
   9        3          1          12
  10        3          1          12
  11        3          1          12
  12        1          0        
************************************************************************
REQUESTS/DURATIONS:
jobnr. mode duration  R 1  R 2  N 1  N 2
------------------------------------------------------------------------
  1      1     0       0    0    0    0
  2      1     3       0    8    3    0
         2     6       0    3    0    2
         3     9       4    0    0    2
  3      1     4      10    0    0    9
         2     4       9    0    5    0
         3    10       0    6    4    0
  4      1     4       0    2    0    4
         2     5       0    1    8    0
         3    10       3    0    0    1
  5      1     2       7    0    7    0
         2     5       0    7    0    8
         3     5       5    0    2    0
  6      1     4       8    0   10    0
         2     5       5    0   10    0
         3     6       0    5    0    6
  7      1     5       0    7    7    0
         2     5       7    0    7    0
         3     5       0    9    0    4
  8      1     3      10    0    7    0
         2     7       8    0    6    0
         3     8       8    0    5    0
  9      1     3       4    0    0    8
         2     5       0    4    0    6
         3     8       3    0    0    2
 10      1     1       6    0    7    0
         2     7       5    0    5    0
         3    10       0    4    3    0
 11      1     4       9    0    5    0
         2     7       0    8    4    0
         3    10       0    7    0    3
 12      1     0       0    0    0    0
************************************************************************
RESOURCEAVAILABILITIES:
  R 1  R 2  N 1  N 2
   19   20   44   31
************************************************************************