import sbt.Keys._
import sbt._

object Compiler {

  private def unmanaged(ver: String, base: File): Seq[File] =
    CrossVersion.partialVersion(ver) match {
      case Some((2, 13)) =>
        Seq(base / "scala-2.13+")

      case _ =>
        Seq(base / "scala-2.13-")

    }

  private val silencerVer = Def.setting[String]("1.17.13")

  lazy val settings = Seq(
    Compile / unmanagedSourceDirectories ++= {
      unmanaged(scalaVersion.value, (Compile / sourceDirectory).value)
    },
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-unchecked",
      "-deprecation",
      "-feature",
      "-Xfatal-warnings",
      "-language:higherKinds"
    ),
    scalacOptions ++= {
      if (scalaBinaryVersion.value startsWith "2.") {
        Seq(
          "-target:jvm-1.8",
          "-Xlint",
          "-g:vars"
        )
      } else Seq()
    },
    scalacOptions ++= {
      val sv = scalaBinaryVersion.value

      if (sv == "2.12") {
        Seq(
          "-Xmax-classfile-name",
          "128",
          "-Ywarn-numeric-widen",
          "-Ywarn-dead-code",
          "-Ywarn-value-discard",
          "-Ywarn-infer-any",
          "-Ywarn-unused",
          "-Ywarn-unused-import",
          "-Xlint:missing-interpolator",
          "-Ywarn-macros:after"
        )
      } else if (sv == "2.11") {
        Seq(
          "-Xmax-classfile-name",
          "128",
          "-Yopt:_",
          "-Ydead-code",
          "-Yclosure-elim",
          "-Yconst-opt"
        )
      } else if (sv == "2.13") {
        Seq(
          "-explaintypes",
          "-Werror",
          "-Wnumeric-widen",
          "-Wdead-code",
          "-Wvalue-discard",
          "-Wextra-implicit",
          "-Wmacros:after",
          "-Wunused"
        )
      } else {
        Seq("-Wunused:all", "-language:implicitConversions")
      }
    },
    Compile / console / scalacOptions ~= {
      _.filterNot(o =>
        o.startsWith("-X") || o.startsWith("-Y") || o.startsWith("-P:silencer")
      )
    },
    Test / scalacOptions ~= {
      _.filterNot(_ == "-Xfatal-warnings")
    },
    libraryDependencies ++= {
      // Silencer
      if (!scalaBinaryVersion.value.startsWith("3")) {
        val silencerVersion = "1.17.13"

        Seq(
          compilerPlugin(
            ("com.github.ghik" %% "silencer-plugin" % silencerVersion)
              .cross(CrossVersion.full)
          ),
          ("com.github.ghik" %% "silencer-lib" % silencerVersion % Provided)
            .cross(CrossVersion.full)
        )
      } else Seq.empty
    },
    // Mock silencer for Scala3
    Test / doc / scalacOptions ++= List("-skip-packages", "com.github.ghik"),
    Compile / packageBin / mappings ~= {
      _.filter { case (_, path) => !path.startsWith("com/github/ghik") }
    },
    Compile / packageSrc / mappings ~= {
      _.filter { case (_, path) => path != "silent.scala" }
    },
    // --
    Compile / doc / scalacOptions ~= {
      _.filterNot(excludeOpt)
    },
    Compile / console / scalacOptions ~= {
      _.filterNot(excludeOpt)
    },
    Test / console / scalacOptions ~= {
      _.filterNot(excludeOpt)
    },
    Test / console / scalacOptions += "-Yrepl-class-based"
  )

  private lazy val excludeOpt: String => Boolean = { opt =>
    (opt.startsWith("-X") && opt != "-Xmax-classfile-name") ||
    opt.startsWith("-Y") || opt.startsWith("-W") ||
    opt.startsWith("-P:silencer") ||
    opt.startsWith("-P:semanticdb")
  }
}
