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

import scala.quoted.*
import scala.compiletime.error

import frawa.inlinefiles.compiletime.FileContents

import diesel.i18n.Lang
import diesel.i18n.MessageFormat.{Segment, TextSegment, ParameterSegment}

object Macros {

  type Props = Map[String, MessageFormat]

  inline def inlineMessages(
    inline path: String,
    inline langByFilename: Map[String, String]
  ): Map[Lang, Props] = ${
    inlineMessages_impl('path, 'langByFilename)
  }

  private def inlineMessages_impl(
    path: Expr[String],
    langByFilename: Expr[Map[String, String]]
  )(using Quotes): Expr[Map[Lang, Props]] =
    given ToExpr[Segment] with
      def apply(v: Segment)(using Quotes): Expr[Segment] =
        v match {
          case ParameterSegment(index) =>
            val vv = Expr(index)
            '{ ParameterSegment($vv) }
          case TextSegment(text)       =>
            val vv = Expr(text)
            '{ TextSegment($vv) }
        }
    given ToExpr[MessageFormat] with
      def apply(v: MessageFormat)(using Quotes): Expr[MessageFormat] =
        val vv = Expr(v.segments)
        '{ MessageFormat(segments = $vv) }

    given ToExpr[Lang] with
      def apply(v: Lang)(using Quotes): Expr[Lang] =
        val vv = Expr(v.lang)
        '{ Lang($vv).get }

    val langByFile = langByFilename.valueOrAbort
    Expr(FileContents.parseTextContentsIn(path.valueOrAbort, ".properties", true) { content =>
      Loader.loadProperties(content).get
    }.map { (file, props) =>
      val lang = langByFile.get(file).getOrElse {
        throw IllegalArgumentException(
          s"missing language for file '$file'"
        )
      }
      Lang(lang).map { lang =>
        (lang, props)
      }.getOrElse {
        throw IllegalArgumentException(
          s"unsupported language '$lang' for file '$file'"
        )
      }
    })

}
