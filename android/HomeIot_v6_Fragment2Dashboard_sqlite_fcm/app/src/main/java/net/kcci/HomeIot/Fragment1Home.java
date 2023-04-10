package net.kcci.HomeIot;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ImageView;
import android.os.Handler;
import android.os.Looper;

import androidx.fragment.app.Fragment;


public class Fragment1Home extends Fragment {
    MainActivity mainActivity; // 메인 액티비티 참조
    ImageButton imageButtonVALVE; // 밸브 제어 버튼
    ImageButton imageButtonSHIELD; // 안전모드 제어 버튼
    ImageButton imageButtondb; // 데이터베이스 접속 버튼
    boolean imageButtonLampCheck; // 밸브 상태 체크
    boolean imageButtonShieldCheck; // 안전모드 상태 체크
    boolean timeCheck = true; // 타이머 상태 체크
    TextView textViewNumber; // 불 강도 출력 텍스트뷰
    TextView textViewCLOCK; // 남은 시간 출력 텍스트뷰
    ImageView imageViewFire; // 화재 이미지뷰

    private Handler countdownHandler; // 카운트다운 핸들러
    private Runnable countdownRunnable; // 카운트다운 러너블
    private int remainingTime; // 남은 시간 변수

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 이 프래그먼트의 레이아웃을 확장합니다.
        View view = inflater.inflate(R.layout.fragment1home, container, false);

        // 레이아웃에서 해당하는 뷰를 찾아 입력 UI를 초기화합니다.
        imageButtonVALVE = view.findViewById(R.id.imageButtonVALVE);
        imageButtonSHIELD = view.findViewById(R.id.imageButtonSHIELD);
        imageButtondb = view.findViewById(R.id.imageButtondb);

        // 레이아웃에서 해당하는 뷰를 찾아 출력 UI를 초기화합니다.
        mainActivity = (MainActivity) getActivity();
        textViewNumber = view.findViewById(R.id.textViewNumber);
        textViewCLOCK = view.findViewById(R.id.textViewCLOCK);
        imageViewFire = view.findViewById(R.id.imageViewFire);

