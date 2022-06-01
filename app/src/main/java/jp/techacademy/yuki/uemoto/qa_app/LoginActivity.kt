package jp.techacademy.yuki.uemoto.qa_app

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth //FirebaseAuthクラスの定義

    // 処理の完了を受け取るリスナーであるOnCompleteListenerクラスについて
    private lateinit var mCreateAccountListener: OnCompleteListener<AuthResult> //アカウント作成処理の定義
    private lateinit var mLoginListener: OnCompleteListener<AuthResult> //ログイン処理の定義
    private lateinit var mDataBaseReference: DatabaseReference //データベースの読み書きに必要なクラスの定義

    // アカウント作成時にフラグを立て、ログイン処理後に名前をFirebaseに保存する
    private var mIsCreateAccount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //mDataBaseReference(データベースの読み書きに必要なクラス)にFirebaseDatabaseへのリファレンスを取得
        //リファレンスはプログラミング分野における、開発の手順や関数・変数の参照文書を意味する語だそう
        mDataBaseReference = FirebaseDatabase.getInstance().reference

        // FirebaseAuthクラスのインスタンスを取得する
        mAuth = FirebaseAuth.getInstance()

        // mCreateAccountListenerはアカウント作成処理のリスナー。
        // 作成処理をOnCompleteListenerクラスで受け取る。onCompleteメソッドをオーバーライドする必要がある
        mCreateAccountListener = OnCompleteListener { task ->
            if (task.isSuccessful) { //引数で渡ってきたTaskクラスのisSuccessfulメソッドを使って、成功したかどうかを確認
                // 成功した場合

                // ログインする。loginメソッドのよび出し
                val email = emailText.text.toString()
                val password = passwordText.text.toString()
                login(email, password)

            } else {
                // 失敗した場合

                // エラーを表示する
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, getString(R.string.create_account_failure_message), Snackbar.LENGTH_LONG).show()

                // プログレスバーを非表示にする
                progressBar.visibility = View.GONE
            }
        }

        // mLoginListenerはログイン処理のリスナー。
        //ログイン処理をOnCompleteListenerクラスで受け取る。
        mLoginListener = OnCompleteListener { task ->

            if (task.isSuccessful) {
                // 成功した場合
                val user = mAuth.currentUser
                val userRef = mDataBaseReference.child(UsersPATH).child(user!!.uid)

                if (mIsCreateAccount) { //mIsCreateAccountを使ってアカウント作成ボタンを押してからのログイン処理か、ログインボタンをタップの場合かで処理を分ける
                    // アカウント作成の時は表示名をFirebaseに保存する
                    val name = nameText.text.toString()
                    val data = HashMap<String, String>()
                    //データをKeyとValueの組み合わせで保存。
                    data["name"] = name
                    //DatabaseReferenceが指し示すKeyにValueを保存するには setValue メソッドを使用します
                    userRef.setValue(data)

                    // 表示名をPreferenceに保存する
                    saveName(name)
                } else {
                    //ログインボタンをタップしたときは、Firebaseから表示名を取得してPreferenceに保存します
                    userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val data = snapshot.value as Map<*, *>?
                            saveName(data!!["name"] as String)
                        }

                        override fun onCancelled(firebaseError: DatabaseError) {}
                    })
                }

                // プログレスバーを非表示にする
                progressBar.visibility = View.GONE

                // Activityを閉じる
                finish()

            } else {
                // 失敗した場合
                // エラーを表示する
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, getString(R.string.login_failure_message), Snackbar.LENGTH_LONG).show()

                // プログレスバーを非表示にする
                progressBar.visibility = View.GONE
            }
        }

        // タイトルの設定
        title = getString(R.string.login_title)

        createButton.setOnClickListener { v ->
            //アカウント作成ボタンをタップした時、 InputMethodManager の hideSoftInputFromWindow メソッドを呼び出してキーボードを閉じ、
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            val email = emailText.text.toString()
            val password = passwordText.text.toString()
            val name = nameText.text.toString()

            if (email.length != 0 && password.length >= 6 && name.length != 0) {
                // ログイン時に表示名を保存するようにmIsCreateAccountにtrueを設定します（フラグを立てる）
                mIsCreateAccount = true
                //createAccountメソッドを呼び出してアカウント作成処理を開始
                createAccount(email, password)
            } else {
                // エラーを表示する
                Snackbar.make(v, getString(R.string.login_error_message), Snackbar.LENGTH_LONG).show()
            }
        }

        loginButton.setOnClickListener { v ->
            // キーボードが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            val email = emailText.text.toString()
            val password = passwordText.text.toString()

            if (email.length != 0 && password.length >= 6) {
                // フラグを落としておく
                mIsCreateAccount = false

                login(email, password)
            } else {
                // エラーを表示する
                Snackbar.make(v, getString(R.string.login_error_message), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    //そしてcreateAccountメソッドを呼び出してアカウント作成処理を開始させます
    private fun createAccount(email: String, password: String) {
        // プログレスバーを表示する
        progressBar.visibility = View.VISIBLE

        // FirebaseAuthクラスのcreateUserWithEmailAndPasswordメソッドでアカウントを作成する
        // createUserWithEmailAndPasswordメソッドの引数にはメールアドレス、パスワードを与え、
        // さらにaddOnCompleteListenerメソッドを呼び出してリスナーを設定します。
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(mCreateAccountListener)
    }

    //ログインボタンのタップした時には同様にキーボードを閉じ、loginメソッドを呼び出してログイン処理を開始させます。
    private fun login(email: String, password: String) {
        // プログレスバーを表示する
        progressBar.visibility = View.VISIBLE

        // FirebaseAuthクラスのsignInWithEmailAndPasswordメソッドでログイン処理
        // signInWithEmailAndPasswordメソッドの引数にはメールアドレス、パスワードを与え
        // さらにaddOnCompleteListenerメソッドを呼び出してリスナーを設定
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(mLoginListener)
    }

    //saveNameメソッドでは引数で受け取った表示名をPreferenceに保存。忘れずにcommitメソッドを呼び出して保存処理を反映
    private fun saveName(name: String) {
        // Preferenceに保存する
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sp.edit()
        editor.putString(NameKEY, name)
        editor.commit()
    }
}