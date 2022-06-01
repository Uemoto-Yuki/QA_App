package jp.techacademy.yuki.uemoto.qa_app

import java.io.Serializable

//質問の回答のモデルクラス。プロパティを以下のように作成し、それぞれGetterを用意する。
// body: Firebaseから取得した回答本文
// name: Firebaseから取得した回答者の名前
// uid: Firebaseから取得した回答者のUID
// answeruid: Firebaseから取得した回答のUID
//このAnswerクラスは、Questionクラスの中で保持されているクラス。
class Answer(val body: String, val name: String, val uid: String, val answerUid: String) : Serializable
