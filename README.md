#Packaging (Not mandatory)

You can directly use a release (https://github.com/Stratehm/stratum-proxy/releases) and go to the Installation and Usage section.

The packaging of the application is done through Maven and will generate a ZIP which contains the program and all its dependencies.

The proxy is is generated as a JAR file called stratum-proxy.jar (contained in the ZIP file) which can be launched on any platform with a JVM installed with version >= 7 (Java Virtual Machine)

To build the package, you must have a JDK installed in version >= 7 and maven 3

##Packaging

_**Advice**_:
It is highly recommended to install NodeJS before building the package. It will speed up the Javascript Optimization phase. If not installed, the packing will still work but may be really long (since the Javascript Optimization phase will use the Java Rhino Javascript Engine which is slower).
If NodeJS is not installed, set the MAVEN_OPTS environment variable before packaging with the following parameters: -Xmx512M -Xss2M

On Windows:
```batch
cd /directory/which/contain/pom.xmlFileOfTheProject

mvn clean package
```

On Linux:
```sh
cd /directory/which/contain/pom.xmlFileOfTheProject

mvn clean package
```

The package is then present in the "target" directory.

#Installation and Usage

##Installation

The latest JVM (Java Virtual Machine) can be downloaded here: http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html
Install the JVM for your operating system (if not already done)

Then unzip the zip file in a directory and launch the proxy through the following command line:

```sh
java -jar stratum-proxy.jar proxyOptions
```

##Usage

```sh
java -jar stratum-proxy.jar --help
```

##Raspberry Pi Installation (Raspbian)
Once your OS is setup on the Raspberry Pi (Raspbian), perform the following (replace the version number with the last available: https://github.com/Stratehm/stratum-proxy/releases):

1. Install JDK and Screen onto the Raspberry Pi
<br/>`sudo apt-get update && sudo apt-get install oracle-java7-jdk screen`

2. Download stratum-proxy to the /opt folder
<br/>`cd /opt`
<br/>`sudo wget https://github.com/Stratehm/stratum-proxy/releases/download/x.x.x/stratum-proxy-x.x.x.zip`

3. Unzip the stratum-proxy zip
<br/>`sudo unzip stratum-proxy-x.x.x.zip`
<br/>`sudo rm -rf stratum-proxy-x.x.x.zip`
<br/>`sudo mv /opt/stratum-proxy-x.x.x /opt/stratum-proxy`
<br/>`cd /opt/stratum-proxy`

4. Configure the proxy by creating/editing the file `stratum-proxy.conf`

5. Test the stratum-proxy by running it
<br/>`sudo java -jar stratum-proxy.jar`

6. Set it to automatically start-up, in a seperate screen, by adding the following line to the file `/etc/rc.local`
<br/>`sudo /usr/bin/screen -dmS proxy /usr/bin/java -jar /opt/stratum-proxy/stratum-proxy.jar -f /opt/stratum-proxy/stratum-proxy.conf`

##Configuration File

In order to use a configuration file instead of the command line parameters, use the following command line options:

```sh
java -jar stratum-proxy.jar -f /path/to/the/configuration/file
```

When -f option is used, all other command line options are discarded and only the configuration file is used.

The files stratum-proxy-minimal-sample.conf and stratum-proxy-full-sample.conf in the package can be used to build your own configuration file.

##WebClient

A WebClient is available at the address: http://127.0.0.1:8888 (The port can be changed with --rest-listen-port or apiListenAddress of configuration file).

![GUI example](http://i.imgur.com/DHvG0jr.jpg)

##API Details

An API is available. Methods parameters or result are in JSON. By default, the methods can be accessed at the URL http://hostIp:8888/proxy/. 

The Content-Type of HTTP requests has to be application/json, else a 415 Unsupported Media Type error may be returned.

Here after is the API methods description:

#

Api Version: 1.0

## APIs
### /
#### Overview
API to manage the proxy

#### **POST** `/hashrate/pool`
##### getPoolHashrateHistory 

Return the hashrate history of a pool.


###### Parameters
- body

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td></td>
        <td><a href="#PoolNameDTO">PoolNameDTO</a></td>
    </tr>
</table>

###### Response
[List[HashrateModel]](#HashrateModel)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 500    | Error during pool hashrate history response build. | - |
| 404    | Pool not found. | - |


- - -
#### **POST** `/hashrate/user`
##### getUserHashrateHistory 

Return the hashrate history of a user.


###### Parameters
- body

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td></td>
        <td><a href="#UserNameDTO">UserNameDTO</a></td>
    </tr>
</table>

###### Response
[List[HashrateModel]](#HashrateModel)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|


- - -
#### **POST** `/address/ban/list`
##### listBannedAddress 

List all banned addresses.


###### Parameters
- body

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td></td>
        <td><a href="#AddressDTO">AddressDTO</a></td>
    </tr>
</table>

###### Response
[List[string]](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|


- - -
#### **GET** `/connection/list`
##### getConnectionsList 

Return the list of all worker connections.


###### Parameters

###### Response
[List[WorkerConnectionDTO]](#WorkerConnectionDTO)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|


- - -
#### **GET** `/user/list`
##### getUsersList 

Get the list of users that has been connected at least once since the proxy is started.


###### Parameters

###### Response
[List[UserDetailsDTO]](#UserDetailsDTO)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|


- - -
#### **POST** `/connection/kick`
##### kickConnection 

Kill the connection with the given address and port.


###### Parameters
- body

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td></td>
        <td><a href="#ConnectionIdentifierDTO">ConnectionIdentifierDTO</a></td>
    </tr>
</table>

###### Response
[StatusDTO](#StatusDTO)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|


- - -
#### **POST** `/address/kick`
##### kickAddress 

Kick all connections with the given address.


###### Parameters
- body

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td></td>
        <td><a href="#AddressDTO">AddressDTO</a></td>
    </tr>
</table>

###### Response
[StatusDTO](#StatusDTO)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|


- - -
#### **POST** `/address/ban`
##### banIp 

Ban the given ip address.


###### Parameters
- body

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td></td>
        <td><a href="#AddressDTO">AddressDTO</a></td>
    </tr>
</table>

###### Response
[StatusDTO](#StatusDTO)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|


- - -
#### **GET** `/user/ban/list`
##### listBannedUsers 

List all banned users.


###### Parameters

###### Response
[List[string]](#)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|


- - -
#### **POST** `/address/unban`
##### unbanAddress 

Unban the given ip address.


###### Parameters
- body

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td></td>
        <td><a href="#AddressDTO">AddressDTO</a></td>
    </tr>
</table>

###### Response
[StatusDTO](#StatusDTO)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|


- - -
#### **GET** `/pool/list`
##### getPoolsList 

Return the list of all pools.


###### Parameters

###### Response
[List[PoolDetailsDTO]](#PoolDetailsDTO)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|


- - -
#### **POST** `/pool/add`
##### addPool 

Add a pool.


###### Parameters
- body

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td></td>
        <td><a href="#AddPoolDTO">AddPoolDTO</a></td>
    </tr>
</table>

###### Response
[StatusDTO](#StatusDTO)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 500    | Failed to add the pool. | [StatusDTO](#StatusDTO) |
| 500    | Pool added but not started. | [StatusDTO](#StatusDTO) |


- - -
#### **POST** `/pool/remove`
##### removePool 

Remove a pool.


###### Parameters
- body

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td></td>
        <td><a href="#RemovePoolDTO">RemovePoolDTO</a></td>
    </tr>
</table>

###### Response
[StatusDTO](#StatusDTO)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | Pool not found. | [StatusDTO](#StatusDTO) |


- - -
#### **POST** `/pool/disable`
##### disablePool 

Disable a pool.


###### Parameters
- body

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td></td>
        <td><a href="#PoolNameDTO">PoolNameDTO</a></td>
    </tr>
</table>

###### Response
[StatusDTO](#StatusDTO)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 500    | Failed to start the pool. | [StatusDTO](#StatusDTO) |
| 404    | Pool not found. | [StatusDTO](#StatusDTO) |


- - -
#### **POST** `/pool/enable`
##### enablePool 

Enable a pool.


###### Parameters
- body

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td></td>
        <td><a href="#PoolNameDTO">PoolNameDTO</a></td>
    </tr>
</table>

###### Response
[StatusDTO](#StatusDTO)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | Pool not found. | [StatusDTO](#StatusDTO) |
| 500    | Failed to start the pool. | [StatusDTO](#StatusDTO) |


- - -
#### **POST** `/pool/priority`
##### setPoolPriority 

Change the priority of a pool.


###### Parameters
- body

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td></td>
        <td><a href="#ChangePriorityDTO">ChangePriorityDTO</a></td>
    </tr>
</table>

###### Response
[StatusDTO](#StatusDTO)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 404    | Pool not found. | [StatusDTO](#StatusDTO) |
| 400    | Bad parameter sent. | [StatusDTO](#StatusDTO) |


- - -
#### **POST** `/pool/update`
##### updatePool 

Update a pool.


###### Parameters
- body

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td></td>
        <td><a href="#UpdatePoolDTO">UpdatePoolDTO</a></td>
    </tr>
</table>

###### Response
[StatusDTO](#StatusDTO)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 500    | Failed to update the pool. | [StatusDTO](#StatusDTO) |
| 400    | Bad parameter sent. | [StatusDTO](#StatusDTO) |
| 404    | Pool not found. | [StatusDTO](#StatusDTO) |


- - -
#### **POST** `/log/level`
##### setLogLevel 

Change the log level.


###### Parameters
- body

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td></td>
        <td><a href="#LogLevelDTO">LogLevelDTO</a></td>
    </tr>
</table>

###### Response
[StatusDTO](#StatusDTO)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 400    | Log level not known. | [StatusDTO](#StatusDTO) |


- - -
#### **GET** `/log/level`
##### getLogLevel 

Return the log level.


###### Parameters

###### Response
[LogLevelDTO](#LogLevelDTO)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|


- - -
#### **POST** `/log/since`
##### getLogSince 

Return log message since the given timestamp.


###### Parameters
- body

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td></td>
        <td><a href="#TimestampDTO">TimestampDTO</a></td>
    </tr>
</table>

###### Response
[List[LogEntry]](#LogEntry)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| 400    | Timestamp is empty. | - |


- - -
#### **GET** `/misc/version`
##### getProxyVersion 

Return the version of the proxy.


###### Parameters

###### Response
[ProxyVersionDTO](#ProxyVersionDTO)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|


- - -
#### **GET** `/summary`
##### getSummary 

Return a summary of the current state of the proxy.


###### Parameters

###### Response
[SummaryDTO](#SummaryDTO)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|


- - -
#### **POST** `/user/unban`
##### unbanUser 

Unban a user.


###### Parameters
- body

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td></td>
        <td><a href="#UserNameDTO">UserNameDTO</a></td>
    </tr>
</table>

###### Response
[StatusDTO](#StatusDTO)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|


- - -
#### **POST** `/user/ban`
##### banUser 

Ban the given username until the proxy restart. The user will not be authorized to reconnect.


###### Parameters
- body

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td></td>
        <td><a href="#UserNameDTO">UserNameDTO</a></td>
    </tr>
</table>

###### Response
[StatusDTO](#StatusDTO)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|


- - -
#### **POST** `/user/kick`
##### kickUser 

Kick the given username. Kill all connections where the user has been seen (WARN: this may kill connections supporting other users)


###### Parameters
- body

<table border="1">
    <tr>
        <th>Parameter</th>
        <th>Required</th>
        <th>Description</th>
        <th>Data Type</th>
    </tr>
    <tr>
        <th>body</th>
        <td>false</td>
        <td></td>
        <td><a href="#UserNameDTO">UserNameDTO</a></td>
    </tr>
</table>

###### Response
[StatusDTO](#StatusDTO)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|


- - -

## Data Types


## <a name="AddPoolDTO">AddPoolDTO</a>

<table border="1">
    <tr>
        <th>name</th>
        <th>type</th>
        <th>description</th>
    </tr>
    <tr>
        <td>username</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>poolHost</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>poolName</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>workerNameSeparator</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>appendWorkerNames</td>
        <td>boolean</td>
        <td>-</td>
    </tr>
    <tr>
        <td>isEnabled</td>
        <td>boolean</td>
        <td>-</td>
    </tr>
    <tr>
        <td>weight</td>
        <td>int</td>
        <td>-</td>
    </tr>
    <tr>
        <td>useWorkerPassword</td>
        <td>boolean</td>
        <td>-</td>
    </tr>
    <tr>
        <td>enableExtranonceSubscribe</td>
        <td>boolean</td>
        <td>-</td>
    </tr>
    <tr>
        <td>priority</td>
        <td>int</td>
        <td>-</td>
    </tr>
    <tr>
        <td>password</td>
        <td>string</td>
        <td>-</td>
    </tr>
</table>



## <a name="AddressDTO">AddressDTO</a>

<table border="1">
    <tr>
        <th>name</th>
        <th>type</th>
        <th>description</th>
    </tr>
    <tr>
        <td>address</td>
        <td>string</td>
        <td>-</td>
    </tr>
</table>



## <a name="ChangePriorityDTO">ChangePriorityDTO</a>

<table border="1">
    <tr>
        <th>name</th>
        <th>type</th>
        <th>description</th>
    </tr>
    <tr>
        <td>poolName</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>priority</td>
        <td>int</td>
        <td>-</td>
    </tr>
</table>



## <a name="ConnectionIdentifierDTO">ConnectionIdentifierDTO</a>

<table border="1">
    <tr>
        <th>name</th>
        <th>type</th>
        <th>description</th>
    </tr>
    <tr>
        <td>address</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>port</td>
        <td>int</td>
        <td>-</td>
    </tr>
</table>



## <a name="HashrateModel">HashrateModel</a>

<table border="1">
    <tr>
        <th>name</th>
        <th>type</th>
        <th>description</th>
    </tr>
    <tr>
        <td>acceptedHashrate</td>
        <td>long</td>
        <td>-</td>
    </tr>
    <tr>
        <td>rejectedHashrate</td>
        <td>long</td>
        <td>-</td>
    </tr>
    <tr>
        <td>name</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>captureTime</td>
        <td>long</td>
        <td>-</td>
    </tr>
</table>



## <a name="LogEntry">LogEntry</a>

<table border="1">
    <tr>
        <th>name</th>
        <th>type</th>
        <th>description</th>
    </tr>
    <tr>
        <td>message</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>timestamp</td>
        <td>long</td>
        <td>-</td>
    </tr>
</table>



## <a name="LogLevelDTO">LogLevelDTO</a>

<table border="1">
    <tr>
        <th>name</th>
        <th>type</th>
        <th>description</th>
    </tr>
    <tr>
        <td>logLevel</td>
        <td>string</td>
        <td>-</td>
    </tr>
</table>



## <a name="PoolDetailsDTO">PoolDetailsDTO</a>

<table border="1">
    <tr>
        <th>name</th>
        <th>type</th>
        <th>description</th>
    </tr>
    <tr>
        <td>lastStopCause</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>extranonce2Size</td>
        <td>int</td>
        <td>-</td>
    </tr>
    <tr>
        <td>username</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>numberOfWorkerConnections</td>
        <td>int</td>
        <td>-</td>
    </tr>
    <tr>
        <td>isActiveSince</td>
        <td>long</td>
        <td>-</td>
    </tr>
    <tr>
        <td>appendWorkerNames</td>
        <td>boolean</td>
        <td>-</td>
    </tr>
    <tr>
        <td>rejectedHashesPerSeconds</td>
        <td>long</td>
        <td>-</td>
    </tr>
    <tr>
        <td>isReadySince</td>
        <td>long</td>
        <td>-</td>
    </tr>
    <tr>
        <td>isReady</td>
        <td>boolean</td>
        <td>-</td>
    </tr>
    <tr>
        <td>isEnabled</td>
        <td>boolean</td>
        <td>-</td>
    </tr>
    <tr>
        <td>acceptedHashesPerSeconds</td>
        <td>long</td>
        <td>-</td>
    </tr>
    <tr>
        <td>weight</td>
        <td>int</td>
        <td>-</td>
    </tr>
    <tr>
        <td>rejectedDifficulty</td>
        <td>double</td>
        <td>-</td>
    </tr>
    <tr>
        <td>difficulty</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>isActive</td>
        <td>boolean</td>
        <td>-</td>
    </tr>
    <tr>
        <td>isExtranonceSubscribeEnabled</td>
        <td>boolean</td>
        <td>-</td>
    </tr>
    <tr>
        <td>useWorkerPassword</td>
        <td>boolean</td>
        <td>-</td>
    </tr>
    <tr>
        <td>workerNamesSeparator</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>workerExtranonce2Size</td>
        <td>int</td>
        <td>-</td>
    </tr>
    <tr>
        <td>host</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>name</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>extranonce1</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>password</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>priority</td>
        <td>int</td>
        <td>-</td>
    </tr>
    <tr>
        <td>lastStopDate</td>
        <td>long</td>
        <td>-</td>
    </tr>
    <tr>
        <td>isStable</td>
        <td>boolean</td>
        <td>-</td>
    </tr>
    <tr>
        <td>numberOfDisconnections</td>
        <td>int</td>
        <td>-</td>
    </tr>
    <tr>
        <td>acceptedDifficulty</td>
        <td>double</td>
        <td>-</td>
    </tr>
    <tr>
        <td>uptime</td>
        <td>long</td>
        <td>-</td>
    </tr>
</table>



## <a name="PoolNameDTO">PoolNameDTO</a>

<table border="1">
    <tr>
        <th>name</th>
        <th>type</th>
        <th>description</th>
    </tr>
    <tr>
        <td>poolName</td>
        <td>string</td>
        <td>-</td>
    </tr>
</table>



## <a name="ProxyVersionDTO">ProxyVersionDTO</a>

<table border="1">
    <tr>
        <th>name</th>
        <th>type</th>
        <th>description</th>
    </tr>
    <tr>
        <td>fullName</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>proxyVersion</td>
        <td>string</td>
        <td>-</td>
    </tr>
</table>



## <a name="RemovePoolDTO">RemovePoolDTO</a>

<table border="1">
    <tr>
        <th>name</th>
        <th>type</th>
        <th>description</th>
    </tr>
    <tr>
        <td>poolName</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>keepHistory</td>
        <td>boolean</td>
        <td>-</td>
    </tr>
</table>



## <a name="StatusDTO">StatusDTO</a>

<table border="1">
    <tr>
        <th>name</th>
        <th>type</th>
        <th>description</th>
    </tr>
    <tr>
        <td>message</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>status</td>
        <td>string</td>
        <td>-</td>
    </tr>
</table>



## <a name="SummaryDTO">SummaryDTO</a>

<table border="1">
    <tr>
        <th>name</th>
        <th>type</th>
        <th>description</th>
    </tr>
    <tr>
        <td>poolUptime</td>
        <td>long</td>
        <td>-</td>
    </tr>
    <tr>
        <td>acceptedHashrate</td>
        <td>long</td>
        <td>-</td>
    </tr>
    <tr>
        <td>hashrate</td>
        <td>long</td>
        <td>-</td>
    </tr>
    <tr>
        <td>rejectedHashrate</td>
        <td>long</td>
        <td>-</td>
    </tr>
    <tr>
        <td>currentPoolName</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>totalErrors</td>
        <td>int</td>
        <td>-</td>
    </tr>
</table>



## <a name="TimestampDTO">TimestampDTO</a>

<table border="1">
    <tr>
        <th>name</th>
        <th>type</th>
        <th>description</th>
    </tr>
    <tr>
        <td>timestamp</td>
        <td>long</td>
        <td>-</td>
    </tr>
</table>



## <a name="UpdatePoolDTO">UpdatePoolDTO</a>

<table border="1">
    <tr>
        <th>name</th>
        <th>type</th>
        <th>description</th>
    </tr>
    <tr>
        <td>username</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>appendWorkerNames</td>
        <td>boolean</td>
        <td>-</td>
    </tr>
    <tr>
        <td>weight</td>
        <td>int</td>
        <td>-</td>
    </tr>
    <tr>
        <td>useWorkerPassword</td>
        <td>boolean</td>
        <td>-</td>
    </tr>
    <tr>
        <td>workerNamesSeparator</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>isExtranonceSubscribeEnabled</td>
        <td>boolean</td>
        <td>-</td>
    </tr>
    <tr>
        <td>host</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>name</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>priority</td>
        <td>int</td>
        <td>-</td>
    </tr>
    <tr>
        <td>password</td>
        <td>string</td>
        <td>-</td>
    </tr>
</table>



## <a name="UserDetailsDTO">UserDetailsDTO</a>

<table border="1">
    <tr>
        <th>name</th>
        <th>type</th>
        <th>description</th>
    </tr>
    <tr>
        <td>rejectedHashesPerSeconds</td>
        <td>long</td>
        <td>-</td>
    </tr>
    <tr>
        <td>firstConnectionDate</td>
        <td>long</td>
        <td>-</td>
    </tr>
    <tr>
        <td>acceptedHashesPerSeconds</td>
        <td>long</td>
        <td>-</td>
    </tr>
    <tr>
        <td>rejectedDifficulty</td>
        <td>double</td>
        <td>-</td>
    </tr>
    <tr>
        <td>lastShareSubmitted</td>
        <td>long</td>
        <td>-</td>
    </tr>
    <tr>
        <td>name</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>acceptedShareNumber</td>
        <td>long</td>
        <td>-</td>
    </tr>
    <tr>
        <td>connections</td>
        <td><a href="#WorkerConnectionDTO">Array[WorkerConnectionDTO]</a></td>
        <td>-</td>
    </tr>
    <tr>
        <td>rejectedShareNumber</td>
        <td>long</td>
        <td>-</td>
    </tr>
    <tr>
        <td>acceptedDifficulty</td>
        <td>double</td>
        <td>-</td>
    </tr>
</table>



## <a name="UserNameDTO">UserNameDTO</a>

<table border="1">
    <tr>
        <th>name</th>
        <th>type</th>
        <th>description</th>
    </tr>
    <tr>
        <td>username</td>
        <td>string</td>
        <td>-</td>
    </tr>
</table>



## <a name="WorkerConnectionDTO">WorkerConnectionDTO</a>

<table border="1">
    <tr>
        <th>name</th>
        <th>type</th>
        <th>description</th>
    </tr>
    <tr>
        <td>poolName</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>isActiveSince</td>
        <td>long</td>
        <td>-</td>
    </tr>
    <tr>
        <td>rejectedHashesPerSeconds</td>
        <td>long</td>
        <td>-</td>
    </tr>
    <tr>
        <td>acceptedHashesPerSeconds</td>
        <td>long</td>
        <td>-</td>
    </tr>
    <tr>
        <td>connectionType</td>
        <td>string</td>
        <td>-</td>
    </tr>
    <tr>
        <td>isExtranonceNotificationSupported</td>
        <td>boolean</td>
        <td>-</td>
    </tr>
    <tr>
        <td>authorizedUsers</td>
        <td>Array[string]</td>
        <td>-</td>
    </tr>
    <tr>
        <td>remoteHost</td>
        <td>string</td>
        <td>-</td>
    </tr>
</table>


#License


GPLv3



Of course, if you want make a little donation, you are welcome :)

BTC: 19wv8FQKv3NkwTdzBCQn1AGsb9ghqBPWXi

    
