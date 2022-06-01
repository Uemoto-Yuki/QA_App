package jp.techacademy.yuki.uemoto.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*
import kotlinx.android.synthetic.main.activity_question_detail.fab

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private lateinit var mFavoriteRef: DatabaseReference
    private var isFavorite = false

    private val mEventListener = object : ChildEventListener {
        //onChildAdded はアイテムのリストを取得。また、アイテムのリストへの追加をリッスンする。
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>

            val answerUid = dataSnapshot.key ?: ""
            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }
            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()

        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {
        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {
        }

        override fun onCancelled(databaseError: DatabaseError) {
        }
    }

    private val mFavoriteEventListener = object : ChildEventListener {
        //onChildAdded はアイテムのリストを取得。また、アイテムのリストへの追加をリッスンする。
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            Log.d("questionUid_key", dataSnapshot.key.toString())
            isFavorite = true

            favorite_button.apply {
                setImageResource(R.drawable.ic_star)
            }

        }
        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
        }
        override fun onChildRemoved(dataSnapshot: DataSnapshot) {
        }
        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {
        }
        override fun onCancelled(databaseError: DatabaseError) {
        }
    }

    //渡ってきたQuestionクラスのインスタンスを保持し、タイトルを設定
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras!!.get("question") as Question

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()


        //FABをタップしたらログインしていなければログイン画面に遷移させ、ログインしていれば後ほど作成する回答作成画面に遷移させる準備をしておきます
        fab.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)

            } else { // QuestionDetailActivityからAnswerSendActivityに遷移する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion) //// Questionを渡して回答作成画面を起動する
                startActivity(intent)
            }
        }

        //Firebaseへのリスナーの登録。回答作成画面から戻ってきた時にその回答を表示させるために登録しておきます
        var dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString())
            .child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)

        val user = FirebaseAuth.getInstance().currentUser


        //お気に入りボタンが押されたらfirebaseを参照して、お気に入りに追加・削除できるようにする
        favorite_button.setOnClickListener {
            if (!isFavorite) {
                val user = FirebaseAuth.getInstance().currentUser
                var dataBaseReference = FirebaseDatabase.getInstance()
                mFavoriteRef =
                    dataBaseReference.getReference(FavoritesPATH)
                        .child(user!!.uid) // mFavoriteRefは、useruidまでのパス（道しるべ）
                // 道しるべにそのままバリューはセットできないのでばらけさせて。。。
                // 親個体(useruid)に、導入したい値の箱=mQuestion.QestionUidを付着させて、値（Value）を導入する
                val data = HashMap <String, String>()
                data["genre"] = mQuestion.genre.toString()

                mFavoriteRef.child(mQuestion.questionUid).setValue(data)

                favorite_button.setImageResource(R.drawable.ic_star)
                Toast.makeText(applicationContext, "お気に入りに追加しました", Toast.LENGTH_SHORT).show()

            } else {
                var dataBaseReference = FirebaseDatabase.getInstance()
                mFavoriteRef =
                    dataBaseReference.getReference(FavoritesPATH).child(user!!.uid)
                mFavoriteRef.child(mQuestion.questionUid).removeValue()
                favorite_button.setImageResource(R.drawable.ic_star_border)
                Toast.makeText(applicationContext, "お気に入りから削除しました", Toast.LENGTH_SHORT).show()
            }
            isFavorite = !isFavorite //falseはtrueに、trueはfalseにして返す
        }
    }

    override fun onResume() {
        super.onResume()
        var dataBaseReference = FirebaseDatabase.getInstance().reference
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            // ログインしていなければお気に入りFloatingbuttonを非表示
            favorite_button.visibility = View.INVISIBLE
        } else {
            //　ログインしていればお気に入りFloatingbuttonを表示
            favorite_button.visibility = View.VISIBLE
            mFavoriteRef = dataBaseReference.child(FavoritesPATH).child(user!!.uid).child(mQuestion.questionUid)
            mFavoriteRef.addChildEventListener(mFavoriteEventListener)
        }


    }
}



