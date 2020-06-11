package com.NSL.ECGCertification;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DBHelper extends SQLiteOpenHelper {

    // DBHelper 생성자로 관리할 DB 이름과 버전 정보를 받음
    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    private static final String CREATE_TABLE_1 = "CREATE TABLE ECGDATA (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, time TEXT, ECGdatap TEXT, ECGdataq TEXT, ECGdatar TEXT, ECGdatas TEXT, ECGdatat TEXT, " +
            "HRBPM TEXT, BPsystolic INTEGER, BPdiastolic INTEGER);";
    private static final String CREATE_TABLE_2 = "CREATE TABLE ECGrawdata (num INTEGER PRIMARY KEY AUTOINCREMENT, RawdataECG INTEGER);";

    // DB를 새로 생성할 때 호출되는 함수
    @Override
    public void onCreate(SQLiteDatabase db) {
        // 새로운 테이블 생성
        db.execSQL(CREATE_TABLE_1);
        db.execSQL(CREATE_TABLE_2);
    }

    // DB 업그레이드를 위해 버전이 변경될 때 호출되는 함수
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void insertECGrawdata(ArrayList<Integer> RawdataECG) {
        // 읽고 쓰기가 가능하게 DB 열기
        SQLiteDatabase db = getWritableDatabase();
        // DB에 입력한 값으로 행 추가
        db.execSQL("INSERT INTO ECGrawdata VALUES(null, '" + RawdataECG + "');");
        db.close();
    }

    public void insertECGDATA(String date, String time, String ECGdatap, String ECGdataq, String ECGdatar, String ECGdatas, String ECGdatat, int HRBPM, int BPsystolic, int BPdiastolic) {
        // 읽고 쓰기가 가능하게 DB 열기
        SQLiteDatabase db = getWritableDatabase();
        // DB에 입력한 값으로 행 추가
        db.execSQL("INSERT INTO ECGDATA VALUES(null, '" + date + "', '" + time + "', '" + ECGdatap + "', '" + ECGdataq + "', '" + ECGdatar + "', '" + ECGdatas + "', '" + ECGdatat + "', '" + HRBPM + "', '" + BPsystolic +
                "', '" + BPdiastolic + "');");
        db.close();
    }

    public void delete() {
        SQLiteDatabase db = getWritableDatabase();
        // 삭제
        db.execSQL("DELETE FROM ECGDATA");
        db.execSQL("DELETE FROM ECGrawdata");
        db.close();
    }

    public String getResult() {
        // 읽기가 가능하게 DB 열기
        SQLiteDatabase db = getReadableDatabase();
        String result = "";

        Cursor cursor = db.rawQuery("SELECT * FROM ECGDATA", null);
        while (cursor.moveToNext()) {
            result += cursor.getString(0) + " " + cursor.getString(1) + " " + cursor.getString(2) + "  P: " + cursor.getString(3) + "  Q: " + cursor.getString(4) +
                    "  R: " + cursor.getString(5) + "  S: " + cursor.getString(6) + "  T: " + cursor.getString(7) + "  HR: " + cursor.getString(8) + "  Systolic: " +
                    cursor.getString(9) + "  Diastolic: " + cursor.getString(10) + "\r\n";
        }
            return result;
        }
}
