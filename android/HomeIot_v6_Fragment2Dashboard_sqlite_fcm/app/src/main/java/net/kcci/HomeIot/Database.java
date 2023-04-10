package net.kcci.HomeIot;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Database extends SQLiteOpenHelper {
    private static String DATABASE_NAME = "HomeIot";
    private static String TABLE_NAME = "SensorTable";
    private static int DATABASE_VERSION = 1;
    public SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    public SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("create table if not exists " + TABLE_NAME + "("
                + " _id integer PRIMARY KEY autoincrement, " + " date text, "+ " time text, " + " node text, " + "sensor text, " + " value text);");
        String[] insertArgs = new String[]{"ALLMSG","LAMP","OFF"};
        insertRecordParam(sqLiteDatabase,insertArgs);
        insertArgs = new String[]{"ALLMSG","GAS","OFF"};
        insertRecordParam(sqLiteDatabase,insertArgs);
    }
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
    public int insertRecordParam(SQLiteDatabase db, String insertArgs[]) {
        Date date = new Date();
        String strDate = dateFormat.format(date);
        String strTime = timeFormat.format(date);
        String node = insertArgs[0];
        String sensor = insertArgs[1];
        String value = insertArgs[2];

        int count = 1;
        ContentValues recordValues = new ContentValues();

        recordValues.put("date", strDate);
        recordValues.put("time", strTime);
        recordValues.put("node", node);
        recordValues.put("sensor", sensor);
        recordValues.put("value", value);
        int rowPosition = (int) db.insert(TABLE_NAME, null, recordValues);
//        Log.d("Database","strDate:"+strDate+" strTime:"+strTime+",node:"+node+",sensor:"+sensor+",value:"+value+ ", rowPosition:"+rowPosition);
        return count;
    }

    public int updateRecordParam(SQLiteDatabase db, String updateArgs[]) {
        String node="";
        String sensor="";
        String value="";
        int rowPosition = 0;
        if(updateArgs[0].equals("ALLMSG")) {
            node = updateArgs[0];
            sensor = updateArgs[1];
            value = updateArgs[2];

            String[] whereArgs = {sensor};
            Date date = new Date();
            String strDate = dateFormat.format(date);
            String strTime = timeFormat.format(date);
            ContentValues recordValues = new ContentValues();
            recordValues.put("date", strDate);
            recordValues.put("time", strTime);
            recordValues.put("value", value);
            rowPosition = db.update(TABLE_NAME, recordValues, "sensor = ?", whereArgs);
            Log.d("Database update", " rowPosition :" + rowPosition + ",node:" + node +",sensor:" + sensor + ",date:" + strDate + ",time:" + strTime + ",value:" + value);
        }
        return rowPosition;
    }

    public String[]  executeRawQueryParam(SQLiteDatabase db,String arg) {
        String[] whereArgs = {arg};
        String[] rstStr = new String[5];
//        String sendData="";
//        String tempData="";
//        String date,time,node,sensor,value;
        String SQL = "select * " + " from " + TABLE_NAME + " where sensor == ? ";
        Cursor cursor = db.rawQuery(SQL, whereArgs);
//        int recordCount = cursor.getCount();
//       for (int i = 0; i < recordCount; i++) {
            cursor.moveToNext();
            rstStr[0] = cursor.getString(1);     //date
            rstStr[1] = cursor.getString(2);     //time
            rstStr[2] = cursor.getString(3);     //node
            rstStr[3] = cursor.getString(4);    //sensor
            rstStr[4] = cursor.getString(5);    //value
//            tempData = String.format("%s %s %s %s %s", date, time, node, sensor, value);
//            sendData = sendData.format("%s%s",sendData,tempData);
//        }

        cursor.close();
//        Log.d("Database sendData",rstStr[4] );
        return rstStr;
    }
    public int executeRawQueryCount(SQLiteDatabase db) {
        int count;
        Cursor cursor = db.rawQuery("select count(*) as Total from " + TABLE_NAME,null);
        count = cursor.getCount();
        cursor.moveToNext();
        cursor.close();
        return count;
    }
    public int deleteRecordParam(SQLiteDatabase db, String[] args) {
        int count;
        count = db.delete(TABLE_NAME,  "date = ?", args);
        Log.d("Database",args[0]);
        return count;
    }
    public int deleteTable(SQLiteDatabase db) {
        return db.delete(TABLE_NAME,  null, null);
    }
}
