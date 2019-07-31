package com.exam.asynctaskdemo;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.Toast;

import com.wuxiaolong.androidutils.library.LogUtil;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * MainActivity
 * <p>
 * <p>
 * Created by Alex Y. Lan on 2019-08-01.
 * Copyright © 2019 Alex Y. Lan All rights reserved.
 */
public class MainActivity extends AppCompatActivity {
    ImageAsynTask imageAsynTask;

    Map<String, String> key_urls = new HashMap<String, String>() {{
        put("pic0", "http://img-arch.pconline.com.cn/images/upload/upc/tx/itbbs/1712/29/c22/71298748_1514538295753.jpg");
        put("pic1", "http://img-arch.pconline.com.cn/images/upload/upc/tx/itbbs/1712/29/c22/71298535_1514538301713.jpg");
        put("pic2", "http://img.pconline.com.cn/images/upload/upc/tx/itbbs/1712/29/c22/71298437_1514538306877.jpg");
    }};


    @BindView(R.id.pic0)
    ImageView imageView0;
    @BindView(R.id.pic1)
    ImageView imageView1;
    @BindView(R.id.pic2)
    ImageView imageView2;

    ProgressDialog progressBar;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imageAsynTask != null && imageAsynTask.getStatus() == AsyncTask.Status.RUNNING) {
            //cancel方法只是将对应的AsyncTask标记为cancel状态，并没有真正的取消线程的执行。
            imageAsynTask.cancel(true);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        progressBar = new ProgressDialog(this);
        progressBar.setCancelable(true);
        progressBar.setMessage("Images downloading ...");
        progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressBar.setProgress(0);
        progressBar.setMax(100);
    }

    @OnClick(R.id.btn_1)
    void b() {
        if (imageAsynTask == null || imageAsynTask.isCancelled()) {
            imageAsynTask = new ImageAsynTask(this);
            imageAsynTask.execute(key_urls);
        }
    }

    private static class ImageAsynTask extends AsyncTask<Map<String, String>, Object, Map<String, Bitmap>> {
        private final WeakReference<MainActivity> act;
        private Map<String, Bitmap> keyBitmaps = new HashMap<>();
        private static final int CODE_UPDATE = 0;
        private static final int CODE_FINISH = 1;
        private static final int CODE_ERROR = 2;
        private static final int ALL_FINISH = 3;
        /**
         * 锁住总任务计数的临界代码模块
         */
        private static final String lock = "lock";
        /**
         * 锁住计算已下载的百分比的临界代码模块
         */
        private static final String lock2 = "lock2";
        private volatile int fileCount = 3; // 文件数
        private volatile int executions = 3;// 任务数
        volatile int fileSize = 0;// 文件大小
        volatile int loadedSize = 0;

        public ImageAsynTask(MainActivity act) {
            this.act = new WeakReference<>(act);
        }

        /**
         * (运行在子线程中)(此函数是抽象函数必须实现)
         * <p>
         * 在任务被线程池执行时调用 ，可以在此方法中处理比较耗时的操作，比如下载文件等等。
         *
         * @param keyUrls
         * @return
         */
        @Override
        protected Map<String, Bitmap> doInBackground(Map<String, String>... keyUrls) {
            for (Map.Entry<String, String> keyUrl : keyUrls[0].entrySet()) {
                ChildThread thread = new ChildThread(keyUrl);
                thread.setName(keyUrl.getKey());
                thread.start();
            }
            return keyBitmaps;
        }

        /**
         * (运行在UI线程中) (非必须方法，可以不用实现)
         * <p>
         * 在任务没被线程池执行之前调用，通常用来做一些准备操作，比如下载文件任务执行前，在这个方法中显示一个进度条。
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            act.get().progressBar.show();
        }

        /**
         * (运行在UI线程中) (非必须方法，可以不用实现)
         * <p>
         * 此函数代表任务在线程池中执行结束了，回调给UI主线程的结果。比如下载结果。
         *
         * @param bitmaps
         */
        @Override
        protected void onPostExecute(Map<String, Bitmap> bitmaps) {
//            act.get().progressBar.cancel();
            LogUtil.d("onPostExecute()");
        }

        /**
         * (运行在UI线程中) (非必须方法，可以不用实现)
         * <p>
         * 此函数是任务在线程池中执行处于Running状态，回调给UI主线程的进度，比如上传或者下载进度。
         *
         * @param values
         */
        @Override
        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);
            ProgressDialog progressBar = act.get().progressBar;
            switch ((int) values[0]) {
                case CODE_UPDATE:
                    progressBar.setProgress((int) values[1]);
                    break;
                case CODE_FINISH:
                    if (values[1].equals("pic0")) {
                        act.get().imageView0.setImageBitmap((Bitmap) values[2]);
                    }
                    if (values[1].equals("pic1")) {
                        act.get().imageView1.setImageBitmap((Bitmap) values[2]);
                    }
                    if (values[1].equals("pic2")) {
                        act.get().imageView2.setImageBitmap((Bitmap) values[2]);
                    }
                    break;
                case CODE_ERROR:
                    Toast.makeText(act.get(), (String) values[1], Toast.LENGTH_SHORT).show();
                    break;
                case ALL_FINISH:
                    if (progressBar.isShowing()) {
                        progressBar.cancel();
                    }
                    Toast.makeText(act.get(), (String) values[1], Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }

        private class ChildThread extends Thread {
            Map.Entry<String, String> keyUrl;

            public ChildThread(Map.Entry<String, String> keyUrl) {
                this.keyUrl = keyUrl;
            }

            @Override
            public void run() {
                String key = keyUrl.getKey();
                String url = keyUrl.getValue();
                URLConnection urlConnection = null;
                InputStream is;
                synchronized (ImageAsynTask.this) {// 这里使用使用同步线程,确保 N 个图片的总大小在所有下载未开始之前取得
                    try {
                        urlConnection = new URL(url).openConnection();
                        urlConnection.setConnectTimeout(5 * 1000);
                        fileSize += urlConnection.getContentLength();
                        if (fileCount > 1) {
                            try {
                                fileCount--;
                                ImageAsynTask.this.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else {
                            ImageAsynTask.this.notifyAll();
                            LogUtil.d("fileSize: " + fileSize);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        LogUtil.e(e.getMessage());
                    }
                }
                try {
                    if (urlConnection == null) return;

                    byte[] result;
                    is = urlConnection.getInputStream();
                    BufferedInputStream bf = new BufferedInputStream(is);
                    int len;
                    byte[] buffer = new byte[1024];
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    while ((len = bf.read(buffer)) != -1) {
                        synchronized (lock2) {
                            loadedSize += len;
                        }
                        outputStream.write(buffer, 0, len);
                        publishProgress(CODE_UPDATE, (int) ((float) loadedSize / (float) fileSize * 100));
                    }
                    result = outputStream.toByteArray();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(result, 0, result.length);
                    keyBitmaps.put(key, bitmap);
                    publishProgress(CODE_FINISH, key, bitmap);
                    is.close();
                    bf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    publishProgress(CODE_ERROR, keyUrl.getKey() + " 有问题，请检查链接！");
                } finally {
                    synchronized (lock) {
                        executions--;
                    }
                    if (executions <= 0) {
                        LogUtil.d("finally : " + executions);
                        LogUtil.d("下载结束！");
                        publishProgress(ALL_FINISH, "下载结束！");
                    }
                }
            }
        }

        /**
         *
         */
        @Override
        protected void onCancelled() {
            super.onCancelled();
            LogUtil.d("method onCancelled() run in \"" + Thread.currentThread().getName() + "\" thread");
        }

        @Override
        protected void onCancelled(Map<String, Bitmap> bitmaps) {
            super.onCancelled(bitmaps);
            LogUtil.d("method onCancelled(List<Bitmap> bitmaps) run in \"" + Thread.currentThread().getName() + "\" thread");
        }
    }
}