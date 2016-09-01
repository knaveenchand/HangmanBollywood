package com.timor.naveen.hangmanbollywood;

import java.util.Random;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.NavUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;

import android.content.res.Resources;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.GridView;

import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import org.w3c.dom.Text;


public class GameActivity extends AppCompatActivity implements View.OnClickListener {
    private String[] words;             // Variable to store the array of words from arrays.xml
    private Random rand;                // Variable to store a random value
    private String currWord;            // Variable to store the randomly selected word
    private LinearLayout word;    // Variable to store the characters of the randomly selected word
    private LinearLayout hintLayout;    // variable to store the hint
    private TextView hintView;          // this is for hint view
    private TextView[] charViews;       // Views for each letter in the answer
    private GridView letters;           // Variable to setup the alphabet grid
    private LetterAdapter ltrAdapt;     // Variable to initialize the LetterAdapter class

    private AlertDialog helpAlert;      // Variable to initialize help popup

    private ImageView[] bodyParts;      // Create a an ImageView for the body part images
    private int numParts=6;             // Set the number of body parts = 6
    private int currPart;               // Current part - will increment when wrong answers are chosen (0 = head)
    private int numChars;               // Number of characters in current word
    private int numCorr;                // Number correctly guessed

    private SoundPool ourSounds;
    private int soundID;
    boolean plays = false, loaded = false;
    float actVolume, maxVolume, volume;
    AudioManager audioManager;
    int counter;
    MediaPlayer winningmediaPlayer, losingmediaPlayer;
    int correctmusic;
    int wrongmusic;
    int fullcorrectmusic;
    private int MaxInLine;
    //int fullwrongmusic;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);                     // Calls android.app.AppComaptActivity
        setContentView(R.layout.activity_game);                 // Set screen view to gameplay (gallows) screen
        // Turn on the action bar
        /*
        This currently causes an ERROR on the PHONE
        it causes the app to crash after the PLAY BUTTON is pushed
        and generates a system error
        This is used to access the HELP screen
         */
        //getActionBar().setDisplayHomeAsUpEnabled(true);       // Turn on the screen's Action Bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);




        // Read answer words from the array
        Resources res = getResources();                         // Request the applications resources
        words = res.getStringArray(R.array.words);              // Get values from string-array = names in arrays.xml
        rand = new Random();                                    // Java random function
        currWord = "";                                          // Clear out the current word variable

        bodyParts = new ImageView[numParts];                    // numParts = 6
        bodyParts[0] = (ImageView)findViewById(R.id.head);
        bodyParts[1] = (ImageView)findViewById(R.id.body);
        bodyParts[2] = (ImageView)findViewById(R.id.arm1);
        bodyParts[3] = (ImageView)findViewById(R.id.arm2);
        bodyParts[4] = (ImageView)findViewById(R.id.leg1);
        bodyParts[5] = (ImageView)findViewById(R.id.leg2);

        word = (LinearLayout) findViewById(R.id.word);     // Get the word

        hintView = (TextView)findViewById(R.id.hinttext);     // get the hint

        letters = (GridView)findViewById(R.id.letters);        // get the letters grid layout

        Button skipBtn = (Button) findViewById(R.id.skipbutton); // get the button to skip to next question
        skipBtn.setOnClickListener(this);

