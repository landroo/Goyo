package org.landroo.goyo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.FloatMath;
import android.util.Log;

public class GoyoClass
{
	private static final String TAG = GoyoClass.class.getSimpleName();

	private static final int pathLine = 0;
	private static final int pathElbow = 1;
	private static final int pathT = 2;
	private static final int pathX = 3;
	private static final int pathEnd = 4;
	private static final int pathEmpty = 5;

	private static final int wayLeft = 1;
	private static final int wayRight = 2;
	private static final int wayUp = 4;
	private static final int wayDown = 8;

	public class GoyoTile
	{
		public int id;
		public int type;
		public float posX, posY;
		public float width, height;
		public float rot;
		public BitmapDrawable pathDraw;
		public BitmapDrawable maskDraw;
		public Bitmap mask;
	}

	private OPMethod mOPMethod = null;// maze creator
	public List<GoyoTile> goyoList = new ArrayList<GoyoTile>();
	private ArrayList<ArrayList<Integer>> maCellData = null;// maze values

	private Bitmap[] paths = new Bitmap[6];
	private Bitmap[] masks = new Bitmap[6];

	public int mTableWidth;// playground width
	public int mTableHeight;// playground height

	public float mCellWidth;// cell width
	public float mCellHeight;// cell height

	public boolean ready = false;

	public BitmapDrawable back;
	public BitmapDrawable goyo;

	// consructor
	public GoyoClass(int tableWidth, int tableHeight, float iCellWidth, float iCellHeight, BitmapDrawable back)
	{
		initClass(tableWidth, tableHeight, iCellWidth, iCellHeight);
		this.back = back;
		this.goyo = drawGoyo((float) iCellWidth / 2 - 4, (float) iCellHeight / 2 - 4);
	}

	public void initClass(int tableWidth, int tableHeight, float iCellWidth, float iCellHeight)
	{
		this.mCellWidth = iCellWidth;
		this.mCellHeight = iCellHeight;

		this.mTableWidth = tableWidth / (int) this.mCellWidth;
		this.mTableHeight = tableHeight / (int) this.mCellHeight;

		paths[0] = darwPath1(mCellWidth, mCellHeight, false);
		paths[1] = darwPath2(mCellWidth, mCellHeight);
		paths[2] = darwPath3(mCellWidth, mCellHeight);
		paths[3] = darwPath4(mCellWidth, mCellHeight);
		paths[4] = darwPath1(mCellWidth, mCellHeight, true);
		paths[5] = Bitmap.createBitmap((int) iCellWidth, (int) iCellHeight, Bitmap.Config.ARGB_4444);

		int trackWidth = 9;
		masks[0] = darwMask1(mCellWidth, mCellHeight, false, trackWidth);
		masks[1] = darwMask2(mCellWidth, mCellHeight, trackWidth);
		masks[2] = darwMask3(mCellWidth, mCellHeight, trackWidth);
		masks[3] = darwMask4(mCellWidth, mCellHeight, trackWidth);
		masks[4] = darwMask1(mCellWidth, mCellHeight, true, trackWidth);
		masks[5] = Bitmap.createBitmap((int) iCellWidth, (int) iCellHeight, Bitmap.Config.ARGB_4444);

		this.mOPMethod = new OPMethod(mTableWidth, mTableHeight, (int) mCellWidth, (int) mCellHeight);

		return;
	}

	public boolean newMaze()
	{
		ready = false;

		goyoList = new ArrayList<GoyoTile>();

		try
		{
			this.maCellData = this.mOPMethod.createLabyrinth();
		}
		catch (Exception ex)
		{
			Log.i(TAG, "" + ex);
			return false;
		}

		int type, rot;
		for (int x = 0; x < this.mTableWidth; x++)
		{
			for (int y = 0; y < this.mTableHeight; y++)
			{
				type = getMazeType(x, y, mTableWidth, mTableHeight);
				rot = getAngle(type);
				goyoList.add(addTile(x, y, type, rot));
			}
		}

		ready = true;

		return true;
	}

