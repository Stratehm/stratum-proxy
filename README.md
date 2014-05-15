#Initial Packaging Process

The packaging of the application is done through Maven and will generate a WAR file which can be deployed in a Servlet Container (like Apache Tomcat).

To build the package, you must have a JDK installed in version >= 7 and maven 3

##Prepare the maven repository

Some dependencies of this application are not in the maven central repository. You have to add it manually.

These libraries are contained in the libToAddInM2 directory. (gwt-jsonmaker-1.2.1.jar and org.moxieapps.gwt.highcharts-1.6.0.jar)

To "Mavenize" them, you have to copy the file in your local Maven repository which is in your home directory:


On Windows: c:\Users\<username>\.m2\repository

On linux: /home/<username>/.m2/repository

Then, copy the org.moxieapps.gwt.highcharts-1.6.0.jar file in the directory (create this directory if it does not exist): org/moxieapps/gwt/highcharts/1.6.0 and rename it to highcharts-1.6.0.jar

And copy the gwt-jsonmaker-1.2.1.jar file in the directory (create this directory if it does not exist): org/jsonmaker/gwt-jsonmaker/1.2.1 and rename it to gwt-jsonmaker-1.2.1.jar

##Packaging
cd /directory/which/contain/pom.xmlFileOfTheProject

mvn clean package

The package is present in the target directory.

#Installation

The servlet container has to be compliant with the servlet-api 3.0.1. (Apache Tomcat since v7)

The database used is MongoDB. The database has to be installed on the same server as the servlet container on its default port (27017). The address of the MongoDB server is in files MongoDBConfiguration.java.
(At the moment, there is no configuration file. This may be a good thing to develop ;)

## Installation on Tomcat
On a fresh Apache Tomcat installation, just copy the WAR file in the webapps directory. Tomcat will deploy the application automatically on startup. Once deployed, the application will be accessible at the URL:
http://127.0.0.1:8080/multipool-stats-backend-1.0.0-SNAPSHOT/

The log file of the application will be stored in the /tmp directory. (On windows, this directory will be created on the same drive as the Tomcat insstallation. For example, c:\tmp)
(It may be a good idea to make that configurable in the future)


Oh, and if you want make a little donation, you are welcome :)
BTC: 19wv8FQKv3NkwTdzBCQn1AGsb9ghqBPWXi
