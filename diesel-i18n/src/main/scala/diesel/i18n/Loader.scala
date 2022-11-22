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

import scala.util.Try
import java.io.{ByteArrayInputStream, InputStreamReader}

object Loader {

  def loadProperties(content: String): Option[Map[String, MessageFormat]] = {
    import scala.jdk.CollectionConverters.*
    val result =
      Try {
        val properties = new java.util.Properties()
        properties.load(new InputStreamReader(
          new ByteArrayInputStream(content.getBytes()),
          "utf-8"
        ))
        properties
      } map {
        _.asScala.toMap
      } map {
        _.view.mapValues(MessageFormat(_)).toMap
      }
    result.toEither
      .swap
      .map(e => throw new IllegalArgumentException(s"parsing properties failed", e))
      .swap
      .toOption
  }

}
