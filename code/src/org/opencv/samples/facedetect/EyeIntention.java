package org.opencv.samples.facedetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
//import java.util.ArrayList;
//import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.video.Video;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class EyeIntention extends Activity implements CvCameraViewListener2 {

	private static final String TAG = "Eye-Intention::Activity";
	private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
	

	private Mat mRgba;
	private Mat mGray;

	private File mCascadeFile;
	private CascadeClassifier mFaceDetector;
		
	
	private float mRelFaceSize = 0.2f;
	private int mAbsFaceSize = 0;

	private CameraBridgeViewBase mOpenCvCameraView;


	double xCenter = -1;
	double yCenter = -1;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
//				Log.i(TAG, "OpenCV loaded successfully");


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

					mFaceDetector = new CascadeClassifier(
								mCascadeFile.getAbsolutePath());


					cascadeDir.delete();

				} catch (IOException e) {
					e.printStackTrace();
				}
				mOpenCvCameraView.setCameraIndex(1);
//				mOpenCvCameraView.enableFpsMeter();
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
		
	}

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
//			if (Math.round(height * mRelFaceSize) > 0) {
//				mAbsFaceSize = Math.round(height * mRelFaceSize);
//			}
			mAbsFaceSize = Math.round(height * mRelFaceSize);
		}
		
		MatOfRect faces = new MatOfRect();

		if (mFaceDetector != null)
			mFaceDetector.detectMultiScale(mGray, faces, 1.1, 2,
					2, new Size(mAbsFaceSize, mAbsFaceSize),
					new Size());

		Rect[] facesArray = faces.toArray();
		for (int i = 0; i < facesArray.length; i++) {
			Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(),
					FACE_RECT_COLOR, 3);
			xCenter = (facesArray[i].x + facesArray[i].width + facesArray[i].x) / 2;
			yCenter = (facesArray[i].y + facesArray[i].y + facesArray[i].height) / 2;
			Point center = new Point(xCenter, yCenter);

			Core.circle(mRgba, center, 10, new Scalar(255, 0, 0, 255), 3);

		}
		return mRgba;
	}

}
