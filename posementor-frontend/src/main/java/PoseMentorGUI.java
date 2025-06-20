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

    private CardLayout cardLayout; // 화면 전환
    private JPanel mainPanel; // 각 화면 패널

    // 화면 간 공유할 컴포넌트들
    private JTextField exerciseField; // 운동 이름 입력 필드
    private File selectedFile; // 사용자가 업로드한 영상 파일
    private JTextArea feedbackArea; // GPT 피드백 출력 창
    private JLabel loadingLabel; // 로딩 메시지
    private String exerciseName; // 운동 종류
    private JLabel updateLabel; // 화면 넘어갈 때 라벨 업데이트
    private JProgressBar progressBar;
    private static final String BOUNDARY = "PoseMentorBoundary";
    private JDialog loadingDlg; // 모달(Modal) 로딩 다이얼로그
    private JProgressBar loadingBar; // 인디케이터 (indeterminate)

    // ────────────────────────── 생성자 ──────────────────────────
    public PoseMentorGUI() {
        super("PoseMentor 자세 분석");
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        setSize(650, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 카드 등록
        mainPanel.add(createStartPanel(), "start");
        mainPanel.add(createUploadPanel(), "upload");
        mainPanel.add(createLoadingPanel(), "loading");
        mainPanel.add(createResultPanel(), "result");
        add(mainPanel);
        cardLayout.show(mainPanel, "start");
    }

    // ────────────────────────── 1. 시작 화면 ──────────────────────────
    private JPanel createStartPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel("PoseMentor에 오신 것을 환영합니다!", SwingConstants.CENTER);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subLabel = new JLabel("자세 교정을 원하는 운동을 선택해 주세요!", SwingConstants.CENTER);
        subLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titlePanel.add(Box.createVerticalStrut(30));
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(8));
        titlePanel.add(subLabel);
        titlePanel.add(Box.createVerticalStrut(50));
        panel.add(titlePanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 20, 20));
        String[] exercises = { "골프", "볼링", "축구", "야구", "당구", "농구" };
        for (String ex : exercises) {
            JButton btn = new JButton(ex);
            btn.setPreferredSize(new Dimension(110, 40));
            btn.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
            btn.addActionListener(e -> {
                exerciseName = ex;
                updateLabel.setText(String.format(
                        "<html>%s 자세를 촬영한 동영상을 업로드하고 분석 시작 버튼을 눌러 주세요!<br><br>(선택한 운동과 다른 영상을 올리면 결과가 부정확할 수 있습니다.)</html>",
                        exerciseName));
                cardLayout.show(mainPanel, "upload");
            });
            buttonPanel.add(btn);
        }
        JPanel centerWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centerWrap.add(buttonPanel);
        panel.add(centerWrap, BorderLayout.CENTER);
        return panel;
    }

    // ────────────────────────── 2. 업로드 화면 ──────────────────────────
    private JPanel createUploadPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        JButton back = new JButton("←");
        back.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        back.setFocusPainted(false);
        back.setContentAreaFilled(false);
        back.setBorderPainted(false);
        back.addActionListener(e -> cardLayout.show(mainPanel, "start"));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        top.add(back, gbc);
        updateLabel = new JLabel("", SwingConstants.CENTER);
        updateLabel.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        updateLabel.setPreferredSize(new Dimension(550, 100));
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        top.add(updateLabel, gbc);
        panel.add(top, BorderLayout.NORTH);

        JPanel btnBox = new JPanel(new MigLayout("wrap 1, center", "[grow 0]", "[]25[]"));
        Dimension size = new Dimension(170, 48);
        Font btnFont = new Font("맑은 고딕", Font.BOLD, 14);
        JButton upload = new JButton("영상 업로드");
        upload.setPreferredSize(size);
        upload.setFont(btnFont);
        JButton analyze = new JButton("분석 시작");
        analyze.setPreferredSize(size);
        analyze.setFont(btnFont);
        btnBox.add(upload, "align center");
        btnBox.add(analyze, "align center");
        panel.add(btnBox, BorderLayout.CENTER);

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

   private JPanel createLoadingPanel() {
    // 전체 패널: 중앙 정렬
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(40, 20, 40, 20));
    panel.setOpaque(false);

    // 수직 박스: 안내 문구 ↑ + 로딩 바 ↓
    Box vbox = Box.createVerticalBox();
    vbox.setAlignmentX(Component.CENTER_ALIGNMENT);

    // ── 안내 문구 ──
    JLabel line1 = new JLabel("AI가 운동 자세를 분석하는 중입니다.", SwingConstants.CENTER);
    line1.setFont(new Font("맑은 고딕", Font.BOLD, 18));   // 크기 ↑, 굵게
    line1.setAlignmentX(Component.CENTER_ALIGNMENT);

    JLabel line2 = new JLabel("피드백을 생성 중이니 잠시만 기다려 주세요!", SwingConstants.CENTER);
    line2.setFont(new Font("맑은 고딕", Font.PLAIN, 16)); // 크기 ↑
    line2.setAlignmentX(Component.CENTER_ALIGNMENT);

    vbox.add(line1);
    vbox.add(Box.createVerticalStrut(6));  // 문구 사이 여백
    vbox.add(line2);
    vbox.add(Box.createVerticalStrut(20)); // 문구 ↔ 로딩바 간 여백

    // ── 인디케이터 막대 ──
    JProgressBar bar = new JProgressBar();
    bar.setIndeterminate(true);
    bar.setPreferredSize(new Dimension(180, 12)); // 조금 더 넓고 두껍게
    bar.setBorderPainted(false);
    bar.setAlignmentX(Component.CENTER_ALIGNMENT);

    vbox.add(bar);
    panel.add(vbox);                         // 중앙(GridBagLayout 기본 위치)

    return panel;
}


    // ────────────────────────── 4. 결과 화면 ──────────────────────────
    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel framesCon = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        framesCon.setName("framesCon");
        JScrollPane scrollFrames = new JScrollPane(framesCon);
        scrollFrames.setPreferredSize(new Dimension(520, 180));
        panel.add(scrollFrames, BorderLayout.NORTH);
        feedbackArea = new JTextArea();
        feedbackArea.setEditable(false);
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);
        JScrollPane scrollFb = new JScrollPane(feedbackArea);
        scrollFb.setPreferredSize(new Dimension(520, 300));
        panel.add(scrollFb, BorderLayout.CENTER);
        JButton retry = new JButton("다른 운동 피드백 받기");
        retry.addActionListener(e -> {
            selectedFile = null;
            feedbackArea.setText("");
            framesCon.removeAll();
            framesCon.revalidate();
            framesCon.repaint();
            cardLayout.show(mainPanel, "start");
        });
        panel.add(retry, BorderLayout.SOUTH);
        return panel;
    }

    // ────────────────────────── 5. 백엔드 호출 (SwingWorker) ──────────────────────────
    private void runFeedbackAsync() {
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                String exerciseEnum = mapExerciseNameToEnum(exerciseName);
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest.BodyPublisher body = ofMimeMultipartData(selectedFile, exerciseEnum);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/analyze"))
                        .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                        .POST(body).build();
                return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
            }

            @Override
            protected void done() {
                try {
                    String body = get(); // 서버에서 받은 JSON 문자열

                    // --- 간단 수동 파싱 (외부 라이브러리 불필요) ---
                    String fbKey = "\"feedback\":\"";
                    int i1 = body.indexOf(fbKey) + fbKey.length();
                    int i2 = body.indexOf("\"", i1);
                    String fb = body.substring(i1, i2);
                    fb = fb.replace("\\n", "\n");
                    String frKey = "\"frames\":";
                    int f1 = body.indexOf(frKey) + frKey.length();
                    int f2 = body.indexOf("]", f1);
                    String arr = body.substring(f1, f2 + 1)
                            .replace("\\/", "/")
                            .replaceAll("[\\[\\]\"]", "");
                    String[] urls = arr.split(",");

                    // (1) 프레임 이미지 업데이트
                    JPanel framesCon = findByName(mainPanel, "framesCon", JPanel.class);
                    framesCon.removeAll();
                    for (String u : urls) {
                        try {
                            URL url = new URL(u);
                            ImageIcon ic = new ImageIcon(url);
                            JLabel pic = new JLabel(ic);
                            pic.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                            framesCon.add(pic);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    framesCon.revalidate();

                    // (2) 피드백 텍스트 업데이트
                    feedbackArea.setText(fb);

                    cardLayout.show(mainPanel, "result");

                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(PoseMentorGUI.this, "오류: " + ex.getMessage());
                    cardLayout.show(mainPanel, "result");
                } finally {
                    loadingDlg.setVisible(false);
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
                if (name.equals(jc.getName()))
                    return (T) jc;
                if (c instanceof Container cont) {
                    T child = findByName(cont, name, cls);
                    if (child != null)
                        return child;
                }
            }
        }
        return null;
    }

    // 운동 종류 영어 enum 매핑 메서드
    private String mapExerciseNameToEnum(String kor) {
        return switch (kor) {
            case "축구" -> "SOCCER";
            case "골프" -> "GOLF";
            case "볼링" -> "BOWLING";
            case "야구" -> "BASEBALL";
            case "당구" -> "BILLIARDS";
            case "농구" -> "BASKETBALL";
            default -> "GOLF"; // 기본값
        };
    }

    // 파일+텍스트를 multipart로 전송해주는 커스텀 함수
    private static HttpRequest.BodyPublisher ofMimeMultipartData(File file, String exercise) throws IOException {
        var byteArrays = new ArrayList<byte[]>();

        // 1. exerciseType 파트
        byteArrays.add(("--" + BOUNDARY + "\r\n").getBytes());
        byteArrays.add("Content-Disposition: form-data; name=\"exerciseType\"\r\n\r\n".getBytes());
        byteArrays.add(exercise.getBytes());
        byteArrays.add("\r\n".getBytes());

        // 2. file 파트
        byteArrays.add(("--" + BOUNDARY + "\r\n").getBytes());
        byteArrays.add(
                ("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n").getBytes());
        byteArrays.add("Content-Type: video/mp4\r\n\r\n".getBytes());
        byteArrays.add(Files.readAllBytes(file.toPath()));
        byteArrays.add("\r\n".getBytes());

        // 3. 종료
        byteArrays.add(("--" + BOUNDARY + "--\r\n").getBytes());

        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }

    // 프로그램 시작 지점
    public static void main(String[] args) {
        // 라이트 테마
        FlatLightLaf.setup();
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 999);
        // SwingUtilities.invokeLater(() -> new PoseMentorGUI().createAndShow());

        SwingUtilities.invokeLater(() -> new PoseMentorGUI().setVisible(true));
    }
}