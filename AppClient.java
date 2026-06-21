package chatapplink;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.StringTokenizer;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;

public class AppClient extends JFrame {

    private String iD = " ";
    private DataInputStream din;
    private DataOutputStream dout;
    private DefaultListModel<String> dlm;

    private JList<String> UL;
    private JLabel idLabel;
    private JLabel profilePicLabel; // NEW: Profile Picture display
    private JTextArea msgBox;
    private JTextField idText;
    private JButton connectBtn;
    private JTextField chatInputText;
    private JButton sendBtn;

    // NEW: Feature Buttons
    private JButton emojiBtn;
    private JButton fileBtn;
    private JButton videoBtn;
    private JButton profileBtn;

    public AppClient() {
        initComponents();
        setupLoginLayout();
    }

    public AppClient(String id, Socket s) {
        try {
            this.iD = id;
            initComponents();
            setupActiveClientLayout();

            idLabel.setText("Logged in as: " + id);

            din = new DataInputStream(s.getInputStream());
            dout = new DataOutputStream(s.getOutputStream());

            new Read().start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    class Read extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    String m = din.readUTF();
                    if (m.startsWith(":;.,/=")) {
                        m = m.substring(6);
                        dlm.clear();
                        StringTokenizer st = new StringTokenizer(m, ",");
                        while (st.hasMoreTokens()) {
                            String u = st.nextToken();
                            if (!iD.equals(u)) {
                                dlm.addElement(u);
                            }
                        }
                    } else {
                        msgBox.append(m + "\n");
                    }
                } catch (Exception ex) {
                    msgBox.append("Connection to server lost.\n");
                    break;
                }
            }
        }
    }

    public void connectButtonActionPerformed(ActionEvent evt) {
        String id = idText.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a valid User ID first.");
            return;
        }

        try {
            Socket s = new Socket("localhost", 2089);

            DataInputStream localDin = new DataInputStream(s.getInputStream());
            DataOutputStream localDout = new DataOutputStream(s.getOutputStream());

            localDout.writeUTF(id);
            String response = localDin.readUTF();

            if (response.startsWith("REJECTED")) {
                JOptionPane.showMessageDialog(this, "Registration failed: " + response);
                s.close();
            } else {
                java.awt.EventQueue.invokeLater(() -> {
                    new AppClient(id, s).setVisible(true);
                });
                this.dispose();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: Cannot connect to server on port 2089.");
            ex.printStackTrace();
        }
    }

    // --- NEW FEATURE ACTIONS ---
 private void emojiButtonActionPerformed(ActionEvent evt) {
        // A simple pop-up to pick an emoji
        String[] emojis = {"😂", "❤️", "👍", "😢", "🔥", "😎"};
        String choice = (String) JOptionPane.showInputDialog(this, "Choose an Emoji",
                "Emoji Picker", JOptionPane.PLAIN_MESSAGE, null, emojis, emojis[0]);

        if (choice != null) {
            chatInputText.setText(chatInputText.getText() + choice);
        }
    }

    private void fileButtonActionPerformed(ActionEvent evt) {
        JFileChooser chooser = new JFileChooser();
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            // Instead of crashing the server sending bytes, we send a notification message
            String fileMsg = "📎 Sent a file: " + file.getName() + " (Ready for download)";
            sendMessageToServer(fileMsg);
        }
    }

    private void videoButtonActionPerformed(ActionEvent evt) {
        String selectedUser = UL.getSelectedValue();
        if (selectedUser == null || selectedUser.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a user from the Active Users list to call.");
            return;
        }

        // Show a fake calling screen
        JOptionPane.showMessageDialog(this, "📹 Ringing " + selectedUser + "...\n(Camera starting...)", "Video Call", JOptionPane.INFORMATION_MESSAGE);
        sendMessageToServer("📹 Started a video call with you!");
    }

    private void profileButtonActionPerformed(ActionEvent evt) {
        JFileChooser chooser = new JFileChooser();
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            ImageIcon icon = new ImageIcon(file.getAbsolutePath());
            // Resize image to fit the label
            Image img = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            profilePicLabel.setIcon(new ImageIcon(img));
            profilePicLabel.setText(""); // Hide the default text
        }
    }

    // Extracted the sending logic so all buttons can use it easily
    private void sendMessageToServer(String rawMessage) {
        if (rawMessage.isEmpty()) return;

        String timeNow = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String finalMessage = "[" + timeNow + "] " + rawMessage;

        try {
            String selectedUser = UL.getSelectedValue();

            if (selectedUser != null && !selectedUser.isEmpty()) {
                String privatePayload = "#4344554@@@@@67667@@" + selectedUser + ":" + finalMessage;
                dout.writeUTF(privatePayload);
                msgBox.append("< Private to " + selectedUser + " > " + finalMessage + "\n");
            } else {
                dout.writeUTF(finalMessage);
                msgBox.append("< You to All > " + finalMessage + "\n");
            }
            chatInputText.setText("");
        } catch (IOException ex) {
            msgBox.append("Failed to send message.\n");
        }
    }

    private void sendButtonActionPerformed(ActionEvent evt) {
        sendMessageToServer(chatInputText.getText().trim());
    }

    private void initComponents() {
        dlm = new DefaultListModel<>();
        UL = new JList<>(dlm);
        idLabel = new JLabel("Status: Disconnected");
        profilePicLabel = new JLabel(" 👤 "); // Default blank profile
        profilePicLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));

        msgBox = new JTextArea(15, 30);
        msgBox.setEditable(false);
        msgBox.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14)); // Important for rendering Emojis!

        idText = new JTextField(15);
        connectBtn = new JButton("Connect");
