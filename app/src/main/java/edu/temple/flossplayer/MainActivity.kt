package edu.temple.flossplayer

import android.annotation.SuppressLint

import android.app.SearchManager

import android.content.*

import android.content.ContentValues.TAG

import android.os.*

import android.view.View

import android.widget.*

import androidx.appcompat.app.AppCompatActivity

import androidx.lifecycle.ViewModelProvider

import com.android.volley.RequestQueue

import com.android.volley.toolbox.JsonArrayRequest

import com.android.volley.toolbox.Volley

import edu.temple.audlibplayer.PlayerService

import android.util.Log

class MainActivity : AppCompatActivity(), BookControlFragment.controlInterface {

    private val searchURL = "https://kamorris.com/lab/flossplayer/search.php?query="

    private lateinit var playerBinder: PlayerService.MediaControlBinder // Created Binder

    lateinit var seekBar: SeekBar // Defines seekBar

    private var isBound = false // This determine whether audio book is placed properly or not

    private var activeBookID = -1 // This defines the ID of the active book used

    private var progressTime = 0 // This defines the progress time

    private lateinit var serviceIntent: Intent // Defines Service Intent

    //NowPlaying TextView

    lateinit var nowPlayingTextView: TextView

    // onReceive

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent?.action == "edu.temple.floss-player.SelectedBookProgress") {

                activeBookID = intent.getIntExtra("id", -1) // starts searching for ID

                progressTime = intent.getIntExtra("progress", 0) // begins the progress of audio playing

            }

        }

    }

    // handleMessage

    private val handler = @SuppressLint("HandlerLeak")

    object : Handler() {

        @SuppressLint("SetTextI18n")

        override fun handleMessage(msg: Message) {

            val bookProgress = (msg.obj as PlayerService.BookProgress)

            seekBar.progress = bookProgress.progress

        }

    }

    // onServiceConnected+Disconnected

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {

            playerBinder = (service as PlayerService.MediaControlBinder)

            playerBinder.setProgressHandler(handler)

            isBound = true

        }

        override fun onServiceDisconnected(name: ComponentName?) {

            Log.e(TAG, "onServiceDisconnected")

            isBound = false

        }

    }

    private val requestQueue : RequestQueue by lazy { Volley.newRequestQueue(this) }

    private val isSingleContainer : Boolean by lazy { findViewById<View>(R.id.container2) == null }

    private val bookViewModel : BookViewModel by lazy { ViewModelProvider(this)[BookViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // This is the SeekBar

        seekBar = findViewById(R.id.seekBar)

        seekBar.setOnSeekBarChangeListener(progressListener)

        // nowPlaying text view of when it rolls by the SeekBar or not

        nowPlayingTextView = findViewById(R.id.NowPlayingText)

        // to register the receiver

        registerReceiver(receiver, IntentFilter("edu.temple.floss-player.SelectedBookProgress"))

        serviceIntent = Intent(this, PlayerService::class.java)

        // to start service

        startService(serviceIntent)

        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        // If we're switching from one container to two containers clear BookPlayerFragment from container1

        if (supportFragmentManager.findFragmentById(R.id.container1) is BookPlayerFragment) {

            supportFragmentManager.popBackStack()

        }

        // If this is the first time the activity is loading, go ahead and add a BookListFragment

        if (savedInstanceState == null) {

            supportFragmentManager.beginTransaction()

                .add(R.id.container1, BookListFragment())

                .commit()

        // If activity loaded previously, there's already a BookListFragment

        // If we have a single container and a selected book, place it on top

        } else if (isSingleContainer && bookViewModel.getSelectedBook()?.value != null) {

            supportFragmentManager.beginTransaction()

                    .replace(R.id.container1, BookPlayerFragment())

                    .setReorderingAllowed(true)

                    .addToBackStack(null)

                    .commit()

        }

        // If we have two containers but no BookPlayerFragment, add one to container2

        if (!isSingleContainer && supportFragmentManager.findFragmentById(R.id.container2) !is BookPlayerFragment)

            supportFragmentManager.beginTransaction()

                .add(R.id.container2, BookPlayerFragment())

                .commit()

        // Respond to selection in portrait mode using flag stored in ViewModel

        bookViewModel.getSelectedBook()?.observe(this) {

            if (!bookViewModel.hasViewedSelectedBook()) {

                if (isSingleContainer) {

                    supportFragmentManager.beginTransaction()

                        .replace(R.id.container1, BookPlayerFragment())

                        .setReorderingAllowed(true)

                        .addToBackStack(null)

                        .commit()

                }

                bookViewModel.markSelectedBookViewed()

            }

        }

        findViewById<View>(R.id.searchImageButton).setOnClickListener {

            onSearchRequested()

        }

    }

    override fun onBackPressed() {

        // BackPress clears the selected book

        bookViewModel.clearSelectedBook()

        super.onBackPressed()

    }

    override fun onNewIntent(intent: Intent?) {

        super.onNewIntent(intent)

        if (Intent.ACTION_SEARCH == intent!!.action) {

            intent.getStringExtra(SearchManager.QUERY)?.also {

                searchBooks(it)

                // Please, do not select the previous book selection

                bookViewModel.clearSelectedBook()

                // Get rid of any unwanted DisplayFragments instances from the stack

                supportFragmentManager.popBackStack()

            }

        }

    }

    private fun searchBooks(searchTerm: String) {

        requestQueue.add(

            JsonArrayRequest(searchURL + searchTerm,

                { bookViewModel.updateBooks(it) },

                { Toast.makeText(this, it.networkResponse.toString(), Toast.LENGTH_SHORT).show() })

        )

    }

    override fun pauseBook() { if(playerBinder.isPlaying) { playerBinder.pause() } }

    @SuppressLint("SetTextI18n")

    override fun playBook() {

        if(bookViewModel.getSelectedBook() != null) {

            var selectedBook = bookViewModel.getSelectedBook()?.value

            if(activeBookID == -1 || (selectedBook as PlayerService.FlossAudioBook).getBookId() != activeBookID) {

                playerBinder.play(selectedBook as PlayerService.FlossAudioBook)

                // nowPlaying updates with the title

                nowPlayingTextView.text = "Now Playing: " + bookViewModel.getSelectedBook()?.value!!.title // this displays the audio book

            } else if(!playerBinder.isPlaying) { playerBinder.pause() } // this pauses the book audio

        }

    }

    // SeekBar

    val progressListener = object : SeekBar.OnSeekBarChangeListener {

        override fun onProgressChanged(seekBar: SeekBar?, int: Int, boolean: Boolean) {

            if(boolean) { playerBinder.seekTo(int) }

        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) { }

        override fun onStopTrackingTouch(seekBar: SeekBar?) { }

    }

    // onDestroy

    override fun onDestroy() {

        super.onDestroy()

        if(isBound) applicationContext.unbindService(serviceConnection)

        unregisterReceiver(receiver)

    }

}