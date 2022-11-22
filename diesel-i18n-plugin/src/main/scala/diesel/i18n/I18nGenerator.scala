/*
 * Copyright 2021 The Diesel Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package diesel.i18n

import sbt.io.IO

import java.nio.file.Path
import java.io.File

object I18nGenerator {
  val locales: Seq[String] = Seq("en", "de", "es", "fr", "it", "ja", "pt", "zh", "zh_tw")

  private def i18nInputFilesWithLocale(source: Path): Seq[(String, File)] = {
    locales
      .map { locale => (locale, source.resolve(s"Messages_$locale.properties")) }
      .map { case (locale, path) => (locale, path.toFile) }
  }

  def i18nInputFiles(source: Path): Seq[File] = i18nInputFilesWithLocale(source).map(_._2)

  def i18nFiles(source: Path, pkg: String, objName: String): String = {
    val files = i18nInputFilesWithLocale(source)
      .map { case (locale, file) => (locale, IO.readLines(file)) }
      .map { case (locale, lines) => (locale, lines.mkString("\n")) }
      .map { case (locale, content) => s""" "$locale" -> \"\"\"$content\"\"\"""" }

    s"""package $pkg
       |
       |object $objName {
       |  // $$COVERAGE-OFF$$
       |  def messages: Map[String,String] = Map(
       |    ${files.mkString(",\n    ")}
       |  )
       |  // $$COVERAGE-ON$$
       |}""".stripMargin
  }
}
