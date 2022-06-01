package jp.techacademy.yuki.uemoto.qa_app

import java.io.Serializable
import java.util.ArrayList

//Firebaseから取得した質問のデータを保持するモデルクラスとしてQuestionクラスを作成。
//プロパティはvalで定義し、コンストラクタで値を設定。
// Serializableクラスを実装している理由はIntentでデータを渡せるようにするため
/*
title: Firebaseから取得したタイトル
body: Firebaseから取得した質問本文
name: Firebaseから取得した質問者の名前
uid: Firebaseから取得した質問者のID
questionUid: Firebaseから取得した質問のUID
genre: 質問のジャンル
imageBytes: Firebaseから取得した画像をbyte型の配列にしたもの
answers: Firebaseから取得した質問のモデルクラスであるAnswerのArrayList
 */
class Question(val title: String, val body: String, val name: String, val uid: String, val questionUid: String,
               val genre: Int, bytes: ByteArray, val answers: ArrayList<Answer>) : Serializable {


    val imageBytes: ByteArray

    init {
        imageBytes = bytes.clone()
    }
}