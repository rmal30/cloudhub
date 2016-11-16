package com.x.cloudhub;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
public class ProgressActivity extends Activity{
	static ProgressDialog progress;
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		progress.show();
		finish();
	}
}