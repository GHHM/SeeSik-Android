package org.androidtown.myapplication;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.Context.MODE_PRIVATE;


public class DataBase extends AppCompatActivity {
    SQLiteDatabase foodDB;
    SQLiteDatabase userDB;
    final String userDBName = "seeSik";
    String TAG = "DATABASE";
    Context context;
    boolean first = true;

    public DataBase(Context c) {
        context = c;// 디비를 열고 닫을 때, context가 필요하므로
        userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);//openOrCreateDatabase는 context가 필요해 오류가 났었음!
        createTable();
    }

    //테이블 생성(user info, dailyList, intakeList)
    public void createTable() {
        //To create userInfo, intakeList in database
        if (first) {
            userDB.execSQL("create table if not exists userInfo (age integer, gender integer);");// Create userInfo table
            userDB.execSQL("create table if not exists dailyList(date text,times integer,foodName text ,sugar int,na int,chol int,fat int );");// Create intakeList table
            userDB.execSQL("create table if not exists intakeList(date text, sugar int, na int, chol int, fat int, highestIngredient int);");
            File folder = new File("data/data/org.androidtown.myapplication/databases/");
            if (!folder.exists()) folder.mkdir();
            File file = new File("data/data/org.androidtown.myapplication/databases/foodList.db");
            AssetManager assetManager = context.getAssets();
            try {

                file.createNewFile();

                InputStream is = assetManager.open("foodList.db");
                long filesize = is.available();

                byte[] tempdata = new byte[(int) filesize];

                is.read(tempdata);
                is.close();

                FileOutputStream fos = new FileOutputStream(file);
                fos.write(tempdata);
                fos.close();
                first = false;
            } catch (Exception e) {
                Toast.makeText(context, "오류가 났어....", Toast.LENGTH_LONG).show();
            }
            foodDB = context.openOrCreateDatabase("foodList.db", MODE_PRIVATE, null);
            first = false;
        }
        userDB.close();
        foodDB = context.openOrCreateDatabase("foodList.db", MODE_PRIVATE, null);
        foodDB.close();
    }

    /*
    * 사용자가 음식을 선택했을 경우
    * 그 음식을 디비에서 찾아와서
    * 그 정보를 dailyList에 추가하는 함수를 부른다.
    */
    public void searchInDatabaseAndInsert(String table, String str) {
        foodDB = context.openOrCreateDatabase("foodList.db", MODE_PRIVATE, null);
        String SQL = "select * from " +table + " where foodName = '" + str + "';";
        Cursor c1 = foodDB.rawQuery(SQL, null);

        int num = c1.getCount();
        int sugar = 0;
        int na = 0;
        int chol = 0;
        int fat = 0;
        // 어차피 name value로 같은 값을 찾기 때문에 따로 복사는 안함
        c1.moveToFirst();
        sugar = c1.getInt(3);
        na = c1.getInt(4);
        chol = c1.getInt(5);
        fat = c1.getInt(6);
        Log.d("search in database"," "+c1.getString(2)+" "+c1.getInt(3)+" "+c1.getInt(4)+" "+c1.getInt(5)+" "+c1.getInt(6));
        c1.close();
        foodDB.close();
        insertDailyList(str, sugar, na, chol, fat);
    }

    /*
    * 선택된 음식이 DailyList에 추가된다.
    * 만약, 같은 음식이 한번 이상 선택되면
    * 원래 먹은 횟수에서 1을 증가시킨다.
    *
    *   음식이 추가 되면
    * 1.가장 많이 초과한 성분이
    * 바뀔 가능성이 있으므로
    * calculateHighestIngredient()를 부른다.
    * 2. 섭취한 날짜와 섭취한 성분에 대한 총 합을
    * 저장하고 있는 intakeList()를 부른다.
    */
    public void insertDailyList(String foodName, int sugar, int na, int chol, int fat) {
        userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        Date date = new Date();
        String today = dateFormat.format(date);

        String SQL = "SELECT date FROM  dailyList WHERE date = '" + today + "';";// 넣기 전, 같은 날짜의 값이 있는지 확인한다. // 후에 검색 초반으로 옮길 수 있으나 현재는 이렇게
        Cursor c1 = userDB.rawQuery(SQL, null);
        int num = c1.getCount();
        c1.moveToFirst();
        if (num == 0) {//테이블에 들어있는 아이템이 없을 경우
            String SQL1 = "INSERT INTO dailyList VALUES('" + today + "'," + 1 + ",'" + foodName + "'," + sugar + "," + na + "," + chol + "," + fat + ");";
            userDB.execSQL(SQL1);
        } else {
            String SQL1 = "SELECT times from  dailyList where foodName = '" + foodName + "';"; // 음식이름이 같은게 있나 확인
            Cursor c2 = userDB.rawQuery(SQL1, null);
            num = c2.getCount();
            c2.moveToFirst();
            if (num == 1) {
                int originTimes = c2.getInt(0);
                int newTimes = originTimes +1;
                String SQL2 = "UPDATE dailyList SET times =" + newTimes + " WHERE foodName = '" + foodName + "';";
                userDB.execSQL(SQL2);
            } else {
                String SQL2 = "INSERT INTO dailyList VALUES('" + today + "'," + 1 + ",'" + foodName + "'," + sugar + "," + na + "," + chol + "," + fat + ");";
                userDB.execSQL(SQL2);
            }
        }

        calculateHighestIngredient();

        logDailyList("insert dailyList",foodName);
        insertIntakeList(sugar,na,chol,fat);
    }

    public void deleteDailyList(String foodName) {
        userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);
        String SQL = "SELECT sugar,na,chol,fat,times FROM dailyList WHERE foodName='"+foodName +"'";
        Cursor c1 = userDB.rawQuery(SQL, null);

        int sugar = 0;
        int na = 0;
        int chol = 0;
        int fat = 0;
        c1.moveToFirst();

        sugar = c1.getInt(0);
        na = c1.getInt(1);
        chol = c1.getInt(2);
        fat = c1.getInt(3);
        int times = c1.getInt(4);

        String SQL1 = "DELETE FROM dailyList WHERE foodName = '"+ foodName + "';";
        logDailyList("After delete dailyList",foodName);
        userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);
        userDB.execSQL(SQL1);
        c1.close();
        userDB.close();

        calculateHighestIngredient();
        deleteIntakeList(sugar, na, chol, fat,times);
    }

    /*
     * DailyList에 음식이 추가되거나 섭취횟수가 변경되었을 때
     * 섭취한 날짜에 대한 각각의 성분 합을 업데이트 한다.
    */
    private void insertIntakeList(int sugar, int na, int chol, int fat) {
        userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        Date date = new Date();
        String today = dateFormat.format(date);

        String SQL = "SELECT sugar, na, chol, fat from  IntakeList WHERE date = '" + today + "';";
        Cursor c1 = userDB.rawQuery(SQL, null);
        int num = c1.getCount();

        c1.moveToFirst();

        if (num == 0) {
            String SQL1 = "INSERT INTO IntakeList VALUES('" + today + "'," + sugar + "," + na + "," + chol + "," + fat + "," + 0+");";
            userDB.execSQL(SQL1);
        } else {
            c1.moveToFirst();
            int newSugar = c1.getInt(0);
            int newNa = c1.getInt(1);
            int newChol = c1.getInt(2);
            int newFat = c1.getInt(3);

            newSugar = newSugar + sugar;
            newNa = newNa + na;
            newChol = newChol + chol;
            newFat = newFat + fat; // 값 업데이트

            String SQL1 = "UPDATE IntakeList SET sugar =" + newSugar + ", na = " + newNa + ", chol = " + newChol + ", fat= " + newFat + " WHERE date = '" + today + "';";
            userDB.execSQL(SQL1);
        }

        c1.close();
        userDB.close();

        logIntakeList("insert IntakeList",today);

    }


    private void deleteIntakeList(int sugar, int na, int chol, int fat,int times) {
        userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        Date date = new Date();
        String today = dateFormat.format(date);
        String SQL = "SELECT sugar, na, chol, fat FROM IntakeList WHERE date = '" + today + "';";

        Cursor c1 = userDB.rawQuery(SQL, null);
        int num = c1.getCount();

        int originSugar = 0;
        int originNa = 0;
        int originChol = 0;
        int originFat = 0;

        c1.moveToFirst();
        originSugar = c1.getInt(0);
        originNa = c1.getInt(1);
        originChol = c1.getInt(2);
        originFat = c1.getInt(3);

        int newSugar = originSugar - (sugar*times);
        int newNa = originNa - (na*times);
        int newChol = originChol - (chol*times);
        int newFat = originFat - (fat*times); // 값 업데이트

        userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);// 위의 로그 때문에 넣은 것, 같이 지워야 함
        String SQL1 = "UPDATE IntakeList SET sugar =" + newSugar + ", na = " + newNa + ", chol = " + newChol + ", fat = " + newFat + " WHERE date = '" +today + "';";// 값 업데이트
        userDB.execSQL(SQL1);
        userDB.close();

        logIntakeList("Before Delete",today);
        logIntakeList("After Delete",today);


    }


    /*
   * 리스트에서 '+'버튼이 선택되면
   * 선택된 아이템에 해당하는 음식 이름을 가져와(input)
   * DailyList에 저장된 그 음식에 대한 섭취횟수(times)를 1 증가시킨다.
   *
   * calculateHighestIngredient()와 intakeList()를 부른다.
   */
    public void plusTimes(String foodName) {
        userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);
        int originTimes, newTimes = 0;
        int sugar, na, chol, fat;
        String SQL1 = "SELECT * FROM dailyList WHERE foodName='" + foodName + "'", SQL2;
        Cursor cur = userDB.rawQuery(SQL1, null);
        cur.moveToFirst();

        originTimes = cur.getInt(1);
        sugar = cur.getInt(3);
        na = cur.getInt(4);
        chol = cur.getInt(5);
        fat = cur.getInt(6);

        newTimes = originTimes + 1;
        Log.d("ASD", newTimes + "");
        SQL2 = "UPDATE dailyList SET times =" + newTimes + " WHERE foodName = '" + foodName + "';";

        userDB.execSQL(SQL2);
        userDB.close();

        calculateHighestIngredient();

        logDailyList("plusTimes",foodName);
        insertIntakeList(sugar,na,chol,fat);
    }

    /*
     * 리스트에서 '-'버튼이 선택되면
    * 선택된 아이템에 해당하는 음식 이름을 가져와(input)
    * DailyList에 저장된 그 음식에 대한 섭취횟수(times)를 1 감소시킨다.
     *
     * calculateHighestIngredient()와 deleteList()를 부른다.
     */
    public void minusTimes(String foodName) {
        userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);
        int originTimes, newTimes = 0;
        int sugar, na, chol, fat;
        String SQL1 = "SELECT times,sugar,na,chol,fat FROM dailyList WHERE foodName='" + foodName + "'", SQL2;
        Cursor cur = userDB.rawQuery(SQL1, null);
        cur.moveToFirst();

        originTimes = cur.getInt(0);
        sugar = cur.getInt(1);
        na = cur.getInt(2);
        chol = cur.getInt(3);
        fat = cur.getInt(4);

        newTimes = originTimes - 1;
        Log.d("ASD", newTimes + "");
        SQL2 = "UPDATE dailyList SET times =" + newTimes + " WHERE foodName = '" + foodName + "';";
        userDB.execSQL(SQL2);
        userDB.close();

        calculateHighestIngredient();

        logDailyList("After minusTime",foodName);
        deleteIntakeList(sugar,na,chol,fat,1);
    }

    public String[] getFoodName() {
        foodDB = context.openOrCreateDatabase("foodList.db", MODE_PRIVATE, null);
        String SQL = "select foodName from foodList;";
        Cursor c1 = foodDB.rawQuery(SQL, null);
        int num = c1.getCount();

        String foodName[] = new String[num];
        for (int i = 0; i < num; i++) {
            c1.moveToNext();
            foodName[i] = c1.getString(0);
        }
        c1.close();
        foodDB.close();
        return foodName;
    }
    public int getItemTimes(String foodName) {
        userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);
        String SQL = "SELECT times FROM dailyList WHERE foodName = '" + foodName + "';";
        Cursor c = userDB.rawQuery(SQL, null);
        c.moveToFirst();
        userDB.close();
        return c.getInt(0);
    }

    public int getSugar() {
        userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        Date date = new Date();
        String strDate = dateFormat.format(date);
        String SQL = "select sugar from  IntakeList where date = '" + strDate + "';"; // 같은 날의 value가 있는지 확인

        Cursor c1 = userDB.rawQuery(SQL, null);
        int num = c1.getCount();
        int sugar = 0;
        if (num == 0) {
            num = 0;
        } else {
            c1.moveToFirst();
            sugar = c1.getInt(0);
            c1.close();
            Log.d("getSugar", "" + sugar);
            userDB.close();
        }
        return sugar;
    }

    public int getNa() {
        userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        Date date = new Date();
        String strDate = dateFormat.format(date);
        String SQL = "select na from  IntakeList where date = '" + strDate + "';"; // 같은 날의 value가 있는지 확인

        Cursor c1 = userDB.rawQuery(SQL, null);
        int num = c1.getCount();
        int na;
        if (num == 0) {
            na = 0;
        } else {
            c1.moveToFirst();
            na = c1.getInt(0);
            c1.close();
            Log.d("getNa", "" + na);
            userDB.close();
        }
        return na;
    }

    public int getChol() {
        userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        Date date = new Date();
        String strDate = dateFormat.format(date);
        String SQL = "select chol from  IntakeList where date = '" + strDate + "';"; // 같은 날의 value가 있는지 확인

        Cursor c1 = userDB.rawQuery(SQL, null);
        int num = c1.getCount();
        int chol;
        if (num == 0) {
            chol = 0;
        } else {
            c1.moveToFirst();
            chol = c1.getInt(0);
            c1.close();
            Log.d("getChol", "" + chol);
            userDB.close();
        }
        return chol;
    }

    public int getFat() {
        userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        Date date = new Date();
        String strDate = dateFormat.format(date);
        String SQL = "select fat from  IntakeList where date = '" + strDate + "';"; // 같은 날의 value가 있는지 확인

        Cursor c1 = userDB.rawQuery(SQL, null);
        int num = c1.getCount();
        int fat = 0;
        if (num == 0) {
            fat = 0;
        } else {
            c1.moveToFirst();
            fat = c1.getInt(0);
            c1.close();
            Log.d("getFat", "" + fat);
            userDB.close();
        }
        return fat;
    }

    public void calculateHighestIngredient()
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        Date date = new Date();
        String strDate = dateFormat.format(date);

        double ingredient[] = new double[4]; //1 : Na 2: fat 3: chol 4: sugar
        ingredient[0] = getNa();
        ingredient[1] = getFat();
        ingredient[2] = getChol();
        ingredient[3] = getSugar();

        double recommendAmount[] = new double[4];
        recommendAmount[0] = 2000;
        recommendAmount[1] = 15;
        recommendAmount[2] = 600;
        recommendAmount[3] = 50;

        double highValue = ingredient[0]/ recommendAmount[0];
        int highIndex=0;
        double temp=0;
        for(int i=1;i<4;i++)
        {
            temp = ingredient[i]/recommendAmount[i];
            if(highValue< temp)
            {
                highValue = temp;
                highIndex = i;
            }
        }

        if(highValue>=1) {
            userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);
            userDB.execSQL("update intakeList set highestIngredient =" + (highIndex + 1) + " where date = '" + strDate + "';");
            userDB.close();
            logDailyList("하이밸류 바뀜?",strDate);
        }
        else{
            userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);
            userDB.execSQL("update intakeList set highestIngredient =" + 0 + " where date = '" + strDate + "';");
            userDB.close();
        }

       // logIntakeList("하이 밸류 설정 후: ",strDate);
    }

    public int getHighestIngredient(String strDate) {
        //return 1:Na  2:fat  3:chol  4:sugar
        userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);

        String SQL = "select highestIngredient from IntakeList where date = '" + strDate + "';"; // 같은 날의 value가 있는지 확인
        Cursor c1 = userDB.rawQuery(SQL, null);

        if(c1.getCount()==0)
            return 0;
        else {
            c1.moveToFirst();
            int ingredient = c1.getInt(0);

          //  logDailyList("달력에 넘어가는 하이값: ",strDate);
            userDB.close();
            return ingredient;
        }


    }

    public String[] getAllFoodName()
    {
        userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);
        String sql = "SELECT foodName FROM dailyList;";
        Cursor cursor = userDB.rawQuery(sql,null);
        int num = cursor.getCount();
        String []foodName = new String[num];
        cursor.moveToFirst();
        for(int i=0;i<num;i++)
        {
            foodName[i] = cursor.getString(0);
            cursor.moveToNext();
        }

        return foodName;
    }

    public int[] getAlltimes()
    {
        userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);
        String sql = "SELECT times FROM dailyList;";
        Cursor cursor = userDB.rawQuery(sql,null);
        int num = cursor.getCount();
        int []times = new int[num];
        cursor.moveToFirst();
        for(int i=0;i<num;i++)//?오류?
        {
            times[i]= cursor.getInt(0);
            cursor.moveToNext();
        }

        return times;
    }

    public boolean checkDateChanged()
    {
        userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        Date date = new Date();
        String today = dateFormat.format(date);

        String SQL = "SELECT date FROM  dailyList WHERE date = '" + today + "';";// 넣기 전, 같은 날짜의 값이 있는지 확인한다. // 후에 검색 초반으로 옮길 수 있으나 현재는 이렇게
        Cursor c1 = userDB.rawQuery(SQL, null);
        int num = c1.getCount();
        if(num==0)
        {
            String SQL2 = "SELECT times FROM  dailyList;";
            Cursor c2 = userDB.rawQuery(SQL2,null);
            num = c2.getCount();
            userDB.close();
            if(num==0)
                return false;
            else
                return true;
        }
        else {
            userDB.close();
            return false;
        }
    }

    //TODO: 이 부분 왜 한건지 알려줘!
    public void resetDailyList()//Search Food에 들어갈 때마다 비교해 봐야 할 것 같당
    {
        userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);
        String SQL;
        SQL = "DELETE FROM dailyList;";
        userDB.execSQL(SQL);
        userDB.close();
    }

    public void logDailyList(String state,String foodName)
    {
        userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);
        String sql = "SELECT * FROM dailyList WHERE foodName = '" + foodName + "';";
        Cursor c3 = userDB.rawQuery(sql, null);
        int d = c3.getCount();
        c3.moveToFirst();
       // Log.d( state, " 오늘날짜: "+c3.getString(0) + ", 횟수: " + c3.getInt(1) + " ,음식이름: " + c3.getString(2) + " ,당: " + c3.getInt(3) + " ,나트륨: " + c3.getInt(4) + " ,콜레스테롤: " + c3.getInt(5) + " ,포화지방: " + c3.getInt(6));
        c3.close();
        userDB.close();
    }

    public void logIntakeList(String state,String date)
    {
        userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);
        String sql = "SELECT * FROM intakeList WHERE date = '" + date + "';";
        Cursor c3 = userDB.rawQuery(sql, null);
        int d = c3.getCount();
        c3.moveToFirst();
        Log.d( state, "날짜: "+c3.getString(0) + " ,당: " + c3.getInt(1) + " ,나트륨: " + c3.getInt(2) + " ,콜레스테롤: " + c3.getInt(3) + " ,포화지방: " + c3.getInt(4) + " , 가장 많이 섭취한 성분(나팻콜슈):"+c3.getInt(5));
        c3.close();
        userDB.close();
    }

    public void logAll()
    {
        userDB = context.openOrCreateDatabase(userDBName, MODE_PRIVATE, null);
        String sql = "SELECT * FROM dailyList;";
        String sql2 = "SELECT * FROM intakeList;";

        Cursor c1 = userDB.rawQuery(sql,null);
        Cursor c2 = userDB.rawQuery(sql2,null);

        int num1 = c1.getCount();
        int num2 = c2.getCount();

        c1.moveToFirst();
        c2.moveToFirst();

        for(int i=0;i<num1;i++)
        {
            Log.d( "Daily List All", " 오늘날짜: "+c1.getString(0) + ", 횟수: " + c1.getInt(1) + " ,음식이름: " + c1.getString(2) + " ,당: " + c1.getInt(3) + " ,나트륨: " + c1.getInt(4) + " ,콜레스테롤: " + c1.getInt(5) + " ,포화지방: " + c1.getInt(6));
            c1.moveToNext();
        }
        c1.close();
        for(int i=0;i<num2;i++)
        {
            Log.d("Intake List All", "날짜: "+c2.getString(0) + " ,당: " + c2.getInt(1) + " ,나트륨: " + c2.getInt(2) + " ,콜레스테롤: " + c2.getInt(3) + " ,포화지방: " + c2.getInt(4) + " , 가장 많이 섭취한 성분(나팻콜슈):"+c2.getInt(5));
            c2.moveToNext();
        }
        c2.close();
        userDB.close();
    }
}

