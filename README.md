# KumuluzEE Serverless Framework
In this tutorial, we'll go through the main benefits of using the KumuluzEE serverless framework, as well as a simple tutorial, that will show you how to start using it!

## Motivation
The purpose of the KumuluzEE serverless framework is to allow you to deploy any JavaEE application to serverless environments without worrying about the configuration required, and without making any changes to the code. The project is still in its early stages and it only supports Azure Functions, but we plan on extending this functionality to other cloud providers as soon as possible!

If you ever tried to deploy a Function app on Azure, you know that you either have to manually create all the `function.json` and `host.json` files, or that you have to use the provided library, that, as a downside, cannot be easily integrated with JavaEE applications. With our framework, there is no need for you to worry about all the `.json` files, that are required to run the app, nor any details about how to deploy the app to the cloud. You just take *any* existing JavaEE application, created with the KumuluzEE framework, add a single plugin to the `pom.xml`, and issue one command when you want to deploy the app. Sounds interesting? Keep reading, I'll tell you exactly how it's done!

Note that this is just a gentle hello world example - for more information, please refer to the `README.md` that you can find on [GitHub](https://github.com/BorisRado/azure-functions-plugin).

## Getting started
In order to complete the tutorial, the tools you will need are the following:
* `java` 8 or newer. Currently, the Azure Functions only support versions `8` and `11`, so I suggest using version `11`; 
* `maven` 3.2.1 or newer;
* `func`: will allow us to test the application locally. To download it, follow the instructions contained in the [official repo](https://github.com/Azure/azure-functions-core-tools). If you downloaded it correctly, you should be able to run `func --version` and get prompted back a value such as `4.0.4483`;
* An active Azure subscription.

### Creating the application to be deployed
**TL-DR**: if you already have a JavaEE application with the KumuluzEE framework, just add the following plugin into the root `pom.xml`:
```xml
<plugin>
    <groupId>com.kumuluz.ee</groupId>
    <artifactId>config-generator-maven-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
    <executions>
        <execution>
            <goals>                                         
                <goal>generate-config-files</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

If you don't have an application ready, follow along! We'll be creating a very simple application that has two endpoints:
* `POST /movies`: insert a new movie. The name of the movie to be inserted must be contained in the body of the request;
* `GET /movies`: get list of all movies;

For simplicity, the application won't even be connected to an Azure SQL database. Of course, this is a bad practice, because when designing a serverless application, not unlike when designing a microservices application, we should ensure, that the single instances running the application are stateless, i.e. that no data is stored in memory. In our case, instead of storing the movie names in an external and persistent database, we do just that, i.e. we store the movies in the local memory. Because of that, we may loose the data. If you want to create a more serious application, you may refer to the sample project available in [another Github repository](https://github.com/BorisRado/azure-functions-serverless-tutorial-jdbc). 

First, create the skeleton application:
```bash
$ mvn -B archetype:generate \
    -DarchetypeGroupId=org.apache.maven.archetypes \
    -DgroupId=com.acme \
    -DartifactId=movies
$ cd movies
```
Add the required dependencies to the `pom.xml` file. In particular, add the regular dependencies as in a regular KumuluzEE application (core, jetty server, jax-rs, ...), and the Kumuluz serverless plugin. In the end, the file should look similar to the following one:
```yaml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.acme</groupId>
    <artifactId>movies</artifactId>
    <packaging>jar</packaging>
    <version>1.0-SNAPSHOT</version>
    <name>movies</name>

    <properties>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <kumuluzee.version>3.13.0-SNAPSHOT</kumuluzee.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.kumuluz.ee</groupId>
                <artifactId>kumuluzee-bom</artifactId>
                <version>${kumuluzee.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>com.kumuluz.ee</groupId>
            <artifactId>kumuluzee-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.kumuluz.ee</groupId>
            <artifactId>kumuluzee-servlet-jetty</artifactId>
        </dependency>
        <dependency>
            <groupId>com.kumuluz.ee</groupId>
            <artifactId>kumuluzee-jax-rs-jersey</artifactId>
        </dependency>
    </dependencies>

    <build>
        <finalName>movies_app</finalName>
        <plugins>
            <plugin>
                <groupId>com.kumuluz.ee</groupId>
                <artifactId>kumuluzee-maven-plugin</artifactId>
                <version>${kumuluzee.version}</version>
                <executions>
                    <execution>
                        <id>package</id>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>com.kumuluz.ee</groupId>
                <artifactId>serverless-maven-plugin</artifactId>
                <version>1.0-SNAPSHOT</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>azf-generate-config</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```
Note that you are required to use at least version `4.0.0` of the KumuluzEE framework, and note also, that the serverless plugin is still not available on the Maven central repository, so you are required to clone the [repository](https://github.com/BorisRado/azure-functions-plugin) and install it locally with the command `mvn clean install`.

You can now delete the pre-populated `App.java` class and Junit tests, and create the base application class (`CustomApplication.java`) and the class containing actual endpoints (`MoviesResource.java`), respectively with the following content:
```java
package com.acme;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("")
public class CustomApplication extends Application {
}
```
```java
package com.acme;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Produces(MediaType.APPLICATION_JSON)
@Path("movies")
public class MoviesResource {

    private static Logger log = Logger.getLogger(MoviesResource.class.getName());
    private static List<String> movies = new ArrayList<>();

    @GET
    public Response getMovies() {
        log.info("Received GET request!");
        return Response.status(Response.Status.OK).entity(movies).build();
    }

    @POST
    public Response postMovie(String movieName) {
        if (movieName.isEmpty()) {
            log.info("Received POST request with empty body.");
            return Response.status(Response.Status.BAD_REQUEST).build();
        } else {
            log.info("Received POST request for movie " + movieName + "!");
            movies.add(movieName);
            return Response.status(Response.Status.CREATED).build();
        }
    }

}
```


Please refer to the [Getting started tutorial from KumuluzEE](https://ee.kumuluz.com/tutorials/develop-microservices-with-java-ee-and-kumuluzee) to get more information regarding the steps we have taken when building the app.

### Testing locally
At this point, you should be able to build the application just like any other maven application:
```bash
mvn clean package
```
Now, there are two ways in which you can test your application:
* the *traditional* way: use the command `java -jar target/movies_app.jar`; this will start a process, that will listen on port `8080` and respond to any incoming request;
* with *azure func*: navigate to `target/azf-config` and notice, that the folder contains a `host.json` file, the `jar` containing all the logic and dependencies, and a folder for every endpoint. Inside that folder, run the command `func start -p 8080` so to simulate what will happen on the cloud: a host will be started, and will redirect the incoming HTTP traffic to the java process that will actually respond to the requests. For more information, see the [documentation for custom handlers](https://docs.microsoft.com/en-us/azure/azure-functions/functions-custom-handlers).

Either way, you should be able to navigate to `localhost:8080/movies` and get the responses from the microservice. For example, you can issue the following sequence of commands or use a tool such as Postman:
```bash
$ curl http://localhost:8080/movies
[]
$ curl -X POST http://localhost:8080/movies -d 'Harry Potter'
$ curl -X POST http://localhost:8080/movies -d 'The lord of the rings'
$ curl http://localhost:8080/movies
["Harry Potter","The lord of the rings"]
```

### Deploy on Azure Cloud
Now that we made sure everything works ok on our local computer, we are ready to deploy the app on the cloud! First, you need to create a deployment user, for example with the command `az webapp deployment user set --user-name <user> --password <password>`, or from the azure portal.

Next, you have to create a function app. With your browser, navigate to [the azure portal](https://portal.azure.com/#home), and then search for the `Function App service`. Click on `+Create` (on the top left), and configure the fields that appear - you can set the Function App name and region as you wish. When filling the required fields, make only sure to select Java with the correct version as the runtime stack, and to use Windows as the operating system - the deployment with the REST methods, that the plugin is using, fails when using a Linux OS on the consumption serverless plan.

When you filled the required fields, click on `Review + Create` and then on `Create`.

While the deployment is taking place, create a `.azf` file inside the root folder of the project (the one that contains the `pom.xml` file) with the following content:
```
AZF_USER=<user>
AZF_USER_PSW=<password>
FUNCTION_APP=<function_app>
```
Set `<user>` and `<password>` to the values of the deployment user, and replace the `<function_app>` with the Function App name of the Function you just created.

Finally, when the deployment has completed, run the command `mvn com.kumuluz.ee:serverless-maven-plugin:1.0-SNAPSHOT:azf-deploy` and in a minute or two, your app will be up and running on the cloud, and you will be able to issue the same sequence of commands shown above by using the Azure URL in place of `localhost:8080`.

### Conclusion
You have successfully deployed an application to a serverless environment! If you run into any problems while completing this tutorial, or spot any issues when using the serverless plugin, please feel free to open an issue or get in touch with us!

# Limitations
The plugins that we used in the tutorial still do not support [these use cases](https://stackoverflow.com/questions/25755130/nested-resources-in-jersey-jax-rs-how-to-implement-restangular-example/25775371#25775371).
