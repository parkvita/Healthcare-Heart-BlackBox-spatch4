package com.NSL.ECGCertification;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

public class PickUp {
    /**
     * 13000개에 5개의 심장박동 사이클이 있다는 가정
     * 디폴트
     * */
    public int SS ;
    public int y_data [] = new int[5];
    public static int DEFAULT_RANGE = 1300;

    /**
     * 1300개는 5개의 사이클이 생성된다
     * */
    public static int COUNT_OF_DEFAULT_RANGE = 5;

    /**
     * 1300개를 8초로 잡는다
     * */
    public static final int SEC_OF_DEFAULT_RANGE = 8;

    public static final int GREEN = 0;
    public static final int ORANGE = 1;
    public static final int RED = 2;

    public static final int MODE_CALIBRATION = 0;
    public static final int MODE_REALTIME = 1;
    private int MODE_CURRENT = 0;

    private int RSTPQ [];
    private int ECG_data[];
    private OnFindListener onFindListener;
    private Context mContext;
    private SharedPreferences prefs;

    public static ArrayList<int []> PickR = new ArrayList<>();

    public interface OnFindListener{
         void onFind(Integer [] rstpq, int times);

    }

    public PickUp(Context context)
    {
        mContext = context;
        prefs = context.getSharedPreferences(PrefActivity.PREF_NAME, context.MODE_PRIVATE);
        RSTPQ = new int[5];
        ECG_data = new int[DEFAULT_RANGE];
    }

    public void setList(int [] arr)
    {
        DEFAULT_RANGE = arr.length;
        ECG_data = new int[DEFAULT_RANGE];
        ECG_data = arr;

        Log.i("DEFAULT_RANGE", DEFAULT_RANGE + "");
        RSTPQ = new int[5];
    }

    /**
     * Calibration, Realtime
     * */
    public void setMode(int mode)
    {
        MODE_CURRENT = mode;
    }

    /**
     * 리스트에서 몇개의 싸이클?
     * */
    private int getTimeToCount()
    {
        return
                Integer.parseInt(prefs.getString(PrefActivity.PREF_KEY_CALIBRATION_TIME, "8"))
                        / SEC_OF_DEFAULT_RANGE * COUNT_OF_DEFAULT_RANGE;

    }

    public  void setOnFindListener(OnFindListener listener)
    {
        onFindListener = listener;
    }

    public void find()
    {
        int tem_R = 0;// 현재 루프에서는 다음 R위치 , 다음 루프에서는 현재의 R이 됨
        boolean R_f = true; //반복문에서 처음 한번만 작동하도록 변수 정의


        for (int i = 0; i < getTimeToCount(); i++) { // RSTPQ 파형 5개 찾도록 5번 반복하였음

            try{
                if(RSTPQ[0] != 0 && RSTPQ[0] == Next_R(RSTPQ[0], RSTPQ[1])) continue;
                RSTPQ[0] = Next_R(RSTPQ[0], RSTPQ[1]); // (첫번째 loop 제외)   현재(기준) R 탐색

                if (R_f) {
                    RSTPQ[0] = find_R(); //루프 처음 한번만 find_R 메소드가 호출되고 다음 반복부터는 Next_R 메소드만을 이용할 수 있도록 함
                    R_f = false;
                }
                RSTPQ[1] = find_S(RSTPQ[0]); //  S 탐색 메소드
                tem_R = Next_R(RSTPQ[0], RSTPQ[1]); //현재(기준) R -> 다음 R 탐색을 위함임
                RSTPQ[2] = find_T(RSTPQ[1]); //  T 탐색 메소드
                RSTPQ[4] = find_Q(tem_R);  // Q 탐색 메소드
                RSTPQ[3] = find_P(RSTPQ[4]); // P 탐색 메소드
                SS = find_SS(RSTPQ[1],RSTPQ[3]);

                for(int j=0; j<5; j++) {
                  y_data[j] = ECG_data[RSTPQ[j]] ;
                //    Log.e("y데이터   ",i +" , "+ String.valueOf(y_data[j]));
                }
                 SS= ECG_data[SS];


                //R값이 1000 보다 떨어지는 처음에 나오는 값들은 켈리브레이션에서 제외한다.
                if (MODE_CURRENT == MODE_CALIBRATION){
                    if (RSTPQ[0] < 1000) continue; // why
                }

                //마지막
                if (i == getTimeToCount() - 1){
                    onFindListener.onFind(null, i);
                    break;
                }
                onFindListener.onFind(toConvertInteger(RSTPQ), i); // onfind( 한 파형RSTPQ값   , 0~4 숫자 던져줌)

            }catch (Exception e)
            {
                e.printStackTrace();
                onFindListener.onFind(null, i);
                break;

            }
        }

    }

