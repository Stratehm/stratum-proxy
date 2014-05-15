#Initial Packaging Process

The packaging of the application is done through Maven and will generate a ZIP which contains the program and all its dependencies.

The proxy is is generated as a JAR file called stratum-proxy.jar (contained in the ZIP file) which can be launched on any platform with a JVM installed with version >= 7 (Java Virtual Machine)

To build the package, you must have a JDK installed in version >= 7 and maven 3

##Packaging
*cd /directory/which/contain/pom.xmlFileOfTheProject*

*mvn clean package*

The package is then present in the "target" directory.

#Installation and Usage

##Installation
Only unzip the zip file in a directory and launch the proxy through the following command line:

*java -jar stratum-proxy.jar proxyOptions*

##Usage

*java -jar stratum-proxy.jar --help*

##API Details

A REST API is available with the following methods. Methods parameters or result are in JSON. By default, the methods can be accessed at the URL http://<hostIp>:8888/proxy/

pool/list: List all the pools.
 
Parameters: *None*
 
Return:


pool/priority: Change the priority of a pool

Parameters:
{"poolName": "nameOfThePool", "priority": 0}

Return:


log/level: Change the log level. 

Parameters:
{"logLevel": "LEVEL"}. Valid levels are FATAL, ERROR, WARN, INFO, DEBUG, TRACE, OFF.



Oh, and if you want make a little donation, you are welcome :)
BTC: 19wv8FQKv3NkwTdzBCQn1AGsb9ghqBPWXi
