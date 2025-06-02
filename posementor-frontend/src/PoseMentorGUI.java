import javax.swing.*; // GUI 구성 요소들
import java.awt.*; // 레이아웃, 색상 등
import java.awt.event.*; // 버튼 클릭 이벤트
import java.io.File; // 파일 선택 기능

// 메인 클래스: GUI 앱의 전체 구조 정의
public class PoseMentorGUI extends JFrame {

    private CardLayout cardLayout; //화면 전환을 레이아웃
    private JPanel mainPanel;      // 각 화면 패널을 담는 메인 패널

    // 화면 간 공유할 컴포넌트들
    private JTextField exerciseField; // 운동 이름 입력 필드
    private File selectedFile;        // 사용자가 업로드한 영상 파일
    private JTextArea feedbackArea;   // GPT 피드백 출력 창
    private JLabel loadingLabel;      // 로딩 메시지
    private String exerciseName; //운동 종류
    private JLabel updateLabel; //화면 넘어갈 때 라벨 업데이트
    // 생성자 구성
    public PoseMentorGUI() {
        setTitle("PoseMentor 자세 분석");
        setSize(550, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 화면 중앙 정렬

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout); // 카드 레이아웃 적용

        // 4개의 화면 추가
        mainPanel.add(createStartPanel(), "start");
        mainPanel.add(createUploadPanel(), "upload");
        mainPanel.add(createLoadingPanel(), "loading");
        mainPanel.add(createResultPanel(), "result");

        add(mainPanel);
        cardLayout.show(mainPanel, "start"); // 시작화면 먼저 보여줌
    }

    // 시작 화면: 운동 이름 입력 + 확인 버튼
    private JPanel createStartPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("PoseMentor에 오신 것을 환영합니다!", SwingConstants.CENTER);
        label.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        label.setBorder(BorderFactory.createEmptyBorder(40, 0, 50, 0));
        panel.add(label, BorderLayout.NORTH);
        

        // JPanel inputPanel = new JPanel(new FlowLayout());
        // exerciseField = new JTextField(20); // 운동 이름 입력칸

        // JButton nextButton = new JButton("확인");

        // inputPanel.add(new JLabel("피드백 받을 운동 이름 입력: "));
        // inputPanel.add(exerciseField);
        // inputPanel.add(nextButton);

        // panel.add(inputPanel, BorderLayout.CENTER);
        // 수정 예정
        JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        String[] selectExercise = {"헬스", "골프", "볼링", "야구", "당구", "농구"};

        for (String exercise : selectExercise) {
            JButton btn = new JButton(exercise);
            btn.addActionListener(e -> {
                exerciseName = exercise;
                updateLabel.setText(String.format("사용자의 %s 동영상을 업로드하고 분석 시작 버튼을 눌러주세요!", exerciseName));
                cardLayout.show(mainPanel, "upload");
            });
            buttonPanel.add(btn);
        }        
        JPanel centerWrap = new JPanel(new FlowLayout());
        centerWrap.add(buttonPanel);
        panel.add(centerWrap, BorderLayout.CENTER);
        return panel;
        }

    

    // 영상 업로드 & 분석 버튼 화면
    private JPanel createUploadPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        //상단바 : 뒤로가기 + 안내 텍스트트
        JPanel topPanel = new JPanel(new BorderLayout());
        JButton backButton = new JButton("←");
        backButton.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        backButton.setMargin(new Insets(10, 10, 80, 0));
        backButton.setFocusPainted(false);
        backButton.setContentAreaFilled(false); // 배경 없애면 좀 더 자연스러움
        backButton.setBorderPainted(false);
        backButton.addActionListener(e -> cardLayout.show(mainPanel, "start"));

        updateLabel = new JLabel("", SwingConstants.CENTER);
        panel.add(updateLabel, BorderLayout.NORTH);
        updateLabel.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        updateLabel.setBorder(BorderFactory.createEmptyBorder(40, 0, 50, 30));
        topPanel.add(backButton, BorderLayout.WEST);
        topPanel.add(updateLabel, BorderLayout.CENTER);
        panel.add(topPanel, BorderLayout.NORTH);
        //가운데 : 영상 업로드 + 분석 시작 버튼
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.Y_AXIS));

        JButton uploadBtn = new JButton("영상 업로드");
        JButton analyzeBtn = new JButton("분석 시작");




        btnPanel.add(uploadBtn);
        uploadBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        uploadBtn.setMaximumSize(new Dimension(130, 50));
        uploadBtn.setFont(new Font("맑은 고딕", Font.BOLD, 14));

        btnPanel.add(Box.createVerticalStrut(40));//버튼 간격

        btnPanel.add(analyzeBtn);
        analyzeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        analyzeBtn.setMaximumSize(new Dimension(130, 50));
        analyzeBtn.setFont(new Font("맑은 고딕", Font.BOLD, 14));


        panel.add(btnPanel, BorderLayout.CENTER);

        // 영상 업로드 버튼
        uploadBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(); // 파일 탐색기 띄움
            int res = fileChooser.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile(); // 선택된 파일 저장
            }
        });

        // 분석 시작 버튼
        analyzeBtn.addActionListener(e -> {
            if (selectedFile == null) {
                JOptionPane.showMessageDialog(this, "영상을 먼저 업로드하세요.");
                return;
            }
            cardLayout.show(mainPanel, "loading"); // 로딩 화면으로 전환
            runFeedbackAsync(); // 백그라운드에서 피드백 생성
        });

        return panel;
    }

    // 로딩 중 화면
    private JPanel createLoadingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        loadingLabel = new JLabel("⏳ AI가 피드백을 생성 중입니다. 잠시만 기다려주세요.", SwingConstants.CENTER);
        panel.add(loadingLabel, BorderLayout.CENTER);
        return panel;
    }

    // 결과 화면: GPT 피드백 표시 + 다시 분석 버튼
    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel title = new JLabel("📋 피드백 생성 완료", SwingConstants.CENTER);
        feedbackArea = new JTextArea();
        feedbackArea.setEditable(false); // 사용자 편집 불가

        JButton retryBtn = new JButton("다른 운동 피드백 받기");
        retryBtn.addActionListener(e -> {
            // 입력/업로드 초기화 후 시작화면으로 복귀
            exerciseField.setText("");
            selectedFile = null;
            feedbackArea.setText("");
            cardLayout.show(mainPanel, "start");
        });

        panel.add(title, BorderLayout.NORTH);
        panel.add(new JScrollPane(feedbackArea), BorderLayout.CENTER);
        panel.add(retryBtn, BorderLayout.SOUTH);

        return panel;
    }

    // GPT 피드백 요청을 백그라운드에서 실행
    private void runFeedbackAsync() {
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                // 실제 GPT 서버에 요청 보낼 수 있는 부분
                Thread.sleep(3000); // 3초 대기(수정 예정정)
                String exercise = exerciseField.getText();
                return "운동 종류: " + exercise + "\n✅ 허리 각도가 올바릅니다!\n❌ 무릎이 너무 앞으로 나갔어요!"; //예시 피드백(추후 api 연동 예정정)
            }

            @Override
            protected void done() {
                try {
                    String feedback = get(); // 백그라운드 결과 받기
                    feedbackArea.setText(feedback);
                    cardLayout.show(mainPanel, "result"); // 결과 화면으로 전환
                } catch (Exception e) {
                    feedbackArea.setText("오류 발생: " + e.getMessage());
                    cardLayout.show(mainPanel, "result");
                }
            }
        };
        worker.execute(); // 작업 실행
    }

    // 프로그램 시작 지점
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PoseMentorGUI().setVisible(true));
    }
}
