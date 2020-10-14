package jp.techacademy.chika.kaburagi.autoslideshowapp

import android.Manifest
import android.app.AlertDialog
import android.content.ContentUris
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), View.OnClickListener {
    // 画像権限
    private val PERMISSIONS_REQUEST_CODE = 100
    // 画像操作関連
    private var images = ArrayList<Uri>() // 画像URLを配列に
    private var maxSize: Int = 0          // 全画像数
    private var imgIndex: Int = 0         // images配列の添字

    // タイマー用の時間のための変数
    private var mTimer: Timer? = null
    private var mHandler = Handler(Looper.getMainLooper())
    private var flag: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Android 6.0以降の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 許可されている(既に許可されている時に表示)
                getContentsInfo()
                next_button.setOnClickListener(this)
                back_button.setOnClickListener(this)
                play_button.setOnClickListener(this)
            } else {
                // 許可されていないので許可ダイアログを表示する
                requestPermissions(
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        PERMISSIONS_REQUEST_CODE
                )
            }
            // Android 5系以下の場合
        } else {
            getContentsInfo()
            next_button.setOnClickListener(this)
            back_button.setOnClickListener(this)
            play_button.setOnClickListener(this)
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 1番最初の許可時に表示
                    getContentsInfo()
                    next_button.setOnClickListener(this)
                    back_button.setOnClickListener(this)
                    play_button.setOnClickListener(this)
                } else {
                    showAlertDialog()
                }
        }

    }
    private fun showAlertDialog() {
        // AlertDialog.Builderクラスを使ってAlertDialogの準備をする
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder
            .setTitle("確認")
            .setMessage("アプリを利用するには、ストレージ権限を許可してください。アプリを終了します。")
            .setNegativeButton("戻る") { _, _ ->
                requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSIONS_REQUEST_CODE
                )
            }
            // ダイアログ内の終了ボタンで終了
            .setPositiveButton("終了") { _, _->
                finish()
            // ダイアログ外押下、バックボタン押下時も終了
            }.setOnCancelListener {
                finish()
            }.show()
    }

    private fun getContentsInfo() {
        // 画像の情報を取得する
        val resolver = contentResolver
        val cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
                null, // 項目(null = 全項目)
                null, // フィルタ条件(null = フィルタなし)
                null, // フィルタ用パラメータ
                null // ソート (null ソートなし)
        )

        if (cursor!!.moveToFirst()) {
            do {
                // indexからIDを取得し、そのIDから画像のURIを取得する
                val fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                var id = cursor.getLong(fieldIndex)
                var imageUri =
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                // uri(URL)を配列に追加
                images.add(imageUri)
            } while (cursor.moveToNext())

            // 配列の要素数(maxのindex)を取得,配列のkeyとずれるので-1
            maxSize = images.size - 1

            // 最初の表示
            imageView.setImageURI(images[imgIndex])

        }

    }

    override fun onClick(v: View) {
        // 進むを押した時
        if (v.id == R.id.next_button) {
            if (maxSize > imgIndex) {
                imgIndex += 1
            } else {
                imgIndex = 0
            }
        // 戻るを押した時
        } else if(v.id == R.id.back_button){
            if (0 < imgIndex) {
                imgIndex -= 1
            } else {
                imgIndex = maxSize
            }
        // 再生/停止を押した時
        } else if(v.id == R.id.play_button){
            if(flag == 0 ) {
                // テキストを再生中に
                play_button.text = getString(R.string.stop)
                flag = 1
                // 進む戻るボタンを無効化
                next_button.isEnabled = false
                back_button.isEnabled = false
                if (mTimer == null) {
                    mTimer = Timer()
                    mTimer!!.schedule(object : TimerTask() {
                        override fun run() {
                            mHandler.post() {
                                if (maxSize > imgIndex) {
                                    imgIndex += 1
                                } else {
                                    imgIndex = 0
                                }
                                imageView.setImageURI(images[imgIndex])
                            }
                        }
                    }, 2000, 2000)
                }
            } else {
                // 進む戻るボタンを有効化
                next_button.isEnabled = true
                back_button.isEnabled = true
                // 元のテキストに切り替え
                play_button.text = getString(R.string.play)
                flag = 0
                if (mTimer != null){
                    mTimer!!.cancel()
                    mTimer = null
                }
            }
        }
        imageView.setImageURI(images[imgIndex])
    }
}