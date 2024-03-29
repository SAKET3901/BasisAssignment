package com.example.basisassignment.ui


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import com.example.basisassignment.R
import com.example.basisassignment.adapter.CardAdapter
import com.example.basisassignment.data.MyJsonConverter
import com.example.basisassignment.data.model.PostModel
import com.example.basisassignment.data.remote.NetworkService
import com.example.basisassignment.utils.Common
import com.example.basisassignment.utils.Logger
import com.example.basisassignment.utils.OnSwipeTouchListener
import com.example.basisassignment.utils.network.NetworkHelper
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import com.yuyakaido.android.cardstackview.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

class MainActivity : AppCompatActivity(), CardStackListener {

    companion object {
        const val TAG = "MainActivity"
    }

    private var baseUrl = "https://git.io/"
    lateinit var cardStackLayoutManager: CardStackLayoutManager
    private var myCompositeDisposable: CompositeDisposable = CompositeDisposable()
    private var size: Int = 0
    private lateinit var adapter: CardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadData()

        retry_btn.setOnClickListener {
            loadData()
        }

    }

    private fun loadData() {

        //retrofit initializer
        val requestInterface = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(
                        HttpLoggingInterceptor()
                            .apply {
                                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                                else HttpLoggingInterceptor.Level.NONE
                            })
                    .build()
            )
            .addConverterFactory(MyJsonConverter.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(NetworkService::class.java)

        //network call to fetch data from api
        if (NetworkHelper(this).isNetworkConnected()) {
            progressBar.visibility = View.VISIBLE
            retry_btn.visibility = View.GONE
            myCompositeDisposable.add(
                requestInterface.getDummyData()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe({
                        progressBar.visibility = View.GONE
                        handleResponse(it)
                    }, {
                        Toast.makeText(this@MainActivity, "Error " + it.getLocalizedMessage(), Toast.LENGTH_SHORT)
                            .show();
                    })
            )
        } else {
            Toast.makeText(this@MainActivity, getString(R.string.network_connection_error), Toast.LENGTH_LONG).show()
            retry_btn.visibility = View.VISIBLE

        }

    }

    private fun handleResponse(postModel: PostModel?) {
        Logger.d(TAG, postModel!!.data.size.toString())
        size = postModel.data.size
        val rewindSetting = RewindAnimationSetting.Builder()
            .setDirection(Direction.Top)
            .setDuration(200)
            .setInterpolator(DecelerateInterpolator())
            .build()

        cardStackLayoutManager = CardStackLayoutManager(applicationContext, this)
        adapter = CardAdapter(postModel.data, this);
        cardStackLayoutManager.setDirections(arrayListOf(Direction.Right, Direction.Left, Direction.Bottom))
        cardStackLayoutManager.setCanScrollHorizontal(true)
        cardStackLayoutManager.setVisibleCount(3)
        cardStackLayoutManager.setStackFrom(StackFrom.Bottom)
        cardStackLayoutManager.setTranslationInterval(5f)
        cardStackLayoutManager.setRewindAnimationSetting(rewindSetting)
        cardStackLayoutManager.setCanScrollVertical(false)
        cardStackLayoutManager.setMaxDegree(90f)
        cardStackLayoutManager.setScaleInterval(0.95f)
        stack_cards.layoutManager = cardStackLayoutManager
        stack_cards.adapter = adapter
        updateData()

        // Button Interactions
        reload.setOnClickListener {
            if (cardStackLayoutManager.topPosition != 0) {
                adapter.notifyDataSetChanged()
                stack_cards.smoothScrollToPosition(0)
                count_text.visibility = View.VISIBLE
            }
        }
        //previous button to show previous card
        previous.setOnClickListener {
            stack_cards.rewind()
        }
        //next button to show next card
        next.setOnClickListener {
            stack_cards.swipe()
        }

        //bottom swipe to show previous card
        stack_cards.setOnTouchListener(object : OnSwipeTouchListener(this@MainActivity) {
            override fun onSwipeDown() {
                stack_cards.rewind()
            }
        })

    }

    private fun updateData() {
        val pos = cardStackLayoutManager.topPosition
        val count = cardStackLayoutManager.itemCount

        //set Progress bar position
        progressHorizontal.progress = Common().calculatePercentage(pos, count)
        if (pos < count) {
            count_text.text = getString(R.string.count_text, pos + 1, count)
            if (pos == 0) {
                previous.visibility = View.GONE
                next.visibility = View.VISIBLE
            } else if (pos == count) {
                next.visibility = View.GONE
            } else {
                previous.visibility = View.VISIBLE
                next.visibility = View.VISIBLE
            }
            reload.visibility = View.GONE
        } else {
            count_text.visibility = View.GONE
            reload.visibility = View.VISIBLE
        }

    }


    override fun onCardDragging(direction: Direction?, ratio: Float) {
        //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCardSwiped(direction: Direction?) {
        updateData()
    }

    override fun onCardCanceled() {
        //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCardRewound() {
        updateData()
    }


    override fun onDestroy() {
        super.onDestroy()
        //Clear all your disposables//
        myCompositeDisposable.clear()

    }
}