    public static Integer[] toConvertInteger(int[] ids) { // 임시로  RSTPQ 5개 배열, 인트변환용 newarray[5] 생성됨

        Integer[] newArray = new Integer[ids.length];
        for (int i = 0; i < ids.length; i++) {
            newArray[i] = Integer.valueOf(ids[i]);
        }
        return newArray;
    }

    private int find_R()
    {
        int ecg_R = 0;

        for (int i = 1; i<200; i++) { // 최대한 앞부분에서 R탐색 시작 할 수 있도록, 400 전에서 최초 R값 추출
            if (ECG_data[i] >= ECG_data[ecg_R]) // data 400개 비교하여 MAX 값  ecg_R 에 저장
                ecg_R = i;
        }

        return 	ecg_R;
    }

    private int find_S(int bp) { // bp == R 위치 값
        int dep_1, dep_2 = 0; // 15범위의 데이터에서 양 끝 점의 차이 값을 기울기로 이용
        int ecg_S = 0;
        int temp_s = bp; // 탐색된 R 위치에서 시작
        boolean b = true; // 루프에서 처음 한번만 동작하도록 [변수 b 정의]

        while (true) {
            for (int i = 0; i < 70; i++) { // 범위 15로 특정
                if (ECG_data[bp + i] <= ECG_data[temp_s]) //15 범위 안에서  Min값 탐색, 임시 S 로 지정
                    temp_s = bp + i;
            }

            if (b) {// 루프 처음 한번만 동작
                ecg_S = temp_s; //처음 S 값 = R 위치로 초기화
                b = false;
            }

            if (ECG_data[ecg_S] >= ECG_data[temp_s]) ecg_S = temp_s;  // 이전 15범위 의 Min 과 다음 15범위에서의 Min 비교후 더작은 값 선택

            dep_1 = ECG_data[bp + 5] - ECG_data[bp]; // dep_1 은 현재 15범위 위치에서 기울기

           if (dep_1 >= 0 )  // s 값은 R 다음 data 중 ↘↗ 의 형태여야 하므로 dep_1은 + 이며 dep_2는 - 인 시점의 Min 값을 반환, S 로 지정
                return ecg_S;

             bp += 5; // 다음 범위 15
          //  dep_2 = dep_1; // dep_2 는 이전 10범위 위치에서의 기울기
        }
    }

    private int find_T(int bp) {
        int depth=0,depth2=0;
        int ecg_T = 0;
        int temp_t = bp; // S
        boolean b = true; // 루프 처음 한번만 동작

        while (true) {
            for (int i = 0; i < 50; i++) {
                if (ECG_data[bp + i] >= ECG_data[temp_t]) //15범위 안에서 MAX값 탐색, 임시 T로 지정
                    temp_t = bp + i;
            }

            if (b) {
                ecg_T = temp_t;
                b = false;
            }

            if (ECG_data[ecg_T] <= ECG_data[temp_t]) ecg_T = temp_t; // 이전 15범위 T 값과 이후 15범위의 T값 비교 , 큰 값 T로 지정

            depth = ECG_data[bp + 10] - ECG_data[bp];

            if (depth < 0 && depth2 >=0) // S 위치 부터 다음 data는 계속 증가할 것이며 , 기울기 - 시점 이 T 가 될것임
                return ecg_T;

            bp += 10;
            depth2=depth;

        }
    }
    private int find_SS(int bp,int p) {
        int temp_SS = bp; // S
        while(p==bp) {
            if (ECG_data[temp_SS] > ECG_data[bp]) temp_SS =bp;
                bp++;
        }
        return temp_SS;
    }
    private int find_P(int bp) {
        int dep_1,dep_2=0; // dep_1 현재 기울기 , dep_2 이전 기울기
        int ecg_P = 0;
        bp -= 10; // Q 위치를 기준으로 하므로 역방향 진행
        int temp_p = bp; //Q위치 에서 시작
        boolean b = true; // 루프 처음 한번만 동작

        while (true) {
            for (int i = 0; i < 10; i++) {
                if (ECG_data[bp - i] >= ECG_data[temp_p]) //15범위중 Max값 탐색 임시 P로 지정
                    temp_p = bp - i;
            }

            if (b) { // 루프 처음 한번만 동작
                ecg_P = temp_p;
                b = false;
            }

            if (ECG_data[ecg_P] >= ECG_data[temp_p]) ecg_P = temp_p; // 이전 15범위 P와 현재 15범위 P 비교하여 , 큰값 P로 지정

            dep_1 = ECG_data[bp] - ECG_data[bp-9]; // 현재 15범위 기울기

            if (dep_1 > 0 && dep_2 < 0 ) // P 값은 ↗↘ 의 형태여야 하므로 dep_2는 - dep_1은 + 인 시점에서의 P 값 결정
                return ecg_P;

            bp -= 10;
            dep_2 = dep_1; // 이전 15범위 기울기
        }
    }