	private GoyoTile addTile(int x, int y, int type, int rot)
	{
		GoyoTile tile = new GoyoTile();
		tile.type = type;
		tile.posX = x * mCellWidth;
		tile.posY = y * mCellHeight;
		tile.rot = rot;

		tile.id = goyoList.size();

		Bitmap bitmap = paths[getTileType(tile.type)];
		tile.height = bitmap.getHeight();
		tile.width = bitmap.getWidth();

		tile.pathDraw = new BitmapDrawable(bitmap);
		tile.pathDraw.setBounds(0, 0, (int) mCellWidth, (int) mCellHeight);
		tile.pathDraw.setAlpha(64);

		tile.mask = masks[getTileType(tile.type)]; 
		tile.maskDraw = new BitmapDrawable(tile.mask);
		tile.maskDraw.setBounds(0, 0, (int) mCellWidth, (int) mCellHeight);

		return tile;
	}

	// line
	private Bitmap darwPath1(float w, float h, boolean half)
	{
		Bitmap bitmap = Bitmap.createBitmap((int) w, (int) h, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bitmap);

		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.STROKE);

		int[] colors = new int[7];
		colors[0] = Color.BLACK;
		colors[1] = Color.BLACK;
		colors[2] = Color.WHITE;
		colors[3] = Color.GRAY;
		colors[4] = Color.WHITE;
		colors[5] = Color.BLACK;
		colors[6] = Color.BLACK;

		// end path
		if (half)
		{
			int[] cols = new int[4];
			cols[0] = Color.GRAY;
			cols[1] = Color.WHITE;
			cols[2] = Color.BLACK;
			cols[3] = Color.BLACK;

			float rad = (h + w) / 4;
			RadialGradient gradient = new RadialGradient(w / 2, h / 2, rad, cols, null, android.graphics.Shader.TileMode.CLAMP);
			paint.setShader(gradient);
			paint.setStyle(Paint.Style.FILL);

			canvas.drawCircle(w / 2, h / 2, rad, paint);
		}

		LinearGradient gradient = new LinearGradient(0, 0, 0, h, colors, null, android.graphics.Shader.TileMode.CLAMP);
		paint.setShader(gradient);
		paint.setStrokeWidth(w * 3 / 4);

		// end path
		if (half)
		{
			canvas.drawLine(0, h / 2, w / 2, h / 2, paint);
		}
		else
		{
			canvas.drawLine(0, h / 2, w, h / 2, paint);
		}