        // 밸브 버튼의 클릭 리스너를 설정합니다.
        imageButtonVALVE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 데이터를 보내기 전에 소켓이 null이 아닌지 확인합니다.
                if (ClientThread.socket != null) {
                    // 버튼의 현재 상태에 따라 VALVEON 또는 VALVEOFF 명령을 보냅니다.
                    if (imageButtonLampCheck)
                        mainActivity.clientThread.sendData(ClientThread.arduinoId + "VALVEOFF\n");
                    else
                        mainActivity.clientThread.sendData(ClientThread.arduinoId + "VALVEON\n");
                }
            }
        });

        // 실드 버튼의 클릭 리스너를 설정합니다.
        imageButtonSHIELD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 데이터를 보내기 전에 소켓이 null이 아닌지 확인합니다.
                if (ClientThread.socket != null) {
                    // 버튼의 현재 상태에 따라 KIDON 또는 KIDOFF 명령을 보냅니다.
                    if (imageButtonShieldCheck)
                        mainActivity.clientThread.sendData(ClientThread.arduinoId + "SAFEOFF\n");
                    else
                        mainActivity.clientThread.sendData(ClientThread.arduinoId + "SAFEON\n");
                }
            }
        });

        // 데이터베이스 버튼의 클릭 리스너를 설정합니다.
        imageButtondb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 데이터베이스에 액세스하기 위한 URL을 구성합니다.
                String read_db = ("http://" + ClientThread.serverIp);
                // 지정된 URL로 브라우저를 시작하는 인텐트를 생성합니다.
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(read_db));
                // 브라우저 액티비티를 시작합니다.
                startActivity(intent);
            }
        });
        // 화재 이미지의 초기 가시성을 보이지 않게 설정합니다.
        imageViewFire.setVisibility(View.INVISIBLE);
        return view;// 이 프래그먼트의 뷰를 반환합니다.
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        countdownHandler = new Handler(Looper.getMainLooper());
    }

    void recvDataProcess(String strRecvData) {
        // 수신된 데이터 문자열을 구분자 '[', ']', '@' 및 '\n'을 사용하여 splitLists 배열로 분할합니다.
        String[] splitLists = strRecvData.toString().split("\\[|]|@|\\n");

        // splitLists 배열의 각 요소를 로깅합니다.
        for (int i = 0; i < splitLists.length; i++)
            Log.d("recvDataProcess", " i: " + i + ", value: " + splitLists[i]);

        // splitLists[2]로 받은 명령에 따라 버튼 UI를 업데이트합니다.
        buttonUpdate(splitLists[2]);

        // 명령이 "FIREON"인 경우 확인합니다.
        if (splitLists[2].equals("FIREON")) {
            // splitLists[3]에서 받은 불 세기 값을 파싱하고 textViewNumber를 업데이트합니다.
            int receivedNumber = Integer.parseInt(splitLists[3]);
            textViewNumber.setText("FIRE : " + receivedNumber);

            //불 세기 값이 0보다 큰 경우, 화재 이미지의 가시성 및 크기를 업데이트합니다.
            if (receivedNumber > 0) {
                // 불 이미지 뷰를 보이게 설정
                imageViewFire.setVisibility(View.VISIBLE);
                // receivedNumber를 사용하여 불 이미지의 스케일 계수를 계산
                float scaleFactor = calculateScaleFactor(receivedNumber);
                // 불 이미지 뷰의 X 및 Y 스케일 값을 계산된 스케일 계수로 설정
                imageViewFire.setScaleX(scaleFactor);
                imageViewFire.setScaleY(scaleFactor);
            }

        } else if (splitLists[2].equals("FIREOFF")) { // 명령이 "FIREOFF"인 경우 확인합니다.
            // textViewNumber를 "FIREOFF"로 업데이트하고 fire 이미지의 가시성을 보이지 않게 설정합니다.
            textViewNumber.setText("FIREOFF");
            imageViewFire.setVisibility(View.INVISIBLE);

        } else if (splitLists[2].equals("CLOCK")) { // 명령이 "CLOCK"인 경우 확인합니다.
            // splitLists[3]에서 받은 카운트다운 값을 사용하여 카운트다운 타이머를 시작합니다.
            remainingTime = Integer.parseInt(splitLists[3]);
            if (timeCheck) {
                startCountdown();
                timeCheck = false;
            }

        } else if (splitLists.length == 3) { // 수신된 데이터에 세 개의 요소가 있는 경우 확인합니다.
            // 명령과 인수를 저장하는 배열을 생성합니다.
            String[] insertArgs = new String[3];
            insertArgs[0] = splitLists[1]; // ex) "ARD"
            int index = splitLists[2].indexOf('O');
            String tempStr = splitLists[2].substring(0, index);
            insertArgs[1] = tempStr; //  ex) "LAMP"
            insertArgs[2] = splitLists[2].substring(index);

            // 명령과 인수를 로깅합니다.
            Log.d("database", " 0 : " + insertArgs[0] + " 1 : " + insertArgs[1] + " 2 : " + insertArgs[2]);

            // 데이터베이스를 명령과 인수로 업데이트합니다.
            mainActivity.database.updateRecordParam(mainActivity.SQLdb, insertArgs);
        }
    }


    private float calculateScaleFactor(int receivedNumber) {
        float minScale = 1f; // 최소 스케일 계수를 선언하고 초기화
        float maxScale = 5.0f; // 최대 스케일 계수를 선언하고 초기화
        int minReceivedNumber = 1; // 최소 receivedNumber를 선언하고 초기화
        int maxReceivedNumber = 8; // 최대 receivedNumber를 선언하고 초기화

        // 선형 보간 공식을 사용하여 스케일 계수를 계산
        float scaleFactor = minScale + (maxScale - minScale) * ((float) (receivedNumber - minReceivedNumber) / (maxReceivedNumber - minReceivedNumber));

        return scaleFactor; // 계산된 스케일 계수 반환
    }


    @Override
    public void onStart() {
        super.onStart();
        // 1. 데이터베이스에서 LAMP의 저장된 상태를 가져옵니다.
        String rstStr[] = mainActivity.database.executeRawQueryParam(mainActivity.SQLdb, "LAMP");

        // 2. 저장된 상태에 따라 버튼 UI를 업데이트합니다.
        buttonUpdate(rstStr[3] + rstStr[4]);

        // 3. 디버깅 목적으로 데이터베이스에서 가져온 데이터를 로깅합니다.
        Log.d("MainActivity", rstStr[0] + " ," + rstStr[1] + " ," + rstStr[2] + " ," + rstStr[3] + " ," + rstStr[4]);
    }


    public void buttonUpdate(String cmd) {
        Log.d("MainActivity ", cmd);
        if (cmd.equals("VALVEON")) {
            imageButtonVALVE.setImageResource(R.drawable.valve);
            imageButtonVALVE.setBackgroundColor(Color.GREEN);
            imageButtonLampCheck = true;
        } else if (cmd.equals("VALVEOFF")) {
            imageButtonVALVE.setImageResource(R.drawable.valve);
            imageButtonVALVE.setBackgroundColor(Color.LTGRAY);
            imageButtonLampCheck = false;
        } else if (cmd.equals("SAFEOFF")) {
            imageButtonSHIELD.setImageResource(R.drawable.shield);
            imageButtonShieldCheck = false;
        } else if (cmd.equals("SAFEON")) {
            imageButtonSHIELD.setImageResource(R.drawable.shield_check);
            imageButtonShieldCheck = true;
        }
    }

    private void startCountdown() {
        // 카운트다운 타이머를 위한 Runnable 생성
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                // 남은 시간이 있는지 확인
                if (remainingTime > 0) {
                    // 남은 시간에서 시간, 분, 초 계산
                    int clock_S, clock_M, clock_H;
                    clock_S = remainingTime % 60;
                    clock_M = (remainingTime / 60) % 60;
                    clock_H = (remainingTime / 3600);

                    // 남은 시간을 형식화하여 textViewCLOCK 업데이트
                    textViewCLOCK.setText(clock_H + "시간" + clock_M + "분" + clock_S + "초 남음");

                    // 남은 시간을 1초씩 감소
                    remainingTime--;

                    // 1초 지연으로 countdownHandler에 Runnable 게시
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    // 남은 시간이 없으면 textViewCLOCK 텍스트를 "CLOCK"으로 설정
                    textViewCLOCK.setText("CLOCK");
                    timeCheck = true;
                }
            }
        };
        // countdownHandler에 Runnable을 게시하여 카운트다운 타이머 시작
        countdownHandler.post(countdownRunnable);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
    }
}