    private int find_Q(int bp) {
        int depth;
        int ecg_Q = 0;
        int temp_q = bp; // 다음 R 값 위치를 기준으로 시작
        boolean b = true; // 루프에서 처음 한번만 동작

        while (true) {
            for (int i = 0; i < 15; i++) {// 15범위에서 Min값 임시 Q로 지정 , 다음 R 위치 기준으로 역방향(←) 진행
                if (ECG_data[bp - i] < ECG_data[temp_q])
                    temp_q = bp - i;
            }

            if (b) {
                ecg_Q = temp_q;
                b = false;
            }

            if (ECG_data[ecg_Q] < ECG_data[temp_q]) ecg_Q = temp_q; // 이전 임시 Q 와 현재 임시 Q 비교,  Min값 Q로 지정

            depth = ECG_data[bp] - ECG_data[bp-14]; // 15범위에서 기울기 구함


            if (depth > 100) // R값은 예외적으로 매우 큰 값을 가지기에 좀더 명확히 특정될 수 있도록 기울기 차가 (+)100이상 나는 시점에서 Q(최소)값 리턴
                return ecg_Q;

            bp -= 15; // 역방향(←) 진행
        }
    }

    private int Next_R(int R, int S) { // 현재 R값, S값을 이용하여 다음 사이클의 R값 탐색

        int ecg_R = R;

        for (int i = S; i < DEFAULT_RANGE; i++) { //S 위치부터 탐색 시작
            if (ECG_data[R] - 200 <= ECG_data[i] && ECG_data[R] + 200 >= ECG_data[i]) { // (R 데이터 값은 거의 일정하므로)현재 R 기준 +200 ,-200 범위의 위치 탐색 / ecg_R에 저장
                ecg_R = i;
                break;
            }
        }

        for (int i = ecg_R + 1; i < ecg_R+15; i++) { // ecg_R에 저장된 위치 기준으로  15범위에서 가장 큰 Max값 R로 지정
            if (ECG_data[ecg_R] <= ECG_data[i]) {
                ecg_R = i;
            }
        }
        return 	ecg_R;
    }

    public static void printR(int [] r, String tag)
    {
        String result = "";
        for (int i = 0; i < r.length; i++)
        {
            result += result + r[i] + ",";
        }

        Log.e(tag, result);
    }

    public static int[] toIntArray(ArrayList<Integer> integers) //( )arraylist -> int 배열로
    {
        int[] ret = new int[integers.size()];
        for (int i=0; i < ret.length; i++)
        {
            ret[i] = integers.get(i).intValue();
        }
        return ret;
    }

    /**
     * 검출된 rstpq 값을 검증할 때 쓰임
     * @return true : 0 포함됨 , false : 0 포함 되지 않음
     * */
    public static boolean isContainZero(int [] r)
    {

        for (int v : r)
        {
            if (v == 0) return true;
        }

        return false;
    }

