# Simple File Storage Server & CLI

1. [Prerequisites](#Prerequisites)
2. [Build & Packaging Instructions](#Build-&-Packaging-Instructions)
3. [Running the Server](#Running-the-Server)
   * [Server Notes](#Server-Notes)
4. [Running the Client](#Running-the-Client)
    * [Client Notes](#Client-Notes)
5. [Testing Notes](#Testing)

## Prerequisites 
JDK 14 or newer, JAVA_HOME set to the JDK installation directory

## Build & Packaging Instructions

Run the following command from the file-storage directory to build the binaries of both client and server
```shell script
./gradlew clean build distribute
```

Client and server executables will be deployed in the _file-storage/build_ folder under the folders _fsclient/_ and 
respectively _fsserver/_ 

## Running the Server

To run the file storage server navigate to *file-storage/build/fsserver* and then run
```shell script
java -jar file-storage-server-1.0.0-SNAPSHOT.jar
```
### Server Notes

- By default, the server will start up at http://localhost:8080, you should be able to confirm that it's up by checking 
its swagger page at http://localhost:8080/q/swagger-ui/ . You will also find the REST API documentation here with 
the explanation for each status code returned by each endpoint
- Several properties are kept in the _application.properties_ file under _src/main/resources_ but there are also plenty 
default properties assumed by Quarkus. If you wish to override some of these (for example the port number), you can do 
so by passing the override with _-D_ args to the jar e.g.
```shell script
java -Dquarkus.http.port=8085 -jar file-storage-server-1.0.0-SNAPSHOT.jar
```
- By default, Quarkus framework stores _multipart/form-data_ files in a temporary location from where they will be copied to
a designated persistent storage folder (called _data-server_). Quarkus automatically removes the files from the 
temporary location after serving the request
- While no total storage limit is imposed on the server side, there is a limit of 10Mb on each file we want to upload.
This is driven by the config _quarkus.http.limits.max-form-attribute-size_ and any file exceeding that size will yield
a HTTP 413 (CLI can fetch this limit via a **GET** to _/v1/stats/fileUploadSizeLimit_)
- Versioning-wise, the REST API starts at **/v1** (as seen appended to the root URL)

## Running the Client

From a separate command line, navigate to *file-storage/build/fsclient* and then run
```shell script
java -jar file-storage-client-1.0.0-SNAPSHOT.jar -l
```

### Client Notes
- The client will expect certain command line options and/or arguments. If they are not provided, or provided wrongly 
the CLI will exit and a usage guide will be printed as below:
```
usage: file-storage-client
To the usage command above, this CLI needs exactly one of the options:
 -d,--delete-file <arg>   Deletes from the server the file provided as
                          argument. The file must exist on the server or
                          else an error will be thrown
 -l,--list-files          List all uploaded files on the server. No extra
                          arguments needed
 -u,--upload-file <arg>   Uploads the file provided as argument. The file
                          must exist locally and must have the size <= 10M
                          or else an error will be thrown
```
- By default, the client will attempt to find the server at http://localhost:8080. You can however choose to overwrite 
this value by passing the JVM arg _-Dfsserver.api.rootUrl_ to the CLI executable e.g.
```shell script
java -Dfsserver.api.rootUrl="http://127.0.0.1:8085" -jar file-storage-client-1.0.0-SNAPSHOT.jar -l
```

## Testing
- Jacoco test coverage 79% in both server and CLI
- Tested on macOS Ventura 13.1 and Windows 11 21H2
