
//Specifies that there is a project called 'root' that lives in directory "."
lazy val root = (project in file(".")).
  settings(
    name := "Fireflower",
    version := "1.0",
    scalaVersion := "2.11.8",

    fork in run := true,

    mainClass in assembly := Some("fireflower.PlayerTests"),

    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-language:existentials",
      "-unchecked",
      "-Xfatal-warnings",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Xfuture"
    ),

    javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint"),
    
    initialize := {
     val _ = initialize.value
     if (sys.props("java.specification.version") != "1.8")
         sys.error("Java 8 is required for this project.")
    }

  )
