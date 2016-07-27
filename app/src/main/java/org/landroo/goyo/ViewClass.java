package org.landroo.goyo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ViewClass extends View
{
	private static final String TAG = ViewClass.class.getSimpleName();
	
	private float x = 0;
	private float y = 0;
	
	private ScrollView scrollView;
	private GoyoClass goyo;
	private float tileHeight;
	private float tileWidth;
	
	private BitmapDrawable backDrawable;
	
	private String title = "";
	
	private Paint paint = new Paint();
	
	private float centerX = 0;
	private float centerY = 0;
	
	private LinearLayout ll;

	public ViewClass(Context context, ViewGroup view)
	{
		super(context);
		
		paint.setTextSize(24);
		paint.setColor(Color.WHITE);
		paint.setStyle(Style.FILL);
		paint.setAntiAlias(true);
		paint.setFakeBoldText(true);
		paint.setShadowLayer(3, 0, 0, Color.BLACK);
		
		ll = (LinearLayout)view.getChildAt(0);
		ll.setLayoutParams(new LinearLayout.LayoutParams(500, 500));
		ll.measure(MeasureSpec.makeMeasureSpec(ll.getLayoutParams().width, MeasureSpec.EXACTLY),
			    MeasureSpec.makeMeasureSpec(ll.getLayoutParams().height, MeasureSpec.EXACTLY));
		ll.layout(10, 100, ll.getMeasuredWidth() + 100, ll.getMeasuredHeight() + 100);
	}
	
	public void initClass(GoyoClass goyo, ScrollView scrollView, BitmapDrawable backDrawable)
	{
		this.goyo = goyo;
		this.scrollView = scrollView;
		this.tileHeight = goyo.mCellHeight;
		this.tileWidth = goyo.mCellWidth;
		this.backDrawable = backDrawable;
		
		centerX = scrollView.displayWidth / 2 - tileHeight / 2;
		centerY = scrollView.displayHeight / 2 - tileWidth / 2;
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		try
		{
			float xPos = 0;
			float yPos = 0;

			float zoomX = 1;
			float zoomY = 1;

			if (scrollView != null)
			{
				xPos = scrollView.xPos();
				yPos = scrollView.yPos();
				
				zoomX = scrollView.getZoomX();
				zoomY = scrollView.getZoomY();
			}
			
			/*if(backDrawable != null)
			{
				backDrawable.draw(canvas);
			}*/

			if (goyo != null)
			{
				for (GoyoClass.GoyoTile tile : goyo.goyoList)
				{
					if (tile != null)
					{
						x = tile.posX * zoomX + xPos;
						y = tile.posY * zoomY + yPos;

						if (x > -tileWidth * zoomX && x < canvas.getWidth() && y > -tileHeight * zoomY && y < canvas.getHeight())
						{
							canvas.save();
							
							canvas.translate(x, y);
							
							backDrawable.draw(canvas);
							
							canvas.translate(-x, -y);

							canvas.rotate(tile.rot, (tile.posX * zoomX) + xPos + tile.width * zoomX / 2,
									(tile.posY * zoomY) + yPos + tile.height * zoomY / 2);
							
							canvas.translate(x, y);

							tile.pathDraw.draw(canvas);
							//tile.maskDraw.draw(canvas);

							// canvas.translate(-xPos, -yPos);
							canvas.restore();
						}
					}
				}

				x = centerX;
				y = centerY;
				
				canvas.translate(x, y);
				goyo.goyo.draw(canvas);
				canvas.translate(-x, -y);
			}
			else Log.i(TAG, "view is null!");
			
			if(!title.equals("")) canvas.drawText(title, 0, 20, paint);
			
			//canvas.drawText("velX " + Math.round(velocityX * 100) + " velY " + Math.round(velocityY * 100), 0, 40, paint);
			//canvas.drawText("xPos " + Math.round(xPos) + " yPos " + Math.round(yPos), 0, 60, paint);
			
			//ll.draw(canvas);
		}
		catch (Exception ex)
		{
			Log.i(TAG, "" + ex);
		}
	}
	
	public void setTitle(String title)
	{
		this.title = title;
	}
}
