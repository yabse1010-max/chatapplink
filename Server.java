package chatapplink;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.StringTokenizer;
import javax.swing.SwingUtilities;

    public class Server extends javax.swing.JFrame {
        private ServerSocket ss;
        private final HashMap<String, Socket> clientColl = new HashMap<>();

        public Server() {
            initComponents();
            try {
                ss = new ServerSocket(2089);
                sStatus.setText("Server Started on port 2089.");
                new ClientAccept().start();
            } catch (IOException ex) {
                updateMsgBox("Server failed to start: " + ex.getMessage() + "\n");
            }
        }

        private void updateMsgBox(final String text) {
            SwingUtilities.invokeLater(() -> msgBox.append(text));
        }

        class ClientAccept extends Thread {
            @Override
            public void run() {
                while (ss != null && !ss.isClosed()) {
                    try {
                        Socket s = ss.accept();
                        DataInputStream din = new DataInputStream(s.getInputStream());
                        DataOutputStream dout = new DataOutputStream(s.getOutputStream());

                        String clientName = din.readUTF();

                        synchronized (clientColl) {
                            if (clientColl.containsKey(clientName)) {
                                dout.writeUTF("REJECTED: Already Registered!");
                                s.close();
                            } else {
                                clientColl.put(clientName, s);
                                updateMsgBox(clientName + " Joined !\n");
                                dout.writeUTF("ACCEPTED");

                                new MsgRead(s, clientName).start();
                                new PrepareClientList().start();
                            }
                        }
                    } catch (IOException ex) {
                        break;
                    }
                }
            }
        }

        class MsgRead extends Thread {
            private final Socket s;
            private final String ID;

            MsgRead(Socket s, String i) {
                this.s = s;
                this.ID = i;
            }

            @Override
            public void run() {
                try (DataInputStream din = new DataInputStream(s.getInputStream())) {
                    while (!s.isClosed()) {
                        String message = din.readUTF();

                        if ("mkoihgteazdcvgyhujb09678554AXTY".equals(message)) {
                            removeClient(ID);
                            break;
                        }
                        else if (message.contains("#4344554@@@@@67667@@")) {
                            message = message.substring(20);
                            // Find the exact location of the very first colon
                            int firstColon = message.indexOf(":");
                            if (firstColon != -1) {
                                String targetId = message.substring(0, firstColon);
                                String actualMsg = message.substring(firstColon + 1);
                                sendPrivateMessage(targetId, actualMsg);
                            }
                        }
                        else {
                            broadcastMessage("< " + ID + " to All > " + message);
                        }
                    }
                } catch (IOException ex) {
                    removeClient(ID);
                }
            }

            private void sendPrivateMessage(String targetId, String msg) {
                synchronized (clientColl) {
                    Socket targetSocket = clientColl.get(targetId);
                    if (targetSocket != null) {
                        try {
                            DataOutputStream dout = new DataOutputStream(targetSocket.getOutputStream());
                            dout.writeUTF("< Private from " + ID + " > " + msg);
                        } catch (IOException ex) {
                            removeClient(targetId);
                        }
                    }
                }
            }

            private void broadcastMessage(String formattedMsg) {
                synchronized (clientColl) {
                    clientColl.forEach((key, socket) -> {
                        if (!key.equalsIgnoreCase(ID)) {
                            try {
                                DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                                dout.writeUTF(formattedMsg);
                            } catch (IOException ex) {
                                SwingUtilities.invokeLater(() -> removeClient(key));
                            }
                        }
                    });
                }
            }

            private void removeClient(String clientId) {
                synchronized (clientColl) {
                    if (clientColl.containsKey(clientId)) {
                        try {
                            clientColl.get(clientId).close();
                        } catch (IOException ignored) {}
                        clientColl.remove(clientId);
                        updateMsgBox(clientId + ": removed! \n");
                        new PrepareClientList().start();
                    }
                }
            }
        }

        class PrepareClientList extends Thread {
            @Override
            public void run() {
                StringBuilder ids = new StringBuilder();
                synchronized (clientColl) {
                    for (String key : clientColl.keySet()) {
                        ids.append(key).append(",");
                    }

                    String clientListString = ids.toString();
                    if (clientListString.endsWith(",")) {
                        clientListString = clientListString.substring(0, clientListString.length() - 1);
                    }

                    final String finalPayload = ":;.,/=" + clientListString;

                    clientColl.forEach((key, socket) -> {
                        try {
                            DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                            dout.writeUTF(finalPayload);
                        } catch (IOException ex) {
                            // Managed by message reader drops
                        }
                    });
                }
            }
        }

        private void initComponents() {
            jPanel1 = new javax.swing.JPanel();
            jScrollPane1 = new javax.swing.JScrollPane();
            msgBox = new javax.swing.JTextArea();
            jLabel1 = new javax.swing.JLabel();
            sStatus = new javax.swing.JLabel();

            setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

            jPanel1.setBackground(new java.awt.Color(204, 255, 255));
            msgBox.setColumns(20);
            msgBox.setEditable(false);
            msgBox.setRows(5);
            jScrollPane1.setViewportView(msgBox);

            jLabel1.setFont(new java.awt.Font("Tahoma", 1, 12));
            jLabel1.setText("Server Status:");
            sStatus.setText("Stopped");

            javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
            jPanel1.setLayout(jPanel1Layout);
            jPanel1Layout.setHorizontalGroup(
                    jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addGap(20, 20, 20)
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 360, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGroup(jPanel1Layout.createSequentialGroup()
                                                    .addComponent(jLabel1)
                                                    .addGap(10, 10, 10)
                                                    .addComponent(sStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addContainerGap(20, Short.MAX_VALUE))
            );
            jPanel1Layout.setVerticalGroup(
                    jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addGap(20, 20, 20)
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                            .addComponent(jLabel1)
                                            .addComponent(sStatus))
                                    .addGap(15, 15, 15)
                                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addContainerGap(20, Short.MAX_VALUE))
            );

            javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
            getContentPane().setLayout(layout);
            layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
            layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));

            pack();
            setLocationRelativeTo(null);
        }

        public static void main(String args[]) {
            try {
                for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        javax.swing.UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ex) {}
            java.awt.EventQueue.invokeLater(() -> new Server().setVisible(true));
        }
        private javax.swing.JLabel jLabel1;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JTextArea msgBox;
        private javax.swing.JLabel sStatus;
    }
