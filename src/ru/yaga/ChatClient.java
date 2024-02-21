package ru.yaga;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

public class ChatClient extends JFrame {
    // Сокет для соединения с сервером
    private final Socket socket;
    // Поток ввода данных от сервера
    private final DataInputStream dataInputStream;
    // Поток вывода данных к серверу
    private final DataOutputStream dataOutputStream;
    // Область вывода сообщений от сервера
    private final JTextPane outTextPane;
    // Поле ввода сообщений пользователем
    private final JTextField inTextField;
    // Имя пользователя
    private final String username;

    // Путь к файлу с именами пользователей
    private static final String USERS_FILE = "users.txt";

    // Конструктор класса
    public ChatClient(Socket socket, DataInputStream dataInputStream, DataOutputStream dataOutputStream, String username) {
        super("Chat Client - " + username);
        this.socket = socket;
        this.dataInputStream = dataInputStream;
        this.dataOutputStream = dataOutputStream;
        this.username = username;

        // Настройка окна приложения
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Создание выпадающего списка пользователей
        JComboBox<String> userComboBox = new JComboBox<>();
        add(BorderLayout.NORTH, userComboBox);

        // Обработчик события выбора пользователя из списка
        userComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Действия при выборе пользователя из списка
                String selectedUser = (String) userComboBox.getSelectedItem();
            }
        });

        // Отображение списка пользователей в выпадающем списке
        displayUserList(userComboBox);

        // Вызов метода для обновления списка пользователей
        updateUserList(userComboBox);

        // Создание области вывода сообщений
        outTextPane = new JTextPane();
        outTextPane.setEditable(false);
        add(new JScrollPane(outTextPane));

        // Создание поля ввода сообщений
        inTextField = new JTextField();
        add(BorderLayout.SOUTH, inTextField);

        // Обработчик события ввода текста пользователем
        inTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Получение текста сообщения и его форматирование
                    String message = inTextField.getText();
                    String formattedMessage = getCurrentDateTime() + " " + username + ": " + message;

                    // Отправка сообщения на сервер
                    dataOutputStream.writeUTF(formattedMessage);
                    dataOutputStream.flush();

                    // Очистка поля ввода
                    inTextField.setText("");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        // Установка видимости окна
        setVisible(true);
        // Установка фокуса на поле ввода
        inTextField.requestFocus();

        // Запуск фонового потока для чтения сообщений от сервера
        new ChatWorker().execute();

        // Вызов метода для запуска потока обновления списка пользователей
        startUserListUpdater(userComboBox);
    }

    // Метод для отображения выпадающего списка пользователей
    private void displayUserList(JComboBox<String> userComboBox) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(USERS_FILE))) {
            List<String> userList = new ArrayList<>();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                userList.add(line.split(":")[1]); // Получение имени пользователя из строки
            }

            // Отображение списка пользователей в выпадающем списке
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(userList.toArray(new String[0]));
            userComboBox.setModel(model);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Метод для обновления списка пользователей
    private void updateUserList(JComboBox<String> userComboBox) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(USERS_FILE))) {
            List<String> userList = new ArrayList<>();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                userList.add(line.split(":")[1]); // Получение имени пользователя из строки
            }

            // Обновление списка в выпадающем списке
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(userList.toArray(new String[0]));
            userComboBox.setModel(model);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Метод для запуска потока обновления списка пользователей
    private void startUserListUpdater(JComboBox<String> userComboBox) {
        new Thread(() -> {
            while (true) {
                try {
                    // Задержка перед обновлением списка (каждые 10 секунд)
                    Thread.sleep(10000);

                    // Обновление списка пользователей
                    updateUserList(userComboBox);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //Метод, выполняющий фоновые операции, читая данные от сервера в бесконечном цикле.
    private class ChatWorker extends SwingWorker<Void, String> {
        @Override
        protected Void doInBackground() {
            try {
                while (true) {
                    // Чтение строки от сервера и публикация для отображения в главном потоке
                    String line = dataInputStream.readUTF().trim();
                    publish(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // Скрытие поля ввода и перерисовка окна при завершении
                inTextField.setVisible(false);
                validate();
            }
            return null;
        }

        //Метод, обрабатывающий и отображающий полученные строки в главном потоке.
        @Override
        protected void process(java.util.List<String> chunks) {
            for (String line : chunks) {
                appendStyledMessage(line);
            }
        }
    }

    //Метод для добавления стилизованного сообщения в область вывода сообщений.
    private void appendStyledMessage(String message) {
        StyledDocument doc = outTextPane.getStyledDocument();

        // Создание стилей для разных компонентов сообщения
        Style dateStyle = doc.addStyle("DateStyle", null);
        Style timeStyle = doc.addStyle("TimeStyle", null);
        Style usernameStyle = doc.addStyle("UsernameStyle", null);
        Style textStyle = doc.addStyle("TextStyle", null);

        // Разделение сообщения на части: дату, время, имя пользователя и текст
        String[] parts = message.split(" ", 3);
        if (parts.length == 3) {
            String date = parts[0];
            String time = parts[1];
            String username = parts[2].substring(0, parts[2].indexOf(":"));
            String text = parts[2].substring(parts[2].indexOf(":") + 2);

            // Установка цвета для каждого компонента сообщения
            StyleConstants.setForeground(dateStyle, Color.BLUE);
            StyleConstants.setForeground(timeStyle, Color.GREEN);
            StyleConstants.setForeground(usernameStyle, Color.RED);
            StyleConstants.setForeground(textStyle, Color.BLACK);

            try {
                // Вставка каждой компоненты сообщения в область вывода
                doc.insertString(doc.getLength(), date + " ", dateStyle);
                doc.insertString(doc.getLength(), time + " ", timeStyle);
                doc.insertString(doc.getLength(), username, usernameStyle);
                doc.insertString(doc.getLength(), ": " + text + "\n", textStyle);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    //Метод для получения текущей даты и времени в формате "yyyy-MM-dd HH:mm:ss".
    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        return sdf.format(now);
    }

    //Точка входа в приложение.
    public static void main(String[] args) {
        String site = "localhost";
        String port = "8082";

        try {
            // Получение имени пользователя с помощью диалогового окна
            String username = JOptionPane.showInputDialog(null, "Введите имя пользователя:");

            // Создание сокета и потоков ввода/вывода данных
            Socket socket = new Socket(site, Integer.parseInt(port));
            DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            // Отправка имени пользователя на сервер
            dataOutputStream.writeUTF(username);
            dataOutputStream.flush();

            // Создание объекта клиента
            new ChatClient(socket, dataInputStream, dataOutputStream, username);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


