package com.example.vermond.kotlintest.com.example.vermond.kotlintest.soda

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.vermond.kotlintest.R
import com.example.vermond.kotlintest.com.example.vermond.kotlintest.soda.db.AppDatabase
import com.example.vermond.kotlintest.com.example.vermond.kotlintest.soda.db.ScoreData
import com.example.vermond.kotlintest.com.example.vermond.kotlintest.soda.db.ScoreDataDao

class SodaRocketRank: AppCompatActivity() {

    enum class DBWorkType(val type: Int) {
        INSERT(1),
        DELETE_ALL(2),
        READ_COUNT(3),
        READ_ALL(99)
    }

    private var db:AppDatabase? = null
    private var scoreDataDao:ScoreDataDao? = null
    var score:Double = 0.0
    var scoreList : List<ScoreData>? = null
    var workType: DBWorkType = DBWorkType.READ_ALL

    //메인 쓰레드에서 DB 접속 불가능하므로 별도 쓰레드로 등록한다
    var dbHandlerThread:HandlerThread = HandlerThread("DBHandlerThread")
    lateinit var dbHandler:Handler
    var dbRunnable:Runnable = Runnable {
        testDBWork(workType)
    }

    //UI은 메인 쓰레드에서만 동작하므로 UI 업데이트용으로 만든다
    var scoreUpdateHandler:Handler = Handler()
    var scoreUpdateRunnable:Runnable = Runnable {
        if (scoreList != null) {
            for (i in 0..9) {
                var text: TextView?;

                when (i) {
                    0 -> text = findViewById(R.id.ranking_text_1)
                    1 -> text = findViewById(R.id.ranking_text_2)
                    2 -> text = findViewById(R.id.ranking_text_3)
                    3 -> text = findViewById(R.id.ranking_text_4)
                    4 -> text = findViewById(R.id.ranking_text_5)
                    5 -> text = findViewById(R.id.ranking_text_6)
                    6 -> text = findViewById(R.id.ranking_text_7)
                    7 -> text = findViewById(R.id.ranking_text_8)
                    8 -> text = findViewById(R.id.ranking_text_9)
                    9 -> text = findViewById(R.id.ranking_text_10)
                    else -> text = null
                }

                if (text != null) {
                    if (i <= scoreList!!.indices.last) text.setText(String.format("%d. %.2f", i+1, scoreList!![i].score))  //scoreList!![i].score.toString())
                    else text.setText(String.format("%d. 0", i+1))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranking)

        if (intent.extras != null) score = intent.extras.getDouble("score")

        //db 작업
        //메인 쓰레드에서는 DB 작업이 안되므로 별도의 핸들러용 쓰레드를 만들어서 돌려야 한다
        dbHandlerThread.start()
        dbHandler = Handler(dbHandlerThread.looper)
        db = AppDatabase.getInstance(applicationContext)
        if (db != null) scoreDataDao = db?.scoreDataDao()

        // 유효한 점수만 기록한다
        if (score > 0) workType = DBWorkType.INSERT
        else workType = DBWorkType.READ_COUNT

        dbHandler.post(dbRunnable)

        //버튼 리스너 등록
        findViewById<Button>(R.id.ranking_button_reset).setOnClickListener {
            workType = DBWorkType.DELETE_ALL
            dbHandler.post(dbRunnable)
            Toast.makeText(this as Context, "모든 점수가 삭제되었습니다", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.ranking_button_back).setOnClickListener {
            setResult(Activity.RESULT_OK);
            finish()
        }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        super.onBackPressed()
    }

    fun testDBWork(type:DBWorkType) {
        when (type) {
            DBWorkType.INSERT -> {
                //db에 점수 저장
                var scoreData: ScoreData = ScoreData(score)
                scoreDataDao?.insertScore(scoreData)

                testDBWork(DBWorkType.READ_COUNT)
            }
            DBWorkType.READ_COUNT -> {
                //UI는 UI 스레드에서만 변경됨
                scoreList = scoreDataDao?.getScores(10)
                scoreUpdateHandler.post(scoreUpdateRunnable)
            }
            DBWorkType.DELETE_ALL -> {
                scoreDataDao?.deleteAllScore()
                scoreList = scoreDataDao?.getAllScore()
                scoreUpdateHandler.post(scoreUpdateRunnable)
            }
            DBWorkType.READ_ALL -> {
                // default 역할을 위해 남겨둠
                scoreList = scoreDataDao?.getAllScore()
                scoreUpdateHandler.post(scoreUpdateRunnable)
            }
        }
    }
}