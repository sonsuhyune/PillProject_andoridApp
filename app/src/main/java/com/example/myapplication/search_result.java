package com.example.myapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;

import static com.example.myapplication.MainActivity.mark;
import static com.example.myapplication.MainActivity.shape;
import static com.example.myapplication.show_detail.pill;

public class search_result extends AppCompatActivity {

    /* 이미지 socket 통신 부분 */
    private Handler mHandler;
    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;
    private InputStream in;
    private BufferedInputStream bis;
    private String ip = "203.255.176.79";
    private int port = 8089; //DB_img_server.py


    private static String TAG = "search_result";
    private static final String TAG_JSON="user_result";
    private static final String TAG_IMG = "img";
    private static final String TAG_NAME = "pill_name";

    private int pill_num = 0;
    private byte[][] img_list = null;
    private String[] name_list = null;

    ListView mlistView;
    ListViewAdapterResult adapter;
    String mJsonString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_result_before_login);

        adapter = new  ListViewAdapterResult();
        mlistView = (ListView) findViewById(R.id.result_listView_before_login);


        GetData task = new GetData();
        task.execute();


    }

    private class GetData extends AsyncTask<Void, Void, String> {
        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(search_result.this,
                    "Please Wait", null, true, true);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();
            Log.d(TAG, "response  - " + result);

            mJsonString = result;

            System.out.println(mJsonString);
            connect();
            System.out.println("socket done");
            showResult();
            mlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    pill = adapter.getPillName(position);
                    System.out.println(pill);

                    show_detail_before_login();
                }
            });
        }

        @Override
        protected String doInBackground(Void... unused) {

            if(shape.equals("oval")){
                shape = "타원형";
            }
            else if (shape.equals("circle")){
                shape ="원형";
            }
            else if (shape.equals("hexagon")){
                shape = "팔각형";
            }
            else if(shape.equals("octagon")){
                shape = "육각형";
            }
            String[] marks = mark.split(",");
            System.out.println(marks.length);
            if(marks.length >= 2){
                //System.out.println(marks[0].length());
                //System.out.println(marks[1].length());
                int one = marks[0].length();
                int two = marks[1].length();
                mark = marks[0].substring(1,one-1)+"|"+marks[1].substring(2,two-1);
                //System.out.println(mark);
            }
            else if(marks.length == 1){
                int one = marks[0].length();
                mark = marks[0].substring(1,one-1);
            }

            /* 인풋 파라메터값 생성 */
            String param = "mark="+mark+""+ "&shape=" + shape + "";;
            System.out.println(param);
            Log.e("POST",param);

            try {
                /* 서버연결 */
                URL url = new URL("http://203.255.176.79:8000/get_result.php");
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();


                /* 안드로이드 -> 서버 파라메터값 전달 */
                OutputStream outs = httpURLConnection.getOutputStream();
                outs.write(param.getBytes("UTF-8"));
                outs.flush();
                outs.close();

                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "response code - " + responseStatusCode);

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                }
                else{
                    inputStream = httpURLConnection.getErrorStream();
                }


                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line;

                while((line = bufferedReader.readLine()) != null){
                    sb.append(line);
                }


                bufferedReader.close();


                return sb.toString().trim();


            } catch (Exception e) {

                Log.d(TAG, "InsertData: Error ", e);
                errorString = e.toString();

                return null;
            }

        }
    }

    public byte[] InputStreamToByteArray(int data_len, DataInputStream in) { //https://evnt-hrzn.tistory.com/18

        int loop = (int)(data_len/1024);
        System.out.println("loop"+Integer.toString(loop));
        byte[] resbytes = new byte[data_len];
        int offset = 0;
        try {
            for (int i=0; i<loop; i++){
                in.readFully(resbytes, offset, 1024);
                offset += 1024;
            }
            in.readFully(resbytes, offset, data_len-(loop*1024));
            System.out.println("resbytes len:"+Integer.toString(resbytes.length));
            System.out.println("image get!!!!");
        } catch (IOException e){
            e.printStackTrace();
        }
        return resbytes;
    }

    void connect() {
        mHandler = new Handler();

        Log.w("connect", "연결 하는중");
        Thread checkUpdate = new Thread() {
            public void run() {
                // 서버 접속
                try {
                    socket = new Socket(ip, port);
                    Log.w("서버 접속됨", "서버 접속됨");
                } catch (IOException e1) {
                    Log.w("서버접속못함", "서버접속못함");
                    e1.printStackTrace();
                }

                Log.w("edit 넘어가야 할 값 : ", "안드로이드에서 서버로 연결요청");

                try {
                    dis = new DataInputStream(socket.getInputStream());
                    //bis = new BufferedInputStream(socket.getInputStream(), 1024);
                    dos = new DataOutputStream(socket.getOutputStream());

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.w("버퍼", "버퍼생성 잘못됨");
                }
                Log.w("버퍼", "버퍼생성 잘됨");

                // 리스트뷰 참조 및 Adapter달기
                try {
                    JSONObject jsonObject = new JSONObject(mJsonString);
                    JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);

                    pill_num = jsonArray.length();
                    img_list = new byte[pill_num][];
                    name_list = new String[pill_num];

                    try {
                        dos.write(pill_num);
                        dos.flush();
                        System.out.println("the number of image: " + Integer.toString(pill_num));

                    } catch (IOException e) {
                        Log.d(TAG, "send pill number error");
                    }

                    for (int i = 0; i < pill_num; i++) {
                        JSONObject item = jsonArray.getJSONObject(i);
                        int data_len = 0;

                        String img = item.getString(TAG_IMG);
                        name_list[i] = item.getString(TAG_NAME);

                        try {
                            dos.writeUTF(img); // server path + img -> server
                            dos.flush();
                            System.out.println("send image file name done!");

                            data_len = dis.readInt();
                            System.out.println(data_len);

                        } catch (IOException e) {
                            Log.d(TAG, "send image name error");
                        }

                        // server send img -> android
                        img_list[i] = InputStreamToByteArray(data_len, dis);
                        System.out.println("One img done");
                    }
                } catch (JSONException e) {
                    Log.d(TAG, "showResult : ", e);
                }
                try{
                    socket.close();
                }catch(IOException e){

                }
            }
        };
        checkUpdate.start();
        try {
            checkUpdate.join();
        }catch (InterruptedException e){

        }
        System.out.println("Thread terminated");
    }

    private void showResult(){
        for (int i=0; i<pill_num; i++){
            Bitmap img_bitmap = BitmapFactory.decodeByteArray(img_list[i], 0, img_list[i].length);
            Drawable img_drawable = new BitmapDrawable(img_bitmap);

            adapter.addItem(img_drawable, name_list[i]);
        }
        mlistView.setAdapter(adapter);
        if (pill_num == 0){
            Toast.makeText(getApplicationContext()," 식별마크를 인식하지 못했습니다", Toast.LENGTH_SHORT).show();
        }
    }

    public void search_result_back(View v) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);

        startActivity(intent);
        overridePendingTransition(R.transition.anim_slide_a, R.transition.anim_slide_b);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //Toast.makeText(getApplicationContext()," 뒤로가기가 눌렸습니다.", Toast.LENGTH_SHORT).show();
    }

    public void show_detail_before_login (){
        Intent intent = new Intent(getApplicationContext(), show_detail_before_login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.transition.anim_slide_in_left, R.transition.anim_slide_out_right);

        //Toast.makeText(getApplicationContext()," 자세히 보기가 눌렸습니다.", Toast.LENGTH_SHORT).show();

    }

}