    /**
     * rstqp 의 min, max 를 preference 에 저장
     * */
    public  void saveRstpqMinMax(CalibList<int []> list)
    {
        Log.e("saveRstpqMinMax", list.size() + "여기여기");
        ArrayList<Integer> rList = new ArrayList<Integer>();
        ArrayList<Integer> sList = new ArrayList<Integer>();
        ArrayList<Integer> tList = new ArrayList<Integer>();
        ArrayList<Integer> pList = new ArrayList<Integer>();
        ArrayList<Integer> qList = new ArrayList<Integer>();

        for (int b = 0; b < list.size(); b++)
        {
            Integer [] r = (Integer[]) list.get(b);
            int i = 0;
            for (int v : r)
            {
                if (i == 0) rList.add(v);
                if (i == 1) sList.add(v);
                if (i == 2) tList.add(v);
                if (i == 3) pList.add(v);
                if (i == 4) qList.add(v);
                i++;
            }
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PrefActivity.PREF_KEY_R_MAX, (Integer)Collections.max(rList));
        editor.putInt(PrefActivity.PREF_KEY_R_MIN, (Integer)Collections.min(rList));
        editor.putInt(PrefActivity.PREF_KEY_S_MAX, (Integer)Collections.max(sList));
        editor.putInt(PrefActivity.PREF_KEY_S_MIN, (Integer)Collections.min(sList));
        editor.putInt(PrefActivity.PREF_KEY_T_MAX, (Integer)Collections.max(tList));
        editor.putInt(PrefActivity.PREF_KEY_T_MIN, (Integer)Collections.min(tList));
        editor.putInt(PrefActivity.PREF_KEY_P_MAX, (Integer)Collections.max(pList));
        editor.putInt(PrefActivity.PREF_KEY_P_MIN, (Integer)Collections.min(pList));
        editor.putInt(PrefActivity.PREF_KEY_Q_MAX, (Integer)Collections.max(qList));
        editor.putInt(PrefActivity.PREF_KEY_Q_MIN, (Integer)Collections.min(qList));
        editor.commit();

        //test
        Log.i("PREF_KEY_R_MAX", prefs.getInt(PrefActivity.PREF_KEY_R_MAX, 0) + "");
        Log.i("PREF_KEY_R_MIN", prefs.getInt(PrefActivity.PREF_KEY_R_MIN, 0) + "");
        Log.i("PREF_KEY_S_MAX", prefs.getInt(PrefActivity.PREF_KEY_S_MAX, 0) + "");
        Log.i("PREF_KEY_S_MIN", prefs.getInt(PrefActivity.PREF_KEY_S_MIN, 0) + "");
        Log.i("PREF_KEY_T_MAX", prefs.getInt(PrefActivity.PREF_KEY_T_MAX, 0) + "");
        Log.i("PREF_KEY_T_MIN", prefs.getInt(PrefActivity.PREF_KEY_T_MIN, 0) + "");
        Log.i("PREF_KEY_P_MAX", prefs.getInt(PrefActivity.PREF_KEY_P_MAX, 0) + "");
        Log.i("PREF_KEY_P_MIN", prefs.getInt(PrefActivity.PREF_KEY_P_MIN, 0) + "");
        Log.i("PREF_KEY_Q_MAX", prefs.getInt(PrefActivity.PREF_KEY_Q_MAX, 0) + "");
        Log.i("PREF_KEY_Q_MIN", prefs.getInt(PrefActivity.PREF_KEY_Q_MIN, 0) + "");
    }

    /**
     * @return 그린, 주황, 적색
     * */
    @Deprecated
    public int result(CalibList<int []> list)
    {
        //rstpq
        int badCount = 0;

        for (int b = 0; b < list.size(); b++)
        {
            badCount = 0;

            Integer [] r = (Integer[]) list.get(b);
            int i = 0;
            for (int v : r)
            {
                switch (i) {
                    case 0 :
                        if (v > prefs.getInt(PrefActivity.PREF_KEY_R_MAX, 0) || v < prefs.getInt(PrefActivity.PREF_KEY_R_MIN, 0))
                        {
                            badCount++;
                        }
                        break;
                    case 1 :
                        if (v > prefs.getInt(PrefActivity.PREF_KEY_S_MAX, 0) || v < prefs.getInt(PrefActivity.PREF_KEY_S_MIN, 0))
                        {
                            badCount++;
                        }
                        break;
                    case 2:
                        break;
                    case 3 :
                        break;
                    case 4 :
                        if (v > prefs.getInt(PrefActivity.PREF_KEY_Q_MAX, 0) || v < prefs.getInt(PrefActivity.PREF_KEY_Q_MIN, 0))
                        {
                            badCount++;
                        }
                        break;
                    default:
                        break;
                 }

                i++;
            }
            if (badCount == 3) return RED;
            if (badCount == 2 || badCount == 1 ) return ORANGE;
            if (badCount == 0) return GREEN;
        }
        return 0;
    }

}
