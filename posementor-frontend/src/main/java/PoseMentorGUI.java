import javax.swing.*; 
import java.awt.*; 
import java.awt.event.*; 
import java.io.File; 
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.net.URL;
import java.net.MalformedURLException;
import com.formdev.flatlaf.FlatLightLaf;

import net.miginfocom.swing.MigLayout;
// 메인 클래스: GUI 앱의 전체 구조 정의
public class PoseMentorGUI extends JFrame {

    private CardLayout cardLayout; //화면 전환
    private JPanel mainPanel;      // 각 화면 패널

    // 화면 간 공유할 컴포넌트들
    private JTextField exerciseField; // 운동 이름 입력 필드
    private File selectedFile;        // 사용자가 업로드한 영상 파일
    private JTextArea feedbackArea;   // GPT 피드백 출력 창
    private JLabel loadingLabel;      // 로딩 메시지
    private String exerciseName; //운동 종류
    private JLabel updateLabel; //화면 넘어갈 때 라벨 업데이트

    private static final String BOUNDARY = "PoseMentorBoundary";
    // 생성자 구성
    public PoseMentorGUI() {
        setTitle("PoseMentor 자세 분석");
        setSize(650, 400);
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
        

        JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 15, 15));
        String[] selectExercise = {"헬스", "골프", "볼링", "야구", "당구", "농구"};

        for (String exercise : selectExercise) {
            JButton btn = new JButton(exercise);
           // btn.setSize(int 80, int 40);

            btn.addActionListener(e -> {
                exerciseName = exercise;
                updateLabel.setText(String.format("<html>  %s 자세를 촬영한 동영상을 업로드하고 분석 시작 버튼을 눌러주세요!<br><br> (선택한 운동과 다른 종류의영상을 업로드 시 결과가 부정확 할 수 있습니다.)</html>", exerciseName));
                cardLayout.show(mainPanel, "upload");
            });
            buttonPanel.add(btn);
        }        
        JPanel centerWrap = new JPanel(new FlowLayout());
        centerWrap.add(buttonPanel);
        panel.add(centerWrap, BorderLayout.CENTER);
        return panel;
        }