		return bitmap;
	}

	// line
	private Bitmap darwMask1(float w, float h, boolean half, int width)
	{
		Bitmap bitmap = Bitmap.createBitmap((int) w, (int) h, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bitmap);

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(width);
		paint.setColor(Color.BLACK);

		if (half)
		{
			canvas.drawLine(0, h / 2, w / 2, h / 2, paint);
		}
		else
		{
			canvas.drawLine(0, h / 2, w, h / 2, paint);
		}

		return bitmap;
	}

	// curve
	private Bitmap darwPath2(float w, float h)
	{
		Bitmap bitmap = Bitmap.createBitmap((int) w, (int) h, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bitmap);

		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.STROKE);

		int[] colors = new int[7];
		colors[0] = Color.BLACK;
		colors[1] = Color.BLACK;
		colors[2] = Color.WHITE;
		colors[3] = Color.GRAY;
		colors[4] = Color.WHITE;
		colors[5] = Color.BLACK;
		colors[6] = Color.BLACK;

		RadialGradient gradient = new RadialGradient(0, 0, w, colors, null, android.graphics.Shader.TileMode.CLAMP);
		paint.setShader(gradient);
		paint.setStrokeWidth(w * 3 / 4);

		// float ox = w / (EDGE * 100 / 2);
		// float oy = h / (EDGE * 100 / 2);
		float ox = 0;
		float oy = 0;

		RectF rect = new RectF();
		rect.set(-w / 2 + ox, -h / 2 + oy, w / 2, h / 2);
		canvas.drawArc(rect, 0, 90, false, paint);

		return bitmap;
	}

	// curve
	private Bitmap darwMask2(float w, float h, int width)
	{
		Bitmap bitmap = Bitmap.createBitmap((int) w, (int) h, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bitmap);

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(width);
		paint.setColor(Color.BLACK);

		RectF rect = new RectF();
		rect.set(-w / 2, -h / 2, w / 2, h / 2);
		canvas.drawArc(rect, 0, 90, false, paint);

		return bitmap;
	}

	// three direction
	private Bitmap darwPath3(float w, float h)
	{
		Bitmap bitmap = Bitmap.createBitmap((int) w, (int) h, Bitmap.Config.ARGB_4444);
		bitmap.eraseColor(Color.TRANSPARENT);
		Canvas canvas = new Canvas(bitmap);

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setAntiAlias(true);

		int[] colors = new int[7];
		colors[0] = Color.BLACK;
		colors[1] = Color.BLACK;
		colors[2] = Color.WHITE;
		colors[3] = Color.GRAY;
		colors[4] = Color.GRAY;
		colors[5] = Color.GRAY;
		colors[6] = Color.GRAY;

		paint.setStrokeWidth(w * 3 / 4);
		RadialGradient gradient = new RadialGradient(0, 0, w, colors, null, android.graphics.Shader.TileMode.CLAMP);
		paint.setShader(gradient);

		RectF rect = new RectF();
		rect.set(-w / 2, -h / 2, w / 2, h / 2);
		canvas.drawArc(rect, 0, 90, false, paint);

		Bitmap corner1 = Bitmap.createBitmap((int) (w / 2), (int) (h / 2), Bitmap.Config.ARGB_4444);
		Canvas canv1 = new Canvas(corner1);
		Rect src = new Rect(0, 0, (int) (w / 2), (int) (h / 2));
		Rect dst = new Rect(0, 0, (int) (w / 2), (int) (h / 2));
		canv1.drawBitmap(bitmap, src, dst, paint);
		Matrix matrix = new Matrix();
		matrix.setRotate(0, corner1.getWidth() / 2, corner1.getHeight() / 2);

		Bitmap corner2 = Bitmap.createBitmap((int) (w / 2), (int) (h / 2), Bitmap.Config.ARGB_4444);
		Canvas canv2 = new Canvas(corner2);
		canv2.drawBitmap(corner1, matrix, paint);

		colors[0] = Color.BLACK;
		colors[1] = Color.BLACK;
		colors[2] = Color.WHITE;
		colors[3] = Color.GRAY;
		colors[4] = Color.WHITE;
		colors[5] = Color.BLACK;
		colors[6] = Color.BLACK;

		LinearGradient grad = new LinearGradient(0, 0, 0, h, colors, null, android.graphics.Shader.TileMode.CLAMP);
		paint.setShader(grad);
		paint.setStrokeWidth(w * 3 / 4);

		canvas.drawLine(0, h / 2, w, h / 2, paint);

		canvas.drawBitmap(corner2, 0, 0, paint);

		corner2 = Bitmap.createBitmap((int) (w / 2), (int) (h / 2), Bitmap.Config.ARGB_4444);
		canv2 = new Canvas(corner2);

		matrix.setRotate(90, corner1.getWidth() / 2, corner1.getHeight() / 2);
		canv2.drawBitmap(corner1, matrix, paint);

		canvas.drawBitmap(corner2, w / 2, 0, paint);

		return bitmap;
	}

	// three direction
	private Bitmap darwMask3(float w, float h, int width)
	{
		Bitmap bitmap = Bitmap.createBitmap((int) w, (int) h, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bitmap);

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(width);
		paint.setColor(Color.BLACK);

		canvas.drawLine(0, h / 2, w, h / 2, paint);

		RectF rect = new RectF();
		rect.set(-w / 2, -h / 2, w / 2, h / 2);
		canvas.drawArc(rect, 0, 90, false, paint);
		rect.set(w / 2, -h / 2, w + w / 2, h / 2);
		canvas.drawArc(rect, 270, 360, false, paint);

		Point pt = new Point();
		pt.x = (int) (w / 2);
		pt.y = (int) (h / 3);
		floodFill(bitmap, pt, Color.TRANSPARENT, Color.BLACK);
		
		canvas.drawPoint(pt.x, pt.y, paint);

		return bitmap;
	}

	// four direction
	private Bitmap darwPath4(float w, float h)
	{
		Bitmap bitmap = Bitmap.createBitmap((int) w, (int) h, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bitmap);
		RectF rect = new RectF();

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setAntiAlias(true);

		int[] colors = new int[7];
		colors[0] = Color.BLACK;
		colors[1] = Color.BLACK;
		colors[2] = Color.WHITE;
		colors[3] = Color.GRAY;
		colors[4] = Color.GRAY;
		colors[5] = Color.GRAY;
		colors[6] = Color.GRAY;

		paint.setStrokeWidth(w * 3 / 4);
		RadialGradient gradient = new RadialGradient(0, 0, w, colors, null, android.graphics.Shader.TileMode.CLAMP);
		paint.setShader(gradient);

		rect.set(-w / 2, -h / 2, w / 2, h / 2);
		canvas.drawArc(rect, 0, 90, false, paint);

		gradient = new RadialGradient(w, h, w, colors, null, android.graphics.Shader.TileMode.CLAMP);
		paint.setShader(gradient);

		rect.set(w / 2, h / 2, w + w / 2, h + h / 2);
		canvas.drawArc(rect, 180, 90, false, paint);

		Bitmap corner1 = Bitmap.createBitmap((int) (w / 2), (int) (h / 2), Bitmap.Config.ARGB_4444);
		Canvas canv1 = new Canvas(corner1);
		Rect src = new Rect(0, 0, (int) (w / 2), (int) (h / 2));
		Rect dst = new Rect(0, 0, (int) (w / 2), (int) (h / 2));
		canv1.drawBitmap(bitmap, src, dst, paint);
		Matrix matrix = new Matrix();
		matrix.setRotate(270, corner1.getWidth() / 2, corner1.getHeight() / 2);

		Bitmap corner2 = Bitmap.createBitmap((int) (w / 2), (int) (h / 2), Bitmap.Config.ARGB_4444);
		Canvas canv2 = new Canvas(corner2);
		canv2.drawBitmap(corner1, matrix, paint);

		canvas.drawBitmap(corner2, 0, h / 2, paint);

		corner2 = Bitmap.createBitmap((int) (w / 2), (int) (h / 2), Bitmap.Config.ARGB_4444);
		canv2 = new Canvas(corner2);

		matrix.setRotate(90, corner1.getWidth() / 2, corner1.getHeight() / 2);
		canv2.drawBitmap(corner1, matrix, paint);

		canvas.drawBitmap(corner2, w / 2, 0, paint);

		return bitmap;
	}

	// four direction
	private Bitmap darwMask4(float w, float h, int width)
	{
		Bitmap bitmap = Bitmap.createBitmap((int) w, (int) h, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bitmap);

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(width);
		paint.setColor(Color.BLACK);

		RectF rect = new RectF();
		rect.set(-w / 2, -h / 2, w / 2, h / 2);
		canvas.drawArc(rect, 0, 90, false, paint);
		rect.set(w / 2, -h / 2, w + w / 2, h / 2);
		canvas.drawArc(rect, 270, 360, false, paint);
		rect.set(-w / 2, h / 2, w / 2, h + h / 2);
		canvas.drawArc(rect, 180, 270, false, paint);
		rect.set(w / 2, h / 2, w + w / 2, h + h / 2);
		canvas.drawArc(rect, 270, 360, false, paint);

		Point pt = new Point();
		pt.x = (int) (w / 2);
		pt.y = (int) (h / 2);
		floodFill(bitmap, pt, Color.TRANSPARENT, Color.BLACK);

		return bitmap;
	}

	// return with a cell type
	public int getMazeType(int x, int y, int tableWidth, int tableHeight)
	{
		int iRet = 0;
		boolean bLeft = false;
		boolean bRight = false;
		boolean bUp = false;
		boolean bDown = false;

		// left wall
		if ((this.maCellData.get(x).get(y) & 2) == 0) bLeft = true;
		// right wall or end
		if (x + 1 == tableWidth) bRight = true;
		else if ((this.maCellData.get(x + 1).get(y) & 2) == 0) bRight = true;
		// upward wall
		if ((this.maCellData.get(x).get(y) & 1) == 0) bUp = true;
		// downward wall or end
		if (y + 1 == tableHeight) bDown = true;
		else if ((this.maCellData.get(x).get(y + 1) & 1) == 0) bDown = true;

		if (bLeft && !bRight && bUp && !bDown) iRet = 1;// L from right to down way, left up wall
		if (!bLeft && bRight && bUp && !bDown) iRet = 2;// L from left to down way, right, up wall
		if (!bLeft && bRight && !bUp && bDown) iRet = 3;// L from up to left way, right up wall
		if (bLeft && !bRight && !bUp && bDown) iRet = 4;// L from up to right way, left down wall

		if (!bLeft && !bRight && bUp && bDown) iRet = 5;// I horizontal way, up, down wall
		if (bLeft && bRight && !bUp && !bDown) iRet = 6;// I vertical way left,
														// right wall

		if (!bLeft && !bRight && bUp && !bDown) iRet = 7;// T left, down and right way, up wall
		if (bLeft && !bRight && !bUp && !bDown) iRet = 8;// T up, right and down way, left wall
		if (!bLeft && bRight && !bUp && !bDown) iRet = 9;// T up, left and down way, right wall
		if (!bLeft && !bRight && !bUp && bDown) iRet = 10;// T up, right and left way, down wall

		if (!bLeft && bRight && bUp && bDown) iRet = 11;// E way from right
		if (bLeft && !bRight && bUp && bDown) iRet = 12;// E way from left
		if (bLeft && bRight && bUp && !bDown) iRet = 13;// E way from down
		if (bLeft && bRight && !bUp && bDown) iRet = 14;// E way from up

		if (!bLeft && !bRight && !bUp && !bDown) iRet = 15;// + no wall

		return iRet;
	}

	// tile type
	private int getTileType(int iType)
	{
		switch (iType)
		{
		case 0:
			return pathEmpty;// empty
		case 1:
			return pathElbow;// L
		case 2:
			return pathElbow;// L
		case 3:
			return pathElbow;// L
		case 4:
			return pathElbow;// L
		case 5:
			return pathLine;// I
		case 6:
			return pathLine;// I
		case 7:
			return pathT;// T
		case 8:
			return pathT;// T
		case 9:
			return pathT;// T
		case 10:
			return pathT;// T
		case 11:
			return pathEnd;// end
		case 12:
			return pathEnd;// end
		case 13:
			return pathEnd;// end
		case 14:
			return pathEnd;// end
		case 15:
			return pathX;// X
		}

		return 0;
	}

	// original rotation
	private int getAngle(int iType)
	{
		switch (iType)
		{
		case 1:
			return 180;
		case 2:
			return 270;
		case 3:
			return 0;
		case 4:
			return 90;
		case 5:
			return 0;
		case 6:
			return 90;
		case 7:
			return 180;
		case 8:
			return 90;
		case 9:
			return 270;
		case 10:
			return 0;
		case 11:
			return 0;
		case 12:
			return 180;
		case 13:
			return 270;
		case 14:
			return 90;
		case 15:
			return 0;
		}

		return 0;
	}

	// draw goyo
	private BitmapDrawable drawGoyo(float w, float h)
	{
		Bitmap bitmap = Bitmap.createBitmap((int) w, (int) h, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bitmap);

		Paint paint = new Paint();
		paint.setAntiAlias(true);

		int[] cols = new int[4];
		cols[0] = Color.WHITE;
		cols[1] = Color.WHITE;
		cols[2] = Color.BLACK;
		cols[3] = Color.WHITE;

		float rad = (h + w) / 2;
		RadialGradient gradient = new RadialGradient(w / 3, h / 3, rad, cols, null, android.graphics.Shader.TileMode.CLAMP);
		paint.setShader(gradient);
		paint.setStyle(Paint.Style.FILL);

		rad = (h + w) / 6;
		canvas.drawCircle(w / 2, h / 2, rad, paint);

		BitmapDrawable drawable = new BitmapDrawable(bitmap);
		drawable.setBounds(0, 0, (int) w, (int) h);

		return drawable;
	}

	// open side 1-left 2-right 4-up 8-bottom
	public int getWay(GoyoTile tile)
	{
		int iRet = 0;

		switch (getTileType(tile.type))
		{
		case pathLine: // I
			if (tile.rot == 180 || tile.rot == 0)
			{
				iRet |= wayLeft;
				iRet |= wayRight;
			}
			else
			{
				iRet |= wayUp;
				iRet |= wayDown;
			}
			break;
		case pathElbow: // L
			if (tile.rot == 0)
			{
				iRet |= wayLeft;
				iRet |= wayUp;
			}
			else if (tile.rot == 90)
			{
				iRet |= wayRight;
				iRet |= wayUp;
			}
			else if (tile.rot == 180)
			{
				iRet |= wayRight;
				iRet |= wayDown;
			}
			else if (tile.rot == 270)
			{
				iRet |= wayLeft;
				iRet |= wayDown;
			}
			break;
		case pathT: // T
			if (tile.rot == 0)
			{
				iRet |= wayLeft;
				iRet |= wayRight;
				iRet |= wayUp;
			}
			else if (tile.rot == 90)
			{
				iRet |= wayRight;
				iRet |= wayUp;
				iRet |= wayDown;
			}
			else if (tile.rot == 180)
			{
				iRet |= wayLeft;
				iRet |= wayRight;
				iRet |= wayDown;
			}
			else if (tile.rot == 270)
			{
				iRet |= wayLeft;
				iRet |= wayUp;
				iRet |= wayDown;
			}
			break;
		case pathX: // X
			iRet |= wayLeft;
			iRet |= wayRight;
			iRet |= wayUp;
			iRet |= wayDown;
			break;
		case pathEnd: // end
			if (tile.rot == 0) iRet |= wayLeft;
			else if (tile.rot == 90) iRet |= wayUp;
			else if (tile.rot == 180) iRet |= wayRight;
			else if (tile.rot == 270) iRet |= wayDown;
			break;
		}
		// Log.i(TAG, "Type: " + getTileType(tile.type) + " Rot: " + tile.rot +
		// " Way: " + iRet);
		return iRet;
	}

	// return the direction of a cell under the x, y position 
	public int getTileWay(float x, float y)
	{
		int tx = (int) (x / mCellWidth);
		int ty = (int) (y / mCellHeight);
		int idx = tx * this.mTableHeight + ty;
		if (tx < this.mTableWidth && ty < this.mTableHeight && idx < goyoList.size())
		{
			GoyoTile tile = goyoList.get(idx);

			return getWay(tile);
		}

		return 0;
	}

	// is the goyo inside the track 
	public int getGoyoPath(float x, float y)
	{
		int tx = (int) (x / mCellWidth);
		int ty = (int) (y / mCellHeight);
		int idx = tx * this.mTableHeight + ty;
		int iRes = -1;
		if (tx < this.mTableWidth && ty < this.mTableHeight && idx < goyoList.size())
		{
			GoyoTile tile = goyoList.get(idx);

			tx = (int)x - tx * (int)mCellWidth;
			ty = (int)y - ty * (int)mCellHeight;
			
			iRes = tile.mask.getPixel(tx, ty);
			
			Log.i(TAG, "tx: " + tx + " ty: " + ty + " iRes: " + iRes);
			
			return iRes;
		}

		return iRes;
	}

	// flood fill
	public void floodFill(Bitmap image, Point node, int oldColor, int newColor)
	{
		int width = image.getWidth();
		int height = image.getHeight();
		int target = oldColor;
		int replacement = newColor;
		if (target != replacement)
		{
			Queue<Point> queue = new LinkedList<Point>();
			do
			{
				int x = node.x;
				int y = node.y;
				while (x > 0 && image.getPixel(x - 1, y) == target)
				{
					x--;
				}
				boolean spanUp = false;
				boolean spanDown = false;
				while (x < width && image.getPixel(x, y) == target)
				{
					image.setPixel(x, y, replacement);
					if (!spanUp && y > 0 && image.getPixel(x, y - 1) == target)
					{
						queue.add(new Point(x, y - 1));
						spanUp = true;
					}
					else if (spanUp && y > 0 && image.getPixel(x, y - 1) != target)
					{
						spanUp = false;
					}
					if (!spanDown && y < height - 1 && image.getPixel(x, y + 1) == target)
					{
						queue.add(new Point(x, y + 1));
						spanDown = true;
					}
					else if (spanDown && y < height - 1 && image.getPixel(x, y + 1) != target)
					{
						spanDown = false;
					}
					x++;
				}
			}
			while ((node = queue.poll()) != null);
		}
	}
}
