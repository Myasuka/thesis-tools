name := "MllibMatrix"

version := "1.0"

scalaVersion := "2.10.4"

libraryDependencies +=  "org.apache.spark" % "spark-mllib_2.10" % "1.5.2"

resolvers += Resolver.mavenLocal

resolvers += "Local Maven Repository" at "file:///D:/.m2/repository"