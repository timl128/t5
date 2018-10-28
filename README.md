# Logging application


### Assumption
The error rate should be low. 

### Design

The application will find the trace Id with error first because the error rate is low. Otherwise, it will take more memory to load more logs without errors.
The trace id with the errors will be saved in a set. Then it extract logs with those traces Id . This action can be done concurrency but
the transaction wont display in order.  

A hash map is used to sort the log with same trace id by parent span id. The key is the `parent span id` and the value 
is a priority queue. The queue contains logs with same parent span Id and it is sorted by time. 


For example : 
```
Log A  = {span_id : s1 ,  trace_id : ABC  , Timestamp : 1}
Log B  = {span_id : s1 ,  trace_id : ABC  , Timestamp : 2}
Log C =  {span_id : c1 , trace_id : ABC, parent_span_id : s1, Timestamp : 10}
Log D =  {span_id : z1 , trace_id : ABC, parent_span_id : c1, Timestamp : 101}
```

        Hash map for trace Id  ABC
    -------------------------------------
    |  Parent span id  |   Queue         |
    |------------------+-----------------|
    | null             |  A,B            |
    |------------------+-----------------|
    | s1                | C              |
    |------------------+-----------------|
    | c1               |  D              |
    --------------------------------------

The root level has `null` parent span id. So the application starts from the root level generate logs.
The application will find child spans by calling the map `logMap.get(A.span_id)` after retrieving the log item
from the queue. The application will run this process recursive.

The result from the above

```
A
  C
    D
B
```

### Efficient



Time complexity : 

1. Reading the file : `O(n)`
1. Scanning all json objects to find trace id with error : `O(n)`
2. Scanning all json objects to extract logs with trace Id and put it in a hash map with a priority queue
        
        2.1 insert parent span in into hash map : `O(1)`
        
        2.2 insert into the queue and sort it :  `O(log(n))`
        
3. Printing the result recursively: `O(n)`



Memory complexity

1. Loading file content into memory : `O(n)`
2. Scanning all json objects to extract logs with trace Id and put it in a hash map with a priority queue

    2.1 Hash map size  : `O(1)` because we assume the error rate is low
    
    2.2 Queue size : `O(n)`


Overall time complexity : 

`O(n) + O(n) +  O(1) * O(log(n)) + O(n)  = 3 * O(n) + O(log(n)) = O(n)` 

Overall memory complexity :

`O(n) + O(1) * O(n) = 2* O(n) = O(n)` 


### Worst case
If the error rate is high and most of the logs contain a parent span id and shares the same parent span id.
 

Overall time complexity : `O(n^2)`
Overall memory complexity : `O(n^2)`

### Requirement 
1. gradle 4.8+
2. java 1.8+


### Set up

1. Please download the source code from git hub
2. Change the file permission of `gradlew`  to 755.

The `format-all-1.0jar` is in the project root directory.

Compile source code (Optional):

```
cd [project root]
./gradlew clean fatjar 
```

The jar will be generated in `[project root]/build/libs/format-all-1.0.jar` .


Run the jar directly :
run `java -jar format-all-1.0jar [file_name] [turn on concurrency]` 

For example

```
java -jar build/libs/format-all-1.0.jar log-data.json false
java -jar build/libs/format-all-1.0.jar log-data.json true 
```

### Testing
You can run the test by
```
cd [project root]
./gradle test
```
