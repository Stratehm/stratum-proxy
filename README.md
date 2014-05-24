#Packaging (Not mandatory)

You can directly use a release (https://github.com/Stratehm/stratum-proxy/releases) and go to the Installation and Usage section.

The packaging of the application is done through Maven and will generate a ZIP which contains the program and all its dependencies.

The proxy is is generated as a JAR file called stratum-proxy.jar (contained in the ZIP file) which can be launched on any platform with a JVM installed with version >= 7 (Java Virtual Machine)

To build the package, you must have a JDK installed in version >= 7 and maven 3

##Packaging

```sh
cd /directory/which/contain/pom.xmlFileOfTheProject

mvn clean package
```

The package is then present in the "target" directory.

#Installation and Usage

##Installation
Only unzip the zip file in a directory and launch the proxy through the following command line:

```sh
java -jar stratum-proxy.jar proxyOptions
```

##Usage

```sh
java -jar stratum-proxy.jar --help
```

##API Details

A REST API is available. Methods parameters or result are in JSON. By default, the methods can be accessed at the URL http://hostIp:8888/proxy/. 

The Content-Type of HTTP requests has to be application/json, else a 415 Unsupported Media Type error may be returned.

Here after is the API methods description:

 * pool/list: (GET) List all the pools.

```
Parameters: None
Return: [ { "name": string, "host": string, "username": string, "password": string, "isActive": boolean, "isEnabled": boolean, "isStable": boolean, "isActiveSince": Date(dd-MM-yy HH:mm:ss Z), "difficulty": string, "extranonce1": string, "extranonce2Size": integer, "workerExtranonce2Size": integer, "numberOfWorkerConnections": integer, "priority": integer, "acceptedDifficulty": double, "rejectedDifficulty": double, "isExtranonceSubscribeEnabled": boolean, "acceptedHashesPerSeconds": long, "rejectedHashesPerSeconds": long }, ... ]
```

 * user/list: (GET) List all the seen users.
```
Parameters: None
Return: [ { "name": string, "firstConnectionDate": Date(dd-MM-yy HH:mm:ss Z), "lastShareSubmitted": Date(dd-MM-yy HH:mm:ss Z), "acceptedHashesPerSeconds": long, "rejectedHashesPerSeconds": long, "connections": [ { "remoteHost": string, "authorizedUsers": [ string, ... ], "acceptedHashesPerSeconds": long, "rejectedHashesPerSeconds": long, "isActiveSince": Date(dd-MM-yy HH:mm:ss Z), "poolName": string }, ... ] } ]
```


 * connection/list: (GET) List all the active workers connection
```
Parameters: None
Return: [ { "remoteHost": string, "authorizedUsers": [ string, ... ], "acceptedHashesPerSeconds": long, "rejectedHashesPerSeconds": long, "isActiveSince": Date(dd-MM-yy HH:mm:ss Z), "poolName": string }, ... ]
```

 * pool/add: (POST) Add a new pool
```
Parameters: { "poolName": string (optional=poolHost), "poolHost": string, "username": string, "password": string, "priority": integer (optional=lowestOne), "enableExtranonceSubscribe": boolean (optional=false),  "isEnabled": boolean (optional=true)}
Return: {"status": string, "message": string}. Returned statuses are DONE, DONE_PARTIALLY (if added but not started) or FAILED.
```

 * pool/remove: (POST) Remove a pool
```
Parameters: { "poolName": string}
Return: {"status": string, "message": string}. Returned statuses are DONE or FAILED.
```

 * pool/priority: (POST) Change the priority of a pool
```
Parameters: {"poolName": string, "priority": integer}
Return: {"status": string, "message": string}. Returned statuses are DONE or FAILED.
```

 * pool/disable: (POST) Disable the pool with the given name
```
Parameters: {"poolName": string}
Return: {"status": string, "message": string}. Returned statuses are DONE or FAILED.
```

 * pool/enable: (POST) Disable the pool with the given name
```
Parameters: {"poolName": string}
Return: {"status": string, "message": string}. Returned statuses are DONE or FAILED.
```

 * log/level: (POST) Change the log level. 
```
Parameters: {"logLevel": string}. Valid Levels are FATAL, ERROR, WARN, INFO, DEBUG, TRACE, OFF.
Return: {"status": string, "message": string}. Returned statuses are DONE or FAILED.
```

 * user/kick: (POST) Kick all connections of the user 
```
Parameters: {"username": string}.
Return: {"status": string, "message": string}. Returned statuses are DONE or FAILED.
```

 * user/ban: (POST) Kick all connections of the user then ban the user until the next proxy restart.
```
Parameters: {"username": string}.
Return: {"status": string, "message": string}. Returned statuses are DONE or FAILED.
```

 * user/unban: (POST) Unban the user
```
Parameters: {"username": string}.
Return: {"status": string, "message": string}. Returned statuses are DONE or FAILED.
```

 * user/ban/list: (GET) List all banned users
```
Parameters: none
Return: [ string, ... ]. Returned statuses are DONE or FAILED.
```


#License


GPLv3



Of course, if you want make a little donation, you are welcome :)

BTC: 19wv8FQKv3NkwTdzBCQn1AGsb9ghqBPWXi

    
