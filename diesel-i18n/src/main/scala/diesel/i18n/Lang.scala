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

object Lang extends Enumeration {
  protected case class LangVal(lang: String) extends super.Val(lang)

  import scala.language.implicitConversions
  implicit def valueToLangVal(x: Value): LangVal = x.asInstanceOf[LangVal]

  // needs to be in sync with diesel-i18n-plugin's diesel.i18n.I18nGenerator
  val EN: LangVal   = LangVal("en")
  val DE: LangVal   = LangVal("de")
  val ES: LangVal   = LangVal("es")
  val FR: LangVal   = LangVal("fr")
  val IT: LangVal   = LangVal("it")
  val JA: LangVal   = LangVal("ja")
  val PT: LangVal   = LangVal("pt")
  val ZH: LangVal   = LangVal("zh")
  val ZHTW: LangVal = LangVal("zh_tw")

  def apply(lang: String): Option[Value] = values.find(_.lang == lang)

  // see https://tools.ietf.org/rfc/bcp/bcp47.txt
  def fromNavigator(language: String): Option[Value] = {
    apply(language.toLowerCase)
      .orElse(apply(language.replace('-', '_').toLowerCase))
      .orElse(apply(language.split('-')(0).toLowerCase))
  }
}
