package ru.yaga;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

public class ChatClient extends JFrame implements Runnable {

    private final Socket socket;
    private final DataInputStream dataInputStream;
    private final DataOutputStream dataOutputStream;
    private final JTextArea outTextArea;
    private final JTextField inTextField;

    public ChatClient(Socket socket, DataInputStream dataInputStream, DataOutputStream dataOutputStream) {
        super("Client");
        this.socket = socket;
        this.dataInputStream = dataInputStream;
        this.dataOutputStream = dataOutputStream;

        setSize(400, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        outTextArea = new JTextArea();
        add(outTextArea);
        inTextField = new JTextField();
        add(BorderLayout.SOUTH, inTextField);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                try {
                    dataOutputStream.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        inTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    dataOutputStream.writeUTF(inTextField.getText());
                    dataOutputStream.flush();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                inTextField.setText("");
            }
        });

        setVisible(true);
        inTextField.requestFocus();
        new Thread(this).start();
    }


    public static void main(String[] args) {
        String site = "localhost";
        String port = "8082";

        Socket socket = null;
        DataInputStream dataInputStream = null;
        DataOutputStream dataOutputStream = null;

        try {
            socket = new Socket(site, Integer.parseInt(port));
            dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            dataOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            new ChatClient(socket, dataInputStream, dataOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                String line = dataInputStream.readUTF();
                outTextArea.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            inTextField.setVisible(false);
            validate();
        }
    }
}