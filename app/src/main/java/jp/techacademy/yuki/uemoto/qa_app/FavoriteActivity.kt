package jp.techacademy.yuki.uemoto.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ListView
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FavoriteActivity : AppCompatActivity() {

    //プロパティとしてFirebaseへのアクセスに必要なDatabaseReferenceクラスと、ListView、QuestionクラスのArrayList、QuestionsListAdapterを定義
    private lateinit var mFavoriteRef: DatabaseReference
    private lateinit var mAdapter: QuestionsListAdapter
    private lateinit var mQuestionRef: DatabaseReference
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mListView: ListView

    //データに追加・変化があった時に受け取るのがChildEventListener
    private val mFavoriteEventListener = object : ChildEventListener {
        //onChildAddedメソッドは、要素が追加されたとき、つまり質問が追加された時に呼ばれるメソッド
        //この中でQuestionクラスとAnswerを作成し、ArrayListに追加
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String> //valueのみ
            val questionUid = dataSnapshot.key ?: "" //Keyのみ
            val genre = map["genre"] ?: ""
            Log.d("map", questionUid)
            Log.d("map", genre)
            Log.d("map", dataSnapshot.toString()) //DataSnapshot { key = -N2Aq34Ea9GYKtKyjRUP, value = {genre=1} }

            val dataBaseReference = FirebaseDatabase.getInstance().reference
            mQuestionRef = dataBaseReference.child(ContentsPATH).child(genre).child(questionUid)

            mQuestionRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) { //check
                    val map = snapshot.value as Map<String, String>
                    val title = map["title"] ?: ""
                    val body = map["body"] ?: ""
                    val name = map["name"] ?: ""
                    val uid = map["uid"] ?: ""
                    val imageString = map["image"] ?: ""
                    val bytes =
                        if (imageString.isNotEmpty()) {
                            Base64.decode(imageString, Base64.DEFAULT)
                        } else {
                            byteArrayOf()
                        }
                    val answerArrayList = ArrayList<Answer>()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            answerArrayList.add(answer)
                        }
                    }
                    Log.d("map", snapshot.toString())//

                    val question = Question(
                        title, body, name, uid, dataSnapshot.key ?: "", genre.toInt(), bytes, answerArrayList
                    )
                    mQuestionArrayList.add(question)
                    mAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
        }
        override fun onChildRemoved(p0: DataSnapshot) {
        }
        override fun onChildMoved(p0: DataSnapshot, p1: String?) {
        }
        override fun onCancelled(p0: DatabaseError) {
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)
        mListView = findViewById(R.id.listView)
        mAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
        mAdapter.notifyDataSetChanged()

        title = "お気に入り"

        mListView.setOnItemClickListener { parent, view, position, id ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }

    }

    override fun onResume() {
        super.onResume()

        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        mListView.adapter = mAdapter

        val user = FirebaseAuth.getInstance().currentUser
        val dataBaseReference = FirebaseDatabase.getInstance().reference

        mFavoriteRef = dataBaseReference.child(FavoritesPATH).child(user!!.uid)
        mFavoriteRef.addChildEventListener(mFavoriteEventListener)

    }
}