        initializeSoundPool();
        playGame();                                             // Call playGame()

    }

    public void onClick(View view) {
        if (view.getId() == R.id.skipbutton) {
            disableBtns();                                                          // Call disableBtns method...

            AlertDialog.Builder loseBuild = new AlertDialog.Builder(this);          // Create an AlertDialog...
            loseBuild.setTitle("HA HA HA HA");                                      // AlertDialog title text...
            loseBuild.setMessage("You lose!\n\nThe answer was:\n\n"+currWord);      // AlertDialog message...
            losingmediaPlayer = MediaPlayer.create(this, R.raw.lostgame);
            losingmediaPlayer.start();


            loseBuild.setPositiveButton("Play Again", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {              // Positive button recall playGame()
                    losingmediaPlayer.stop();
                    GameActivity.this.playGame();
                }});

            loseBuild.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {                    // Negative button terminates application
                    losingmediaPlayer.stop();

                    GameActivity.this.finish();
                }});

            loseBuild.show();                                                       // Show the AlertDialog

        }
    }

        private void initializeSoundPool() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build();

            ourSounds = new SoundPool.Builder()
                    .setMaxStreams(1)
                    .setAudioAttributes(audioAttributes)
                    .build();
            correctmusic = ourSounds.load(this, R.raw.correct, 1);
            wrongmusic = ourSounds.load(this, R.raw.wrong,1);
            //fullcorrectmusic = ourSounds.load(this, R.raw.winsupermachiaud, 1);
            //fullwrongmusic = ourSounds.load(this, R.raw.lostgame,1);
        } else {
            ourSounds = new SoundPool(1, AudioManager.STREAM_MUSIC, 1);
            correctmusic = ourSounds.load(this, R.raw.correct, 1);
            wrongmusic = ourSounds.load(this, R.raw.wrong,1);
            //fullcorrectmusic = ourSounds.load(this, R.raw.winsupermachiaud, 1);
            //fullwrongmusic = ourSounds.load(this, R.raw.lostgame,1);
        }
        ourSounds.setOnLoadCompleteListener(new OnLoadCompleteListener() {
            public void onLoadComplete(SoundPool ourSounds, int sampleId,int status) {
                loaded = true;
            }
        });

    }

    private void playGame() {

        currPart = 0;                                   // Set the current body part to 0 (head)...
        for (int p = 0; p < numParts; p++) {            // Make int p 0, then increment until = numParts = 6
            bodyParts[p].setVisibility(View.INVISIBLE);     // Set the visibility of body parts 0, 1, 2, 3,  ++ to invisible
        }

        String newWordwithMeaning = words[rand.nextInt(words.length)];                             // Choose a word from the array at random
        String newWord = newWordwithMeaning.substring(0,newWordwithMeaning.indexOf("~"));   // break the word and hint at ~
        String newHint = newWordwithMeaning.substring(newWordwithMeaning.indexOf("~") + 1); // hint from ~ till end.

        while(newWord.equals(currWord)) newWord = words[rand.nextInt(words.length)];    // Make sure not to choose the same word twice

        currWord = newWord;                                                             // Set currWord to the selected newWord

        charViews = new TextView[currWord.length()];                                    // Create views for the characters in currWord

        word.removeAllViews();                                                    // Remove all views


        numChars = currWord.replace(" ","").length();                   // Set the number of characters to the length of the current word
        numCorr = 0;                                    // Set the number of correctly guessed letters
        MaxInLine = 10;


        for (int c = 0; c < currWord.length(); c++) {           // Make int c, then increment until c = currWord length
            charViews[c] = new TextView(this);                  // Create a new TextView for each letter c

            charViews[c].setText("" + currWord.charAt(c));      // Set the TextView to the character at the location c of currWord
            charViews[c].setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            charViews[c].setGravity(Gravity.CENTER);
            charViews[c].setTextColor(Color.WHITE);
            if (currWord.charAt(c)==' ')
            {
                charViews[c].setBackgroundResource(R.drawable.letter_bg_space);
            }
            else
            {
                charViews[c].setBackgroundResource(R.drawable.letter_bg);                   // Make charView background letter_bg.xml
            }


            word.addView(charViews[c]);                                           // Add word charViews to layout

        }

        hintView.setText("Hint: "+newHint);

        // Reset adapter
        ltrAdapt=new LetterAdapter(this);
        letters.setAdapter(ltrAdapt);

    }

    public void letterPressed(View view) {                  // User has pressed a letter to guess...


        String ltr=((TextView)view).getText().toString();   // First, capture the the character button input from the screen
        char letterChar = ltr.charAt(0);                    // Next, take only the first character from the string
        view.setEnabled(false);                             // Disable the button...
        view.setBackgroundResource(R.drawable.letter_dn);   // Change the background color to the color in letter_dn.xml (black)

        boolean correct = false;                            // Set boolean correct to "false" before testing the pressed letter

        for(int k = 0; k < currWord.length(); k++) {        // As long as k is less than currWord length, then increase k
            if (currWord.charAt(k) == letterChar) {         // If the letter located at position k same as letterChar (last pressed letter)
                correct = true;                             // Set boolean correct to "true"
                numCorr++;                                  // Increase numCorr (number of correctly guessed letters)
                charViews[k].setTextColor(Color.BLACK);     // Change the text color to black

            }
        }

        if (correct) {                                          //
            //correct guess
            if (loaded) {
                ourSounds.play(correctmusic, 0.9f, 0.9f, 1, 0, 1);
                Context context = getApplicationContext();
                CharSequence text = "Good Guess!";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();

            }

        }
        else if (currPart < numParts) {                         // If there are some guesses left...
            bodyParts[currPart].setVisibility(View.VISIBLE);    // Make the current part visible...
            currPart++;                                         // Increase the value stored in currPart

            if (loaded) {
                ourSounds.play(wrongmusic, 0.9f, 0.9f, 1, 0, 1);
                Context context = getApplicationContext();
                CharSequence text = "Oops! Wrong Guess.";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();

            }

        }

        else{                                                                       // There are no guesses left...

            disableBtns();                                                          // Call disableBtns method...

            AlertDialog.Builder loseBuild = new AlertDialog.Builder(this);          // Create an AlertDialog...
            loseBuild.setTitle("HA HA HA HA");                                      // AlertDialog title text...
            loseBuild.setMessage("You lose!\n\nThe answer was:\n\n"+currWord);      // AlertDialog message...
            losingmediaPlayer = MediaPlayer.create(this, R.raw.lostgame);
            losingmediaPlayer.start();


            loseBuild.setPositiveButton("Play Again", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {              // Positive button recall playGame()
                    losingmediaPlayer.stop();
                    GameActivity.this.playGame();
                }});

            loseBuild.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {                    // Negative button terminates application
                    losingmediaPlayer.stop();

                    GameActivity.this.finish();
                }});

            loseBuild.show();                                                       // Show the AlertDialog


        }

        if (numCorr == numChars) {              // If the number of correct guesses = number of characters in word...

            disableBtns();                      // Call disableBtns()...

            AlertDialog.Builder winBuild = new AlertDialog.Builder(this);           // Create an AlertDialog...
            winBuild.setTitle("Wow, Awesome!");                      // AlertDialog title text...
            winBuild.setMessage("You win!\n\nThe answer was:\n\n" + currWord);      // AlertDialog message...
            winningmediaPlayer = MediaPlayer.create(this, R.raw.supermachiaudone);
            winningmediaPlayer.start();


            winBuild.setPositiveButton("Play Again", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {               // Positive button recall playGame()
                    winningmediaPlayer.stop();
                    GameActivity.this.playGame();
                }});

            winBuild.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    winningmediaPlayer.stop();
                    GameActivity.this.finish();                                     // Negative button terminates application

                }});

            winBuild.show();                                                        // Show the AlertDialog



        }
    }

    public void disableBtns() {
        int numLetters = letters.getChildCount();
        for (int l = 0; l < numLetters; l++) {
            letters.getChildAt(l).setEnabled(false);
        }
    }

    public void showHelp() {

        AlertDialog.Builder helpBuild = new AlertDialog.Builder(this);
        helpBuild.setTitle("Help");
        helpBuild.setMessage("Guess the word by selecting the letters.\n\n" + "You only have 6 wrong selections then it's game over!");
        helpBuild.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                helpAlert.dismiss();
            }});
        helpAlert = helpBuild.create();

        helpBuild.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_help:
                showHelp();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