private JPanel createUploadPanel() {
    JPanel panel = new JPanel(new BorderLayout());

    /* ────────────────── 1. 상단 바 ────────────────── */
    JPanel top = new JPanel(new GridBagLayout());           // ← 핵심: GridBag
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(10, 10, 10, 10);                 // 공통 패딩
    gbc.gridy = 0;

    // (1) 뒤로가기 버튼 (col 0, 가로 고정)
    JButton back = new JButton("←");
    back.setFont(new Font("맑은 고딕", Font.BOLD, 15));
    back.setFocusPainted(false);
    back.setContentAreaFilled(false);
    back.setBorderPainted(false);
    back.addActionListener(e -> cardLayout.show(mainPanel, "start"));
    gbc.gridx = 0;
    gbc.anchor = GridBagConstraints.WEST;                    // 왼쪽 고정
    gbc.weightx = 0;                                         // 공간 안 먹음
    top.add(back, gbc);

    // (2) 안내 문구 (col 1, 가로 100 % 차지)
    updateLabel = new JLabel("", SwingConstants.CENTER);
    updateLabel.setFont(new Font("맑은 고딕", Font.BOLD, 15));
    updateLabel.setPreferredSize(new Dimension(550,100));
    updateLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
    gbc.gridx = 1;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.weightx = 1;                                         // **가로 전체 확보**
    top.add(updateLabel, gbc);

    panel.add(top, BorderLayout.NORTH);

    /* ────────────────── 2. 중앙 버튼 영역 (MigLayout) ────────────────── */
    JPanel btnBox = new JPanel(new MigLayout("wrap 1, center", "[grow 0]", "[]25[]"));

    Dimension size = new Dimension(170, 48);
    Font btnFont = new Font("맑은 고딕", Font.BOLD, 14);

    JButton upload = new JButton("영상 업로드");
    upload.setPreferredSize(size);  upload.setFont(btnFont);
    JButton analyze = new JButton("분석 시작");
    analyze.setPreferredSize(size); analyze.setFont(btnFont);

    btnBox.add(upload,   "align center");
    btnBox.add(analyze,  "align center");

    panel.add(btnBox, BorderLayout.CENTER);

    /* ─────────── 3. 버튼 기능 ─────────── */
    upload.addActionListener(e -> {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            selectedFile = fc.getSelectedFile();
    });

    analyze.addActionListener(e -> {
        if (selectedFile == null) {
            JOptionPane.showMessageDialog(this, "영상을 먼저 업로드하세요.");
            return;
        }
        cardLayout.show(mainPanel, "loading");
        runFeedbackAsync();
    });

    return panel;
}



    // 영상 업로드 & 분석 버튼 화면
    // private JPanel createUploadPanel() {
    //     JPanel panel = new JPanel(new BorderLayout());
    //     //상단바 : 뒤로가기 + 안내 텍스트
    //     JPanel topPanel = new JPanel(new BorderLayout());
    //     JButton backButton = new JButton("←");
    //     backButton.setFont(new Font("맑은 고딕", Font.BOLD, 18));
    //     backButton.setMargin(new Insets(10, 10, 80, 0));
    //     backButton.setFocusPainted(false);
    //     backButton.setContentAreaFilled(false);
    //     backButton.setBorderPainted(false);
    //     backButton.addActionListener(e -> cardLayout.show(mainPanel, "start"));

    //     updateLabel = new JLabel("", SwingConstants.CENTER);
    //     panel.add(updateLabel, BorderLayout.NORTH);
        
    //     updateLabel.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        
    //     topPanel.add(backButton, BorderLayout.WEST);
    //     topPanel.add(updateLabel, BorderLayout.CENTER);
    //     panel.add(topPanel, BorderLayout.NORTH);
    //     //가운데 : 영상 업로드 + 분석 시작 버튼
    //     JPanel btnPanel = new JPanel();
    //     btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.Y_AXIS));

    //     JButton uploadBtn = new JButton("영상 업로드");
    //     JButton analyzeBtn = new JButton("분석 시작");




    //     btnPanel.add(uploadBtn);
    //     uploadBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
    //     uploadBtn.setMaximumSize(new Dimension(130, 50));
    //     uploadBtn.setFont(new Font("맑은 고딕", Font.BOLD, 14));

    //     btnPanel.add(Box.createVerticalStrut(40));//버튼 간격

    //     btnPanel.add(analyzeBtn);
    //     analyzeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
    //     analyzeBtn.setMaximumSize(new Dimension(130, 50));
    //     analyzeBtn.setFont(new Font("맑은 고딕", Font.BOLD, 14));


    //     panel.add(btnPanel, BorderLayout.CENTER);

    //     // 영상 업로드 버튼
    //     uploadBtn.addActionListener(e -> {
    //         JFileChooser fileChooser = new JFileChooser(); // 파일 탐색기 띄움
    //         int res = fileChooser.showOpenDialog(this);
    //         if (res == JFileChooser.APPROVE_OPTION) {
    //             selectedFile = fileChooser.getSelectedFile(); // 선택된 파일 저장
    //         }
    //     });

    //     // 분석 시작 버튼
    //     analyzeBtn.addActionListener(e -> {
    //         if (selectedFile == null) {
    //             JOptionPane.showMessageDialog(this, "영상을 먼저 업로드하세요.");
    //             return;
    //         }
    //         cardLayout.show(mainPanel, "loading"); // 로딩 화면으로 전환
    //         runFeedbackAsync(); // 백그라운드에서 피드백 생성
    //     });

    //     return panel;
    // }

    // 로딩 중 화면
    private JPanel createLoadingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        loadingLabel = new JLabel("⏳ AI가 피드백을 생성 중입니다. 잠시만 기다려주세요.", SwingConstants.CENTER);
        panel.add(loadingLabel, BorderLayout.CENTER);
        return panel;
    }
    // 결과 화면: 분석한 프레임 이미지 + 피드백만 표시
    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // (1) 프레임 이미지 컨테이너
        JPanel framesCon = new JPanel(new FlowLayout(FlowLayout.CENTER,10,10));
        framesCon.setName("framesCon");
        JScrollPane scrollFrames = new JScrollPane(framesCon);
        scrollFrames.setPreferredSize(new Dimension(520,180));
        panel.add(scrollFrames, BorderLayout.NORTH);

        // (2) 피드백 텍스트
        feedbackArea = new JTextArea();
        feedbackArea.setEditable(false);
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);
        JScrollPane scrollFb = new JScrollPane(feedbackArea);
        scrollFb.setPreferredSize(new Dimension(520,300));
        panel.add(scrollFb, BorderLayout.CENTER);

        // (3) 다시 분석 버튼
        JButton retry = new JButton("다른 운동 피드백 받기");
        retry.addActionListener(e-> {
            selectedFile = null;
            feedbackArea.setText("");
            framesCon.removeAll();
            framesCon.revalidate();
            framesCon.repaint();
            cardLayout.show(mainPanel,"start");
        });
        panel.add(retry, BorderLayout.SOUTH);

        return panel;
    }



    // GPT 피드백 요청을 백그라운드에서 실행
    private void runFeedbackAsync() {
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                //운동 이름을 영어 enum으로 매핑
                String exerciseEnum = mapExerciseNameToEnum(exerciseName);
                //POST 요청
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest.BodyPublisher bodyPublisher = ofMimeMultipartData(selectedFile, exerciseEnum);
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/analyze"))
                    .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                    .POST(bodyPublisher)
                    .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                return response.body(); // 서버의 피드백 결과 반환
            }

            @Override
            protected void done() {
                try {
                    String body = get();  // 서버에서 받은 JSON 문자열

                    // --- 간단 수동 파싱 (외부 라이브러리 불필요) ---
                    String fbKey = "\"feedback\":\"";
                    int i1 = body.indexOf(fbKey) + fbKey.length();
                    int i2 = body.indexOf("\"", i1);
                    String fb = body.substring(i1, i2);
                    fb = fb.replace("\\n", "\n");
                    String frKey = "\"frames\":";
                    int f1 = body.indexOf(frKey) + frKey.length();
                    int f2 = body.indexOf("]", f1);
                    String arr = body.substring(f1, f2+1)
                                .replace("\\/","/")
                                .replaceAll("[\\[\\]\"]","");
                    String[] urls = arr.split(",");

                    // (1) 프레임 이미지 업데이트
                    JPanel framesCon = findByName(mainPanel,"framesCon",JPanel.class);
                    framesCon.removeAll();
                    for (String u : urls) {
                        try {
                            URL url = new URL(u);
                            ImageIcon ic = new ImageIcon(url);
                            JLabel pic = new JLabel(ic);
                            pic.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                            framesCon.add(pic);
                        } catch (Exception ex) { ex.printStackTrace(); }
                    }
                    framesCon.revalidate();

                    // (2) 피드백 텍스트 업데이트
                    feedbackArea.setText(fb);

                    cardLayout.show(mainPanel,"result");

                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(PoseMentorGUI.this,"오류: "+ex.getMessage());
                    cardLayout.show(mainPanel,"result");
                }
            }

        };
        worker.execute(); // 작업 실행
    }
    // ── 유틸: 이름으로 컴포넌트 찾아주는 메서드 ──
    @SuppressWarnings("unchecked")
    private <T extends JComponent> T findByName(Container root, String name, Class<T> cls) {
        for (Component c : root.getComponents()) {
            if (c instanceof JComponent jc) {
                if (name.equals(jc.getName())) return (T) jc;
                if (c instanceof Container cont) {
                    T child = findByName(cont, name, cls);
                    if (child != null) return child;
                }
            }
        }
        return null;
    }
    //운동 종류 영어 enum 매핑 메서드
    private String mapExerciseNameToEnum(String kor) {
        return switch (kor) {
            case "헬스" -> "FITNESS";
            case "골프" -> "GOLF";
            case "볼링" -> "BOWLING";
            case "야구" -> "BASEBALL";
            case "당구" -> "BILLIARDS";
            case "농구" -> "BASKETBALL";
            default -> "FITNESS"; // 기본값
        };
    }
    //파일+텍스트를 multipart로 전송해주는 커스텀 함수
    private static HttpRequest.BodyPublisher ofMimeMultipartData(File file, String exercise) throws IOException {
        var byteArrays = new ArrayList<byte[]>();

        // 1. exerciseType 파트
        byteArrays.add(("--" + BOUNDARY + "\r\n").getBytes());
        byteArrays.add("Content-Disposition: form-data; name=\"exerciseType\"\r\n\r\n".getBytes());
        byteArrays.add(exercise.getBytes());
        byteArrays.add("\r\n".getBytes());

        // 2. file 파트
        byteArrays.add(("--" + BOUNDARY + "\r\n").getBytes());
        byteArrays.add(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n").getBytes());
        byteArrays.add("Content-Type: video/mp4\r\n\r\n".getBytes());
        byteArrays.add(Files.readAllBytes(file.toPath()));
        byteArrays.add("\r\n".getBytes());

        // 3. 종료
        byteArrays.add(("--" + BOUNDARY + "--\r\n").getBytes());

        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }



    // 프로그램 시작 지점
    public static void main(String[] args) {
        //라이트 테마
        FlatLightLaf.setup();
        UIManager.put("Component.arc",12);
        UIManager.put("Button.arc", 999);
       // SwingUtilities.invokeLater(() -> new PoseMentorGUI().createAndShow());
        
        SwingUtilities.invokeLater(() -> new PoseMentorGUI().setVisible(true));
    }
}