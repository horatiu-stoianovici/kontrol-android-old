package com.example.kontrol.views;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.AsyncTask;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.example.kontrol.animating.AnimationTransitions;
import com.example.kontrol.carpeli.EditableAccomodatingLatinIMETypeNullIssues;
import com.example.kontrol.carpeli.InputConnectionAccomodatingLatinIMETypeNullIssues;
import com.example.kontrol.graphics.HBaseGraphics;
import com.example.kontrol.graphics.HContext;
import com.example.kontrol.graphics.KeyboardButtonSwitch;
import com.example.kontrol.graphics.LeftClickGraphics;
import com.example.kontrol.graphics.RightClickGraphics;
import com.example.kontrol.graphics.TouchpadGraphics;
import com.example.kontrol.tcpcommunications.HRequest;

public class TouchpadView extends SurfaceView implements Runnable {
	double offset = 0;
	private float targetOffset = 0;
	double startingOffset = 0;
	private volatile boolean isTransitioning = false;
	@SuppressWarnings("unused")
	private int canvasWidth, canvasHeight;
	private volatile long transitionStartTime = 0;
	private static final long transitionDuration = 500000000;
	private int transitionFrames;
	private SurfaceHolder myHolder;
	private boolean isRunning = false;
	//private int currentContext = HContext.TouchpadScreen;

	ArrayList<HBaseGraphics> graphicElements = new ArrayList<HBaseGraphics>();
	ArrayList<HBaseGraphics> drawGraphicElements, eventGraphicElements;

	public TouchpadView(Context context) {
		super(context);

		init(context);
	}

	public TouchpadView(Context context, AttributeSet attrs) {
		super(context, attrs);

		init(context);
	}

