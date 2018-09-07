package com.gupar.p2pchat.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;

public class ImageUtil {
	public static String rootPath = "/sdcard/miceImage/";

	public ImageUtil() {
		// TODO Auto-generated constructor stub
	}

	public static boolean saveBitmap2file(Context mContext, Bitmap bmp) {
		Long now_time = Math.round(System.currentTimeMillis() / 1000.0);
		String filename = now_time + ".jpg";
		
		File fileFolder = new File(rootPath);
		if (!fileFolder.exists()) { // 如果目录不存在，则创建该目录
			fileFolder.mkdir();
		}
		CompressFormat format = Bitmap.CompressFormat.JPEG;
		int quality = 100;
		OutputStream stream = null;
		try {
			stream = new FileOutputStream(rootPath + filename);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// 通知更新相册
//		Intent intent1 = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//		Uri uri = Uri.fromFile(new File(rootPath + filename));
//		intent1.setData(uri);
//		mContext.sendBroadcast(intent1);
		return bmp.compress(format, quality, stream);
	}
}
