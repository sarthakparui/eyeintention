package org.opencv.samples.facedetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
//import java.util.ArrayList;
//import java.util.List;
import android.widget.TextView;

public class EyeIntention extends Activity implements CvCameraViewListener2 {

	private static final String TAG = "Eye-Intention";
	private static final Scalar FACE_RECT_COLOR = new Scalar(255, 0, 0, 255);

	private Mat mRgba;
	private Mat mGray;

	private File mCascadeFile;
	private CascadeClassifier mFaceDetector;
	private CascadeClassifier mEyeDetector;

	private int learn_frames = 0;
	private Mat templateR;
	private Mat templateL;
	private float mRelFaceSize = 0.2f;
	private int mAbsFaceSize = 0;

	private CameraBridgeViewBase mOpenCvCameraView;

	double xCenter = -1;
	double yCenter = -1;

	Point lastPointL = new Point(-1, -1), lastPointR = new Point(-1, -1);
	Point tPointL = new Point(-2, -2), tPointR = new Point(-2, -2);
	Point lastGridR = new Point(1, 2), lastGridL = new Point(1, 2);
	double xThresh = 5;
	double yThresh = 7;
	int tMod = 1;
	int countMod = 256;
	int count = 0;
	double decay = 0.1;
	// bool isLeft;
	boolean slidekorbo=true;

	// S:UI
	LinearLayout imageRow1;
	LinearLayout imageRow2;

	int screenHeight, screenWidth;
	int numberOfImagesInRow1 = 8, numberOfImagesInRow2 = 8;
	int row1ShiftCounter = 0, row2ShiftCounter = 0;
	boolean displayModified = false;

	// S:UI
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				// Log.i(TAG, "OpenCV loaded successfully");

