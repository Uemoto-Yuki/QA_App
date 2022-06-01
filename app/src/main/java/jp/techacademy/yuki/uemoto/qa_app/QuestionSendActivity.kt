package jp.techacademy.yuki.uemoto.qa_app

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_question_send.*
import java.io.ByteArrayOutputStream

class QuestionSendActivity : AppCompatActivity(), View.OnClickListener,
    DatabaseReference.CompletionListener {
    companion object {
        private val PERMISSIONS_REQUEST_CODE = 100
        private val CHOOSER_REQUEST_CODE = 100
    }

    private var mGenre: Int = 0
    private var mPictureUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_send)

        // 渡ってきたジャンルの番号を取り出してmGenreで保持
        val extras = intent.extras
        mGenre = extras!!.getInt("genre")

        // UIの準備
        title = getString(R.string.question_send_title)

        sendButton.setOnClickListener(this)
        imageView.setOnClickListener(this)
    }

    //onActivityResultメソッドではIntent連携から戻ってきた時に画像を取得し、ImageViewに設定します。
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CHOOSER_REQUEST_CODE) {

            if (resultCode != Activity.RESULT_OK) {
                if (mPictureUri != null) {
                    contentResolver.delete(mPictureUri!!, null, null)
                    mPictureUri = null
                }
                return
            }

            // 画像を取得
            //dataがnullかdata.getData()の場合はカメラで撮影したときなので画像の取得にmPictureUriを使う
            val uri = if (data == null || data.data == null) mPictureUri else data.data

            // URIからBitmapを取得する
            val image: Bitmap
            try {
                val contentResolver = contentResolver
                val inputStream = contentResolver.openInputStream(uri!!)
                image = BitmapFactory.decodeStream(inputStream)
                inputStream!!.close()
            } catch (e: Exception) {
                return
            }

            // 取得したBimapの長辺を500ピクセルにリサイズする
            val imageWidth = image.width
            val imageHeight = image.height
            val scale = Math.min(500.toFloat() / imageWidth, 500.toFloat() / imageHeight) // (1)

            val matrix = Matrix()
            matrix.postScale(scale, scale)

            val resizedImage =
                Bitmap.createBitmap(image, 0, 0, imageWidth, imageHeight, matrix, true)

            // BitmapをImageViewに設定する
            imageView.setImageBitmap(resizedImage)

            mPictureUri = null
        }
    }

    //添付画像を選択・表示するImageViewをタップした時と、投稿ボタンをタップした時の処理
    override fun onClick(v: View) {

        if (v === imageView) {
            // パーミッションの許可状態を確認する
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //Androidversion が6.0以降の場合
                //checkSelfPermissionメソッドで外部ストレージへの書き込みが許可されているか確認
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    // 許可されている
                    showChooser() //Intent連携でギャラリーとカメラを選択するダイアログを表示させるshowChooserメソッドを呼び出し
                } else {
                    // 許可されていないので許可ダイアログを表示する
                    requestPermissions(
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PERMISSIONS_REQUEST_CODE
                    )

                    return
                }
            } else {
                showChooser() //Android5以前の場合はパーミッションの許可状態を確認せずにshowChooserメソッドを呼び出し
            }
        } else if (v === sendButton) {
            // 投稿ボタンがタップされた時はキーボードを閉じ
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS)

            var dataBaseReference = FirebaseDatabase.getInstance().reference
            val genreRef = dataBaseReference.child(ContentsPATH).child(mGenre.toString())

            val data = HashMap<String, String>()

            // UID
            data["uid"] = FirebaseAuth.getInstance().currentUser!!.uid

            // タイトルと本文を取得する
            val title = titleText.text.toString()
            val body = bodyText.text.toString()

            if (title.isEmpty()) {
                // タイトルが入力されていない時はエラーを表示するだけ
                Snackbar.make(v, getString(R.string.input_title), Snackbar.LENGTH_LONG).show()
                return
            }

            if (body.isEmpty()) {
                // 質問が入力されていない時はエラーを表示するだけ
                Snackbar.make(v, getString(R.string.question_message), Snackbar.LENGTH_LONG).show()
                return
            }

            // Preferenceから名前を取る
            val sp = PreferenceManager.getDefaultSharedPreferences(this)
            val name = sp.getString(NameKEY, "")

            data["title"] = title
            data["body"] = body
            data["name"] = name!!

            // 添付画像を取得する
            // as? は安全なキャスト演算子と言うもので、キャストに失敗したらnullを返す
            val drawable = imageView.drawable as? BitmapDrawable

            // 添付画像が設定されていれば画像を取り出してBASE64エンコードする
            // Firebaseは文字列や数字しか保存できませんがこうすることで画像をFirebaseに保存可能
            if (drawable != null) {
                val bitmap = drawable.bitmap
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val bitmapString = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

                data["image"] = bitmapString
            }

            //保存する際はDatabaseReferenceクラスのsetValueを使う
            //第2引数にはCompletionListenerクラスを指定します（今回はActivityがCompletionListenerクラスを実装している）。
            genreRef.push().setValue(data, this)
            progressBar.visibility = View.VISIBLE
        }
    }

    //onRequestPermissionsResultメソッドは許可ダイアログでユーザが選択した結果を受け取ります
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                // if (grantResults[0] == PackageManager.PERMISSION_GRANTED)とすることで許可したかどうかを判断できる
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 許可された場合はshowChooserメソッドを呼び出し
                    showChooser()
                }
                return
            }
        }
    }

    // showChooserメソッドではギャラリーから選択するIntentとカメラで撮影するIntentを作成
    // さらにそれらを選択するIntentを作成してダイアログを表示させます
    private fun showChooser() {
        // ギャラリーから選択するIntentを作成（IntentのKeyの種類？はお決まりルールっぽい。）
        //ギャラリーから画像を選択するときは Intent.ACTION_GET_CONTENT を使いますよ
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.type = "image/*"
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE)

        // カメラで撮影するIntent作成
        //カメラを起動して撮影した画像を取得するときは MediaStore.ACTION_IMAGE_CAPTURE を使いますよ
        val filename = System.currentTimeMillis().toString() + ".jpg"
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, filename)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

        /* What's contentResolver!!!????
        ContentResolverは、あるファイルの位置に関して
        "content://・・・"という形式(以下contentスキーマ)で表現したURIを、
        そのファイルに関する他の情報によって検索、取得するためのテーブルを管理、利用する機能を提供するクラス
         */
        mPictureUri = contentResolver
            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPictureUri)

        // ギャラリー選択のIntentを与えてcreateChooserメソッドを呼ぶ
        // 第一引数にギャラリーから選択するIntentを指定、第二引数にダイアログに表示させるタイトルを指定
        val chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.get_image))

        // chooserIntentに二個目のIntentを与える。 =ギャラリー選択Intentにカメラ撮影のIntentが代入される。
        // EXTRA_INITIAL_INTENTSにカメラ撮影のIntentを追加
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))

        startActivityForResult(chooserIntent, CHOOSER_REQUEST_CODE)
    }

    //onCompleteメソッドについて。Firebaseへの保存が完了したらfinishメソッドを呼び出してActivityを閉じる
    override fun onComplete(databaseError: DatabaseError?, databaseReference: DatabaseReference) {
        progressBar.visibility = View.GONE

        if (databaseError == null) {
            finish()
        } else {
            Snackbar.make(
                findViewById(android.R.id.content),
                getString(R.string.question_send_error_message),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }
}