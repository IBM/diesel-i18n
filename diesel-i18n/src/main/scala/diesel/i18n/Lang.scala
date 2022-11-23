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

enum Lang(val lang: String) {
  case EN   extends Lang("en")
  case DE   extends Lang("de")
  case ES   extends Lang("es")
  case FR   extends Lang("fr")
  case IT   extends Lang("it")
  case JA   extends Lang("ja")
  case PT   extends Lang("pt")
  case ZH   extends Lang("zh")
  case ZHTW extends Lang("zh_tw")
}

object Lang {

  def apply(lang: String): Option[Lang] = Lang.values.find(_.lang == lang)

  // see https://tools.ietf.org/rfc/bcp/bcp47.txt
  def fromNavigator(language: String): Option[Lang] = {
    apply(language.toLowerCase)
      .orElse(apply(language.replace('-', '_').toLowerCase))
      .orElse(apply(language.split('-')(0).toLowerCase))
  }
}