	public TouchpadView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		init(context);
	}

	public void onPause(){
		isRunning = false;
	}

	public void onResume(){
		if(!isRunning){
			isRunning = true;
			new Thread(this).start();
		}
	}

	@Override
	public void run() {
		while (isRunning) {

			if (!myHolder.getSurface().isValid())
				continue;
			Canvas canvas = myHolder.lockCanvas();
			canvas.drawColor(Color.BLACK);

			canvasWidth = canvas.getWidth();
			canvasHeight = canvas.getHeight();

			if(isTransitioning){
				long currentElapsed = System.nanoTime() - transitionStartTime;
				double percentage = currentElapsed * 1.0f / transitionDuration;
				if(percentage >= 1.0f){
					isTransitioning = false;
					Log.d("Kontrol", String.valueOf(transitionFrames));
					offset = targetOffset;
				}
				else {
					offset = AnimationTransitions.EaseIn(currentElapsed, startingOffset, targetOffset, transitionDuration);
					transitionFrames++;
				}
			}

			for (HBaseGraphics element : drawGraphicElements) {
				float myOffset = (float)offset;
				switch(element.getContext())
				{
				case HContext.MusicScreen:
					myOffset -= canvasHeight;
					break;
				case HContext.TouchpadScreen:
					break;
				}
				element.draw(canvas, (float)myOffset);
			}
			myHolder.unlockCanvasAndPost(canvas);

		}
	}
	
	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		//Passing FALSE as the SECOND ARGUMENT (fullEditor) to the constructor 
		  // will result in the key events continuing to be passed in to this 
		  // view.  Use our special BaseInputConnection-derived view
		  InputConnectionAccomodatingLatinIMETypeNullIssues baseInputConnection = 
		    new InputConnectionAccomodatingLatinIMETypeNullIssues(this, false);

		   //In some cases an IME may be able to display an arbitrary label for a 
		   // command the user can perform, which you can specify here.  A null value
		   // here asks for the default for this key, which is usually something 
		   // like Done.
		   outAttrs.actionLabel = null;

		   //Special content type for when no explicit type has been specified. 
		   // This should be interpreted (by the IME that invoked 
		   // onCreateInputConnection())to mean that the target InputConnection 
		   // is not rich, it can not process and show things like candidate text 
		   // nor retrieve the current text, so the input method will need to run 
		   // in a limited "generate key events" mode.  This disables the more 
		   // sophisticated kinds of editing that use a text buffer.
		   outAttrs.inputType = InputType.TYPE_NULL;

		   //This creates a Done key on the IME keyboard if you need one
		   outAttrs.imeOptions = EditorInfo.IME_ACTION_DONE;

		   return baseInputConnection;	
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		for(HBaseGraphics element : eventGraphicElements){
			float myOffset = (float)offset;
			switch(element.getContext())
			{
			case HContext.MusicScreen:
				myOffset -= canvasHeight;
				break;
			case HContext.TouchpadScreen:
				break;
			}
			if(element.onMotionEvent(event, myOffset)){
				break;
			}
		}

		invalidate();
		return true;
	}

	/**
	 * Initializes items depended to context or generally used
	 * 
	 * @param context
	 */
	private void init(Context context) {
		graphicElements.add(new LeftClickGraphics(context));
		graphicElements.add(new RightClickGraphics(context));
		graphicElements.add(new KeyboardButtonSwitch(context, this));
		graphicElements.add(new TouchpadGraphics(context));
		
		drawGraphicElements = new ArrayList<HBaseGraphics>(graphicElements);
		eventGraphicElements = new ArrayList<HBaseGraphics>(graphicElements);

		Collections.sort(drawGraphicElements, new Comparator<HBaseGraphics>() {

			@Override
			public int compare(HBaseGraphics lhs, HBaseGraphics rhs) {
				return lhs.getDrawZIndex() - rhs.getDrawZIndex();
			}
		});

		Collections.sort(eventGraphicElements, new Comparator<HBaseGraphics>(){
			@Override
			public int compare(HBaseGraphics lhs, HBaseGraphics rhs) {
				return rhs.getTouchEventZIndex() - lhs.getTouchEventZIndex();
			}
		});

		myHolder = getHolder();
	}

	/**
	 * Swithces to a different screen animated
	 */
	public void switchToContext(int context){
		switch(context)
		{
		case HContext.TouchpadScreen:
			startTransition(0, transitionDuration);
			break;

		case HContext.MusicScreen:
			startTransition(canvasHeight + 1, transitionDuration);
			break;
		}
	}

	/**
	 * Animated transition
	 * @param target - the target offset
	 * @param time - time of the transition
	 */
	private void startTransition(float target, long time){
		startingOffset = offset;
		targetOffset = target;
		transitionFrames = 0;
		transitionStartTime = System.nanoTime();
		isTransitioning = true;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		List<String> params = new ArrayList<String>();
		int keyCode;
		if(event.getAction() == KeyEvent.ACTION_UP){
			if(event.isPrintingKey()){
				keyCode = event.getUnicodeChar(event.getMetaState());
			}
			else if(event.getKeyCode() == KeyEvent.KEYCODE_DEL){
				keyCode = 8;
			}
			else if(event.getKeyCode() == KeyEvent.KEYCODE_ENTER){
				keyCode = 13;
			}
			else if(event.getKeyCode() == KeyEvent.KEYCODE_SPACE){
				keyCode = 32;
			}
			else {
				return false;
			}
			if(event.getUnicodeChar() == 
		       (int)EditableAccomodatingLatinIMETypeNullIssues.ONE_UNPROCESSED_CHARACTER.charAt(0))
		     {
		       //We are ignoring this character, and we want everyone else to ignore it, too, so 
		       // we return true indicating that we have handled it (by ignoring it).   
		       return true; 
		     }
			params.add(String.valueOf(keyCode));
			final HRequest req = new HRequest("keyboard", params);
			new AsyncTask<Void, Void, Void>() {

				@Override
				protected Void doInBackground(Void... params) {
					try {
						req.SendTCP();
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return null;
				}
			}.execute();
		}
		else {
			Log.d("Kontrol", "key event unknown");
		}
		return super.dispatchKeyEvent(event);
	}
}