				try {
					InputStream is = getResources().openRawResource(
							R.raw.lbpcascade_frontalface);
					File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
					mCascadeFile = new File(cascadeDir,
							"lbpcascade_frontalface.xml");
					FileOutputStream os = new FileOutputStream(mCascadeFile);

					byte[] buffer = new byte[4096];
					int bytesRead;
					while ((bytesRead = is.read(buffer)) != -1) {
						os.write(buffer, 0, bytesRead);
					}
					is.close();
					os.close();

					InputStream iser = getResources().openRawResource(
							R.raw.haarcascade_lefteye_2splits);
					File RightEyecascadeDir = getDir("RightEyecascade",
							Context.MODE_PRIVATE);
					File right_eye_cascadeFile = new File(RightEyecascadeDir,
							"haarcascade_eye_right.xml");
					FileOutputStream oser = new FileOutputStream(
							right_eye_cascadeFile);

					byte[] bufferER = new byte[4096];
					int bytesReadER;
					while ((bytesReadER = iser.read(bufferER)) != -1) {
						oser.write(bufferER, 0, bytesReadER);
					}
					iser.close();
					oser.close();

					mFaceDetector = new CascadeClassifier(
							mCascadeFile.getAbsolutePath());

					mEyeDetector = new CascadeClassifier(
							right_eye_cascadeFile.getAbsolutePath());
					cascadeDir.delete();

				} catch (IOException e) {
					e.printStackTrace();
				}
				mOpenCvCameraView.setCameraIndex(1);
				// mOpenCvCameraView.enableFpsMeter();
				mOpenCvCameraView.enableView();

			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	public EyeIntention() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.eye_intention_surface_view);

		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.eye_intention_surface_view);
		mOpenCvCameraView.setCvCameraViewListener(this);

		// S:UI
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		screenHeight = displaymetrics.heightPixels;
		screenWidth = displaymetrics.widthPixels;
	}
		// S:UI
		
		// S:UI

	// S:UI

	public void changeScrollView(int rownum, boolean isRight) {
		HorizontalScrollView hsv;
		int shiftBy = 0;
		if (rownum == 1) {
			hsv = (HorizontalScrollView) findViewById(R.id.hsv1);
			if (isRight) {
				shiftBy = row1ShiftCounter++;
				// row1ShiftCounter=row1ShiftCounter>numberOfImagesInRow1?numberOfImagesInRow1:row1ShiftCounter;
			} else {
				shiftBy = row1ShiftCounter--;
				row1ShiftCounter = row1ShiftCounter < 0 ? 0 : row1ShiftCounter;
			}
		} else {
			hsv = (HorizontalScrollView) findViewById(R.id.hsv2);
		}
		Log.e("scroll", "shiftBy : " + shiftBy);
		int shiftMultiplier = 25; // Need to be made dynamic... number of items
									// to be shifted
		hsv.smoothScrollTo(hsv.getLeft() + shiftBy * shiftMultiplier, 0);
	}

	public void userExperienceEnhancer() {
		modifyDisplay();
	}

	private void modifyDisplay() {
		Log.e("change", "Inside modifyDisplay");
		ImageView image = (ImageView) findViewById(R.id.apl1);
		image.setImageResource(R.drawable.acc1);
		image = (ImageView) findViewById(R.id.apl2);
		image.setImageResource(R.drawable.acc2);
		image = (ImageView) findViewById(R.id.apl3);
		image.setImageResource(R.drawable.acc3);
		image = (ImageView) findViewById(R.id.apl4);
		image.setImageResource(R.drawable.acc4);
		image = (ImageView) findViewById(R.id.apl5);
		image.setImageResource(R.drawable.acc5);
		image = (ImageView) findViewById(R.id.apl6);
		image.setImageResource(R.drawable.acc6);
		image = (ImageView) findViewById(R.id.apl7);
		image.setImageResource(R.drawable.acc7);
		image = (ImageView) findViewById(R.id.apl8);
		image.setImageResource(R.drawable.acc8);

		TextView text = (TextView) findViewById(R.id.row2text);
		text.setHighlightColor(Color.GREEN);
		text.setText("Row 2: Mobile Accesories");
		displayModified = true;
	}

	public void changeFocus(int row) {
		TextView text = (TextView) findViewById(R.id.currentfocus);
		if (row == 1) {
			text.setText("Current Focus : Mobile Devices");
		} else {
			if (displayModified)
				text.setText("Current Focus : Mobile Accesories");
			else
				text.setText("Current Focus : Appliances");
		}
	}

	// S:UI

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
				mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		mOpenCvCameraView.disableView();
	}

	public void onCameraViewStarted(int width, int height) {
		mGray = new Mat();
		mRgba = new Mat();
	}

	public void onCameraViewStopped() {
		mGray.release();
		mRgba.release();
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

		mRgba = inputFrame.rgba();
		mGray = inputFrame.gray();

		if (mAbsFaceSize == 0) {
			int height = mGray.rows();
			// if (Math.round(height * mRelFaceSize) > 0) {
			// mAbsFaceSize = Math.round(height * mRelFaceSize);
			// }
			mAbsFaceSize = Math.round(height * mRelFaceSize);
		}

		MatOfRect faces = new MatOfRect();

		if (mFaceDetector != null)
			mFaceDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, new Size(
					mAbsFaceSize, mAbsFaceSize), new Size());

		Rect[] facesArray = faces.toArray();
		// count=0;

		for (int i = 0; i < facesArray.length; i++) {
			learn_frames = 0;

			Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(),
					FACE_RECT_COLOR, 3);
			xCenter = (facesArray[i].x + facesArray[i].width + facesArray[i].x) / 2;
			yCenter = (facesArray[i].y + facesArray[i].y + facesArray[i].height) / 2;
			Point center = new Point(xCenter, yCenter);

			Rect r = facesArray[i];

			Rect righteyearea = new Rect(r.x + r.width / 16,
					(int) (r.y + (r.height / 4.5)),
					(r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
			Rect lefteyearea = new Rect(r.x + r.width / 16
					+ (r.width - 2 * r.width / 16) / 2,
					(int) (r.y + (r.height / 4.5)),
					(r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));

			Log.d(TAG, "Green eyes ..");
			Core.rectangle(mRgba, lefteyearea.tl(), lefteyearea.br(),
					new Scalar(0, 255, 0, 255), 2);
			Core.rectangle(mRgba, righteyearea.tl(), righteyearea.br(),
					new Scalar(0, 255, 0, 255), 2);

			// Log.i(TAG, " count: "+count);
			if (learn_frames < 5) {
				// Log.d(TAG, "learn_frames: "+learn_frames);
				// Set tPointR and tPointL inside
				templateR = get_template(mEyeDetector, righteyearea, 24, false);
				templateL = get_template(mEyeDetector, lefteyearea, 24, true);
				learn_frames++;
			} else {
				Log.d(TAG, "ELSE : learn_frames: " + learn_frames);
				match_eye(righteyearea, templateR, false);
				match_eye(lefteyearea, templateL, true);
			}

			Log.i(TAG, " count: " + count);
			// Log.i(TAG," The tpointR : "+tPointR.x +" "+tPointR.y);
			Log.i(TAG, " The tpointL : " + tPointL.x + " " + tPointL.y);

			count++;
			count = count % countMod;
			// Comment123
			if (count % tMod == tMod - 1) {
				lastGridR = get_Gridlocation(false);
				lastGridL = get_Gridlocation(true);

				// Log.i(TAG,"The GridR info: "+lastGridR.x +" "+lastGridR.y);
				Log.i(TAG, "The GridL info: " + lastGridL.x + " " + lastGridL.y);
			}
			lastPointR.x = (1 - decay) * lastPointR.x + decay * tPointR.x;
			lastPointR.y = (1 - decay) * lastPointR.y + decay * tPointR.y;
			lastPointL.x = (1 - decay) * lastPointL.x + decay * tPointL.x;
			lastPointL.y = (1 - decay) * lastPointL.y + decay * tPointL.y;

			// Log.i(TAG,"The lastpointR : "+lastPointR.x +" "+lastPointR.y);
			Log.i(TAG, "The lastpointL : " + lastPointL.x + " " + lastPointL.y);

			if (lastGridR.x == lastGridL.x) {
				Log.i(TAG, "Both eyes set on column : " + lastGridL.x);
			}
			if (lastGridR.y == lastGridL.y) {
				Log.i(TAG, "Both eyes set on row : " + lastGridL.y);
			}

			// S:UI
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
						if (slidekorbo)
						{
							changeScrollView(1, true);
							slidekorbo=false;
						}
						else
							slidekorbo=true;
					}
				
			});

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Log.e("change", "count = " + count);
					if (count == 0) {
						//userExperienceEnhancer();
					}

				}
			});

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					//if (lastGridL.y == 1)
						//changeFocus(1);
					//else
						//changeFocus(2);
				}
			});

		}

		// S:TEST GRID UI
		// drawCircleAtGrid();

		return mRgba;
	}

	private Point get_Gridlocation(boolean isLeft) {
		// Grid = >Point grid;
		// 1 2 3
		// 4 5 6
		//
		Point lastGrid;
		Point tPoint;
		Point lastPoint;

		if (isLeft) {
			lastGrid = lastGridL;
			tPoint = tPointL;
			lastPoint = lastPointL;
		} else {
			lastGrid = lastGridR;
			tPoint = tPointR;
			lastPoint = lastPointR;
		}

		// if(tPoint.x -lastPoint.x >xThresh){
		if (tPoint.x - lastPoint.x < -xThresh) {
			lastGrid.x = lastGrid.x + 1;
			if (lastGrid.x > 3) {
				lastGrid.x = 3;
			}
			// }else if(tPoint.x - lastPoint.x < - xThresh){
		} else if (tPoint.x - lastPoint.x > xThresh) {
			lastGrid.x = lastGrid.x - 1;
			if (lastGrid.x < 1) {
				lastGrid.x = 1;
			}
		}

		if (tPoint.y - lastPoint.y > yThresh) {
			lastGrid.y = lastGrid.y + 1;
			if (lastGrid.y > 2) {
				lastGrid.y = 2;
			}
		} else if (tPoint.y - lastPoint.y < -yThresh) {
			lastGrid.y = lastGrid.y - 1;
			if (lastGrid.y < 1) {
				lastGrid.y = 1;
			}
		}
		return lastGrid;
	}

	private Mat get_template(CascadeClassifier clasificator, Rect area,
			int size, boolean isLeft) {
		Mat template = new Mat();
		Mat mROI = mGray.submat(area);
		MatOfRect eyes = new MatOfRect();
		Point iris = new Point();
		Rect eye_template = new Rect();
		clasificator.detectMultiScale(mROI, eyes, 1.15, 2,
				Objdetect.CASCADE_FIND_BIGGEST_OBJECT
						| Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30),
				new Size());

		Rect[] eyesArray = eyes.toArray();
		for (int i = 0; i < eyesArray.length;) {
			Rect e = eyesArray[i];
			e.x = area.x + e.x;
			e.y = area.y + e.y;
			Rect eye_rect = new Rect((int) e.tl().x,
					(int) (e.tl().y + e.height * 0.4), (int) e.width,
					(int) (e.height * 0.6));
			mROI = mGray.submat(eye_rect);
			Mat vyrez = mRgba.submat(eye_rect);

			Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);
			Log.d(TAG, "White Circle =>Pupil");

			Core.circle(vyrez, mmG.minLoc, 2, new Scalar(255, 255, 255, 255), 2);

			iris.x = mmG.minLoc.x + eye_rect.x;
			iris.y = mmG.minLoc.y + eye_rect.y;
			eye_template = new Rect((int) iris.x - size / 2, (int) iris.y
					- size / 2, size, size);

			/*
			 * if(isLeft){ Core.putText(mRgba, "[" + mmG.minLoc.x + "," +
			 * mmG.minLoc.y + "]", new Point(iris.x + 20, iris.y + 20),
			 * Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 255, 255));
			 * }else{ Core.putText(mRgba, "[" + mmG.minLoc.x + "," +
			 * mmG.minLoc.y + "]", new Point(iris.x - 20, iris.y + 20),
			 * Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 255, 255));
			 * }
			 */

			if (isLeft) {
				tPointL = mmG.minLoc;
			} else {
				tPointR = mmG.minLoc;
			}

			Log.e(TAG, "mmG.minLoc = " + mmG.minLoc.x + " " + mmG.minLoc.y);

			Log.d(TAG, "Red Rectangale =><Eye>");
			Core.rectangle(mRgba, eye_template.tl(), eye_template.br(),
					new Scalar(255, 0, 0, 255), 2);
			template = (mGray.submat(eye_template)).clone();
			return template;
		}
		return template;
	}

	private void match_eye(Rect area, Mat mTemplate, boolean isLeft) {
		Point matchLoc;
		Mat mROI = mGray.submat(area);
		int result_cols = mROI.cols() - mTemplate.cols() + 1;
		int result_rows = mROI.rows() - mTemplate.rows() + 1;
		// Check for bad template size
		if (mTemplate.cols() == 0 || mTemplate.rows() == 0) {
			if (isLeft) {
				tPointL = new Point(0, 0);
			} else {
				tPointR = new Point(0, 0);
			}
			return;
		}
		Mat mResult = new Mat(result_cols, result_rows, CvType.CV_8U);

		Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCORR_NORMED);

		Core.MinMaxLocResult mmres = Core.minMaxLoc(mResult);

		matchLoc = mmres.maxLoc;
		Log.d(TAG, "matLoc = " + matchLoc.x + " " + matchLoc.y);
		Point matchLoc_tx = new Point(matchLoc.x + area.x, matchLoc.y + area.y);
		Point matchLoc_ty = new Point(matchLoc.x + mTemplate.cols() + area.x,
				matchLoc.y + mTemplate.rows() + area.y);

		if (isLeft) {
			tPointL = matchLoc;
		} else {
			tPointR = matchLoc;
		}

		Core.rectangle(mRgba, matchLoc_tx, matchLoc_ty, new Scalar(255, 255, 0,
				255));

	}

}
