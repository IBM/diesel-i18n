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

import sbt.Keys._
import sbt._
import sbt.io.IO
import sbt.Tracked

import java.io.File

object I18nPlugin extends AutoPlugin {
  override val trigger: PluginTrigger = noTrigger

  override val requires: Plugins = plugins.JvmPlugin

  object autoImport extends I18nKeys
  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    i18nDir       := baseDirectory.value / "src" / "main" / "i18n",
    i18nClassName := "",
    Compile / sourceGenerators += genI18Task.taskValue
  )

  private def genI18Task = Def.task {
    import sbt.util.CacheImplicits._

    val log             = sLog.value
    val targetClassName = i18nClassName.value

    log.debug("diesel-i18n-plugin : generating source files from messages")
    log.debug(" * i18nDir       : " + i18nDir.value)
    log.debug(" * i18nClassName : " + i18nClassName.value)
    if (!i18nDir.value.exists()) {
      throw new IllegalArgumentException("i18nDir not found : " + i18nDir.value)
    }
    if (targetClassName.isEmpty) {
      throw new IllegalArgumentException("i18nClassName cannot be empty")
    }
    val parts        = targetClassName.split("\\.")
    val className    = parts.last
    val fileName     = className + ".scala"
    val packageParts = parts.dropRight(1)
    val filePath     = packageParts.mkString("/") + "/" + fileName
    val targetFile   = new File((Compile / sourceManaged).value, filePath)

    val cache      = streams.value.cacheDirectory / "I18nPlugin"
    val cachedTask =
      Tracked.inputChanged(cache / "input") { (inChanged, in: Seq[ModifiedFileInfo]) =>
        if (inChanged) {
          val s =
            I18nGenerator.i18nFiles(i18nDir.value.toPath, packageParts.mkString("."), className)
          IO.write(targetFile, s)
          log.info("I18nFiles written to " + targetFile)
        } else {
          log.debug("I18nFiles up-to-date in " + targetFile)
        }
      }
    val inputs     =
      I18nGenerator.i18nInputFiles(i18nDir.value.toPath).map(f => FileInfo.lastModified(f))
    cachedTask(inputs)
    Seq(targetFile)
  }

  private def logDetails() = {}
}