chatInputText = new JTextField(25);
        sendBtn = new JButton("Send");

        // Initialize new buttons
        emojiBtn = new JButton("😃 Emoji");
        fileBtn = new JButton("📎 File");
        videoBtn = new JButton("📹 Video Call");
        profileBtn = new JButton("Set Profile Pic");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("App Client GUI");

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (dout != null) {
                    try {
                        dout.writeUTF("mkoihgteazdcvgyhujb09678554AXTY");
                    } catch (IOException ignored) {}
                }
            }
        });
    }

    private void setupLoginLayout() {
        this.setSize(350, 150);
        this.setLocationRelativeTo(null);
        this.setLayout(new FlowLayout());

        this.add(new JLabel("Enter Registration ID:"));
        this.add(idText);
        this.add(connectBtn);

        connectBtn.addActionListener(this::connectButtonActionPerformed);
    }

    private void setupActiveClientLayout() {
        this.setSize(650, 500); // Made window slightly larger for new features
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout(5, 5));

        // Top Panel: Profile Pic, ID, and Profile Button
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(new Color(230, 240, 255));
        topPanel.add(profilePicLabel);
        topPanel.add(idLabel);
        topPanel.add(new JLabel("  |  "));
        topPanel.add(profileBtn);
        this.add(topPanel, BorderLayout.NORTH);

        this.add(new JScrollPane(msgBox), BorderLayout.CENTER);

        // Right Panel: Active Users
        JPanel rightPanel = new JPanel();
        rightPanel.add(new JLabel(" Active Users "));
        rightPanel.add(new JScrollPane(UL));
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        this.add(rightPanel, BorderLayout.EAST);

        // Bottom Panel: Toolbar and Chat Input
        JPanel bottomContainer = new JPanel();
        bottomContainer.setLayout(new BoxLayout(bottomContainer, BoxLayout.Y_AXIS));

        // The new Feature Toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(emojiBtn);
        toolBar.addSeparator();
        toolBar.add(fileBtn);
        toolBar.addSeparator();
        toolBar.add(videoBtn);

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.add(chatInputText);
        inputPanel.add(sendBtn);

        bottomContainer.add(toolBar);
        bottomContainer.add(inputPanel);
        this.add(bottomContainer, BorderLayout.SOUTH);

        // Action Listeners
        sendBtn.addActionListener(this::sendButtonActionPerformed);
        chatInputText.addActionListener(this::sendButtonActionPerformed);
        emojiBtn.addActionListener(this::emojiButtonActionPerformed);
        fileBtn.addActionListener(this::fileButtonActionPerformed);
        videoBtn.addActionListener(this::videoButtonActionPerformed);
        profileBtn.addActionListener(this::profileButtonActionPerformed);
    }

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> {
            new AppClient().setVisible(true);
        });
    }
}



