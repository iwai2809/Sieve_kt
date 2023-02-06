package com.example.sieve_kt

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.room.*
import java.math.BigDecimal
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    // boolean型配列の初期値はfalse
    private var proveNumbers = BooleanArray(0) // 素数ならばtrue、それ以外にはfalseが入る

    private fun primeCheck(n: Int): Boolean {
        for (i in 2..sqrt(n.toDouble()).toInt()) { // 篩い落とし作業は、入力された数字の平方根に達するまで続ける
            if (proveNumbers[i] && n % i == 0) {
                return false
            }
        }
        return true
    }

    private fun add(): Long { // numbers配列のtrueになっている要素番号(素数)を足している
        var sum: Long = 2
        var i = 3
        while (i < proveNumbers.size) {
            if (proveNumbers[i]) {
                sum += i.toLong()
            }
            i += 2
        }
        return sum
    }
    private fun eratosthenes(): Long {
        proveNumbers[2] = true
        var i = 3
        while (i < proveNumbers.size) {
            if (primeCheck(i)) {
                proveNumbers[i] = true // 素数ならばtrue
            }
            i += 2
        }
        return add() // 素数の和
    }

    private lateinit var container: LinearLayout

    // キーボード表示を制御するためのオブジェクト
    private lateinit var inputMethodManager: InputMethodManager

    lateinit var mUserDao: UserDao
    lateinit var mAdapter: ArrayAdapter<String>

    private var mUserList: List<User> = listOf()


    @Entity
    data class User(
        @PrimaryKey(autoGenerate = true)
        val id: Int,

        var name: String,

        @ColumnInfo(name = "log")
        var log: Int
    )

    @Dao
    interface UserDao {
        @Query("SELECT * FROM user")
        fun getAll(): List<User>

        @Insert
        fun insert(user: User)

        @Update
        fun update(user: User)

        @Delete
        fun delete(user: User)
    }


    @Database(entities = [User::class], version = 1)
    abstract class UserDatabase : RoomDatabase() {

        abstract fun userDao(): UserDao

        companion object {

            private var INSTANCE: UserDatabase? = null

            private val lock = Any()

            fun getInstance(context: Context): UserDatabase {
                synchronized(lock) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(context.applicationContext,
                            UserDatabase::class.java, "User.db")
                            .allowMainThreadQueries()
                            .build()
                    }
                    return INSTANCE!!
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 背景のレイアウトを取得
        container = findViewById(R.id.container)

        // InputMethodManagerを取得
        inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager


        val listView = findViewById<ListView>(R.id.list_view)
        mUserDao = UserDatabase.getInstance(this).userDao()

        mAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayListOf())

        listView.adapter=mAdapter
        getUser()

        val Start_btn=findViewById<Button>(R.id.Start_btn)
        val Reset_btn=findViewById<Button>(R.id.Reset_btn)
        val delete_btn=findViewById<Button>(R.id.delete_btn)
        val Inp_text=findViewById<EditText>(R.id.Inp_text)
        val Ans_text=findViewById<TextView>(R.id.Ans_text)

        Start_btn.setOnClickListener{
            try {
                val Text=Inp_text.text
                val num1=Text.toString().toInt()

                if(num1<2||num1>2000000){
                    Ans_text.text = "範囲外なのでもう一度やり直してください"
                }else{
                    proveNumbers=BooleanArray(num1+1)
                    val startTime = System.nanoTime() // 計測開始
                    val test = eratosthenes()
                    val endTime =System.nanoTime()//計測終了
                    Ans_text.text = ("2～200万までの整数で\n"+Text+"以下の素数の和は"+"\n"+test+"です"
                            +"\n\n------処理速度------"+"\n"+
                            BigDecimal.valueOf((endTime - startTime) / 10.0.pow(9.0)).toString() + "秒"+"\n"+
                            BigDecimal.valueOf((endTime - startTime) / 10.0.pow(6.0)).toString() + "ミリ秒"+"\n")


                    val database = UserDatabase.getInstance(this)
                    val userDao = database.userDao()

                    // 新規Userの作成
                    val newUser = User(0, Text.toString(),test.toInt())

                    // Userの追加
                    mUserDao.insert(newUser)
                    getUser()

                    // キーボードを隠す
                    inputMethodManager.hideSoftInputFromWindow(container.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

                    // 背景にフォーカスを移す
                    container.requestFocus()
                }
            }catch (e: Exception){
                Ans_text.text = "数値が入力されていません"
            }
        }

        Reset_btn.setOnClickListener{
            Inp_text.text.clear()
            Ans_text.text = "2～200万までの整数を入力してください"
            // キーボードを隠す
            inputMethodManager.hideSoftInputFromWindow(container.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            // 背景にフォーカスを移す
            container.requestFocus()
        }

        delete_btn.setOnClickListener{
            deleteUser()
        }
    }

    private fun deleteUser() {
        if (mUserList.isEmpty()) return

        val deleteUser = mUserList.first()
        mUserDao.delete(deleteUser)
        getUser()
    }

    // 画面タップ時に呼ばれる
    override fun onTouchEvent(event: MotionEvent): Boolean {

        // キーボードを隠す
        inputMethodManager.hideSoftInputFromWindow(container.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

        // 背景にフォーカスを移す
        container.requestFocus()

        return false
    }

        override fun onCreateOptionsMenu(menu: Menu): Boolean {
            menuInflater.inflate(R.menu.option_menu, menu)
            return true
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            when(item.itemId){
                R.id.LightTheme->{
                    delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO
                }
                R.id.DarkTheme->{
                    delegate.localNightMode=AppCompatDelegate.MODE_NIGHT_YES
                }
            }
            return true
        }

    private fun getUser() {
        mUserList = mUserDao.getAll()
        val userInfoList = mUserList.map { it.id.toString() + " | " + it.name + "以下の素数の和=" +it.log.toString() }
        mAdapter.clear()
        mAdapter.addAll(userInfoList)
    }

}