#Initial Packaging Process

The packaging of the application is done through Maven and will generate a ZIP which contains the program and all its dependencies.

The proxy is is generated as a JAR file called stratum-proxy.jar (contained in the ZIP file) which can be launched on any platform with a JVM installed with version >= 7 (Java Virtual Machine)

To build the package, you must have a JDK installed in version >= 7 and maven 3

##Packaging
cd /directory/which/contain/pom.xmlFileOfTheProject

mvn clean package

The package is then present in the "target" directory.

#Installation and Usage

##Installation
Only unzip the zip file in a directory and launch the proxy through the following command line:

java -jar stratum-proxy.jar <proxyOptions>

##Usage





Oh, and if you want make a little donation, you are welcome :)
BTC: 19wv8FQKv3NkwTdzBCQn1AGsb9ghqBPWXi
