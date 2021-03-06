# Workload Evaluation

After column ordering and / or duplication, we can
[Generate Queries](https://github.com/dbiir/rainbow/blob/master/rainbow-core/README.md#evaluation) and
evaluation query performance by executing the queries in SQL-on-Hadoop systems.
But it is a boring job to execute hundreds of queries manually. `rainbow-evaluate` module
provides an easier way to evaluate query performance automatic.

## Prepare

- Finish the steps in [Rainbow Core](https://github.com/dbiir/rainbow/blob/master/rainbow-core/README.md).

## Build

Enter the root directory of rainbow, run:
```bash
$ mvn clean
$ mvn package
$ cd ./rainbow-evaluate
```

Then you get `rainbow-evaluate-xxx-full.jar` in `target` subdirectory.
Now you are ready to start rainbow workload evaluation.

## Usage

To get usage information:
```bash
$ java -jar target/rainbow-evaluate-xxx-full.jar -h
```

There is only one argument required:
- `-p / param_file`, specifies the parameter file to be used.

Template of the parameter file can be found in `./src/main/resources/params/`.
Edit the parameters before running workload evaluation.

To perform workload evaluations, run:
```bash
$ java -jar target/rainbow-evaluate-xxx-full.jar -f ./src/main/resources/params/WORKLOAD_EVALUATION.properties
```

Parameters in `WORKLOAD_EVALUATION.properties` are:
```
# LOCAL or SPARK
method=SPARK
# the path of unordered table directory on HDFS,
table.dir=/rainbow/parq
# the file path of workload file
workload.file=/rainbow/workload.txt
# the local directory used to write evaluation results
log.dir=/tmp/log/dir
# true or false, whether or not to drop file cache on each node in the cluster
drop.cache=true
# the file path of drop_caches.sh
drop.caches.sh=/rainbow/drop_caches.sh
```

Currently, we only support automatic workload evaluation on **Parquet** format tables.

For the two evaluation `method`, LOCAL and SPARK:
- **LOCAL** is to read the accessed columns of a query from HDFS by a Parquet reader.
- **SPARK** is to execute the queries in Spark. The duration of the first mapPartitions stage is
recorded as the read latency of the query. Such a latency includes task initialization, scheduling and garbage
collection overheads.

`table.dir` is the directory on HDFS which stores the Parquet files.

`workload.file` is the path of workload file in [Rainbow Core](https://github.com/dbiir/rainbow/blob/master/rainbow-core/README.md).

`log.dir` is the directory on local fs to store the evaluation results.
A `spark_duration.csv` or `local_duration.csv` file (depends on which method been used) will be generated in this directory.
In LOCAL method, an `accessed_columns.txt` file is also generated. It records the Parquet column index and column name of
each query's accessed columns.

`drop.cache` indicates whether or not to drop file system cache on each node in the cluster after the execution of a query.
If you are using a small dataset to evaluate the effects of data layout optimization, it is a
good idea to drop cache on each node.

`drop.caches.sh` is the path of shell script file which is used to drop file system cache on each node
in the cluster. A very simple example of this file is:
```bash
# This is just a simple example of drop_caches.sh
#!/bin/bash

for ((i=1; i<=5; i+=1))
do
  ssh node$i "echo 3 > /proc/sys/vm/drop_caches"
  ssh node$i sync
done
```

You should ensure that the system user running rainbow jars have the right permissions
to execute this script and clear the fs cache on the cluster nodes.