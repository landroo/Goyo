package org.landroo.goyo;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.landroo.ui.UI;
import org.landroo.ui.UIInterface;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GoyoActivity extends Activity implements UIInterface
{
	private static final String TAG = GoyoActivity.class.getSimpleName();

	private UI ui = null;
	private GoyoClass goyo;

	private int displayWidth;
	private int displayHeight;

	public float pictureWidth;
	public float pictureHeight;
	public float origWidth;
	public float origHeight;

	private ScrollView scrollView;
	private ViewClass viewClass;

	private float xPos = 0;
	private float yPos = 0;

	private float tileSize = 128;
	
	private SensorManager sensorManager;
	private float sx0 = 0, sy0 = 0, sz0 = 0;
	private float sx1 = 0, sy1 = 0, sz1 = 0;
	private float sx2 = 0, sy2 = 0, sz2 = 0;
	private float velocityX, velocityY;
	private Timer scrollTimer;
	
	private boolean isRun = true;
	
	private SensorEventListener sensorEventListener = new SensorEventListener()
	{
		public void onAccuracyChanged(Sensor sensor, int accuracy)
		{
		}

		public void onSensorChanged(SensorEvent event)
		{
			if(event.sensor.getType() == Sensor.TYPE_ORIENTATION)
			{
				sx1 = event.values[0];// 0, 360 compass
				sy1 = event.values[1];// -180, 180 head-foot
				sz1 = event.values[2];// 180, -180 left-right
				
				if(sx0 == 0 && sy0 == 0 && sz0 == 0)
				{
					sx0 = sx1;
					sy0 = sy1;
					sz0 = sz1;
					
					sx2 = sx1;
					sy2 = sy1;
					sz2 = sz1;
					
					return;
				}
				
				velocityX -= sz2 - sz1; 
				velocityY -= sy2 - sy1;
						
				sx2 = sx1;
				sy2 = sy1;
				sz2 = sz1;
			}
			else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			{
			}
			else if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			{
			}
		}
	};
	
	// sensor update timer class
	class scrollTask extends TimerTask
	{
		public void run()
		{
			if(isRun == true)
			{
				xPos = scrollView.xPos();
				yPos = scrollView.yPos();
				
				float dx = velocityX / 20;
				float dy = velocityY / 20;
				
				float cx = displayWidth / 2;
				float cy = displayHeight / 2;

				//float bx = (x - xPos) * (origWidth / pictureWidth);
				//float by = (y - yPos) * (origHeight / pictureHeight);
				float posX = (cx - xPos) * (origWidth / pictureWidth);
				float posY = (cy - yPos) * (origHeight / pictureHeight);
				
				int selTile = goyo.getTileWay(posX, posY);
				int pathColor = goyo.getGoyoPath(posX, posY);
				if(pathColor != -1)
				{
					// open side 1-left 2-right 4-up 8-bottom
					if((selTile & 1) != 1 && dx > 0) dx = 0;
					if((selTile & 2) != 2 && dx < 0) dx = 0;
					if((selTile & 4) != 4 && dy > 0) dy = 0;
					if((selTile & 8) != 8 && dy < 0) dy = 0;
				}
				
				viewClass.setTitle("dx " + Math.round(dx * 1000) + " dy " + Math.round(dy * 1000) + " tile " + selTile + " color " + pathColor);
				
				if(xPos + dx < displayWidth - (pictureWidth + displayWidth / 2) || xPos + dx > displayWidth / 2) dx = 0;
				if(yPos + dy < displayHeight - (pictureHeight + displayHeight / 2) || yPos + dy > displayHeight / 2) dy = 0;
	
				scrollView.setXPos(xPos + dx);
				scrollView.setYPos(yPos + dy);
				
				viewClass.postInvalidate();
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.activity_goyo);
		
		LayoutInflater inflater = getLayoutInflater();
		ViewGroup view = (ViewGroup) inflater.inflate(R.layout.activity_goyo, null);

		viewClass = new ViewClass(this, view);
		setContentView(viewClass);

		Display display = getWindowManager().getDefaultDisplay();
		displayWidth = display.getWidth();
		displayHeight = display.getHeight();
		
		sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

		ui = new UI(this);
		
		initApp();
		
		scrollTimer = new Timer();
		scrollTimer.scheduleAtFixedRate(new scrollTask(), 10, 1);
	}

	// initialize the game
	private void initApp()
	{
		pictureWidth = displayWidth * 8 + tileSize;
		pictureHeight = displayHeight * 8 + tileSize;
		origWidth = pictureWidth;
		origHeight = pictureHeight;
		
		Bitmap texture = BitmapFactory.decodeResource(this.getResources(), R.drawable.wood2);
		Bitmap wood = strechImage(texture, tileSize, tileSize);
		BitmapDrawable backDrawable = new BitmapDrawable(wood);
		backDrawable.setBounds(0, 0, (int)tileSize, (int)tileSize);

		goyo = new GoyoClass((int) pictureWidth, (int) pictureHeight, tileSize, tileSize, backDrawable);
		goyo.newMaze();

		scrollView = new ScrollView(displayWidth, displayHeight, (int) pictureWidth, (int) pictureHeight, viewClass, goyo);
		
		viewClass.initClass(goyo, scrollView, backDrawable);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.goyo, menu);
		return true;
	}
	
	@Override
    protected void onResume() 
	{
       super.onResume();
       isRun = true;
       sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
       sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
       sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_NORMAL);
    }
	
	@Override
    protected void onPause() 
	{
       super.onPause();
       isRun = false;
       sensorManager.unregisterListener(sensorEventListener);    
    }

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		return ui.tapEvent(event);
	}

	@Override
	public void onDown(float x, float y)
	{
		scrollView.onDown(x, y);
	}

	@Override
	public void onUp(float x, float y)
	{
		scrollView.onUp(x, y);
	}

	@Override
	public void onTap(float x, float y)
	{
		// TODO Auto-generated method stub
		float bx = (x - xPos) * (origWidth / pictureWidth);
		float by = (y - yPos) * (origHeight / pictureHeight);
		
		int selTile = goyo.getTileWay(bx, by);
	}

	@Override
	public void onHold(float x, float y)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onMove(float x, float y)
	{
		scrollView.onMove(x, y);
	}

	@Override
	public void onSwipe(int direction, float velocity, float x1, float y1, float x2, float y2)
	{
		scrollView.onSwipe(direction, velocity, x1, y1, x2, y2);
	}

	@Override
	public void onDoubleTap(float x, float y)
	{
		scrollView.onDoubleTap(x, y);
	}

	@Override
	public void onZoom(int mode, float x, float y, float distance, float xdiff, float ydiff)
	{
		scrollView.onZoom(mode, x, y, distance, xdiff, ydiff);
	}

	@Override
	public void onRotate(int mode, float x, float y, float angle)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onFingerChange()
	{
		// TODO Auto-generated method stub

	}
	
	//resize bitmap
	private Bitmap strechImage(Bitmap image, float width, float height)
	{
		Bitmap bitmap = null;
		try
		{
			int origWidth = image.getWidth();
			int origHeight = image.getHeight();
			float newheight = height;
			float newwidth = width;
			if(height == 0) newheight = (float) width / (float) origWidth * (float) origHeight;
			if(width == 0) newwidth = (float) height / (float) origHeight * (float) origWidth;
			float scaleWidth = newwidth / origWidth;
			float scaleHeight = newheight / origHeight;
			Matrix matrix = new Matrix();
			matrix.postScale(scaleWidth, scaleHeight);
			bitmap = Bitmap.createBitmap(image, 0, 0, origWidth, origHeight, matrix, false);
		}
		catch (OutOfMemoryError e)
		{
			Log.e(TAG, "Out of memory error in new page!");
		}
		catch (Exception ex)
		{
			Log.e(TAG, "Load image error!");
		}		
		
		return bitmap;
	}

}
