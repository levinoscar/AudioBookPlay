package edu.temple.flossplayer

import android.os.Bundle

import androidx.fragment.app.Fragment

import android.view.*

import com.google.android.material.floatingactionbutton.FloatingActionButton

class BookControlFragment : Fragment() {

    lateinit var playBook: FloatingActionButton

    lateinit var pauseBook: FloatingActionButton

    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        // Inflate this fragment layout

        return inflater.inflate(R.layout.fragment_book_control, container, false).apply {

            pauseBook = findViewById(R.id.pauseButton)

            playBook = findViewById(R.id.playButton)

        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        playBook.setOnClickListener {

            (requireActivity() as controlInterface).playBook()

        }

        pauseBook.setOnClickListener() {

            (requireActivity() as controlInterface).pauseBook()

        }

    }

    interface controlInterface {

        fun playBook()

        fun pauseBook()

    }

}