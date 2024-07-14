import diesel.i18n.I18nPlugin

lazy val root = (project in file("."))
  .enablePlugins(I18nPlugin)
  .settings(
    scalaVersion  := "2.12.19",
    i18nClassName := "com.myco.myapp.MyI18n"
  )

//
//val i18n: I18n = I18n.load(com.myco.myapp.Generated.theMap)
//
//val myMsg = i18n(LANG).myMsg("toto")
//
//object Generated {
//
//  override val theMap: Map[String, String] = ??? // generated...
//
//}
