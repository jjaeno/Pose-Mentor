package com.example.posementor;

// 필요한 클래스들을 가져옴
import android.os.Bundle;
import android.widget.*;           // TextView, Button, Spinner 등 UI 요소들
import android.view.*;           // View, AdapterView 등 사용자 이벤트 처리용
import android.content.*;        // 앱 내부 기능 호출용 (예: 인텐트)
import androidx.appcompat.app.AppCompatActivity;  // 최신 Activity 기본 클래스

public class MainActivity extends AppCompatActivity {

    // 화면에 있는 요소들을 자바에서 다룰 수 있도록 변수 선언
    Spinner spinnerSport;
    Button btnSelectVideo, btnRecordVideo, btnAnalyze;
    TextView textFeedback;

    // 선택된 운동 종목을 저장하는 변수 (기본값: 골프)
    String selectedSport = "골프";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // 앱이 실행될 때 기본 동작

        // 화면에 표시할 레이아웃 파일을 지정 (activity_main.xml)
        setContentView(R.layout.activity_main);

        // XML에 있는 UI 요소들을 Java 변수와 연결 (id 기준)
        spinnerSport = findViewById(R.id.spinner_sport);
        btnSelectVideo = findViewById(R.id.btn_select_video);
        btnRecordVideo = findViewById(R.id.btn_record_video);
        btnAnalyze = findViewById(R.id.btn_analyze);
        textFeedback = findViewById(R.id.text_feedback);

        // 운동 선택 드롭다운에 표시할 목록 설정
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,            // 기본 스타일
                new String[]{"골프", "헬스", "볼링"}              // 선택지 목록
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSport.setAdapter(adapter); // Spinner에 어댑터 연결

        // Spinner에서 항목을 선택했을 때 호출되는 리스너
        spinnerSport.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 사용자가 선택한 운동 종목을 저장
                selectedSport = (String) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 아무것도 선택하지 않았을 경우 (비워둬도 됨)
            }
        });

        // [영상 선택] 버튼 클릭 시 동작
        btnSelectVideo.setOnClickListener(v -> {
            // TODO: 나중에 갤러리에서 영상 선택 기능 연결
            Toast.makeText(this, "영상 선택 버튼 눌림", Toast.LENGTH_SHORT).show();
        });
        // [촬영] 버튼 클릭 시 동작
        btnRecordVideo.setOnClickListener(v -> {
            // TODO: 나중에 카메라 촬영 기능 연결
            Toast.makeText(this, "촬영 버튼 눌림", Toast.LENGTH_SHORT).show();
        });

        // [분석 시작] 버튼 클릭 시 동작
        btnAnalyze.setOnClickListener(v -> {
            // TODO: 나중에 서버에 영상 보내고 GPT 피드백 받는 부분 구현
            textFeedback.setText("👉 상체가 기울어져 있습니다. 무릎을 더 굽혀보세요!");
        });
    }
}
