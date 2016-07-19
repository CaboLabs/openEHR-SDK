openEHR-OPT
===========

Groovy Support of openEHR Operational Templates for CaboLabs Grails projects

This will be used in CaboLabs apps like EHRGen, EHRServer, EMRApp and XML Rule Engine.


Maven
=====

This project is a maven artifactory that use a github branch as static repository using the [maven-site plugin](https://github.com/github/maven-plugins)

Prepare
------

* Add a new server configuration into your mvn settings $HOME/.m2/settings.xml:

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_USERNAME</username>
      <password>YOUR_PASSWORD</password>
    </server>
  </servers>
</settings>
```

* Customize the repository with your github username

```xml
<repositoryOwner>jagedn</repositoryOwner>    <!-- github username -->
```

Deploy a new version
--------------------

When you are ready to distribute a new version, follow this steps:

* Update the version of the artifact at pom.xml

```xml
<groupId>com.cabolabs</groupId>
<artifactId>openEHR-OPT</artifactId>
<packaging>jar</packaging>
<version>1.0</version>
```
* Deploy

```console
$mvn clean deploy
```

Import
------

To import this artifact into your project you need to include the repository and the dependency

* Maven project
```xml
<repositories>
  <repository>
    <id>github</id>
    <name>openEHR OPT at github</name>
    <layout>default</layout>
    <url>https://raw.github.com/jagedn/openEHR-OPT/mvn-repo/</url>
  </repository>
</repositories>
<dependency>
  <groupId>com.cabolabs</groupId>
	<artifactId>openEHR-OPT</artifactId>
	<version>1.0</version>
</dependency>
```

* Gradle project

```groovy
   mavenRepo "https://raw.github.com/jagedn/openEHR-OPT/mvn-repo/" 
```
