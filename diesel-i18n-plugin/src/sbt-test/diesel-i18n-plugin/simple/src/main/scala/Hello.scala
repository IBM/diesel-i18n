/*
 * IBM Confidential
 * OCO Source Materials
 * 5737-I23
 * Copyright IBM Corp. 2021, 2021
 * The source code for this program is not published or otherwise divested of its trade secrets,
 * irrespective of what has been deposited with the U.S Copyright Office.
 */
import com.myco.myapp.MyI18n

object Main extends App {
  println("Test 1 :")
  println(MyI18n.messages)
  val messagesEn = MyI18n.messages("en")
  println("Test 2 :")
  println(messagesEn)
  assert(messagesEn.contains("invalidType"))
}
