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

import scala.collection.mutable

case class MessageFormat(segments: Seq[MessageFormat.Segment]) {
  import MessageFormat._
  def apply(args: String*): String                      = {
    segments.foldLeft(new StringBuilder) {
      case (acc, ParameterSegment(i)) => args.lift(i).map(acc.append).getOrElse(acc)
      case (acc, TextSegment(text))   => acc.append(text)
    }.toString
  }
  def withArity(count: Int): Either[Int, MessageFormat] = {
    val actual = arity(this)
    if (actual <= count)
      Right(this)
    else
      Left(actual)
  }
  def withPrefix(prefix: String): MessageFormat         = MessageFormat(TextSegment(prefix) +: segments)
}

object MessageFormat {
  sealed trait Segment
  case class TextSegment(text: String)    extends Segment
  case class ParameterSegment(index: Int) extends Segment

  def apply(format: String): MessageFormat = MessageFormat(parse(format))

  def text(text: String): MessageFormat = MessageFormat(Seq(TextSegment(text)))

  def arity(messageFormat: MessageFormat): Int = messageFormat.segments.flatMap {
    case ParameterSegment(i) => Some(i)
    case _                   => None
  }
    .maxOption
    .map(_ + 1)
    .getOrElse(0)

  private def parse(format: String): Seq[Segment] = {
    val r          = "\\{\\d+}".r
    val parts      = r.findAllMatchIn(format)
    val paramParts = mutable.ArrayBuffer[(Int, Int)]()
    for (p <- parts) {
      var paramPart = true
      val start     = p.start
      if (start > 0) {
        val prevChar = format.charAt(start - 1)
        if (prevChar == '\'') {
          paramPart = false
          // look back again, might be escaped !
          if (start > 1) {
            val prevChar2 = format.charAt(start - 2)
            if (prevChar2 == '\'') {
              // escaped ! it's a param part
              paramPart = true
            }
          }
        }
      }
      if (paramPart) {
        paramParts.addOne((p.start, p.end))
      }
    }

    val res =
      if (paramParts.isEmpty) {
        Seq(TextSegment(format))
      } else {
        val segments = mutable.ArrayBuffer[Segment]()
        var lastEnd  = 0
        for (((start, end), index) <- paramParts.zipWithIndex) {
          val textBefore = format.substring(lastEnd, start)
          if (textBefore.nonEmpty) {
            segments.addOne(TextSegment(textBefore))
          }
          val paramValue = format.substring(start + 1, end - 1)
          val paramIndex = paramValue.toInt
          segments.addOne(ParameterSegment(paramIndex))
          lastEnd = end
          if (index == paramParts.length - 1 && lastEnd < format.length) {
            segments.addOne(TextSegment(format.substring(lastEnd)))
          }
        }
        segments.toSeq
      }

    // sanitize : remove all non escaped double quotes
    res.map {
      case TextSegment(text)    =>
        TextSegment(sanitizeDblQuotes(text))
      case ps: ParameterSegment =>
        ps
    }
  }

  private def sanitizeDblQuotes(s: String): String = {
    val sb = new mutable.StringBuilder()
    for ((c, i) <- s.zipWithIndex) {
      if (c == '\'') {
        // eat unless escaped (look before)
        if (i > 0) {
          val prevChar = s.charAt(i - 1)
          if (prevChar == '\'') {
            sb.append('\'')
          }
        }
      } else {
        sb.append(c)
      }
    }
    sb.toString()
  }

}
