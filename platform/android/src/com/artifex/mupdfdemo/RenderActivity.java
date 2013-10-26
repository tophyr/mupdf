package com.artifex.mupdfdemo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;

public class RenderActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();

		final int sectors = intent.getIntExtra("sectors", 8);
		final int height = intent.getIntExtra("height", 0);
		final int width = intent.getIntExtra("width", 0);
		
		if (height <= 0 || width <= 0 || sectors <= 0) {
			setResult(RESULT_CANCELED, new Intent().putExtra("error", new Exception("Height, width and sectors must all be > 0")));
			finish();
			return;
		}
		
		ContentResolver cr = getContentResolver();
		try {
			InputStream is = cr.openInputStream(intent.getData());
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			int read = 0;
			while ((read = is.read(buf)) > 0)
				baos.write(buf, 0, read);
			is.close();
			
			File dir = new File(getExternalFilesDir(null), UUID.randomUUID().toString());
			dir.mkdirs();
			
			MuPDFCore pdfCore = new MuPDFCore(this, baos.toByteArray());
			final int totalPages = pdfCore.countPages();
			Bitmap page = Bitmap.createBitmap(width, height, Config.ARGB_8888);
			Canvas canvas = new Canvas(page);
			Paint p = new Paint();
			
			for (int i = 0; i < totalPages; i++) {
				for (int x = 0; x < sectors; x++) {
					for (int y = 0; y < sectors; y++) {
						Bitmap patch = pdfCore.drawPage(i, width * sectors, height * sectors, width * x, height * y, width, height);
						canvas.drawBitmap(patch, 0, 0, p);					
						File t = new File(dir, String.format("%d_%d_%d.png", i, x, y));
						t.createNewFile();
						FileOutputStream fos = new FileOutputStream(t);
						page.compress(CompressFormat.PNG, 100, fos);
						fos.close();
					}
				}
			}
			setResult(RESULT_OK, new Intent().setDataAndType(Uri.fromFile(dir), "image/png")
											 .putExtra("totalPages", totalPages)
											 .putExtra("sectors", sectors)
											 .putExtra("pageHeight", pdfCore.getPageSize(0).y)
											 .putExtra("pageWidth", pdfCore.getPageSize(0).x));
			finish();
		} catch (Throwable e) {
			setResult(RESULT_CANCELED, new Intent().putExtra("error", e));
			finish();
			return;
		}
	}
}
