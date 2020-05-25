# CNV-19-20

Packages:

pt.ist.meic.cnv.SudokuSolver
In the base package we have:

SudokuSolverApplication (Spring Boot App)
SudokuRestController (REST Controller for the /sudoku endpoint)

...SudokuSolver.balancer
Here we have code related to the Load Balancer
Includes the balancer itself, some custom exceptions and classes:
Request: represents a request, used to estimate cost
InstanceInfo: keeps track of requests running in an AWS Instance

...SudokuSolver.dynamodb
Code interacting with dynamodb table
Fetches data from the table that keeps instrumentation results
Used to estimate cost of requests

...SudokuSolver.scaler
AutoScaler code
Keeps track of running instances and adds / removes instances depending on AVG CPU 
less than 30% = remove instance
higher than 70% = add instance
If an instance scheduled to be removed still has requests running,
a thread is started to check every 10 minutes if it still has requests and then delete the instance if it's done 
- further explained in project report

How to run?\
Put this in an VM with JDK 14 installed and run 'run.sh'\
We are using Amazon Linux 2 VMs so for us the script contains "JAVA_HOME=/usr/lib/jvm/java-14-openjdk-14.0.1.7-2.rolling.el7.x86_64/"\
Depending on distro this can change.